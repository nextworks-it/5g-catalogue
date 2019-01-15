/*
 * Copyright 2018 Nextworks s.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementInterface;
import it.nextworks.nfvmano.catalogue.messages.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdDeletionNotification;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdOnboardingNotification;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4.elements.IdObject;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4.elements.OsmR4InfoObject;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.ArchiveBuilder;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.NsdBuilder;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.OsmNsdPackage;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.osmr4Client.OSMr4Client;
import it.nextworks.nfvmano.libs.osmr4Client.utilities.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;



public class OpenSourceMANOR4Plugin extends MANOPlugin {

    private static final Logger log = LoggerFactory.getLogger(OpenSourceMANOR4Plugin.class);

    private static final File TMP_DIR = new File("/tmp");

    private static final File DEF_IMG = new File(
            OpenSourceMANOR4Plugin.class.getClassLoader().getResource("nxw_logo.png").getFile()
    );

    private final OSMMano osm;

    private OSMr4Client osmr4Client;

    private Map<String, String> catalogIdToOsmId;

    public OpenSourceMANOR4Plugin(MANOType manoType, MANO mano, String kafkaBootstrapServers,
                                  NsdManagementInterface service, String localTopic, String remoteTopic,
                                  KafkaTemplate<String, String> kafkaTemplate) {
        super(manoType, mano, kafkaBootstrapServers, service, localTopic, remoteTopic, kafkaTemplate);
        if (MANOType.OSMR4 != manoType) {
            throw new IllegalArgumentException("OSM R4 plugin requires an OSM R4 type MANO");
        }
        osm = (OSMMano) mano;
        catalogIdToOsmId = new HashMap<>();
    }

    @Override
    public void init() {
        super.init();
        initOsmConnection();
    }

    void initOsmConnection() {
        osmr4Client = new OSMr4Client(osm.getIpAddress(), osm.getUsername(), osm.getPassword(), osm.getProject());
        log.info("OSM R4 instance addr {}, user {}, project {} connected.", osm.getIpAddress(), osm.getUsername(),
                osm.getProject());
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
        log.info("Received NSD onboarding notificationfor NSD {} info id {}.", notification.getNsdId(),
                notification.getNsdInfoId());
        log.debug("Body: {}", notification);
        try {
            DescriptorTemplate descriptorTemplate = retrieveTemplate(notification.getNsdInfoId());
            NsdBuilder nsdBuilder = new NsdBuilder(DEF_IMG);
            Map<String, VNFNode> vnfNodes = descriptorTemplate.getTopologyTemplate().getVNFNodes();
            Map<String, DescriptorTemplate> vnfds = new HashMap<>();
            for (Map.Entry<String, VNFNode> vnfNodeEntry : vnfNodes.entrySet()) {
                String vnfdId = vnfNodeEntry.getValue().getProperties().getDescriptorId();
                String version = vnfNodeEntry.getValue().getProperties().getDescriptorVersion();
                String fileName = vnfNodeEntry.getValue().getProperties().getProductName().concat(".zip");
                try {
                    log.debug("Searching VNFD with vnfdId {} and version {} in pkg {}", vnfdId, version, fileName);
                    File vnfd_file = FileSystemStorageService.loadVnfdAsFile(vnfdId, version, fileName);
                    DescriptorTemplate vnfd = DescriptorsParser.fileToDescriptorTemplate(vnfd_file);
                    vnfds.putIfAbsent(vnfd.getMetadata().getDescriptorId(), vnfd);
                } catch (NotExistingEntityException e) {
                    log.error("VNFD with vnfdId " + vnfdId + " and version " + version + " does not exist in storage: " + e.getMessage());
                } catch (IOException e) {
                    log.error("Unable to read VNFD with vnfdId " + vnfdId + " and version " + version + ": " + e.getMessage());
                }
            }
            nsdBuilder.parseDescriptorTemplate(descriptorTemplate, vnfds);
            OsmNsdPackage packageData = nsdBuilder.getPackage();
            ArchiveBuilder archiver = new ArchiveBuilder(TMP_DIR, DEF_IMG);
            File archive = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue");
            onBoardNsPackage(archive, notification.getNsdInfoId(), notification.getOperationId().toString());
            log.info("Successfully uploaded nsd {} with info ID {}.", notification.getNsdId(),
                    notification.getNsdInfoId());
            sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                    notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                    osm.getManoId()));
        } catch (Exception e) {
            log.error("Could not onboard NSD: {}", e.getMessage());
            log.debug("Error details: ", e);
            sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                    notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                    osm.getManoId()));
        }
    }

    @Override
    public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) {
        log.info("Received NSD change notification.");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) {
        log.info("Received NSD deletion notification for NSD {} info id {}.", notification.getNsdId(),
                notification.getNsdInfoId());
        log.debug("Body: {}", notification);
        try {
            deleteNsd(notification.getNsdId(), notification.getOperationId().toString());
            log.info("Successfully deleted nsd {} with info ID {}.", notification.getNsdId(),
                    notification.getNsdInfoId());
            sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                    notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                    osm.getManoId()));
        } catch (Exception e) {
            log.error("Could not delete NSD: {}", e.getMessage());
            log.debug("Error details: ", e);
            sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                    notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                    osm.getManoId()));
        }
    }

    @Override
    public void acceptPnfdOnBoardingNotification(PnfdOnboardingNotification notification) {
        log.info("Received PNFD onboarding notification.");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotification notification) {
        log.info("Received PNFD deletion notification.");
        log.debug("Body: {}", notification);
    }

    private DescriptorTemplate retrieveTemplate(String nsdInfoId)
            throws FailedOperationException, MalformattedElementException, NotExistingEntityException,
            MethodNotImplementedException, NotPermittedOperationException, IOException {
        Object nsd = service.getNsd(nsdInfoId, true);
        if (!(Resource.class.isAssignableFrom(nsd.getClass()))) {
            throw new MethodNotImplementedException(
                    String.format("NSD storage type %s unsupported.", nsd.getClass().getSimpleName()));
        }
        Resource resource = (Resource) nsd;
        return DescriptorsParser.fileToDescriptorTemplate(resource.getFile());
    }

    private File loadFile(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(filename);
        if (resource == null) {
            throw new IllegalArgumentException(String.format("Invalid resource %s", filename));
        }
        return new File(resource.getFile());
    }

    private String convertCatalogIdToOsmId(String catalogId) throws FailedOperationException{
        String osmId = catalogIdToOsmId.get(catalogId);
        if(osmId == null)
            throw new FailedOperationException("Id not present in OSM");
        return osmId;
    }

    void onBoardNsPackage(File file, String nsdInfoId, String opId) throws FailedOperationException {
        log.info("Onboarding ns package, opId: {}.", opId);
        // Create the NS descriptor resource
        HttpResponse httpResponse = osmr4Client.createNsd();
        IdObject content = parseResponse(httpResponse, opId, IdObject.class).get(0);
        String osmNsdInfoId = content.getId();
        // Upload NS descriptor content
        httpResponse = osmr4Client.uploadNsdContent(osmNsdInfoId, file);
        parseResponse(httpResponse, opId, null );
        catalogIdToOsmId.put(nsdInfoId, osmNsdInfoId);
        log.info("Package onboarding successful. OpId: {}.", opId);
    }

    void deleteNsd(String nsdInfoId, String opId) throws FailedOperationException {
        log.info("Deleting nsd {}, opId: {}.", nsdInfoId, opId);
        String osmNsdInfoId = convertCatalogIdToOsmId(nsdInfoId);
        HttpResponse httpResponse = osmr4Client.deleteNsd(osmNsdInfoId);
        parseResponse(httpResponse, opId, null);
        catalogIdToOsmId.remove(nsdInfoId);
        log.info("Nsd deleting successful. OpId: {}.", opId);
    }

    List<String> getNsdIdList() throws FailedOperationException {
        log.info("Getting nsd id list.");
        HttpResponse httpResponse = osmr4Client.getNsdInfoList();
        List<OsmR4InfoObject> objList = parseResponse(httpResponse, null, OsmR4InfoObject.class);
        return objList.stream().map(OsmR4InfoObject::getDescriptorId).collect(Collectors.toList());
    }

    void onBoardVnfPackage(File file, String vnfdInfoId, String opId) throws FailedOperationException {
        log.info("Onboarding vnf package, opId: {}.", opId);
        // Create the VNF descriptor resource
        HttpResponse httpResponse = osmr4Client.createVnfPackage();
        IdObject content = parseResponse(httpResponse, opId, IdObject.class).get(0);
        String osmVnfdInfoId = content.getId();
        // Upload VNF descriptor content
        httpResponse = osmr4Client.uploadVnfPackageContent(osmVnfdInfoId, file);
        parseResponse(httpResponse, opId, null );
        catalogIdToOsmId.put(vnfdInfoId, osmVnfdInfoId);
        log.info("Package onboarding successful. OpId: {}.", opId);
    }

    void deleteVnfd(String vnfdInfoId, String opId) throws FailedOperationException {
        log.info("Deleting vnfd {}, opId: {}.", vnfdInfoId, opId);
        String osmVnfdInfoId = convertCatalogIdToOsmId(vnfdInfoId);
        HttpResponse httpResponse = osmr4Client.deleteVnfPackage(osmVnfdInfoId);
        parseResponse(httpResponse, opId, null);
        catalogIdToOsmId.remove(vnfdInfoId);
        log.info("Vnfd deleting successful. OpId: {}.", opId);
    }

    List<String> getVnfdIdList() throws FailedOperationException{
        log.info("Getting vnfd id list.");
        HttpResponse httpResponse = osmr4Client.getVnfPackageList();
        List<OsmR4InfoObject> objList = parseResponse(httpResponse, null, OsmR4InfoObject.class);
        return objList.stream().map(OsmR4InfoObject::getDescriptorId).collect(Collectors.toList());
    }

    private static <T> List<T> parseResponse(HttpResponse httpResponse, String opId, Class<T> clazz) throws FailedOperationException {
        List<T> objList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        if ((httpResponse.getCode() >= 300) || (httpResponse.getCode() < 200)) {
            log.error("Unexpected response code {}: {}. OpId: {}.", httpResponse.getCode(), httpResponse.getMessage(), opId);
            log.debug("Response content: {}.", httpResponse.getContent());
            throw new FailedOperationException(String.format("Unexpected code from MANO: %s", httpResponse.getCode()));
        }
        // else, code is 2XX
        if(clazz == null)
            return null;
        if(httpResponse.getFilePath() != null)
            objList.add(clazz.cast(httpResponse.getFilePath().toFile()));
        else
            try {
                Class<T[]> arrayClass = (Class<T[]>) Class.forName("[L" + clazz.getName() + ";");
                objList = Arrays.asList(mapper.readValue(httpResponse.getContent(), arrayClass));
            }catch (Exception e) {
                log.error("Could not obtain objects form response: {}.", e.getMessage());
                e.printStackTrace();
                throw new FailedOperationException("Could not obtain objects from MANO response");
            }
        return objList;
    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) throws MethodNotImplementedException {

    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) throws MethodNotImplementedException {

    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) throws MethodNotImplementedException {

    }

    // TODO
    // check op status
    // GET
    // https://10.0.8.26:8443/composer/api/package-import/jobs/877d9079-360a-46a8-b068-1f2b3a32c1a1?api_server=https://localhost
    // no body
}
