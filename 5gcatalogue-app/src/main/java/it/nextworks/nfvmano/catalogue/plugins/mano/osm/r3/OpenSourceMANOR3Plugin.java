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
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.r3;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.girtel.osmclient.OSMClient;
import com.girtel.osmclient.OSMComponent;
import com.girtel.osmclient.utils.HTTPResponse;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementInterface;
import it.nextworks.nfvmano.catalogue.engine.VnfPackageManagementInterface;
import it.nextworks.nfvmano.catalogue.messages.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdDeletionNotification;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdOnboardingNotification;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.ArchiveBuilder;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.NsdBuilder;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.VnfdBuilder;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.nsDescriptor.OsmNsdPackage;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor.OsmVNFPackage;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4.elements.IdObject;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.osmr4Client.utilities.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenSourceMANOR3Plugin extends MANOPlugin {

	/* TEST DATA
	private static final String NSD_FILE_NAME = "cirros_2vnf_ns.tar.gz"; // Should be a filename in src/main/resources
	private static final String NSD_ID = "cirros_2vnf_ns";

	private static final String[] VNF_FILE_NAMES = { "cirros_vnf.tar.gz" }; // Should be a filename in
																			// src/main/resources
	private static final String[] VNFD_IDS = { "cirros_vnf" };
	*/

    private static final Logger log = LoggerFactory.getLogger(OpenSourceMANOR3Plugin.class);

    private static final File DEF_IMG = new File(
            OpenSourceMANOR3Plugin.class.getClassLoader().getResource("nxw_logo.png").getFile()
    );
    private final OSMMano osm;
    private OSMClient osmClient;

    private static Path tmpDir;
    private static File TMP_DIR;

    public OpenSourceMANOR3Plugin(MANOType manoType, MANO mano, String kafkaBootstrapServers,
                                  NsdManagementInterface nsdService, VnfPackageManagementInterface vnfdService, String localTopic, String remoteTopic,
                                  KafkaTemplate<String, String> kafkaTemplate) {
        super(manoType, mano, kafkaBootstrapServers, nsdService, vnfdService, localTopic, remoteTopic, kafkaTemplate);
        if (MANOType.OSMR3 != manoType) {
            throw new IllegalArgumentException("OSM R3 plugin requires an OSM R3 type MANO");
        }
        osm = (OSMMano) mano;
    }

    @Override
    public void init() {
        super.init();
        tmpDir = Paths.get("/tmp/osmr3");
        try {
            Files.createDirectories(tmpDir);
            TMP_DIR = tmpDir.toFile();
        } catch (IOException e) {
            log.error("Could not initialize tmp directory: " + e.getMessage());
        }
        initOsmConnection();
    }

    void initOsmConnection() {
        osmClient = new OSMClient(osm.getIpAddress(), osm.getUsername(), osm.getPassword(), osm.getProject());
        log.info("OSM R3 instance addr {}, user {}, project {} connected.", osm.getIpAddress(), osm.getUsername(),
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

            List<DescriptorTemplate> includedVnfds = new ArrayList<>();

            for (String vnfPkgInfoId : notification.getIncludedVnfds()) {
                DescriptorTemplate vnfd = DescriptorsParser.fileToDescriptorTemplate(
                        ((Resource) vnfdService.getVnfd(vnfPkgInfoId, true)).getFile());
                includedVnfds.add(vnfd);
            }

            nsdBuilder.parseDescriptorTemplate(descriptorTemplate, includedVnfds, MANOType.OSMR3);
            OsmNsdPackage packageData = nsdBuilder.getPackage();

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

            log.debug("Translated NSD: " + mapper.writeValueAsString(packageData));

            ArchiveBuilder archiver = new ArchiveBuilder(TMP_DIR, DEF_IMG);
            File archive = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue");

            /*
             * Test code loading static package List<String> tIds = new LinkedList<>(); for
             * (String vnfFileName : VNF_FILE_NAMES) { File vnf = loadFile(vnfFileName);
             * tIds.add(onBoardPackage(vnf, UUID.randomUUID().toString())); }
             * log.debug("Required VNFD onboarded, tIds: {} onboarding NSD", tIds); File nsd
             * = loadFile(NSD_FILE_NAME);
             */

            String tId = onBoardPackage(archive, notification.getOperationId().toString());
            log.info("Successfully uploaded nsd {} with info ID {}.", notification.getNsdId(),
                    notification.getNsdInfoId());
            log.debug("tId: {}", tId);
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
            /*
             * Test code deleting static packages deleteNsd(NSD_ID, "test_delete_nsd"); for
             * (String vnfdId : VNFD_IDS) { deleteVnfd(vnfdId, "test_delete_vnf"); }
             */
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
    
    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) {
        log.info("Received Vnfd onboarding notificationfor Vnfd {} info ID {}.", notification.getVnfdId(),
                notification.getVnfPkgInfoId());
        log.debug("Body: {}", notification);

        try {
            DescriptorTemplate descriptorTemplate = retrieveVnfdTemplate(notification.getVnfPkgInfoId());
            VnfdBuilder vnfdBuilder = new VnfdBuilder(DEF_IMG);
            vnfdBuilder.parseDescriptorTemplate(descriptorTemplate, MANOType.OSMR3);
            OsmVNFPackage packageData = vnfdBuilder.getPackage();

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

            log.debug("Translated VNFD: " + mapper.writeValueAsString(packageData));

            ArchiveBuilder archiver = new ArchiveBuilder(TMP_DIR, DEF_IMG);
            File cloudInit = FileSystemStorageService.loadVnfPkgCloudInitAsResource(
                    descriptorTemplate.getMetadata().getDescriptorId(),
                    descriptorTemplate.getMetadata().getVersion(), "cloud.init.txt").getFile();
            File archive = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue", cloudInit);
            String tId = onBoardPackage(archive, notification.getOperationId().toString());

            log.info("Successfully uploaded VNFD {} with info ID {}.", notification.getVnfdId(),
                    notification.getVnfPkgInfoId());
            log.debug("tId: {}", tId);

            sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                    notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                    osm.getManoId()));
        } catch (Exception e) {
            log.error("Could not onboard VNFD: {}", e.getMessage());
            log.debug("Error details: ", e);
            sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                    notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                    osm.getManoId()));
        }
    }

    // TODO: to be implemented according to descriptor format in OSMR3
    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) {
        log.info("Received VNF Pkg change notification.");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) {
        log.info("Received Vnfd deletion notification for Vnfd {} info ID {}.", notification.getVnfPkgInfoId(),
                notification.getVnfPkgInfoId());
        log.debug("Body: {}", notification);
        try {
            deleteVnfd(notification.getVnfdId(), notification.getOperationId().toString());
            log.info("Successfully deleted Vnfd {} with info ID {}.", notification.getVnfdId(),
                    notification.getVnfPkgInfoId());
            sendNotification(new VnfPkgDeletionNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                    notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                    osm.getManoId()));
        } catch (Exception e) {
            log.error("Could not delete Vnfd: {}", e.getMessage());
            log.debug("Error details: ", e);
            sendNotification(new VnfPkgDeletionNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                    notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                    osm.getManoId()));
        }
    }

    String onBoardPackage(File file, String opId) throws FailedOperationException {
        log.info("Onboarding package, opId: {}.", opId);
        HTTPResponse httpResponse = osmClient.uploadPackage(file);
        return parseResponseReturning(httpResponse, opId, true);
    }

    void deleteNsd(String nsdId, String opId) throws FailedOperationException {
        log.info("Deleting nsd {}, opId: {}.", nsdId, opId);
        HTTPResponse httpResponse = osmClient.deleteNSD(nsdId);
        parseResponse(httpResponse, opId);
    }

    void deleteVnfd(String vnfdId, String opId) throws FailedOperationException {
        log.info("Deleting vnfd {}, opId: {}.", vnfdId, opId);
        HTTPResponse httpResponse = osmClient.deleteVNFD(vnfdId);
        parseResponse(httpResponse, opId);
    }

    List<String> getNsdIdList() {
        return osmClient.getNSDList().stream().map(OSMComponent::toString).collect(Collectors.toList());
    }

    List<String> getVnfdIdList() {
        return osmClient.getVNFDList().stream().map(OSMComponent::toString).collect(Collectors.toList());
    }

    private void parseResponse(HTTPResponse httpResponse, String opId) throws FailedOperationException {
        parseResponseReturning(httpResponse, opId, false);
        // Ignoring the return value
    }

    private String parseResponseReturning(HTTPResponse httpResponse, String opId, boolean transaction)
            throws FailedOperationException {
        if ((httpResponse.getCode() >= 300) || (httpResponse.getCode() < 200)) {
            log.error("Unexpected response code {}: {}. OpId: {}.", httpResponse.getCode(), httpResponse.getMessage(),
                    opId);
            log.debug("Response content: {}.", httpResponse.getContent());
            throw new FailedOperationException(String.format("Unexpected code from MANO: %s", httpResponse.getCode()));
        }
        // else, code is 2XX
        log.info("Response code {}: package onboarding successful. OpId: {}.", httpResponse.getCode() + " " + httpResponse.getMessage(), opId);
        if (transaction) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                TransactionResponse transactionResponse = mapper.readValue(httpResponse.getContent(),
                        TransactionResponse.class);
                return transactionResponse.transactionId;
            } catch (IOException e) {
                log.error("Could not read transaction ID from response: {}.", e.getMessage());
                log.debug("Exception details: ", e);
                throw new FailedOperationException("Could not read transaction ID from MANO response");
            }
        }
        // else, return the content, which will probably go unused.
        return httpResponse.getContent();
    }

    private DescriptorTemplate retrieveTemplate(String nsdInfoId)
            throws FailedOperationException, MalformattedElementException, NotExistingEntityException,
            MethodNotImplementedException, NotPermittedOperationException, IOException {
        Object nsd = nsdService.getNsd(nsdInfoId, true);
        if (!(Resource.class.isAssignableFrom(nsd.getClass()))) {
            throw new MethodNotImplementedException(
                    String.format("NSD storage type %s unsupported.", nsd.getClass().getSimpleName()));
        }
        Resource resource = (Resource) nsd;
        return DescriptorsParser.fileToDescriptorTemplate(resource.getFile());
    }

    private DescriptorTemplate retrieveVnfdTemplate(String vnfdInfoId)
            throws FailedOperationException, MalformattedElementException, NotExistingEntityException,
            MethodNotImplementedException, NotPermittedOperationException, IOException {
        Object vnfd = vnfdService.getVnfd(vnfdInfoId, true);
        if (!(Resource.class.isAssignableFrom(vnfd.getClass()))) {
            throw new MethodNotImplementedException(
                    String.format("VNFD storage type %s unsupported.", vnfd.getClass().getSimpleName()));
        }
        Resource resource = (Resource) vnfd;
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

    private static class TransactionResponse {
        @JsonProperty("transaction_id")
        String transactionId;
    }

    // TODO
    // check op status
    // GET
    // https://10.0.8.26:8443/composer/api/package-import/jobs/877d9079-360a-46a8-b068-1f2b3a32c1a1?api_server=https://localhost
    // no body
}
