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
package it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.r4plus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.*;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.PluginOperationalState;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.*;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.repos.MANORepository;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.common.*;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.repos.OsmInfoObjectRepository;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.osmr4PlusClient.OSMr4PlusClient;
import it.nextworks.nfvmano.libs.osmr4PlusClient.utilities.OSMHttpResponse;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.nsDescriptor.OsmNSPackage;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.osmManagement.IdObject;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.osmManagement.OsmInfoObject;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.vnfDescriptor.OsmVNFPackage;
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OpenSourceMANOR4PlusPlugin extends MANOPlugin {

    private static final Logger log = LoggerFactory.getLogger(OpenSourceMANOR4PlusPlugin.class);
    private Path osmDirPath;
    private File osmDir;
    private final File logo;
    private final OSMMano osm;
    private OSMr4PlusClient osmClient;
    private MANORepository MANORepository;
    private OsmInfoObjectRepository osmInfoObjectRepository;
    private long syncPeriod;

    public OpenSourceMANOR4PlusPlugin(MANOType manoType, MANO mano, String kafkaBootstrapServers,
                                      MANORepository MANORepository, OsmInfoObjectRepository osmInfoObjectRepository, String localTopic, String remoteTopic,
                                      KafkaTemplate<String, String> kafkaTemplate, Path osmDirPath, Path logoPath, boolean manoSync, long syncPeriod) {
        super(manoType, mano, kafkaBootstrapServers, localTopic, remoteTopic, kafkaTemplate, manoSync);
        if (MANOType.OSMR4 != manoType && MANOType.OSMR5 != manoType && MANOType.OSMR6 != manoType) {
            throw new IllegalArgumentException("OSM R4+ plugin requires an OSM R4+ type MANO");
        }
        osm = (OSMMano) mano;
        this.MANORepository = MANORepository;
        this.osmInfoObjectRepository = osmInfoObjectRepository;
        this.osmDirPath = osmDirPath;
        this.logo = new File(logoPath.toUri());
        this.syncPeriod = syncPeriod;
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
            osmDirPath = Paths.get(osmDirPath.toString(), "/" + osm.getManoId());
            Files.createDirectories(osmDirPath);
            osmDir = osmDirPath.toFile();
        } catch (IOException e) {
            log.error("Could not initialize tmp directory: " + e.getMessage());
        }
        initOsmConnection();
    }

    void initOsmConnection() {
        osmClient = new OSMr4PlusClient(osm.getIpAddress(), osm.getUsername(), osm.getPassword(), osm.getProject());
        log.info("{} - OSM R4+ instance addr {}, user {}, project {} connected", osm.getManoId(), osm.getIpAddress(), osm.getUsername(),
                osm.getProject());
    }

    @Override
    public Map<String, String> getAllVnf(String project){

        //Scheduling OSM synchronization
        if(isManoSync()) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable syncTask = () -> OSMSynchronization();
            scheduler.scheduleAtFixedRate(syncTask, syncPeriod, syncPeriod, TimeUnit.SECONDS);
        }
        return null;
    }

    @Override
    public KeyValuePair getTranslatedVnfPkgPath(String vnfdId, String vnfdVersion, String project){

        return null;
    }

    @Override
    public void notifyVnfOnboarding(String vnfInfoId, String vnfdId, String vnfdVersion, String project, OperationStatus opStatus){

    }

    private void OSMSynchronization(){
        log.info("{} - Started retrieving NSDs and VNFDs", osm.getManoId());
        Long startSync = Instant.now().getEpochSecond();

        OSMHttpResponse response;
        Optional<OsmInfoObject> osmInfoObject;
        List<OsmInfoObject> vnfPackageInfoList = null;
        try {
            //Retrieve VNFPkgInfos
            response = osmClient.getVnfPackageList();
            vnfPackageInfoList = parseResponse(response, null, OsmInfoObject.class);
        }catch(FailedOperationException e){
            log.error("{} - {}", osm.getManoId(), e.getMessage());
            return;
        }

        //Retrieve the content if is a new VNFPkgInfo or it has been updated
        for(OsmInfoObject vnfPackageInfo :  vnfPackageInfoList) {
            try {
                osmInfoObject = osmInfoObjectRepository.findById(vnfPackageInfo.getId());
                if (osmInfoObject.isPresent()) {
                    log.info("{} - OSM VNF Pkg Info with ID {} already present", osm.getManoId(), vnfPackageInfo.getId());
                    osmInfoObject.get().setEpoch(Instant.now().getEpochSecond());
                    osmInfoObjectRepository.saveAndFlush(osmInfoObject.get());
                /*if(vnfPackageInfo.getAdmin().getModified().compareTo(osmInfoObject.get().getAdmin().getModified()) > 0){
                    log.info("VNF Pkg Info with ID {} has been modified, updating it", vnfPackageInfo.getId());
                    osmInfoObjectRepository.delete(osmInfoObject.get());
                    vnfPackageInfo.setEpoch(Instant.now().getEpochSecond());
                    vnfPackageInfo.setOsmId(osm.getManoId());
                    osmInfoObjectRepository.saveAndFlush(vnfPackageInfo);
                    log.info("Retrieving VNF Package content with ID {}", vnfPackageInfo.getId());
                    osmClient.getVnfPackageContent(vnfPackageInfo.getId(), osmDirPath.toString());
                    //TODO decompress archive and do other things
                }else{
                    osmInfoObject.get().setEpoch(Instant.now().getEpochSecond());
                    osmInfoObjectRepository.saveAndFlush(osmInfoObject.get());
                }*/
                } else {
                    log.info("{} - Retrieving OSM VNF Package with ID {}", osm.getManoId(), vnfPackageInfo.getId());
                    response = osmClient.getVnfPackageContent(vnfPackageInfo.getId(), osmDirPath.toString());
                    parseResponse(response, null, null);
                    File archive = new File(osmDirPath.toString() + "/" + vnfPackageInfo.getId() + ".tar.gz");
                    Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP);
                    archiver.extract(archive, new File(osmDirPath.toString()));
                    String vnfPkgPath = createVnfPkgTosca(vnfPackageInfo);
                    log.info("{} - Uploading TOSCA VNF Package", osm.getManoId());
                    UUID operationId = UUID.randomUUID();
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(null, vnfPackageInfo.getDescriptorId(), null,
                            operationId, ScopeType.SYNC, OperationStatus.SENT, osm.getManoId(), new KeyValuePair(vnfPkgPath, PathType.LOCAL.toString())));
                    osm.getOsmIdTranslation().put(operationId.toString(), vnfPackageInfo.getId());
                    MANORepository.saveAndFlush(osm);
                    vnfPackageInfo.setEpoch(Instant.now().getEpochSecond());
                    vnfPackageInfo.setOsmId(osm.getManoId());
                    osmInfoObjectRepository.saveAndFlush(vnfPackageInfo);
                }
            } catch (FailedOperationException | IOException | MalformattedElementException | IllegalStateException | IllegalArgumentException e) {
                //TODO handle exceptions
                log.error("{} - Sync error: {}", osm.getManoId(), e.getMessage());
                log.debug(null, e);
                //TODO remove OSMPkGInfo?
            }
        }

        /*
        //Retrieve NsdInfos
        response =  osmClient.getNsdInfoList();
        List<OsmInfoObject> nsdInfoList = null;
        try {
            nsdInfoList = parseResponse(response, null, OsmInfoObject.class);
        }catch(FailedOperationException e){
            log.debug(null, e);
        }
        //Retrieve the content
        for(OsmInfoObject nsdInfo :  nsdInfoList){
            log.info("Retrieving NS Package content with ID {}", nsdInfo.getId());
            osmClient.getNsdContent(nsdInfo.getId(), osmDirPath.toString());
        }
        */

        //Delete OSM Pkg no longer present in OSM
        List<OsmInfoObject> osmInfoObjectList = osmInfoObjectRepository.findByOsmId(osm.getManoId());
        for(OsmInfoObject osmInfoObj : osmInfoObjectList){
            if(osmInfoObj.getEpoch().compareTo(startSync) < 0){
                log.info("{} - OSM Pkg with ID {} no longer present, deleting it", osm.getManoId(), osmInfoObj.getId());
                osmInfoObjectRepository.delete(osmInfoObj);
                //TODO delete tar.gz and the corresponding folder
                //TODO send a notification
            }
        }
        log.info("{} - Finished retrieving NSDs and VNFDs", osm.getManoId());
    }

    private String createVnfPkgTosca (OsmInfoObject vnfPackageInfo) throws MalformattedElementException, IllegalStateException, IOException, IllegalArgumentException{
        log.info("{} - Creating TOSCA VNF Descriptor with ID {}", osm.getManoId(), vnfPackageInfo.getDescriptorId());
        DescriptorTemplate vnfd = ToscaDescriptorsParser.generateVnfDescriptor(osmDirPath.toString() + "/" + vnfPackageInfo.getAdmin().getStorage().getDescriptor());
        VNFNode vnfNode = vnfd.getTopologyTemplate().getVNFNodes().values().iterator().next();
        File cloudInit = null;
        if(vnfNode.getInterfaces() != null){
            cloudInit = new File(osmDirPath.toString() + "/" + vnfPackageInfo.getAdmin().getStorage().getPkgDir() + "/cloud_init/" + vnfNode.getInterfaces().getVnflcm().getInstantiate());
        }
        return ToscaArchiveBuilder.createVNFCSAR(vnfd, cloudInit);
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
        log.info("{} - Received NSD onboarding notificationfor Nsd {} info ID {}", osm.getManoId(), notification.getNsdId(), notification.getNsdInfoId());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    //TODO add checks
                    String packagePath = notification.getPackagePath().getKey();
                    File descriptor = null;
                    File metadata;
                    Set<String> metadataFileNames;
                    String metadataFileName;
                    if(notification.getPackagePath().getValue().equals(PathType.LOCAL.toString())) {
                        Set<String> files = Utilities.listFiles(packagePath);
                        if(files.size() != 1) {
                            metadataFileNames = Utilities.listFiles(packagePath + "/TOSCA-Metadata/");
                            //Consider only one metadata file is present
                            metadataFileName = metadataFileNames.stream().filter(name -> name.endsWith(".meta")).findFirst().get();
                            metadata = new File(packagePath + "/TOSCA-Metadata/" + metadataFileName);
                            descriptor = new File(packagePath + "/" + Utilities.getMainServiceTemplateFromMetadata(metadata));
                        }
                        else if (files.iterator().next().endsWith(".yaml") || files.iterator().next().endsWith(".yml"))
                            descriptor = new File(packagePath + "/" + files.iterator().next());
                        else{
                            throw new MalformattedElementException("Descriptor files not found");
                        }
                    }else{
                        //TODO support also other PathType
                        throw new MethodNotImplementedException("Path Type not currently supported");
                    }


                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    DescriptorTemplate descriptorTemplate = mapper.readValue(descriptor, DescriptorTemplate.class);

                    NsdBuilder nsdBuilder = new NsdBuilder(logo);

                    List<DescriptorTemplate> includedVnfds = new ArrayList<>();
                    for(KeyValuePair vnfPathPair : notification.getIncludedVnfds().values()){
                        if(vnfPathPair.getValue().equals(PathType.LOCAL.toString())){
                            packagePath = vnfPathPair.getKey();
                            metadataFileNames = Utilities.listFiles(packagePath + "/TOSCA-Metadata/");
                            //Consider only one metada file is present
                            metadataFileName = metadataFileNames.stream().filter(name -> name.endsWith(".meta")).findFirst().get();
                            metadata = new File(packagePath + "/TOSCA-Metadata/" + metadataFileName);
                            descriptor = new File(packagePath + "/" + Utilities.getMainServiceTemplateFromMetadata(metadata));
                        }else{
                            //TODO support also other PathType
                            throw new MethodNotImplementedException("Path Type not currently supported");
                        }

                        includedVnfds.add(mapper.readValue(descriptor, DescriptorTemplate.class));
                    }

                    nsdBuilder.parseDescriptorTemplate(descriptorTemplate, includedVnfds, MANOType.OSMR4);
                    OsmNSPackage packageData = nsdBuilder.getPackage();

                    mapper = new ObjectMapper();
                    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

                    log.debug("{} - Translated NSD: {} ", osm.getManoId(), mapper.writeValueAsString(packageData));

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

                    log.info("{} - Successfully uploaded Nsd {} with info ID {}", osm.getManoId(), notification.getNsdId(),
                            notification.getNsdInfoId());
                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), null,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId(), null));
                } catch (Exception e) {
                    log.error("{} - Could not onboard Nsd: {}", osm.getManoId(), e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), null,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId(), null));
                }
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - NSD onboarding skipped", osm.getManoId());
                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), null,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId(), null));
                }
            }
        }
    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) {
        log.info("{} - Received Nsd change notification", osm.getManoId());
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) {
        log.info("{} - Received Nsd deletion notification for Nsd {} info ID {}", osm.getManoId(), notification.getNsdId(),
                notification.getNsdInfoId());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    deleteNsd(notification.getNsdInfoId(), notification.getOperationId().toString());
                    log.info("{} - Successfully deleted Nsd {} with info ID {}", osm.getManoId(), notification.getNsdId(),
                            notification.getNsdInfoId());
                    sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId()));
                } catch (Exception e) {
                    log.error("{} - Could not delete Nsd: {}", osm.getManoId(), e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId()));
                }
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - NSD deletion skipped", osm.getManoId());
                    sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId()));
                }
            }
        }
    }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) {
        log.info("{} - Received Vnfd onboarding notification for Vnfd with ID {} and  info ID {}", osm.getManoId(), notification.getVnfdId(),
                notification.getVnfPkgInfoId());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (Utilities.isTargetMano(notification.getSiteOrManoIds(), osm) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    //TODO add checks
                    String packagePath = notification.getPackagePath().getKey();
                    File descriptor;
                    if(notification.getPackagePath().getValue().equals(PathType.LOCAL.toString())) {
                        Set<String> metadataFileNames = Utilities.listFiles(packagePath + "/TOSCA-Metadata/");
                        //Consider only one metada file is present
                        String metadataFileName = metadataFileNames.stream().filter(name -> name.endsWith(".meta")).findFirst().get();
                        File metadata = new File(packagePath + "/TOSCA-Metadata/" + metadataFileName);
                        descriptor = new File(packagePath + "/" + Utilities.getMainServiceTemplateFromMetadata(metadata));
                    }else{
                        //TODO support also other PathType
                        throw new MethodNotImplementedException("Path Type not currently supported");
                    }

                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    DescriptorTemplate descriptorTemplate = mapper.readValue(descriptor, DescriptorTemplate.class);
                    //Check if already present, upload for example from another project
                    Optional<OsmInfoObject> osmInfoObjectOptional = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(descriptorTemplate.getMetadata().getDescriptorId(), descriptorTemplate.getMetadata().getVersion(), osm.getManoId());
                    String osmInfoObjectId;
                    if(!osmInfoObjectOptional.isPresent()) {
                        VnfdBuilder vnfdBuilder = new VnfdBuilder(logo);
                        vnfdBuilder.parseDescriptorTemplate(descriptorTemplate, MANOType.OSMR4);
                        OsmVNFPackage packageData = vnfdBuilder.getPackage();

                        mapper = new ObjectMapper();
                        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

                        log.debug("Translated VNFD: " + mapper.writeValueAsString(packageData));

                        Set<String> fileNames = Utilities.listFiles(packagePath);
                        //TODO take cloudinit file name from descriptor?
                        //Consider only one manifest file is present
                        String manifestPath = fileNames.stream().filter(name -> name.endsWith(".mf")).findFirst().get();
                        File mf = new File(packagePath + "/" + manifestPath);

                        String cloudInitPath = Utilities.getCloudInitFromManifest(mf);

                        ArchiveBuilder archiver = new ArchiveBuilder(osmDir, logo);
                        File cloudInit = null;
                        if (cloudInitPath != null) {
                            cloudInit = new File(packagePath + "/" + cloudInitPath);
                        } else {
                            log.debug("{} - No cloud-init file found for VNF Pkg {}", osm.getManoId(), notification.getVnfPkgInfoId());
                        }
                        File archive = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue", cloudInit);
                        osmInfoObjectId = onBoardVnfPackage(archive, notification.getVnfPkgInfoId(), notification.getOperationId().toString());
                        log.info("{} - Successfully uploaded Vnfd {} with info ID {}", osm.getManoId(), notification.getVnfdId(),
                                notification.getVnfPkgInfoId());
                    }else
                        osmInfoObjectId = osmInfoObjectOptional.get().getId();

                    osm.getOsmIdTranslation().put(notification.getVnfPkgInfoId(), osmInfoObjectId);
                    MANORepository.saveAndFlush(osm);
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), null,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId(), null));
                } catch (Exception e) {
                    log.error("{} - Could not onboard Vnfd: {}", osm.getManoId(), e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), null,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId(), null));
                }
            } else {
                if (!Utilities.isTargetMano(notification.getSiteOrManoIds(), osm) || this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - VNF Pkg onboarding skipped", osm.getManoId());
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), null,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId(), null));
                }
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            if(notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                osm.getOsmIdTranslation().put(notification.getVnfPkgInfoId(), osm.getOsmIdTranslation().remove( notification.getOperationId().toString()));
                MANORepository.saveAndFlush(osm);
            }else{
                //TODO remove osm pkg info
            }
        }
    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) {
        log.info("{} - Received VNF Pkg change notification", osm.getManoId());
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) {
        log.info("{} - Received Vnfd deletion notification for Vnfd {} info ID {}", osm.getManoId(), notification.getVnfPkgInfoId(),
                notification.getVnfPkgInfoId());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (osm.getOsmIdTranslation().containsKey(notification.getVnfPkgInfoId()) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    String osmInfoPkgId = translateCatalogIdToOsmId(notification.getVnfPkgInfoId());
                    if(!osm.getOsmIdTranslation().containsValue(osmInfoPkgId))//If pkg is not present in any project
                        deleteVnfd(notification.getVnfPkgInfoId(), notification.getOperationId().toString());
                    osm.getOsmIdTranslation().remove(notification.getVnfPkgInfoId());
                    MANORepository.saveAndFlush(osm);
                    log.info("{} - Successfully deleted Vnfd {} with info ID {}", osm.getManoId(), notification.getVnfdId(),
                            notification.getVnfPkgInfoId());
                    sendNotification(new VnfPkgDeletionNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId()));
                } catch (Exception e) {
                    log.error("{} - Could not delete Vnfd: {}", osm.getManoId(), e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new VnfPkgDeletionNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId()));
                }
            } else {
                if (!osm.getOsmIdTranslation().containsKey(notification.getVnfPkgInfoId()) || this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - VNF Pkg deletion skipped",  osm.getManoId());
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
        log.info("{} - Received PNFD onboarding notification", osm.getManoId());
        log.debug("Body: {}", notification);
    }

    // TODO: to be implemented according to new descriptor format in OSMR4
    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotificationMessage notification) {
        log.info("{} - Received PNFD deletion notification", osm.getManoId());
        log.debug("Body: {}", notification);
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

    private void onBoardNsPackage(File file, String nsdInfoId, String opId) throws FailedOperationException {
        log.info("{} - Onboarding ns package, opId: {}", osm.getManoId(), opId);
        // Create the NS descriptor resource
        OSMHttpResponse httpResponse = osmClient.createNsd();
        IdObject content = parseResponse(httpResponse, opId, IdObject.class).get(0);
        String osmNsdInfoId = content.getId();
        // Upload NS descriptor content
        httpResponse = osmClient.uploadNsdContent(osmNsdInfoId, file);
        parseResponse(httpResponse, opId, null);
        osm.getOsmIdTranslation().put(nsdInfoId, osmNsdInfoId);
        MANORepository.saveAndFlush(osm);
        log.info("{} - Package onboarding successful. OpId: {}", osm.getManoId(), opId);
    }

    private void deleteNsd(String nsdInfoId, String opId) throws FailedOperationException {
        log.info("{} - Deleting nsd {}, opId: {}", osm.getManoId(), nsdInfoId, opId);
        String osmNsdInfoId = translateCatalogIdToOsmId(nsdInfoId);
        OSMHttpResponse httpResponse = osmClient.deleteNsd(osmNsdInfoId);
        parseResponse(httpResponse, opId, null);
        osm.getOsmIdTranslation().remove(nsdInfoId);
        MANORepository.saveAndFlush(osm);
        log.info("{} - Nsd deleting successful. OpId: {}", osm.getManoId(), opId);
    }

    List<String> getNsdIdList() throws FailedOperationException {
        log.info("{} - Getting nsd id list", osm.getManoId());
        OSMHttpResponse httpResponse = osmClient.getNsdInfoList();
        List<OsmInfoObject> objList = parseResponse(httpResponse, null, OsmInfoObject.class);
        return objList.stream().map(OsmInfoObject::getDescriptorId).collect(Collectors.toList());
    }

    private String onBoardVnfPackage(File file, String vnfdInfoId, String opId) throws FailedOperationException {
        log.info("{} - Onboarding vnf package, opId: {}", osm.getManoId(), opId);
        // Create the VNF descriptor resource
        OSMHttpResponse httpResponse = osmClient.createVnfPackage();
        IdObject content = parseResponse(httpResponse, opId, IdObject.class).get(0);
        String osmVnfdInfoId = content.getId();
        // Upload VNF descriptor content
        httpResponse = osmClient.uploadVnfPackageContent(osmVnfdInfoId, file);
        parseResponse(httpResponse, opId, null);
        //Store OsmPkfInfo
        httpResponse = osmClient.getVnfPackageInfo(osmVnfdInfoId);
        OsmInfoObject osmInfoObject = parseResponse(httpResponse, opId, OsmInfoObject.class).get(0);
        osmInfoObject.setEpoch(Instant.now().getEpochSecond());
        osmInfoObject.setOsmId(osm.getManoId());
        osmInfoObjectRepository.saveAndFlush(osmInfoObject);
        log.info("{} - Package onboarding successful. OpId: {}", osm.getManoId(), opId);
        return osmVnfdInfoId;
    }

    private void deleteVnfd(String vnfdInfoId, String opId) throws FailedOperationException {
        //TODO remove OSM info obj and files
        log.info("{} - Deleting vnfd {}, opId: {}", osm.getManoId(), vnfdInfoId, opId);
        String osmVnfdInfoId = translateCatalogIdToOsmId(vnfdInfoId);
        OSMHttpResponse httpResponse = osmClient.deleteVnfPackage(osmVnfdInfoId);
        parseResponse(httpResponse, opId, null);
        OsmInfoObject osmInfoObject =  osmInfoObjectRepository.findById(vnfdInfoId).get();
        osmInfoObjectRepository.delete(osmInfoObject);
        log.info("{} - Vnfd deleting successful. OpId: {}", osm.getManoId(), opId);
    }

    private List<String> getVnfdIdList() throws FailedOperationException {
        log.info("{} - Getting vnfd id list", osm.getManoId());
        OSMHttpResponse httpResponse = osmClient.getVnfPackageList();
        List<OsmInfoObject> objList = parseResponse(httpResponse, null, OsmInfoObject.class);
        return objList.stream().map(OsmInfoObject::getDescriptorId).collect(Collectors.toList());
    }
}
