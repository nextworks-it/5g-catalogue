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
package it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.*;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.PluginOperationalState;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.*;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.fivegrowth.FIVEGROWTH;
import it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin.model.SoNsInfoObject;
import it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin.model.SoObject;
import it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin.model.SoVnfInfoObject;
import it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin.repos.FivegrowthObjectRepository;
import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.SoObjectType;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.ifa.catalogues.interfaces.messages.OnboardNsdRequest;
import it.nextworks.nfvmano.libs.ifa.descriptors.nsd.Nsd;
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

public class SOPlugin extends MANOPlugin {

    private static final Logger log = LoggerFactory.getLogger(SOPlugin.class);
    private Path soDirPath;
    private File soDir;
    private Path tmpDirPath;
    private final FIVEGROWTH so;
    private SODriver soClient;
    private long syncPeriod;

    private FivegrowthObjectRepository fivegrowthObjectRepository;

    private Long startSync;

    public SOPlugin(MANOType manoType, MANO mano, String kafkaBootstrapServers, String localTopic, String remoteTopic,
                    KafkaTemplate<String, String> kafkaTemplate, FivegrowthObjectRepository fivegrowthObjectRepository, Path soDirPath, Path tmpDir, boolean manoSync, long syncPeriod) {
        super(manoType, mano, kafkaBootstrapServers, localTopic, remoteTopic, kafkaTemplate, manoSync);
        if (MANOType.SO_5GROWTH != manoType) {
            throw new IllegalArgumentException("5GROWTH SO plugin requires a SO_5GROWTH type MANO");
        }
        this.so = (FIVEGROWTH) mano;
        this.soDirPath = soDirPath;
        this.syncPeriod = syncPeriod;
        this.tmpDirPath = tmpDir;
        this.fivegrowthObjectRepository = fivegrowthObjectRepository;
    }

    @Override
    public void init() {
        super.init();
        try {
            soDirPath = Paths.get(soDirPath.toString(), "/" + so.getManoId());
            Files.createDirectories(soDirPath);
            soDir = soDirPath.toFile();
        } catch (IOException e) {
            log.error("Could not initialize tmp directory: " + e.getMessage());
        }
        initSoConnection();
    }

    private void initSoConnection() {
        soClient = new SODriver(so.getIpAddress(), so.getPort());
        log.info("{} - 5GROWTH SO instance addr {}:{} connected", so.getManoId(), so.getIpAddress(), so.getPort());

        if(isManoSync()) {
            log.info("{} - Starting runtime synchronization, sync period every {} minutes", so.getManoId(), syncPeriod);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable syncTask = this::RuntimeSynchronization;
            scheduler.scheduleAtFixedRate(syncTask, syncPeriod, syncPeriod, TimeUnit.MINUTES);
        }
    }

    @Override
    public Map<String, List<String>> getAllVnfd(String project){
        log.info("{} - Startup synchronization, started retrieving 5GROWTH SO Vnf Pkgs from project {}", so.getManoId(), project);
        /*
        this.startSync = Instant.now().getEpochSecond();

        try {
            updateDB();
        } catch(FailedOperationException e){
            log.error("{} - {}", so.getManoId(), e.getMessage());
            return null;
        }
        */

        //Delete ONAP Pkg no longer present in ONAP and add to ids list the others
        Map<String, List<String>> ids = new HashMap<>();
        /*
        List<OnapObject> onapObjectList = fivegrowthObjectRepository.findByOnapIdAndType(onap.getManoId(), OnapObjectType.VNF);
        for(OnapObject onapObject : onapObjectList){
            if(onapObject.getEpoch().compareTo(startSync) < 0){
                log.info("{} - Onap Vnf Pkg with descriptor ID {} and version {} no longer present in project {}", onap.getManoId(), onapObject.getDescriptorId(), onapObject.getVersion(), project);
                fivegrowthObjectRepository.delete(onapObject);
            }else{
                ids.computeIfAbsent(onapObject.getDescriptorId(), k -> new ArrayList<>()).add(onapObject.getVersion());//ids.put(onapObject.getDescriptorId(), onapObject.getVersion());
            }
        }

        log.info("{} - Startup synchronization, finished retrieving ONAP Vnf Pkgs from project {}", onap.getManoId(), project);
        */
        return ids;
    }

