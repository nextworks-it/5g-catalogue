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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementInterface;
import it.nextworks.nfvmano.catalogue.engine.VnfPackageManagementInterface;
import it.nextworks.nfvmano.catalogue.messages.*;
import it.nextworks.nfvmano.catalogue.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.PluginOperationalState;
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
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4.elements.OsmR4InfoObject;
import it.nextworks.nfvmano.catalogue.repos.MANORepository;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.elements.CSARInfo;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.osmr4Client.OSMr4Client;
import it.nextworks.nfvmano.libs.osmr4Client.utilities.OSMHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OpenSourceMANOR4Plugin extends MANOPlugin {

    private static final Logger log = LoggerFactory.getLogger(OpenSourceMANOR4Plugin.class);
    private static File osmDir;
    private final File logo;
    private final OSMMano osm;
    private OSMr4Client osmr4Client;
    private Path osmDirPath;
    private MANORepository MANORepository;

    public OpenSourceMANOR4Plugin(MANOType manoType, MANO mano, String kafkaBootstrapServers,
                                  NsdManagementInterface nsdService, VnfPackageManagementInterface vnfdService, DescriptorsParser descriptorsParser,
                                  MANORepository MANORepository, String localTopic, String remoteTopic,
                                  KafkaTemplate<String, String> kafkaTemplate, Path osmDirPath, Path logoPath) {
        super(manoType, mano, kafkaBootstrapServers, nsdService, vnfdService, descriptorsParser, localTopic, remoteTopic, kafkaTemplate);
        if (MANOType.OSMR4 != manoType && MANOType.OSMR5 != manoType) {
            throw new IllegalArgumentException("OSM R4 plugin requires an OSM R4 type MANO");
        }
        osm = (OSMMano) mano;
        this.MANORepository = MANORepository;
        this.osmDirPath = osmDirPath;
        this.logo = new File(logoPath.toUri());
    }

    private static <T> List<T> parseResponse(OSMHttpResponse httpResponse, String opId, Class<T> clazz) throws FailedOperationException {
        List<T> objList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        if ((httpResponse.getCode() >= 300) || (httpResponse.getCode() < 200)) {
            log.error("Unexpected response code {}: {}. OpId: {}", httpResponse.getCode(), httpResponse.getMessage(), opId);
            log.debug("Response content: {}", httpResponse.getContent());
            throw new FailedOperationException(String.format("Unexpected code from MANO: %s", httpResponse.getCode()));
        }
        // else, code is 2XX
        if (clazz == null)
            return null;
        if (httpResponse.getFilePath() != null)
            objList.add(clazz.cast(httpResponse.getFilePath().toFile()));
        else
            try {
                Class<T[]> arrayClass = (Class<T[]>) Class.forName("[L" + clazz.getName() + ";");
                objList = Arrays.asList(mapper.readValue(httpResponse.getContent(), arrayClass));
            } catch (Exception e) {
                log.error("Could not obtain objects form response: {}", e.getMessage());
                e.printStackTrace();
                throw new FailedOperationException("Could not obtain objects from MANO response");
            }
        return objList;
    }

    @Override
    public void init() {
        super.init();
        try {
            Files.createDirectories(osmDirPath);
            osmDir = osmDirPath.toFile();
        } catch (IOException e) {
            log.error("Could not initialize tmp directory: " + e.getMessage());
        }
        initOsmConnection();
    }

    void initOsmConnection() {
        osmr4Client = new OSMr4Client(osm.getIpAddress(), osm.getUsername(), osm.getPassword(), osm.getProject());
        log.info("OSM R4 instance addr {}, user {}, project {} connected", osm.getIpAddress(), osm.getUsername(),
                osm.getProject());
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
        log.info("Received NSD onboarding notificationfor Nsd {} info ID {}", notification.getNsdId(), notification.getNsdInfoId());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    DescriptorTemplate descriptorTemplate = retrieveNsdTemplate(notification.getNsdInfoId());
                    NsdBuilder nsdBuilder = new NsdBuilder(logo);

                    List<DescriptorTemplate> includedVnfds = new ArrayList<>();

                    for (String vnfPkgInfoId : notification.getIncludedVnfds()) {
                        DescriptorTemplate vnfd = descriptorsParser.fileToDescriptorTemplate(
                                ((Resource) vnfdService.getVnfd(vnfPkgInfoId, true, null)).getFile());
                        includedVnfds.add(vnfd);
                    }

                    nsdBuilder.parseDescriptorTemplate(descriptorTemplate, includedVnfds, MANOType.OSMR4);
                    OsmNsdPackage packageData = nsdBuilder.getPackage();

                    ObjectMapper mapper = new ObjectMapper();
                    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

                    log.debug("Translated NSD: " + mapper.writeValueAsString(packageData));

                /*
                //TODO do we need to send the monitoring stuff to OSM?
                CSARInfo csarInfo = notification.getCsarInfo();
                if(csarInfo != null){
                    File mf = FileSystemStorageService.loadNsPkgMfAsResource(descriptorTemplate.getMetadata().getDescriptorId(),
                            descriptorTemplate.getMetadata().getVersion(),
                            csarInfo.getMfFilename()).getFile();

                    //continue...
                }
                */

                    ArchiveBuilder archiver = new ArchiveBuilder(osmDir, logo);
                    File archive = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue");

                    onBoardNsPackage(archive, notification.getNsdInfoId(), notification.getOperationId().toString());

                    log.info("Successfully uploaded Nsd {} with info ID {}", notification.getNsdId(),
                            notification.getNsdInfoId());
                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId()));
                } catch (Exception e) {
                    log.error("Could not onboard Nsd: {}", e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId()));
                }
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("NSD onboarding skipped");
                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId()));
                }
            }
        }
    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) {
        log.info("Received Nsd change notification");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) {
        log.info("Received Nsd deletion notification for Nsd {} info ID {}", notification.getNsdId(),
                notification.getNsdInfoId());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    deleteNsd(notification.getNsdInfoId(), notification.getOperationId().toString());
                    log.info("Successfully deleted Nsd {} with info ID {}", notification.getNsdId(),
                            notification.getNsdInfoId());
                    sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId()));
                } catch (Exception e) {
                    log.error("Could not delete Nsd: {}", e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId()));
                }
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("NSD deletion skipped");
                    sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId()));
                }
            }
        }
    }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) {
        log.info("Received Vnfd onboarding notificationfor Vnfd {} info ID {}", notification.getVnfdId(),
                notification.getVnfPkgInfoId());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    DescriptorTemplate descriptorTemplate = retrieveVnfdTemplate(notification.getVnfPkgInfoId());
                    VnfdBuilder vnfdBuilder = new VnfdBuilder(logo);
                    vnfdBuilder.parseDescriptorTemplate(descriptorTemplate, MANOType.OSMR4);
                    OsmVNFPackage packageData = vnfdBuilder.getPackage();

                    ObjectMapper mapper = new ObjectMapper();
                    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

                    log.debug("Translated VNFD: " + mapper.writeValueAsString(packageData));

                    CSARInfo csarInfo = notification.getCsarInfo();

                    File mf = FileSystemStorageService.loadFileAsResource(descriptorTemplate.getMetadata().getDescriptorId(),
                            descriptorTemplate.getMetadata().getVersion(),
                            csarInfo.getMfFilename(), true).getFile();

                    String cloudInitFilename = null;

                    BufferedReader br = new BufferedReader(new FileReader(mf));
                    try {
                        String line;
                        String regexRoot = "cloud_init:";
                        String regex = "^Source: (Files\\/Scripts\\/[\\s\\S]*)$";
                        while ((line = br.readLine()) != null) {
                            line = line.trim();
                            log.debug("MF line: <" + line + ">");
                            if (line.matches(regexRoot)) {
                                line = br.readLine();

                                if (line != null) {
                                    line = line.trim();
                                    log.debug("Next MF line: <" + line + ">");
                                    if (line.matches(regex)) {
                                        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                                        Matcher matcher = pattern.matcher(line.trim());
                                        if (matcher.find()) {
                                            cloudInitFilename = matcher.group(1);
                                            log.debug("Found cloud-init file with name: " + cloudInitFilename);
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        br.close();
                    }

                    ArchiveBuilder archiver = new ArchiveBuilder(osmDir, logo);
                    File cloudInit = null;
                    if (cloudInitFilename != null) {
                        cloudInit = FileSystemStorageService.loadFileAsResource(
                                descriptorTemplate.getMetadata().getDescriptorId(),
                                descriptorTemplate.getMetadata().getVersion(), cloudInitFilename, true).getFile();
                    } else {
                        log.debug("No cloud-init file found for VNF Pkg {}", notification.getVnfPkgInfoId());
                    }
                    File archive = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue", cloudInit);
                    onBoardVnfPackage(archive, notification.getVnfPkgInfoId(), notification.getOperationId().toString());
                    log.info("Successfully uploaded Vnfd {} with info ID {}", notification.getVnfdId(),
                            notification.getVnfPkgInfoId());
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId()));
                } catch (Exception e) {
                    log.error("Could not onboard Vnfd: {}", e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId()));
                }
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("VNF Pkg onboarding skipped");
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId()));
                }
            }
        }
    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) {
        log.info("Received VNF Pkg change notification");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) {
        log.info("Received Vnfd deletion notification for Vnfd {} info ID {}", notification.getVnfPkgInfoId(),
                notification.getVnfPkgInfoId());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    deleteVnfd(notification.getVnfPkgInfoId(), notification.getOperationId().toString());
                    log.info("Successfully deleted Vnfd {} with info ID {}", notification.getVnfdId(),
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
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("VNF Pkg deletion skipped");
                    sendNotification(new VnfPkgDeletionNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId()));
                }
            }
        }
    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptPnfdOnBoardingNotification(PnfdOnBoardingNotificationMessage notification) {
        log.info("Received PNFD onboarding notification");
        log.debug("Body: {}", notification);
    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotificationMessage notification) {
        log.info("Received PNFD deletion notification");
        log.debug("Body: {}", notification);
    }

    private DescriptorTemplate retrieveNsdTemplate(String nsdInfoId)
            throws FailedOperationException, MalformattedElementException, NotExistingEntityException,
            MethodNotImplementedException, NotPermittedOperationException, IOException {
        Object nsd = null;
        try {
            nsd = nsdService.getNsdFile(nsdInfoId, true, null);
        } catch (NotAuthorizedOperationException e) {
            log.error("Unable to get NSD file from Catalogue storage");
        }
        if (!(Resource.class.isAssignableFrom(nsd.getClass()))) {
            throw new MethodNotImplementedException(
                    String.format("NSD storage type %s unsupported", nsd.getClass().getSimpleName()));
        }
        Resource resource = (Resource) nsd;
        return descriptorsParser.fileToDescriptorTemplate(resource.getFile());
    }

    private DescriptorTemplate retrieveVnfdTemplate(String vnfdInfoId)
            throws FailedOperationException, MalformattedElementException, NotExistingEntityException,
            MethodNotImplementedException, NotPermittedOperationException, IOException {
        Object vnfd = null;
        try {
            vnfd = vnfdService.getVnfd(vnfdInfoId, true, null);
        } catch (NotAuthorizedOperationException e) {
            log.error("Unable to get VNFD file from Catalogue storage");
        }
        if (!(Resource.class.isAssignableFrom(vnfd.getClass()))) {
            throw new MethodNotImplementedException(
                    String.format("VNFD storage type %s unsupported", vnfd.getClass().getSimpleName()));
        }
        Resource resource = (Resource) vnfd;
        return descriptorsParser.fileToDescriptorTemplate(resource.getFile());
    }

    private File loadFile(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(filename);
        if (resource == null) {
            throw new IllegalArgumentException(String.format("Invalid resource %s", filename));
        }
        return new File(resource.getFile());
    }

    private String translateCatalogIdToOsmId(String catalogId) throws FailedOperationException {
        String osmId = osm.getOsmIdTranslation().get(catalogId);
        if (osmId == null)
            throw new FailedOperationException("Could not find the corresponding ID in OSM");
        return osmId;
    }

    void onBoardNsPackage(File file, String nsdInfoId, String opId) throws FailedOperationException {
        log.info("Onboarding ns package, opId: {}", opId);
        // Create the NS descriptor resource
        OSMHttpResponse httpResponse = osmr4Client.createNsd();
        IdObject content = parseResponse(httpResponse, opId, IdObject.class).get(0);
        String osmNsdInfoId = content.getId();
        // Upload NS descriptor content
        httpResponse = osmr4Client.uploadNsdContent(osmNsdInfoId, file);
        parseResponse(httpResponse, opId, null);
        osm.getOsmIdTranslation().put(nsdInfoId, osmNsdInfoId);
        MANORepository.saveAndFlush(osm);
        log.info("Package onboarding successful. OpId: {}", opId);
    }

    void deleteNsd(String nsdInfoId, String opId) throws FailedOperationException {
        log.info("Deleting nsd {}, opId: {}", nsdInfoId, opId);
        String osmNsdInfoId = translateCatalogIdToOsmId(nsdInfoId);
        OSMHttpResponse httpResponse = osmr4Client.deleteNsd(osmNsdInfoId);
        parseResponse(httpResponse, opId, null);
        osm.getOsmIdTranslation().remove(nsdInfoId);
        MANORepository.saveAndFlush(osm);
        log.info("Nsd deleting successful. OpId: {}", opId);
    }

    List<String> getNsdIdList() throws FailedOperationException {
        log.info("Getting nsd id list");
        OSMHttpResponse httpResponse = osmr4Client.getNsdInfoList();
        List<OsmR4InfoObject> objList = parseResponse(httpResponse, null, OsmR4InfoObject.class);
        return objList.stream().map(OsmR4InfoObject::getDescriptorId).collect(Collectors.toList());
    }

    void onBoardVnfPackage(File file, String vnfdInfoId, String opId) throws FailedOperationException {
        log.info("Onboarding vnf package, opId: {}", opId);
        // Create the VNF descriptor resource
        OSMHttpResponse httpResponse = osmr4Client.createVnfPackage();
        IdObject content = parseResponse(httpResponse, opId, IdObject.class).get(0);
        String osmVnfdInfoId = content.getId();
        // Upload VNF descriptor content
        httpResponse = osmr4Client.uploadVnfPackageContent(osmVnfdInfoId, file);
        parseResponse(httpResponse, opId, null);
        osm.getOsmIdTranslation().put(vnfdInfoId, osmVnfdInfoId);
        MANORepository.saveAndFlush(osm);
        log.info("Package onboarding successful. OpId: {}", opId);
    }

    void deleteVnfd(String vnfdInfoId, String opId) throws FailedOperationException {
        log.info("Deleting vnfd {}, opId: {}", vnfdInfoId, opId);
        String osmVnfdInfoId = translateCatalogIdToOsmId(vnfdInfoId);
        OSMHttpResponse httpResponse = osmr4Client.deleteVnfPackage(osmVnfdInfoId);
        parseResponse(httpResponse, opId, null);
        osm.getOsmIdTranslation().remove(vnfdInfoId);
        MANORepository.saveAndFlush(osm);
        log.info("Vnfd deleting successful. OpId: {}", opId);
    }

    List<String> getVnfdIdList() throws FailedOperationException {
        log.info("Getting vnfd id list");
        OSMHttpResponse httpResponse = osmr4Client.getVnfPackageList();
        List<OsmR4InfoObject> objList = parseResponse(httpResponse, null, OsmR4InfoObject.class);
        return objList.stream().map(OsmR4InfoObject::getDescriptorId).collect(Collectors.toList());
    }
}
