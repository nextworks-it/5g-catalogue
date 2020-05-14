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
package it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin;

import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.*;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.*;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.onap.ONAP;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OnapPlugin extends MANOPlugin {

    private static final Logger log = LoggerFactory.getLogger(OnapPlugin.class);
    private Path onapDirPath;
    private File onapDir;
    private Path tmpDirPath;
    private final ONAP onap;
    private OnapDriver onapClient;
    private long syncPeriod;

    public OnapPlugin(MANOType manoType, MANO mano, String kafkaBootstrapServers, String localTopic, String remoteTopic,
                                      KafkaTemplate<String, String> kafkaTemplate, Path onapDirPath, Path tmpDir, boolean manoSync, long syncPeriod) {
        super(manoType, mano, kafkaBootstrapServers, localTopic, remoteTopic, kafkaTemplate, manoSync);
        if (MANOType.ONAP != manoType) {
            throw new IllegalArgumentException("ONAP plugin requires an ONAP type MANO");
        }
        this.onap = (ONAP) mano;
        this.onapDirPath = onapDirPath;
        this.syncPeriod = syncPeriod;
        this.tmpDirPath = tmpDir;
    }

    @Override
    public void init() {
        super.init();
        try {
            onapDirPath = Paths.get(onapDirPath.toString(), "/" + onap.getManoId());
            Files.createDirectories(onapDirPath);
            onapDir = onapDirPath.toFile();
        } catch (IOException e) {
            log.error("Could not initialize tmp directory: " + e.getMessage());
        }
    }

    private void initOnapConnection() {
        onapClient = new OnapDriver(onap.getIpAddress(), onap.getPort());
        log.info("{} - ONAP instance addr {}:{} connected", onap.getManoId(), onap.getIpAddress(), onap.getPort());

        if(isManoSync()) {
            log.info("{} - Starting runtime synchronization, sync period every {} minutes", onap.getManoId(), syncPeriod);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable syncTask = this::ONAPSynchronization;
            scheduler.scheduleAtFixedRate(syncTask, syncPeriod, syncPeriod, TimeUnit.MINUTES);
        }
    }

    @Override
    public Map<String, List<String>> getAllVnfd(String project){
        log.info("{} - Startup synchronization, started retrieving ONAP VNF Pkgs from project {}", onap.getManoId(), project);
        log.info("{} - Startup synchronization, finished retrieving ONAP VNF Pkgs from project {}", onap.getManoId(), project);

        return null;
    }

    @Override
    public Map<String, List<String>> getAllNsd(String project) {
        log.info("{} - Startup synchronization, started retrieving ONAP NS Pkgs from project {}", onap.getManoId(), project);
        log.info("{} - Startup synchronization, finished retrieving ONAP NS Pkgs from project {}", onap.getManoId(), project);

        return null;
    }

    @Override
    public KeyValuePair getTranslatedPkgPath(String descriptorId, String descriptorVersion, String project){
        log.info("{} - Translating ONAP Pkg with descriptor ID {} and version {} from project {}", onap.getManoId(), descriptorId, descriptorVersion, project);
        log.info("{} - Uploading TOSCA Pkg with descriptor ID {} and version {} to project {}", onap.getManoId(), descriptorId, descriptorVersion, project);

        return new KeyValuePair("pkgPath", PathType.LOCAL.toString());
    }

    @Override
    public void notifyOnboarding(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus){
        log.info("{} - Received Sync Pkg onboarding notification for Descriptor with ID {} and version {} for project {} : {}", onap.getManoId(), descriptorId, descriptorVersion, project, opStatus.toString());
    }

    @Override
    public void notifyDelete(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus){
        log.info("{} - Received Sync Pkg deletion notification for Descriptor with ID {} and version {} for project {} : {}", onap.getManoId(), descriptorId, descriptorVersion, project, opStatus.toString());
    }

    @Override
    public String getManoPkgInfoId(String cataloguePkgInfoId){
       return  null;
    }

    private void ONAPSynchronization(){
        log.info("{} - Runtime synchronization, started retrieving ONAP VNF and NS Pkgs", onap.getManoId());
        log.info("{} - Runtime synchronization, finished retrieving ONAP VNF and NS Pkgs", onap.getManoId());
    }

    private void updateDB() throws FailedOperationException{
        /*
        if(updateVNF) {
            log.info("{} - Updating VNFs DB for project {}", onap.getManoId());
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
        */
    }

    private String createVnfPkgTosca () throws MalformattedElementException, IllegalStateException, IOException, IllegalArgumentException{
        log.info("{} - Creating TOSCA VNF Descriptor with ID {} and version {}", onap.getManoId(), "descriptor_id", "descriptor_version");
        DescriptorTemplate vnfd = ToscaDescriptorsParser.generateVnfDescriptor();
        log.info("{} - Creating TOSCA VNF Pkg with descriptor ID {} and version {}", onap.getManoId(), "descriptor_id", "descriptor_version");
        return ToscaArchiveBuilder.createVNFCSAR();
    }

    private String createNsPkgTosca () throws MalformattedElementException, IllegalStateException, IOException, IllegalArgumentException{
        log.info("{} - Creating TOSCA NS Descriptor with ID {} and version {}", onap.getManoId(), "descriptor_id", "descriptor_version");
        DescriptorTemplate nsd = ToscaDescriptorsParser.generateNsDescriptor();
        log.info("{} - Creating TOSCA NS Pkg with descriptor ID {} and version {}", onap.getManoId(), "descriptor_id", "descriptor_version");
        return ToscaArchiveBuilder.createNSCSAR();
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL){
            log.info("{} - Received NSD onboarding notification for Nsd with ID {} and version {} for project {}", onap.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
            if(Utilities.isTargetMano(notification.getSiteOrManoIds(), onap)) {//No need to send notification back if the plugin is not in the target MANOs
                log.debug("{} - NSD onboarding skipped", onap.getManoId());
                sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(),
                        notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                        onap.getManoId(), null, null));
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            log.info("{} - Received Sync Pkg onboarding notification for NSD with ID {} and version {} for project {} : {}", onap.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(), notification.getOpStatus().toString());
            //TODO handle notification
        }
    }

    @Override
    public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) {
        log.debug("Body: {}", notification);
        log.info("{} - Received Nsd change notification", onap.getManoId());
    }

    @Override
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) {
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            log.info("{} - Received Nsd deletion notification for Nsd with ID {} and version {} for project {}", onap.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject());
            //Onboarding from NBI is not enabled, thus there is no need to delete the package and send notification
        } else if(notification.getScope() == ScopeType.SYNC){
            log.info("{} - Received Sync Pkg deletion notification for NSD with ID {} and version {} for project {} : {}", onap.getManoId(), notification.getNsdId(), notification.getNsdVersion(), notification.getProject(), notification.getOpStatus().toString());
            //TODO handle notification
        }
    }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) {
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            log.info("{} - Received Vnfd onboarding notification for Vnfd with ID {} and version {} for project {}", onap.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
            if (Utilities.isTargetMano(notification.getSiteOrManoIds(), onap)) {//No need to send notification back if the plugin is not in the target MANOs
                log.debug("{} - VNF Pkg onboarding skipped", onap.getManoId());
                sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(),
                        notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                        onap.getManoId(), null,null));
            }
        }else if(notification.getScope() == ScopeType.SYNC){
            log.info("{} - Received Sync Pkg onboarding notification for Vnfd with ID {} and version {} for project {} : {}", onap.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(), notification.getOpStatus().toString());
            //TODO handle notification
        }
    }

    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) {
        log.debug("Body: {}", notification);
        log.info("{} - Received VNF Pkg change notification", onap.getManoId());
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) {
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.LOCAL) {
            log.info("{} - Received Vnfd deletion notification for Vnfd with ID {} and version {} for project {}", onap.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject());
            //Onboarding from NBI is not enabled, thus there is no need to delete the package and send notification
        }else if(notification.getScope() == ScopeType.SYNC){
            log.info("{} - Received Sync Pkg deletion notification for Vnfd with ID {} and version {} for project {} : {}", onap.getManoId(), notification.getVnfdId(), notification.getVnfdVersion(), notification.getProject(), notification.getOpStatus().toString());
            //TODO handle notification
        }
    }

    @Override
    public void acceptPnfdOnBoardingNotification(PnfdOnBoardingNotificationMessage notification) {
        log.debug("Body: {}", notification);
        log.info("{} - Received PNFD onboarding notification", onap.getManoId());
    }

    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotificationMessage notification) {
        log.debug("Body: {}", notification);
        log.info("{} - Received PNFD deletion notification", onap.getManoId());
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