    @Override
    public Map<String, List<String>> getAllNsd(String project) {
        log.info("{} - Startup synchronization, started retrieving 5GROWTH SO Ns Pkgs from project {}", so.getManoId(), project);
        //Delete Onap Pkg no longer present in Onap and add to ids list the others
        Map<String, List<String>> ids = new HashMap<>();
        /*
        List<OnapObject> onapObjectList = fivegrowthObjectRepository.findByOnapIdAndType(onap.getManoId(), OnapObjectType.NS);
        for(OnapObject onapObject : onapObjectList){
            if(onapObject.getEpoch().compareTo(this.startSync) < 0){
                log.info("{} - Onap Ns Pkg with descriptor ID {} and version {} no longer present in project {}", onap.getManoId(), onapObject.getDescriptorId(), onapObject.getVersion(), project);
                fivegrowthObjectRepository.delete(onapObject);
            }else{
                ids.computeIfAbsent(onapObject.getDescriptorId(), k -> new ArrayList<>()).add(onapObject.getVersion());//ids.put(onapObject.getDescriptorId(), onapObject.getVersion());
            }
        }
        log.info("{} - Startup synchronization, finished retrieving ONAP Ns Pkgs from project {}", onap.getManoId(), project);
        */
        return ids;
    }

    @Override
    public KeyValuePair getTranslatedPkgPath(String descriptorId, String descriptorVersion, String project){
        log.info("{} - Translating 5GROWTH SO Pkg with descriptor ID {} and version {} from project {}", so.getManoId(), descriptorId, descriptorVersion, project);
        String pkgPath = null;
        /*
        Optional<OnapObject> onapObject = fivegrowthObjectRepository.findByDescriptorIdAndVersionAndOnapId(descriptorId, descriptorVersion, onap.getManoId());

        try{
            if(onapObject.isPresent()){
                if(onapObject.get().getType().equals(OnapObjectType.VNF))
                    pkgPath = createVnfPkgTosca(onapObject.get());
                else
                    pkgPath = createNsPkgTosca(onapObject.get());
            }
        }catch(Exception e){
            log.error("{} - Unable to generate TOSCA Pkg with descriptor ID {} and version {} for project {}: {}", onap.getManoId(), descriptorId, descriptorVersion, project, e.getMessage());
            log.debug(null, e);
            return  null;
        }

        log.info("{} - Uploading TOSCA Pkg with descriptor ID {} and version {} to project {}", onap.getManoId(), descriptorId, descriptorVersion, project);
        */
        return new KeyValuePair(pkgPath, PathType.LOCAL.toString());
    }

    @Override
    public void notifyOnboarding(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus){
        log.info("{} - Received Sync Pkg onboarding notification for Descriptor with ID {} and version {} for project {} : {}", so.getManoId(), descriptorId, descriptorVersion, project, opStatus.toString());
        /*
        Optional<OnapObject> onapObjectOptional = fivegrowthObjectRepository.findByDescriptorIdAndVersionAndOnapId(descriptorId, descriptorVersion, onap.getManoId());
        if(onapObjectOptional.isPresent()){
            OnapObject onapObject = onapObjectOptional.get();
            if(opStatus.equals(OperationStatus.SUCCESSFULLY_DONE)){
                onapObject.setCatalogueId(infoId);
                fivegrowthObjectRepository.saveAndFlush(onapObject);
            }
        }
        */
    }

    @Override
    public void notifyDelete(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus){
        log.info("{} - Received Sync Pkg deletion notification for Descriptor with ID {} and version {} for project {} : {}", so.getManoId(), descriptorId, descriptorVersion, project, opStatus.toString());
    }

    @Override
    public String getManoPkgInfoId(String cataloguePkgInfoId){
        Optional<SoObject> soObjectOptional = fivegrowthObjectRepository.findByCatalogueIdAndSoId(cataloguePkgInfoId, so.getManoId());
        return soObjectOptional.map(SoObject::getDescriptorId).orElse(null);
    }

