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
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.osm.OSM;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.common.*;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.repos.TranslationInformationRepository;
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
    private Path tmpDirPath;
    private final File logo;
    private final OSM osm;
    private OSMr4PlusClient osmClient;
    private OsmInfoObjectRepository osmInfoObjectRepository;
    private TranslationInformationRepository translationInformationRepository;
    private long syncPeriod;

    private boolean useVimNetworkName;

    public OpenSourceMANOR4PlusPlugin(MANOType manoType, MANO mano, String kafkaBootstrapServers,
                                      OsmInfoObjectRepository osmInfoObjectRepository, TranslationInformationRepository translationInformationRepository, String localTopic, String remoteTopic,
                                      KafkaTemplate<String, String> kafkaTemplate, Path osmDirPath, Path tmpDir, Path logoPath, boolean manoSync, long syncPeriod, boolean useVimNetworkName) {
        super(manoType, mano, kafkaBootstrapServers, localTopic, remoteTopic, kafkaTemplate, manoSync);
        if (MANOType.OSMR4 != manoType && MANOType.OSMR5 != manoType && MANOType.OSMR6 != manoType && MANOType.OSMR7 != manoType) {
            throw new IllegalArgumentException("OSM R4+ plugin requires an OSM R4+ type MANO");
        }
        osm = (OSM) mano;
        this.osmInfoObjectRepository = osmInfoObjectRepository;
        this.translationInformationRepository = translationInformationRepository;
        this.osmDirPath = osmDirPath;
        this.logo = new File(logoPath.toUri());
        this.syncPeriod = syncPeriod;
        this.tmpDirPath = tmpDir;
        this.useVimNetworkName = useVimNetworkName;
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
            Runnable syncTask = this::OSMSynchronization;
            scheduler.scheduleAtFixedRate(syncTask, syncPeriod, syncPeriod, TimeUnit.MINUTES);
        }
    }

    @Override
    public Map<String, List<String>> getAllVnfd(String project){
        log.info("{} - Startup synchronization, started retrieving Osm VNF Pkgs from project {}", osm.getManoId(), project);
        Long startSync = Instant.now().getEpochSecond();

        try {
            updateDB(true, false, project);
        } catch(FailedOperationException e){
            log.error("{} - {}", osm.getManoId(), e.getMessage());
            return null;
        }

        //Delete OSM Pkg no longer present in OSM and add to ids list the others
        Map<String, List<String>> ids = new HashMap<>();
        List<OsmInfoObject> osmInfoObjectList = osmInfoObjectRepository.findByOsmIdAndType(osm.getManoId(), OsmObjectType.VNF);
        for(OsmInfoObject osmInfoObj : osmInfoObjectList){
            if(osmInfoObj.getEpoch().compareTo(startSync) < 0){
                log.info("{} - Osm Pkg with descriptor ID {} and version {} no longer present in project {}", osm.getManoId(), osmInfoObj.getDescriptorId(), osmInfoObj.getVersion(), project);
                osmInfoObjectRepository.delete(osmInfoObj);
                List<TranslationInformation> translationInformationList = translationInformationRepository.findByOsmInfoIdAndOsmManoId(osmInfoObj.getId(), osm.getManoId());
                for(TranslationInformation translationInformation : translationInformationList)
                    translationInformationRepository.delete(translationInformation);
                //TODO delete corresponding folder
            }else{
                String catDescriptorId = getCatDescriptorId(osmInfoObj.getDescriptorId(), osmInfoObj.getVersion());
                if(catDescriptorId != null)
                    ids.computeIfAbsent(catDescriptorId, k -> new ArrayList<>()).add(osmInfoObj.getVersion());//ids.put(catDescriptorId, osmInfoObj.getVersion());
                else
                    ids.computeIfAbsent(osmInfoObj.getDescriptorId(), k -> new ArrayList<>()).add(osmInfoObj.getVersion());//ids.put(osmInfoObj.getDescriptorId(), osmInfoObj.getVersion());
            }
        }
        log.info("{} - Startup synchronization, finished retrieving Osm VNF Pkgs from project {}", osm.getManoId(), project);

        return ids;
    }

    @Override
    public Map<String, List<String>> getAllNsd(String project) {
        log.info("{} - Startup synchronization, started retrieving Osm NS Pkgs from project {}", osm.getManoId(), project);
        Long startSync = Instant.now().getEpochSecond();

        try {
            updateDB(false, true, project);
        } catch(FailedOperationException e){
            log.error("{} - {}", osm.getManoId(), e.getMessage());
            return null;
        }

        //Delete OSM Pkg no longer present in OSM and add to ids list the others
        Map<String, List<String>> ids = new HashMap<>();
        List<OsmInfoObject> osmInfoObjectList = osmInfoObjectRepository.findByOsmIdAndType(osm.getManoId(), OsmObjectType.NS);
        for(OsmInfoObject osmInfoObj : osmInfoObjectList){
            if(osmInfoObj.getEpoch().compareTo(startSync) < 0){
                log.info("{} - Osm Pkg with descriptor ID {} and version {} no longer present in project {}", osm.getManoId(), osmInfoObj.getDescriptorId(), osmInfoObj.getVersion(), project);
                osmInfoObjectRepository.delete(osmInfoObj);
                List<TranslationInformation> translationInformationList = translationInformationRepository.findByOsmInfoIdAndOsmManoId(osmInfoObj.getId(), osm.getManoId());
                for(TranslationInformation translationInformation : translationInformationList)
                    translationInformationRepository.delete(translationInformation);
                //TODO delete corresponding folder
            }else{
                String catDescriptorId = getCatDescriptorId(osmInfoObj.getDescriptorId(), osmInfoObj.getVersion());
                if(catDescriptorId != null)
                    ids.computeIfAbsent(catDescriptorId, k -> new ArrayList<>()).add(osmInfoObj.getVersion());//ids.put(catDescriptorId, osmInfoObj.getVersion());
                else
                    ids.computeIfAbsent(osmInfoObj.getDescriptorId(), k -> new ArrayList<>()).add(osmInfoObj.getVersion());//ids.put(osmInfoObj.getDescriptorId(), osmInfoObj.getVersion());
            }
        }
        log.info("{} - Startup synchronization, finished retrieving Osm NS Pkgs from project {}", osm.getManoId(), project);

        return ids;
    }

    @Override
    public KeyValuePair getTranslatedPkgPath(String descriptorId, String descriptorVersion, String project){
        log.info("{} - Translating Osm Pkg with descriptor ID {} and version {} from project {}", osm.getManoId(), descriptorId, descriptorVersion, project);
        String osmDescriptorId = getOsmDescriptorId(descriptorId, descriptorVersion);
        if(osmDescriptorId == null)
            osmDescriptorId = descriptorId;
        Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, descriptorVersion, osm.getManoId());
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
        log.info("{} - Received Sync Pkg onboarding notification for Descriptor with ID {} and version {} for project {} : {}", osm.getManoId(), descriptorId, descriptorVersion, project, opStatus.toString());
        String osmDescriptorId = getOsmDescriptorId(descriptorId, descriptorVersion);
        if(osmDescriptorId == null)
            osmDescriptorId = descriptorId;
        Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, descriptorVersion, osm.getManoId());
        if(osmInfoObject.isPresent()){
            if(opStatus.equals(OperationStatus.SUCCESSFULLY_DONE)){
                translationInformationRepository.saveAndFlush(new TranslationInformation(infoId, osmInfoObject.get().getId(), descriptorId, osmInfoObject.get().getDescriptorId(), descriptorVersion, osm.getManoId()));
            }//TODO what if FAILED?
        }
    }

    @Override
    public void notifyDelete(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus){
        log.info("{} - Received Sync Pkg deletion notification for Descriptor with ID {} and version {} for project {} : {}", osm.getManoId(), descriptorId, descriptorVersion, project, opStatus.toString());
    }

    @Override
    public String getManoPkgInfoId(String cataloguePkgInfoId){
       return  getOsmInfoId(cataloguePkgInfoId);
    }

    private void OSMSynchronization(){
        log.info("{} - Runtime synchronization, started retrieving Osm VNF and NS Pkgs", osm.getManoId());
        Long startSync = Instant.now().getEpochSecond();

        List<OsmInfoObject> oldOsmInfoObjectList = osmInfoObjectRepository.findByOsmId(osm.getManoId());
        List<String> oldOsmInfoIdList = oldOsmInfoObjectList.stream().map(OsmInfoObject::getId).collect(Collectors.toList());

        try {
            updateDB(true, true, "all");
        } catch(FailedOperationException e){
            log.error("{} - {}", osm.getManoId(), e.getMessage());
            return;
        }

        List<OsmInfoObject> osmInfoObjectList = osmInfoObjectRepository.findByOsmId(osm.getManoId());
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
                operationId = UUID.randomUUID();
                String catDescriptorId = getCatDescriptorId(osmInfoObj.getDescriptorId(), osmInfoObj.getVersion());
                if(catDescriptorId == null)
                    catDescriptorId = osmInfoObj.getDescriptorId();
                if(osmInfoObj.getType().equals(OsmObjectType.VNF))
                    sendNotification(new VnfPkgDeletionNotificationMessage(null, catDescriptorId, osmInfoObj.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, osm.getManoId()));
                else
                    sendNotification(new NsdDeletionNotificationMessage(null, catDescriptorId, osmInfoObj.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, osm.getManoId()));
                osmInfoObjectRepository.delete(osmInfoObj);
                List<TranslationInformation> translationInformationList = translationInformationRepository.findByOsmInfoIdAndOsmManoId(osmInfoObj.getId(), osm.getManoId());
                for(TranslationInformation translationInformation : translationInformationList)
                    translationInformationRepository.delete(translationInformation);
                //TODO delete corresponding folder
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
        return ToscaArchiveBuilder.createVNFCSAR(vnfPackageInfo.getId(), vnfd, tmpDirPath.toString(), cloudInit);
    }

    private String createNsPkgTosca (OsmInfoObject nsPackageInfo) throws MalformattedElementException, IllegalStateException, IOException, IllegalArgumentException{
        log.info("{} - Creating TOSCA NS Descriptor with ID {} and version {}", osm.getManoId(), nsPackageInfo.getDescriptorId(), nsPackageInfo.getVersion());
        List<OsmInfoObject> vnfInfoList = osmInfoObjectRepository.findByOsmIdAndType(osm.getManoId(), OsmObjectType.VNF);
        List<TranslationInformation> translationInformationList = translationInformationRepository.findByOsmManoId(osm.getManoId());
        DescriptorTemplate nsd = ToscaDescriptorsParser.generateNsDescriptor(osmDirPath.toString() + "/" + nsPackageInfo.getAdmin().getStorage().getDescriptor(), vnfInfoList, translationInformationList, osmDirPath);
        log.info("{} - Creating TOSCA NS Pkg with descriptor ID {} and version {}", osm.getManoId(), nsPackageInfo.getDescriptorId(), nsPackageInfo.getVersion());
        return ToscaArchiveBuilder.createNSCSAR(nsPackageInfo.getId(), nsd, tmpDirPath.toString());
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
        log.info("{} - Received NSD onboarding notification for Nsd with ID {} and version {} for project {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            if (Utilities.isTargetMano(notification.getSiteOrManoIds(), osm) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
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

                    //Check if already present, uploaded for example from another project
                    String osmDescriptorId = getOsmDescriptorId(descriptorTemplate.getMetadata().getDescriptorId(), descriptorTemplate.getMetadata().getVersion());
                    if(osmDescriptorId == null)
                        osmDescriptorId = descriptorTemplate.getMetadata().getDescriptorId();
                    Optional<OsmInfoObject> osmInfoObjectOptional = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, descriptorTemplate.getMetadata().getVersion(), osm.getManoId());
                    String osmInfoObjectId;
                    if(!osmInfoObjectOptional.isPresent()) {
                        //OSM cannot permit to onboard descriptors with same ID but different versions
                        if(isTheFirstTranslationInformationEntry(descriptorTemplate.getMetadata().getDescriptorId()))
                            osmDescriptorId = descriptorTemplate.getMetadata().getDescriptorId();
                        else{
                            osmDescriptorId = UUID.randomUUID().toString();
                            descriptorTemplate.getMetadata().setDescriptorId(osmDescriptorId);
                            //Consider only one NSNode present
                            descriptorTemplate.getTopologyTemplate().getNSNodes().values().iterator().next().getProperties().setDescriptorId(osmDescriptorId);
                            descriptorTemplate.getTopologyTemplate().getNSNodes().values().iterator().next().getProperties().setInvariantId(osmDescriptorId);
                        }

                        NsdBuilder nsdBuilder = new NsdBuilder(logo, useVimNetworkName);
                        List<DescriptorTemplate> includedVnfds = new ArrayList<>();
                        for (KeyValuePair vnfPathPair : notification.getIncludedVnfds().values()) {
                            if (vnfPathPair.getValue().equals(PathType.LOCAL.toString())) {
                                packagePath = vnfPathPair.getKey();
                                metadataFileNames = Utilities.listFiles(packagePath + "/TOSCA-Metadata/");
                                //Consider only one metadata file is present
                                metadataFileName = metadataFileNames.stream().filter(name -> name.endsWith(".meta")).findFirst().get();
                                metadata = new File(packagePath + "/TOSCA-Metadata/" + metadataFileName);
                                descriptor = new File(packagePath + "/" + Utilities.getMainServiceTemplateFromMetadata(metadata));
                            } else {
                                //TODO support also other PathType
                                throw new MethodNotImplementedException("Path Type not currently supported");
                            }
                            includedVnfds.add(mapper.readValue(descriptor, DescriptorTemplate.class));
                        }

                        //OSM cannot permit to onboard descriptors with same ID but different versions
                        List<VNFNode> vnfNodeList = new ArrayList<>(descriptorTemplate.getTopologyTemplate().getVNFNodes().values());
                        for(DescriptorTemplate vnfd : includedVnfds){
                            String translatedVnfdId = getOsmDescriptorId(vnfd.getMetadata().getDescriptorId(), vnfd.getMetadata().getVersion());
                            if(translatedVnfdId == null)
                                throw new FailedOperationException("Could not find the corresponding Descriptor ID in OSM");
                            for(VNFNode vnfNode : vnfNodeList){
                                if(vnfNode.getProperties().getDescriptorId().equals(vnfd.getMetadata().getDescriptorId()))
                                    vnfNode.getProperties().setDescriptorId(translatedVnfdId);
                            }
                            vnfd.getMetadata().setDescriptorId(translatedVnfdId);
                            vnfd.getTopologyTemplate().getVNFNodes().values().iterator().next().getProperties().setDescriptorId(translatedVnfdId);
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

                        packagePath = notification.getPackagePath().getKey();
                        Set<String> fileNames = Utilities.listFiles(packagePath);
                        File monitoring = null;
                        List<File> scripts = new ArrayList<>();
                        //Consider only one manifest file is present if the NS is a Pkg
                        if(fileNames.stream().filter(name -> name.endsWith(".mf")).count() != 0) {
                            String manifestPath = fileNames.stream().filter(name -> name.endsWith(".mf")).findFirst().get();
                            File mf = new File(packagePath + "/" + manifestPath);

                            String monitoringPath = Utilities.getMonitoringFromManifest(mf);

                            if (monitoringPath != null) {
                                monitoring = new File(packagePath + "/" + monitoringPath);
                            } else {
                                log.debug("{} - No monitoring file found for NSD with ID {} and version {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion());
                            }

                            List<String> scriptPaths = Utilities.getScriptsFromManifest(mf);
                            if (scriptPaths.size() > 0) {
                                for(String scriptPath : scriptPaths)
                                    scripts.add(new File(packagePath + "/" + scriptPath));
                            } else {
                                log.debug("{} - No script files found for NS with ID {} and version {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion());
                            }
                        }

                        ArchiveBuilder archiver = new ArchiveBuilder(osmDir, logo);
                        File archive = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue", scripts, monitoring);

                        osmInfoObjectId = onBoardNsPackage(archive, notification.getOperationId().toString());

                        log.info("{} - Successfully uploaded Nsd with ID {} and version {} for project {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
                    }else
                        osmInfoObjectId = osmInfoObjectOptional.get().getId();

                    translationInformationRepository.saveAndFlush(new TranslationInformation(notification.getNsdInfoId(), osmInfoObjectId, notification.getNsdId(), osmDescriptorId, notification.getNsdVersion(), osm.getManoId()));
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
            log.info("{} - Received Sync Pkg onboarding notification for NSD with ID {} and version {} for project {} : {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(), notification.getOpStatus().toString());
            if(notification.getPluginId().equals(osm.getManoId()) && notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                String osmDescriptorId = getOsmDescriptorId(notification.getNsdId(), notification.getNsdVersion());
                if(osmDescriptorId == null)
                    osmDescriptorId = notification.getNsdId();
                Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, notification.getNsdVersion(), osm.getManoId());
                if(osmInfoObject.isPresent())
                    translationInformationRepository.saveAndFlush(new TranslationInformation(notification.getNsdInfoId(), osmInfoObject.get().getId(), notification.getNsdId(), osmInfoObject.get().getDescriptorId(), notification.getNsdVersion(), osm.getManoId()));
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
        if (notification.getScope() == ScopeType.LOCAL) {
            if (translationInformationContainsCatInfoId(notification.getNsdInfoId()) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    String osmInfoPkgId = getOsmInfoId(notification.getNsdInfoId());
                    if(osmInfoPkgId == null)
                        throw new FailedOperationException("Could not find the corresponding Info ID in OSM");
                    if(!deleteTranslationInformationEntry(notification.getNsdInfoId()))
                        throw new FailedOperationException("Could bot delete the specified entry");
                    if(!translationInformationContainsOsmInfoId(osmInfoPkgId))//If pkg is not present in other projects
                        deleteNsd(osmInfoPkgId, notification.getOperationId().toString());
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
            log.info("{} - Received Sync Pkg deletion notification for NSD with ID {} and version {} for project {} : {}", osm.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(), notification.getOpStatus().toString());
        }
    }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) {
        log.info("{} - Received Vnfd onboarding notification for Vnfd with ID {} and version {} for project {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
        if (notification.getScope() == ScopeType.LOCAL) {
            if (Utilities.isTargetMano(notification.getSiteOrManoIds(), osm) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
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

                    //Check if already present, uploaded for example from another project
                    String osmDescriptorId = getOsmDescriptorId(descriptorTemplate.getMetadata().getDescriptorId(), descriptorTemplate.getMetadata().getVersion());
                    if(osmDescriptorId == null)
                        osmDescriptorId = descriptorTemplate.getMetadata().getDescriptorId();
                    Optional<OsmInfoObject> osmInfoObjectOptional = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, descriptorTemplate.getMetadata().getVersion(), osm.getManoId());
                    String osmInfoObjectId;
                    if(!osmInfoObjectOptional.isPresent()) {
                        //OSM cannot permit to onboard descriptors with same ID but different versions
                        if(isTheFirstTranslationInformationEntry(descriptorTemplate.getMetadata().getDescriptorId()))
                            osmDescriptorId = descriptorTemplate.getMetadata().getDescriptorId();
                        else{
                            osmDescriptorId = UUID.randomUUID().toString();
                            descriptorTemplate.getMetadata().setDescriptorId(osmDescriptorId);
                            //Consider only one VNFNode present
                            descriptorTemplate.getTopologyTemplate().getVNFNodes().values().iterator().next().getProperties().setDescriptorId(osmDescriptorId);
                        }

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
                        String monitoringPath = Utilities.getMonitoringFromManifest(mf);

                        ArchiveBuilder archiver = new ArchiveBuilder(osmDir, logo);
                        File cloudInit = null;
                        if (cloudInitPath != null) {
                            cloudInit = new File(packagePath + "/" + cloudInitPath);
                        } else {
                            log.debug("{} - No cloud-init file found for VNF with ID {} and version {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion());
                        }
                        File monitoring = null;
                        if (monitoringPath != null) {
                            monitoring = new File(packagePath + "/" + monitoringPath);
                        } else {
                            log.debug("{} - No monitoring file found for VNF with ID {} and version {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion());
                        }
                        File archive = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue", cloudInit, monitoring);
                        osmInfoObjectId = onBoardVnfPackage(archive, notification.getOperationId().toString());
                        log.info("{} - Successfully uploaded Vnfd with ID {} and version {} for project {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
                    }else
                        osmInfoObjectId = osmInfoObjectOptional.get().getId();

                    translationInformationRepository.saveAndFlush(new TranslationInformation(notification.getVnfPkgInfoId(), osmInfoObjectId, notification.getVnfdId(), osmDescriptorId, notification.getVnfdVersion(), osm.getManoId()));
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
            log.info("{} - Received Sync Pkg onboarding notification for NSD with ID {} and version {} for project {} : {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(), notification.getOpStatus().toString());
            if(notification.getPluginId().equals(osm.getManoId()) && notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                String osmDescriptorId = getOsmDescriptorId(notification.getVnfdId(), notification.getVnfdVersion());
                if(osmDescriptorId == null)
                    osmDescriptorId = notification.getVnfdId();
                Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository.findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, notification.getVnfdVersion(), osm.getManoId());
                if(osmInfoObject.isPresent())
                    translationInformationRepository.saveAndFlush(new TranslationInformation(notification.getVnfPkgInfoId(), osmInfoObject.get().getId(), notification.getVnfdId(), osmInfoObject.get().getDescriptorId(), notification.getVnfdVersion(), osm.getManoId()));
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
            if (translationInformationContainsCatInfoId(notification.getVnfPkgInfoId()) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {
                try {
                    String osmInfoPkgId = getOsmInfoId(notification.getVnfPkgInfoId());
                    if(osmInfoPkgId == null)
                        throw new FailedOperationException("Could not find the corresponding Info ID in OSM");
                    if(!deleteTranslationInformationEntry(notification.getVnfPkgInfoId()))
                        throw new FailedOperationException("Could bot delete the specified entry");
                    if(!translationInformationContainsOsmInfoId(osmInfoPkgId))//If pkg is not present in other projects
                        deleteVnfd(osmInfoPkgId, notification.getOperationId().toString());
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
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - VNF Pkg deletion skipped",  osm.getManoId());
                    sendNotification(new VnfPkgDeletionNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            osm.getManoId()));
                }
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            log.info("{} - Received Sync Pkg deletion notification for NSD with ID {} and version {} for project {} : {}", osm.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(), notification.getOpStatus().toString());
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
        OSMHttpResponse httpResponse = osmClient.deleteNsd(nsdInfoId);
        parseResponse(httpResponse, opId, null);
        OsmInfoObject osmInfoObject =  osmInfoObjectRepository.findById(nsdInfoId).get();
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
        OSMHttpResponse httpResponse = osmClient.deleteVnfPackage(vnfdInfoId);
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

    private String getCatDescriptorId(String osmDescriptorId, String descriptorVersion){
        if (osmDescriptorId == null || descriptorVersion == null)
            return null;
        List<TranslationInformation> translationInformationList = translationInformationRepository.findByOsmDescriptorIdAndDescriptorVersionAndOsmManoId(osmDescriptorId, descriptorVersion, osm.getManoId());
        if(translationInformationList.size() != 0)
            return translationInformationList.get(0).getCatDescriptorId();
        return null;
    }


    private String getOsmDescriptorId(String catDescriptorId, String descriptorVersion){
        if (catDescriptorId == null || descriptorVersion == null)
            return null;
        List<TranslationInformation> translationInformationList = translationInformationRepository.findByCatDescriptorIdAndDescriptorVersionAndOsmManoId(catDescriptorId, descriptorVersion, osm.getManoId());
        if(translationInformationList.size() != 0)
            return translationInformationList.get(0).getOsmDescriptorId();
        return null;

    }

    private String getOsmInfoId(String catInfoId){
        if (catInfoId == null)
            return null;
        Optional<TranslationInformation> optionalTranslationInformation = translationInformationRepository.findByCatInfoIdAndOsmManoId(catInfoId, osm.getManoId());
        if(optionalTranslationInformation.isPresent())
            return optionalTranslationInformation.get().getOsmInfoId();
        return null;
    }

    private boolean translationInformationContainsCatInfoId(String catInfoId){
        if (catInfoId == null)
            return false;
        Optional<TranslationInformation> optionalTranslationInformation = translationInformationRepository.findByCatInfoIdAndOsmManoId(catInfoId, osm.getManoId());
        return optionalTranslationInformation.isPresent();
    }

    private boolean translationInformationContainsOsmInfoId(String osmInfoId){
        if (osmInfoId == null)
            return false;
        List<TranslationInformation> translationInformationList = translationInformationRepository.findByOsmInfoIdAndOsmManoId(osmInfoId, osm.getManoId());
        return translationInformationList.size() != 0;
    }

    private boolean deleteTranslationInformationEntry(String catInfoId){
        if (catInfoId == null)
            return false;
        Optional<TranslationInformation> optionalTranslationInformation = translationInformationRepository.findByCatInfoIdAndOsmManoId(catInfoId, osm.getManoId());
        if(optionalTranslationInformation.isPresent()) {
            translationInformationRepository.delete(optionalTranslationInformation.get());
            return true;
        }
        return false;
    }

    private boolean isTheFirstTranslationInformationEntry(String catDescriptorId){
        if (catDescriptorId == null)
            return true;
        List<TranslationInformation> translationInformationList = translationInformationRepository.findByCatDescriptorIdAndOsmManoId(catDescriptorId, osm.getManoId());
        if(translationInformationList.size() != 0) {
            for (TranslationInformation translationInformation : translationInformationList)
                if (translationInformation.getCatDescriptorId().equals(translationInformation.getOsmDescriptorId()))
                    return false;
        }
        return true;
    }
}
