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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.*;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.PluginOperationalState;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.*;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.manoClass.OSMMano;
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
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.osmManagement.OsmObjectType;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.vnfDescriptor.OsmVNFPackage;
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

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
    List<String> toBeDeleted = new ArrayList<>();

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

    private void initOsmConnection() {
        osmClient = new OSMr4PlusClient(osm.getIpAddress(), osm.getUsername(), osm.getPassword(), osm.getProject());
        log.info("{} - OSM R4+ instance addr {}, user {}, project {} connected", osm.getManoId(), osm.getIpAddress(), osm.getUsername(),
                osm.getProject());

        //Scheduling OSM synchronization
        if(isManoSync()) {
            log.info("{} - Starting runtime synchronization, sync period every {} minutes", osm.getManoId(), syncPeriod);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable syncTask = () -> OSMSynchronization();
            scheduler.scheduleAtFixedRate(syncTask, syncPeriod, syncPeriod, TimeUnit.MINUTES);
        }
    }

    @Override
    public Map<String, String> getAllVnfd(String project){
        log.info("{} - Startup synchronization, started retrieving Osm VNF Pkgs from project {}", osm.getManoId(), project);
        Long startSync = Instant.now().getEpochSecond();

        try {
            updateDB(true, false, project);
        } catch(FailedOperationException e){
            log.error("{} - {}", osm.getManoId(), e.getMessage());
            return null;
        }

        //Delete OSM Pkg no longer present in OSM and add to ids list the others
        Map<String, String> ids = new HashMap<>();
        List<OsmInfoObject> osmInfoObjectList = osmInfoObjectRepository.findByOsmIdAndType(osm.getManoId(), OsmObjectType.VNF);
        for(OsmInfoObject osmInfoObj : osmInfoObjectList){
            if(osmInfoObj.getEpoch().compareTo(startSync) < 0){
                log.info("{} - Osm Pkg with descriptor ID {} and version {} no longer present in project {}", osm.getManoId(), osmInfoObj.getDescriptorId(), osmInfoObj.getVersion(), project);
                toBeDeleted.add(osmInfoObj.getId());
            }else{
                ids.put(osmInfoObj.getDescriptorId(), osmInfoObj.getVersion());
            }
        }
        log.info("{} - Startup synchronization, finished retrieving Osm VNF Pkgs from project {}", osm.getManoId(), project);

        return ids;
    }

    @Override
    public Map<String, String> getAllNsd(String project) {
        log.info("{} - Startup synchronization, started retrieving Osm NS Pkgs from project {}", osm.getManoId(), project);
        Long startSync = Instant.now().getEpochSecond();

        try {
            updateDB(false, true, project);
        } catch(FailedOperationException e){
            log.error("{} - {}", osm.getManoId(), e.getMessage());
            return null;
        }

        //Delete OSM Pkg no longer present in OSM and add to ids list the others
        Map<String, String> ids = new HashMap<>();
        List<OsmInfoObject> osmInfoObjectList = osmInfoObjectRepository.findByOsmIdAndType(osm.getManoId(), OsmObjectType.NS);
        for(OsmInfoObject osmInfoObj : osmInfoObjectList){
            if(osmInfoObj.getEpoch().compareTo(startSync) < 0){
                log.info("{} - Osm Pkg with descriptor ID {} and version {} no longer present in project {}", osm.getManoId(), osmInfoObj.getDescriptorId(), osmInfoObj.getVersion(), project);
                toBeDeleted.add(osmInfoObj.getId());
            }else{
                ids.put(osmInfoObj.getDescriptorId(), osmInfoObj.getVersion());
            }
        }
        log.info("{} - Startup synchronization, finished retrieving Osm NS Pkgs from project {}", osm.getManoId(), project);

        return ids;
    }

    @Override
    public KeyValuePair getTranslatedPkgPath(String descriptorId, String descriptorVersion, String project){
        log.info("{} - Translating Osm Pkg with descriptor ID {} and version {} from project {}", osm.getManoId(), descriptorId, descriptorVersion, project);
        Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(descriptorId, descriptorVersion, osm.getManoId());
        String pkgPath = null;
        try{
            if(osmInfoObject.isPresent()){
                if(osmInfoObject.get().getType().equals(OsmObjectType.VNF))
                    pkgPath = createVnfPkgTosca(osmInfoObject.get());
                else
                    pkgPath = createNsPkgTosca(osmInfoObject.get());
            }
        }catch(Exception e){
            log.error("{} - Unable to generate TOSCA Pkg with descriptor ID {} and version {} for project {}: {}", osm.getManoId(), descriptorId, descriptorVersion, project, e.getMessage());
            log.debug(null, e);
            return  null;
        }
        log.info("{} - Uploading TOSCA Pkg with descriptor ID {} and version {} to project {}", osm.getManoId(), descriptorId, descriptorVersion, project);

        return new KeyValuePair(pkgPath, PathType.LOCAL.toString());
    }

    @Override
    public void notifyOnboarding(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus){
        Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(descriptorId, descriptorVersion, osm.getManoId());
        if(osmInfoObject.isPresent()){
            if(opStatus.equals(OperationStatus.SUCCESSFULLY_DONE)){
                osm.getOsmIdTranslation().put(infoId, osmInfoObject.get().getId());
                MANORepository.saveAndFlush(osm);
            }//TODO what if FAILED?
        }
    }

    @Override
    public void notifyDelete(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus){
        if(opStatus.equals(OperationStatus.SUCCESSFULLY_DONE)){
            try {
                String osmInfoId = translateCatalogIdToOsmId(infoId);
                osm.getOsmIdTranslation().remove(infoId);
                MANORepository.saveAndFlush(osm);
                if(!osm.getOsmIdTranslation().containsValue(osmInfoId)) {
                    osmInfoObjectRepository.deleteById(osmInfoId);
                    toBeDeleted.remove(osmInfoId);
                    //TODO delete corresponding folder
                }
            }catch(FailedOperationException e){
                log.error("{} - Error in deleting OSM Pkg : {} ", osm.getManoId(), e.getMessage());
                log.debug(null, e);
            }
        }//TODO what if FAILED?
    }

    private void OSMSynchronization(){
        log.info("{} - Runtime synchronization, started retrieving Osm VNF and NS Pkgs", osm.getManoId());
        Long startSync = Instant.now().getEpochSecond();

        List<OsmInfoObject> oldOsmInfoObjectList = osmInfoObjectRepository.findByOsmId(osm.getManoId());
        List<String> oldOsmInfoIdList = oldOsmInfoObjectList.stream().map(OsmInfoObject::getId).collect(Collectors.toList());

        try {
            updateDB(true, false, "all");//TODO change updateNS in true
        } catch(FailedOperationException e){
            log.error("{} - {}", osm.getManoId(), e.getMessage());
            return;
        }

        List<OsmInfoObject> osmInfoObjectList = osmInfoObjectRepository.findByOsmIdAndType(osm.getManoId(), OsmObjectType.VNF);//TODO change with findByOsmId
        UUID operationId;
        for(OsmInfoObject osmInfoObj : osmInfoObjectList){
            //Upload new OSM Pkg
            if(!oldOsmInfoIdList.contains(osmInfoObj.getId())){
                operationId = UUID.randomUUID();
                String pkgPath;
                try {
                    if(osmInfoObj.getType().equals(OsmObjectType.VNF))
                        pkgPath = createVnfPkgTosca(osmInfoObj);
                    else
                        pkgPath = createNsPkgTosca(osmInfoObj);
                }catch(Exception e){
                    log.error("{} - Unable to generate TOSCA Pkg with descriptor ID {} and version {}: {}", osm.getManoId(), osmInfoObj.getDescriptorId(), osmInfoObj.getVersion(), e.getMessage());
                    log.debug(null, e);
                    continue;
                }
                log.info("{} - Uploading TOSCA Pkg with descriptor ID {} and version {}", osm.getManoId(), osmInfoObj.getDescriptorId(), osmInfoObj.getVersion());
                if(osmInfoObj.getType().equals(OsmObjectType.VNF))
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(null, osmInfoObj.getDescriptorId(), osmInfoObj.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, osm.getManoId(), null, new KeyValuePair(pkgPath, PathType.LOCAL.toString())));
                else
                    sendNotification(new NsdOnBoardingNotificationMessage(null, osmInfoObj.getDescriptorId(), osmInfoObj.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, osm.getManoId(), null, new KeyValuePair(pkgPath, PathType.LOCAL.toString())));
            }
            //Delete OSM Pkg no longer present in OSM
            if(osmInfoObj.getEpoch().compareTo(startSync) < 0){
                log.info("{} - Osm Pkg with descriptor ID {} and version {} no longer present, deleting it", osm.getManoId(), osmInfoObj.getDescriptorId(), osmInfoObj.getVersion());
                toBeDeleted.add(osmInfoObj.getId());
                operationId = UUID.randomUUID();
                if(osmInfoObj.getType().equals(OsmObjectType.VNF))
                    sendNotification(new VnfPkgDeletionNotificationMessage(null, osmInfoObj.getDescriptorId(), osmInfoObj.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, osm.getManoId()));
                else
                    sendNotification(new NsdDeletionNotificationMessage(null, osmInfoObj.getDescriptorId(), osmInfoObj.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, osm.getManoId()));
            }
        }
        log.info("{} - Runtime synchronization, finished retrieving Osm VNF and NS Pkgs", osm.getManoId());
    }

    private void updateDB(boolean updateVNF, boolean updateNS, String project) throws FailedOperationException{
        if(!updateVNF && !updateNS)
            return;

        OSMHttpResponse response;
        Optional<OsmInfoObject> osmInfoObject;
        List<OsmInfoObject> packageInfoList = new ArrayList<>();
        List<OsmInfoObject> aux = new ArrayList<>();
        List<String> vnfPackageInfoIdList = new ArrayList<>();
        List<String> nsPackageInfoIdList = new ArrayList<>();

        if(updateVNF) {
            log.info("{} - Updating VNFs DB for project {}", osm.getManoId(), project);
            //Retrieve VNFPkgInfos
            response = osmClient.getVnfPackageList();
            aux = parseResponse(response, null, OsmInfoObject.class);
            packageInfoList.addAll(aux);
            vnfPackageInfoIdList = aux.stream().map(OsmInfoObject::getId).collect(Collectors.toList());
        }
        if(updateNS){
            log.info("{} - Updating NSs DB for project {}", osm.getManoId(), project);
            //Retrieve NSPkgInfos
            response = osmClient.getNsdInfoList();
            aux = parseResponse(response, null, OsmInfoObject.class);
            packageInfoList.addAll(aux);
            nsPackageInfoIdList = aux.stream().map(OsmInfoObject::getId).collect(Collectors.toList());
        }

        for(OsmInfoObject packageInfo : packageInfoList) {
            try {
                osmInfoObject = osmInfoObjectRepository.findById(packageInfo.getId());
                if (osmInfoObject.isPresent()) {
                    log.info("{} - OSM Pkg with descriptor ID {} and version {} already present in project {}", osm.getManoId(), packageInfo.getDescriptorId(), packageInfo.getVersion(), project);
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
                    log.info("{} - Retrieving OSM Pkg with descriptor ID {} and version {} from project {}", osm.getManoId(), packageInfo.getDescriptorId(), packageInfo.getVersion(), project);
                    if(vnfPackageInfoIdList.contains(packageInfo.getId())) {
                        response = osmClient.getVnfPackageContent(packageInfo.getId(), osmDirPath.toString());
                        packageInfo.setType(OsmObjectType.VNF);
                    }
                    else {
                        response = osmClient.getNsdContent(packageInfo.getId(), osmDirPath.toString());
                        packageInfo.setType(OsmObjectType.NS);
                    }
                    parseResponse(response, null, null);
                    File archive = new File(osmDirPath.toString() + "/" + packageInfo.getId() + ".tar.gz");
                    Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP);
                    archiver.extract(archive, new File(osmDirPath.toString()));
                    packageInfo.setEpoch(Instant.now().getEpochSecond());
                    packageInfo.setOsmId(osm.getManoId());
                    osmInfoObjectRepository.saveAndFlush(packageInfo);
                }
            } catch (FailedOperationException | IOException | IllegalStateException | IllegalArgumentException e) {
                log.error("{} - Sync error: {}", osm.getManoId(), e.getMessage());
                log.debug(null, e);
            }
        }
    }

    private String createVnfPkgTosca (OsmInfoObject vnfPackageInfo) throws MalformattedElementException, IllegalStateException, IOException, IllegalArgumentException{
        log.info("{} - Creating TOSCA VNF Descriptor with ID {} and version {}", osm.getManoId(), vnfPackageInfo.getDescriptorId(), vnfPackageInfo.getVersion());
        DescriptorTemplate vnfd = ToscaDescriptorsParser.generateVnfDescriptor(osmDirPath.toString() + "/" + vnfPackageInfo.getAdmin().getStorage().getDescriptor());
        VNFNode vnfNode = vnfd.getTopologyTemplate().getVNFNodes().values().iterator().next();
        File cloudInit = null;
        if(vnfNode.getInterfaces() != null){
            cloudInit = new File(osmDirPath.toString() + "/" + vnfPackageInfo.getAdmin().getStorage().getPkgDir() + "/cloud_init/" + vnfNode.getInterfaces().getVnflcm().getInstantiate().getImplementation());
        }
        log.info("{} - Creating TOSCA VNF Pkg with descriptor ID {} and version {}", osm.getManoId(), vnfPackageInfo.getDescriptorId(), vnfPackageInfo.getVersion());
        return ToscaArchiveBuilder.createVNFCSAR(vnfPackageInfo.getId(), vnfd, cloudInit);
    }

    private String createNsPkgTosca (OsmInfoObject nsPackageInfo) throws MalformattedElementException, IllegalStateException, IOException, IllegalArgumentException{
        log.info("{} - Creating TOSCA NS Descriptor with ID {} and version {}", osm.getManoId(), nsPackageInfo.getDescriptorId(), nsPackageInfo.getVersion());
        DescriptorTemplate nsd = ToscaDescriptorsParser.generateNsDescriptor(osmDirPath.toString() + "/" + nsPackageInfo.getAdmin().getStorage().getDescriptor());
        log.info("{} - Creating TOSCA NS Pkg with descriptor ID {} and version {}", osm.getManoId(), nsPackageInfo.getDescriptorId(), nsPackageInfo.getVersion());
        return ToscaArchiveBuilder.createNSCSAR(nsPackageInfo.getId(), nsd);
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
        log.info("{} - Received NSD onboarding notification for Nsd with ID {} and version {} for project {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (Utilities.isTargetMano(notification.getSiteOrManoIds(), osm) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    //TODO add checks
                    String packagePath = notification.getPackagePath().getKey();
                    File descriptor;
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

                    //Catalogue requires onboarding of a package previously deleted
                    if(osm.getOsmIdTranslation().containsKey(notification.getNsdInfoId())) {
                        String osmInfoIdToDelete = translateCatalogIdToOsmId(notification.getNsdInfoId());
                        if(toBeDeleted.contains(osmInfoIdToDelete)){
                            osmInfoObjectRepository.deleteById(osmInfoIdToDelete);
                            toBeDeleted.remove(osmInfoIdToDelete);
                            Map<String, String> catalogueToOsm = osm.getOsmIdTranslation();
                            catalogueToOsm.values().removeIf(osmInfoIdToDelete::equals);
                            osm.setOsmIdTranslation(catalogueToOsm);
                            MANORepository.saveAndFlush(osm);
                        }
                    }

                    //Check if already present, uploaded for example from another project
                    Optional<OsmInfoObject> osmInfoObjectOptional = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(descriptorTemplate.getMetadata().getDescriptorId(), descriptorTemplate.getMetadata().getVersion(), osm.getManoId());
                    String osmInfoObjectId;
                    if(!osmInfoObjectOptional.isPresent()) {
                        NsdBuilder nsdBuilder = new NsdBuilder(logo);
                        List<DescriptorTemplate> includedVnfds = new ArrayList<>();
                        for (KeyValuePair vnfPathPair : notification.getIncludedVnfds().values()) {
                            if (vnfPathPair.getValue().equals(PathType.LOCAL.toString())) {
                                packagePath = vnfPathPair.getKey();
                                metadataFileNames = Utilities.listFiles(packagePath + "/TOSCA-Metadata/");
                                //Consider only one metada file is present
                                metadataFileName = metadataFileNames.stream().filter(name -> name.endsWith(".meta")).findFirst().get();
                                metadata = new File(packagePath + "/TOSCA-Metadata/" + metadataFileName);
                                descriptor = new File(packagePath + "/" + Utilities.getMainServiceTemplateFromMetadata(metadata));
                            } else {
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

                        osmInfoObjectId = onBoardNsPackage(archive, notification.getOperationId().toString());

                        log.info("{} - Successfully uploaded Nsd with ID {} and version {} for project {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
                    }else
                        osmInfoObjectId = osmInfoObjectOptional.get().getId();

                    osm.getOsmIdTranslation().put(notification.getNsdInfoId(), osmInfoObjectId);
                    MANORepository.saveAndFlush(osm);
                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId(), null,null));
                } catch (Exception e) {
                    log.error("{} - Could not onboard Nsd: {}", osm.getManoId(), e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId(), null,null));
                }
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - NSD onboarding skipped", osm.getManoId());
                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId(), null,null));
                }
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            if(notification.getPluginId().equals(osm.getManoId()) && notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(notification.getNsdId(), notification.getNsdVersion(), osm.getManoId());
                if(osmInfoObject.isPresent()){
                        osm.getOsmIdTranslation().put(notification.getNsdInfoId(), osmInfoObject.get().getId());
                        MANORepository.saveAndFlush(osm);
                    }
            }//TODO what if FAILED?
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
        log.info("{} - Received Nsd deletion notification for Nsd with ID {} and version {} for project {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (osm.getOsmIdTranslation().containsKey(notification.getNsdInfoId()) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    String osmInfoPkgId = translateCatalogIdToOsmId(notification.getNsdInfoId());
                    if(osm.getOsmIdTranslation().entrySet().stream().filter(map -> map.getValue().equals(osmInfoPkgId)).count() == 1)//If pkg is not present in other projects
                        deleteNsd(notification.getNsdInfoId(), notification.getOperationId().toString());
                    osm.getOsmIdTranslation().remove(notification.getNsdInfoId());
                    MANORepository.saveAndFlush(osm);
                    log.info("{} - Successfully deleted Nsd with ID {} and version {} for project {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
                    sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId()));
                } catch (Exception e) {
                    log.error("{} - Could not delete Nsd: {}", osm.getManoId(), e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId()));
                }
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - NSD deletion skipped", osm.getManoId());
                    sendNotification(new NsdDeletionNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId()));
                }
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            if(notification.getPluginId().equals(osm.getManoId()) && notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                try {
                    String osmInfoId = translateCatalogIdToOsmId(notification.getNsdInfoId());
                    osm.getOsmIdTranslation().remove(notification.getNsdInfoId());
                    MANORepository.saveAndFlush(osm);
                    if(!osm.getOsmIdTranslation().containsValue(osmInfoId)) {
                        osmInfoObjectRepository.deleteById(osmInfoId);
                        toBeDeleted.remove(osmInfoId);
                        //TODO delete corresponding folder
                    }
                }catch(FailedOperationException e){
                    log.error("{} - Error in deleting OSM NS Pkg : {} ", osm.getManoId(), e.getMessage());
                    log.debug(null, e);
                }
            }//TODO what if FAILED?
        }
    }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) {
        log.info("{} - Received Vnfd onboarding notification for Vnfd with ID {} and version {} for project {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
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

                    //Catalogue requires onboarding of a package previously deleted
                    if(osm.getOsmIdTranslation().containsKey(notification.getVnfPkgInfoId())) {
                        String osmInfoIdToDelete = translateCatalogIdToOsmId(notification.getVnfPkgInfoId());
                        if(toBeDeleted.contains(osmInfoIdToDelete)){
                            osmInfoObjectRepository.deleteById(osmInfoIdToDelete);
                            toBeDeleted.remove(osmInfoIdToDelete);
                            Map<String, String> catalogueToOsm = osm.getOsmIdTranslation();
                            catalogueToOsm.values().removeIf(osmInfoIdToDelete::equals);
                            osm.setOsmIdTranslation(catalogueToOsm);
                            MANORepository.saveAndFlush(osm);
                        }
                    }

                    //Check if already present, uploaded for example from another project
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
                            log.debug("{} - No cloud-init file found for VNF with ID {} and version {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion());
                        }
                        File archive = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue", cloudInit);
                        osmInfoObjectId = onBoardVnfPackage(archive, notification.getOperationId().toString());
                        log.info("{} - Successfully uploaded Vnfd with ID {} and version {} for project {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
                    }else
                        osmInfoObjectId = osmInfoObjectOptional.get().getId();

                    osm.getOsmIdTranslation().put(notification.getVnfPkgInfoId(), osmInfoObjectId);
                    MANORepository.saveAndFlush(osm);
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId(), null,null));
                } catch (Exception e) {
                    log.error("{} - Could not onboard Vnfd: {}", osm.getManoId(), e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId(), null,null));
                }
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - VNF Pkg onboarding skipped", osm.getManoId());
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId(), null,null));
                }
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            if(notification.getPluginId().equals(osm.getManoId()) && notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(notification.getVnfdId(), notification.getVnfdVersion(), osm.getManoId());
                if(osmInfoObject.isPresent()){
                    osm.getOsmIdTranslation().put(notification.getVnfPkgInfoId(), osmInfoObject.get().getId());
                    MANORepository.saveAndFlush(osm);
                }
            }//TODO what if FAILED?
        }
    }

    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) {
        log.info("{} - Received VNF Pkg change notification", osm.getManoId());
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) {
        log.info("{} - Received Vnfd deletion notification for Vnfd with ID {} and version {} for project {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (osm.getOsmIdTranslation().containsKey(notification.getVnfPkgInfoId()) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    String osmInfoPkgId = translateCatalogIdToOsmId(notification.getVnfPkgInfoId());
                    if(osm.getOsmIdTranslation().entrySet().stream().filter(map -> map.getValue().equals(osmInfoPkgId)).count() == 1)//If pkg is not present in other projects
                        deleteVnfd(notification.getVnfPkgInfoId(), notification.getOperationId().toString());
                    osm.getOsmIdTranslation().remove(notification.getVnfPkgInfoId());
                    MANORepository.saveAndFlush(osm);
                    log.info("{} - Successfully deleted Vnfd with ID {} and version {} for project {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
                    sendNotification(new VnfPkgDeletionNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            osm.getManoId()));
                } catch (Exception e) {
                    log.error("{} - Could not delete Vnfd: {}", osm.getManoId(), e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new VnfPkgDeletionNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            osm.getManoId()));
                }
            } else {
                if (!osm.getOsmIdTranslation().containsKey(notification.getVnfPkgInfoId()) || this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - VNF Pkg deletion skipped",  osm.getManoId());
                    sendNotification(new VnfPkgDeletionNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId()));
                }
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            if(notification.getPluginId().equals(osm.getManoId()) && notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                try {
                    String osmInfoId = translateCatalogIdToOsmId(notification.getVnfPkgInfoId());
                    osm.getOsmIdTranslation().remove(notification.getVnfPkgInfoId());
                    MANORepository.saveAndFlush(osm);
                    if(!osm.getOsmIdTranslation().containsValue(osmInfoId)) {
                        osmInfoObjectRepository.deleteById(osmInfoId);
                        toBeDeleted.remove(osmInfoId);
                        //TODO delete corresponding folder
                    }
                }catch(FailedOperationException e){
                    log.error("{} - Error in deleting OSM Pkg : {} ", osm.getManoId(), e.getMessage());
                    log.debug(null, e);
                }
            }//TODO what if FAILED?
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
        if (catalogId == null)
            throw new FailedOperationException("ID not specified");
        String osmId = osm.getOsmIdTranslation().get(catalogId);
        if (osmId == null)
            throw new FailedOperationException("Could not find the corresponding ID in OSM");
        return osmId;
    }

    private String onBoardNsPackage(File file, String opId) throws FailedOperationException {
        log.info("{} - Onboarding ns package, opId: {}", osm.getManoId(), opId);
        // Create the NS descriptor resource
        OSMHttpResponse httpResponse = osmClient.createNsd();
        IdObject content = parseResponse(httpResponse, opId, IdObject.class).get(0);
        String osmNsdInfoId = content.getId();
        // Upload NS descriptor content
        httpResponse = osmClient.uploadNsdContent(osmNsdInfoId, file);
        parseResponse(httpResponse, opId, null);
        //Store OsmPkfInfo
        httpResponse = osmClient.getNsdInfo(osmNsdInfoId);
        OsmInfoObject osmInfoObject = parseResponse(httpResponse, opId, OsmInfoObject.class).get(0);
        osmInfoObject.setEpoch(Instant.now().getEpochSecond());
        osmInfoObject.setOsmId(osm.getManoId());
        osmInfoObject.setType(OsmObjectType.NS);
        osmInfoObjectRepository.saveAndFlush(osmInfoObject);
        log.info("{} - Package onboarding successful. OpId: {}", osm.getManoId(), opId);
        return osmNsdInfoId;
    }

    private void deleteNsd(String nsdInfoId, String opId) throws FailedOperationException {
        log.info("{} - Deleting nsd {}, opId: {}", osm.getManoId(), nsdInfoId, opId);
        String osmNsdInfoId = translateCatalogIdToOsmId(nsdInfoId);
        OSMHttpResponse httpResponse = osmClient.deleteNsd(osmNsdInfoId);
        parseResponse(httpResponse, opId, null);
        OsmInfoObject osmInfoObject =  osmInfoObjectRepository.findById(osmNsdInfoId).get();
        osmInfoObjectRepository.delete(osmInfoObject);
        log.info("{} - Nsd deleting successful. OpId: {}", osm.getManoId(), opId);
    }

    List<String> getNsdIdList() throws FailedOperationException {
        log.info("{} - Getting nsd id list", osm.getManoId());
        OSMHttpResponse httpResponse = osmClient.getNsdInfoList();
        List<OsmInfoObject> objList = parseResponse(httpResponse, null, OsmInfoObject.class);
        return objList.stream().map(OsmInfoObject::getDescriptorId).collect(Collectors.toList());
    }

    private String onBoardVnfPackage(File file, String opId) throws FailedOperationException {
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
        osmInfoObject.setType(OsmObjectType.VNF);
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
        OsmInfoObject osmInfoObject =  osmInfoObjectRepository.findById(osmVnfdInfoId).get();
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