    @Override
    public void RuntimeSynchronization(){
        log.info("{} - Runtime synchronization, started retrieving 5GROWTH SO Vnf and Ns Pkgs", so.getManoId());
        /*
        Long startSync = Instant.now().getEpochSecond();

        List<OnapObject> oldOnapObjectList = fivegrowthObjectRepository.findByOnapId(onap.getManoId());
        List<String> oldOnapIdList = oldOnapObjectList.stream().map(OnapObject::getDescriptorId).collect(Collectors.toList());

        try {
            updateDB();
        } catch(FailedOperationException e){
            log.error("{} - {}", onap.getManoId(), e.getMessage());
            return;
        }

        List<OnapObject> onapVnfList = fivegrowthObjectRepository.findByOnapIdAndType(onap.getManoId(), OnapObjectType.VNF);
        List<OnapObject> onapNsList = fivegrowthObjectRepository.findByOnapIdAndType(onap.getManoId(), OnapObjectType.NS);
        UUID operationId;
        String pkgPath;

        for(OnapObject onapVnf : onapVnfList) {
            //upload new Vnf Pkgs
            if(!oldOnapIdList.contains(onapVnf.getDescriptorId())){
                operationId = UUID.randomUUID();
                try {
                    pkgPath = createVnfPkgTosca(onapVnf);
                }catch(Exception e){
                    log.error("{} - Unable to generate TOSCA Vnf Pkg with descriptor ID {} and version {}: {}", onap.getManoId(), onapVnf.getDescriptorId(), onapVnf.getVersion(), e.getMessage());
                    log.debug(null, e);
                    continue;
                }
                log.info("{} - Uploading TOSCA Vnf Pkg with descriptor ID {} and version {}", onap.getManoId(), onapVnf.getDescriptorId(), onapVnf.getVersion());
                sendNotification(new VnfPkgOnBoardingNotificationMessage(null, onapVnf.getDescriptorId(), onapVnf.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, onap.getManoId(), null, new KeyValuePair(pkgPath, PathType.LOCAL.toString())));
            }
        }

        for(OnapObject onapNs : onapNsList){
            //upload new Ns Pkgs
            if(!oldOnapIdList.contains(onapNs.getDescriptorId())){
                operationId = UUID.randomUUID();
                try {
                    pkgPath = createNsPkgTosca(onapNs);
                }catch(Exception e){
                    log.error("{} - Unable to generate TOSCA Ns Pkg with descriptor ID {} and version {}: {}", onap.getManoId(), onapNs.getDescriptorId(), onapNs.getVersion(), e.getMessage());
                    log.debug(null, e);
                    continue;
                }
                log.info("{} - Uploading TOSCA Ns Pkg with descriptor ID {} and version {}", onap.getManoId(), onapNs.getDescriptorId(), onapNs.getVersion());
                sendNotification(new NsdOnBoardingNotificationMessage(null, onapNs.getDescriptorId(), onapNs.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, onap.getManoId(), null, new KeyValuePair(pkgPath, PathType.LOCAL.toString())));
            }
            //Delete Onap Ns Pkg no longer present
            if(onapNs.getEpoch().compareTo(startSync) < 0){
                log.info("{} - Onap Ns Pkg with descriptor ID {} and version {} no longer present, deleting it", onap.getManoId(), onapNs.getDescriptorId(), onapNs.getVersion());
                operationId = UUID.randomUUID();
                sendNotification(new NsdDeletionNotificationMessage(null, onapNs.getDescriptorId(), onapNs.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, onap.getManoId(), null));
                fivegrowthObjectRepository.delete(onapNs);
            }
        }

        for(OnapObject onapVnf : onapVnfList) {
            //Delete Onap Vnf Pkg no longer present
            if(onapVnf.getEpoch().compareTo(startSync) < 0){
                log.info("{} - Onap Vnf Pkg with descriptor ID {} and version {} no longer present, deleting it", onap.getManoId(), onapVnf.getDescriptorId(), onapVnf.getVersion());
                operationId = UUID.randomUUID();
                sendNotification(new VnfPkgDeletionNotificationMessage(null, onapVnf.getDescriptorId(), onapVnf.getVersion(), "all",
                            operationId, ScopeType.SYNC, OperationStatus.SENT, onap.getManoId(), null));
                fivegrowthObjectRepository.delete(onapVnf);
            }
        }
        */
        log.info("{} - Runtime synchronization, finished retrieving 5GROWTH SO Vnf and Ns Pkgs", so.getManoId());
    }

