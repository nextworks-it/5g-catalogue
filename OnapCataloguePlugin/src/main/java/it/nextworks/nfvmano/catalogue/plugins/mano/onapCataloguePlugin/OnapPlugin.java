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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.*;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.PluginOperationalState;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.*;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.onap.ONAP;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
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

public class OnapPlugin extends MANOPlugin {

    private static final Logger log = LoggerFactory.getLogger(OnapPlugin.class);
    private Path onapDirPath;
    private File onapDir;
    private Path tmpDirPath;
    private final ONAP onap;
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

    private void updateDB(boolean updateVNF, boolean updateNS, String project) throws FailedOperationException{
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
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) { }

    @Override
    public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) { }

    @Override
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) { }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) { }

    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) { }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) { }

    @Override
    public void acceptPnfdOnBoardingNotification(PnfdOnBoardingNotificationMessage notification) { }

    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotificationMessage notification) { }

    private File loadFile(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(filename);
        if (resource == null) {
            throw new IllegalArgumentException(String.format("Invalid resource %s", filename));
        }
        return new File(resource.getFile());
    }
}