    private void updateDB(boolean updateVNF, boolean updateNS) throws FailedOperationException{
        if(!updateVNF && !updateNS)
            return;

        List<SoVnfInfoObject> vnfPkgs = new ArrayList<>();
        List<SoNsInfoObject> nsPkgs = new ArrayList<>();
        Optional<SoObject> infoObjectOptional;
        SoObject infoObject;

        if(updateVNF) {
            log.info("{} - Updating Vnfs DB", so.getManoId());
            //Retrieve VNFPkgInfos
            vnfPkgs = soClient.queryVnfPackagesInfo();
        }
        if(updateNS){
            log.info("{} - Updating Nss DB", so.getManoId());
            //Retrieve NSPkgInfos
            nsPkgs = soClient.queryNsds();
        }

        for(SoVnfInfoObject vnfInfoObject : vnfPkgs){
            try {
                String vnfdId = vnfInfoObject.getVnfd().getVnfdId();
                String vnfdVersion = vnfInfoObject.getVnfd().getVnfdVersion();
                infoObjectOptional = fivegrowthObjectRepository.findByDescriptorIdAndVersionAndSoId(vnfdId, vnfdVersion, so.getManoId());
                if (infoObjectOptional.isPresent()) {
                    log.info("{} - Vnf Pkg with descriptor ID {} and version {} already present", so.getManoId(), vnfdId, vnfdVersion);
                    infoObject = infoObjectOptional.get();
                    infoObject.setEpoch(Instant.now().getEpochSecond());
                }else{
                    log.info("{} - Found new Vnf Pkg with descriptor ID {} and version {}", so.getManoId(), vnfdId, vnfdVersion);
                    infoObject = new SoObject();
                    infoObject.setVersion(vnfdVersion);
                    infoObject.setDescriptorId(vnfdId);
                    infoObject.setSoId(so.getManoId());
                    infoObject.setType(SoObjectType.VNF);
                    infoObject.setEpoch(Instant.now().getEpochSecond());
                }
                fivegrowthObjectRepository.saveAndFlush(infoObject);
            }catch (IllegalStateException | IllegalArgumentException e) {
                log.error("{} - Sync error: {}", so.getManoId(), e.getMessage());
                log.debug(null, e);
            }
        }

        for(SoNsInfoObject nsInfoObject : nsPkgs){
            try {
                String nsdId = nsInfoObject.getNsd().getNsdIdentifier();
                String nsdVersion = nsInfoObject.getNsd().getVersion();
                infoObjectOptional = fivegrowthObjectRepository.findByDescriptorIdAndVersionAndSoId(nsdId, nsdVersion, so.getManoId());
                if (infoObjectOptional.isPresent()) {
                    log.info("{} - Vnf Pkg with descriptor ID {} and version {} already present", so.getManoId(), nsdId, nsdVersion);
                    infoObject = infoObjectOptional.get();
                    infoObject.setEpoch(Instant.now().getEpochSecond());
                }else{
                    log.info("{} - Found new Vnf Pkg with descriptor ID {} and version {}", so.getManoId(), nsdId, nsdVersion);
                    infoObject = new SoObject();
                    infoObject.setVersion(nsdVersion);
                    infoObject.setDescriptorId(nsdId);
                    infoObject.setSoId(so.getManoId());
                    infoObject.setType(SoObjectType.NS);
                    infoObject.setEpoch(Instant.now().getEpochSecond());
                }
                fivegrowthObjectRepository.saveAndFlush(infoObject);
            }catch (IllegalStateException | IllegalArgumentException e) {
                log.error("{} - Sync error: {}", so.getManoId(), e.getMessage());
                log.debug(null, e);
            }
        }
    }

    private String createVnfPkgTosca (SoObject soObject) throws MalformattedElementException, IllegalStateException, IOException, IllegalArgumentException{
        log.info("{} - Creating TOSCA VNF Descriptor with ID {} and version {}", so.getManoId(), soObject.getDescriptorId(), soObject.getVersion());
        DescriptorTemplate vnfd = ToscaDescriptorsParser.generateVnfDescriptor(soObject.getPath());//TODO
        log.info("{} - Creating TOSCA VNF Pkg with descriptor ID {} and version {}", so.getManoId(), soObject.getDescriptorId(), soObject.getVersion());
        return ToscaArchiveBuilder.createVNFCSAR(vnfd, tmpDirPath.toString());
    }

    private String createNsPkgTosca (SoObject soObject) throws MalformattedElementException, IllegalStateException, IOException, IllegalArgumentException{
        log.info("{} - Creating TOSCA NS Descriptor with ID {} and version {}", so.getManoId(), soObject.getDescriptorId(), soObject.getVersion());
        DescriptorTemplate nsd = ToscaDescriptorsParser.generateNsDescriptor(soObject.getPath());//TODO
        log.info("{} - Creating TOSCA NS Pkg with descriptor ID {} and version {}", so.getManoId(), soObject.getDescriptorId(), soObject.getVersion());
        return ToscaArchiveBuilder.createNSCSAR(nsd, tmpDirPath.toString());
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        try {
            String json = mapper.writeValueAsString(notification);
            log.debug("RECEIVED MESSAGE: " + json);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse received nsdOnboardingNotificationMessage: " + e.getMessage());
        }

        if (notification.getScope() == ScopeType.LOCAL ){
            String nsdInfoId = notification.getNsdInfoId();
            log.info("{} - Received NSD onboarding notification for Nsd with ID {} and version {} for project {}", so.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
            /*boolean forceOnboard = false;
            if(nsdInfoId.endsWith("_update")){
                forceOnboard = true;
                nsdInfoId = nsdInfoId.replace("_update", "");
            }*/
            if(Utilities.isTargetMano(notification.getSiteOrManoIds(), so) && this.getPluginOperationalState() == PluginOperationalState.ENABLED) {//No need to send notification back if the plugin is not in the target MANOs
                try{
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

                    mapper = new ObjectMapper(new YAMLFactory());
                    DescriptorTemplate descriptorTemplate = mapper.readValue(descriptor, DescriptorTemplate.class);

                    //Check if already present, uploaded for example from another project
                    String descriptorId = descriptorTemplate.getMetadata().getDescriptorId();
                    Optional<SoObject> soInfoObjectOptional = fivegrowthObjectRepository.findByDescriptorIdAndVersionAndSoId(descriptorId, descriptorTemplate.getMetadata().getVersion(), so.getManoId());
                    String soInfoObjectId;
                    if(!soInfoObjectOptional.isPresent() /*|| forceOnboard*/){//TODO forceOnboard used in RuntimeNsChange
                        NsdBuilder nsdBuilder = new NsdBuilder();
                        List<DescriptorTemplate> includedVnfds = new ArrayList<>();
                        /*
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
                        */

                        Nsd nsd = nsdBuilder.parseDescriptorTemplate(descriptorTemplate);
                        OnboardNsdRequest onboardNsdRequest = new OnboardNsdRequest(nsd, null);
                        soInfoObjectId = soClient.onboardNsd(onboardNsdRequest);

                        SoObject infoObject = new SoObject();
                        infoObject.setVersion(nsd.getVersion());
                        infoObject.setDescriptorId(nsd.getNsdIdentifier());
                        infoObject.setSoId(so.getManoId());
                        infoObject.setType(SoObjectType.NS);
                        infoObject.setEpoch(Instant.now().getEpochSecond());
                        infoObject.setSoId(soInfoObjectId);
                        infoObject.setCatalogueId(nsdInfoId);
                        fivegrowthObjectRepository.saveAndFlush(infoObject);
                        log.info("{} - Successfully uploaded Nsd with ID {} and version {} for project {}", so.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
                    }

                    sendNotification(new NsdOnBoardingNotificationMessage(nsdInfoId, notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            so.getManoId(), null,null));
                }catch (Exception e) {
                    log.error("{} - Could not onboard Nsd: {}", so.getManoId(), e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new NsdOnBoardingNotificationMessage(nsdInfoId, notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            so.getManoId(), null,null));
                }
            } else {
                if (this.getPluginOperationalState() == PluginOperationalState.DISABLED || this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - NSD onboarding skipped", so.getManoId());
                    sendNotification(new NsdOnBoardingNotificationMessage(nsdInfoId, notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            so.getManoId(), null,null));
                }
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            log.info("{} - Received Sync Pkg onboarding notification for NSD with ID {} and version {} for project {} : {}", so.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(), notification.getOpStatus().toString());
            if(notification.getPluginId().equals(so.getManoId())) {
                Optional<SoObject> soObjectOptional = fivegrowthObjectRepository.findByDescriptorIdAndVersionAndSoId(notification.getNsdId(), notification.getNsdVersion(), so.getManoId());
                if (soObjectOptional.isPresent()) {
                    SoObject soObject = soObjectOptional.get();
                    if (notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                        soObject.setCatalogueId(notification.getNsdInfoId());
                        fivegrowthObjectRepository.saveAndFlush(soObject);
                    }
                }
            }
        }
    }

    @Override
    public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) {
        log.debug("Body: {}", notification);
        log.info("{} - Received Nsd change notification", so.getManoId());
    }

    @Override
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) {
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            log.info("{} - Received Nsd deletion notification for Nsd with ID {} and version {} for project {}", so.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
            //Onboarding from NBI is not enabled, thus there is no need to delete the package and send notification
        } else if(notification.getScope() == ScopeType.SYNC){
            log.info("{} - Received Sync Pkg deletion notification for NSD with ID {} and version {} for project {} : {}", so.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(), notification.getOpStatus().toString());
            //TODO handle notification
        }
    }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) {
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            log.info("{} - Received Vnfd onboarding notification for Vnfd with ID {} and version {} for project {}", so.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
            if (Utilities.isTargetMano(notification.getSiteOrManoIds(), so)) {//No need to send notification back if the plugin is not in the target MANOs
                log.debug("{} - VNF Pkg onboarding skipped", so.getManoId());
                sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(),
                        notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                        so.getManoId(), null,null));
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            log.info("{} - Received Sync Pkg onboarding notification for Vnfd with ID {} and version {} for project {} : {}", so.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(), notification.getOpStatus().toString());
            if(notification.getPluginId().equals(so.getManoId())) {
                Optional<SoObject> soObjectOptional = fivegrowthObjectRepository.findByDescriptorIdAndVersionAndSoId(notification.getVnfdId(), notification.getVnfdVersion(), so.getManoId());
                if (soObjectOptional.isPresent()) {
                    SoObject soObject = soObjectOptional.get();
                    if (notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                        soObject.setCatalogueId(notification.getVnfPkgInfoId());
                        fivegrowthObjectRepository.saveAndFlush(soObject);
                    }
                }
            }
        }
    }

    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) {
        log.debug("Body: {}", notification);
        log.info("{} - Received VNF Pkg change notification", so.getManoId());
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) {
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            log.info("{} - Received Vnfd deletion notification for Vnfd with ID {} and version {} for project {}", so.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
            //Onboarding from NBI is not enabled, thus there is no need to delete the package and send notification
        }else if(notification.getScope() == ScopeType.SYNC){
            log.info("{} - Received Sync Pkg deletion notification for Vnfd with ID {} and version {} for project {} : {}", so.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(), notification.getOpStatus().toString());
            //TODO handle notification
        }
    }

    @Override
    public void acceptPnfdOnBoardingNotification(PnfdOnBoardingNotificationMessage notification) {
        log.debug("Body: {}", notification);
        log.info("{} - Received PNFD onboarding notification", so.getManoId());
    }

    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotificationMessage notification) {
        log.debug("Body: {}", notification);
        log.info("{} - Received PNFD deletion notification", so.getManoId());
    }

    private File loadFile(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(filename);
        if (resource == null) {
            throw new IllegalArgumentException(String.format("Invalid resource %s", filename));
        }
        return new File(resource.getFile());
    }
}
