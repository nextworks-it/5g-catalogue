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
package it.nextworks.nfvmano.catalogue.engine;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.auth.AuthUtilities;
import it.nextworks.nfvmano.catalogue.auth.projectmanagement.ProjectResource;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.*;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.common.ConfigurationParameters;
import it.nextworks.nfvmano.catalogue.engine.elements.ContentType;
import it.nextworks.nfvmano.catalogue.engine.elements.NsdIdInvariantIdMapping;
import it.nextworks.nfvmano.catalogue.engine.elements.NsdIdInvariantIdMappingRepository;
import it.nextworks.nfvmano.catalogue.engine.resources.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.PackageOnboardingStateType;
import it.nextworks.nfvmano.catalogue.plugins.PluginsManager;
import it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.C2COnboardingStateType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.PluginType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.repos.MANORepository;
import it.nextworks.nfvmano.catalogue.repos.*;
import it.nextworks.nfvmano.catalogue.repos.mec0102.AppPackageInfoResourceRepository;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.ArchiveParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.elements.CSARInfo;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.enums.UsageState;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.pnfd.nodes.PNF.PNFNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.mec.catalogues.descriptors.appd.Appd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static it.nextworks.nfvmano.catalogue.engine.Utilities.*;

@Service
public class NsdManagementService implements NsdManagementInterface {

    private static final Logger log = LoggerFactory.getLogger(NsdManagementService.class);

    @Autowired
    private NsdInfoRepository nsdInfoRepo;

    @Autowired
    private NsdIdInvariantIdMappingRepository nsdIdInvariantIdMappingRepository;

    @Autowired
    private PnfdInfoRepository pnfdInfoRepo;

    @Autowired
    private AppPackageInfoResourceRepository appPackageInfoResourceRepository;

    @Autowired
    private FileSystemStorageService storageService;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private MANORepository MANORepository;

    @Autowired
    private PluginsManager pluginManger;

    @Autowired
    private VnfPackageManagementService vnfPackageManagementService;

    @Autowired
    private AppdManagementService appdManagementService;

    @Autowired
    private VnfPkgInfoRepository vnfPkgInfoRepository;

    @Autowired
    private DescriptorsParser descriptorsParser;

    @Autowired
    private ArchiveParser archiveParser;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Value("${catalogue.storageRootDir}")
    private String rootDir;

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    private Map<String, Map<String, NotificationResource>> operationIdToConsumersAck = new HashMap<>();

    public NsdManagementService() {
    }

    @Override
    public void startupSync(){
        log.info("Starting MANO NS synchronization");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        for (MANOPlugin manoPlugin : pluginManger.manoDrivers.values()) {
            List<ProjectResource> projectResourceList = projectRepository.findAll();
            for (ProjectResource project : projectResourceList) {
                Map<String, List<String>> nsFromManoList = manoPlugin.getAllNsd(project.getProjectId());
                if (nsFromManoList == null) {
                    log.error("Project " + project.getProjectId() + " - Cannot retrieve NSDs list from MANO with ID " + manoPlugin.getPluginId());
                    continue;
                }
                List<NsdInfoResource> nsdInfoResourceList = nsdInfoRepo.findByProjectId(project.getProjectId());
                for (Map.Entry<String, List<String>> nsFromMano : nsFromManoList.entrySet()) {
                    String nsdId = nsFromMano.getKey();
                    for(String nsdVersion : nsFromMano.getValue()) {
                        if (!Utilities.isUUID(nsdId)) {
                            log.error("Project " + project.getProjectId() + " - Cannot retrieve NSD with ID " + nsdId + " and version " + nsdVersion + " from MANO with ID " + manoPlugin.getPluginId() + ", ID is not in UUID format");
                            continue;
                        }
                        Optional<NsdInfoResource> optionalNsdInfoResource = nsdInfoRepo.findByNsdIdAndNsdVersionAndProjectId(UUID.fromString(nsdId), nsdVersion, project.getProjectId());
                        if (!optionalNsdInfoResource.isPresent()) {
                            if (manoPlugin.getManoType().toString().startsWith("OSM")) {//Avoid sync among projects of NSs uploaded from NBI, OSM plugin doesn't handle projects
                                boolean isRetrievedFromMano = true;
                                List<NsdInfoResource> infoResources = nsdInfoRepo.findByNsdIdAndNsdVersion(UUID.fromString(nsdId), nsdVersion);
                                for (NsdInfoResource infoResource : infoResources)
                                    if (!infoResource.isRetrievedFromMANO()) {
                                        isRetrievedFromMano = false;
                                        break;
                                    }
                                if (!isRetrievedFromMano)
                                    continue;
                            }
                            log.info("Project {} - Uploading NSD with ID {} and version {} retrieved from MANO with ID {}", project.getProjectId(), nsdId, nsdVersion, manoPlugin.getPluginId());
                            KeyValuePair nsPath = manoPlugin.getTranslatedPkgPath(nsdId, nsdVersion, project.getProjectId());
                            if (nsPath == null) {
                                log.error("Project " + project.getProjectId() + " - Cannot retrieve NSD with ID " + nsdId + " and version " + nsdVersion + " from MANO with ID " + manoPlugin.getPluginId());
                                continue;
                            }
                            CreateNsdInfoRequest request = new CreateNsdInfoRequest();
                            KeyValuePairs userDefinedData = new KeyValuePairs();
                            userDefinedData.put("isRetrievedFromMANO", "yes");
                            userDefinedData.put(manoPlugin.getPluginId(), "yes");
                            request.setUserDefinedData(userDefinedData);
                            try {
                                NsdInfo nsdInfo = createNsdInfo(request, project.getProjectId(), true);
                                File nsd;
                                if (nsPath.getValue().equals(PathType.LOCAL.toString())) {
                                    nsd = new File(nsPath.getKey());
                                } else {
                                    log.error("Path Type not currently supported");
                                    continue;
                                }
                                MultipartFile multipartFile = Utilities.createMultiPartFromFile(nsd);
                                uploadNsd(nsdInfo.getId().toString(), multipartFile, ContentType.ZIP, false, project.getProjectId());
                                updateNsdInfoOperationStatus(nsdInfo.getId().toString(), manoPlugin.getPluginId(), OperationStatus.SUCCESSFULLY_DONE, CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION);
                                manoPlugin.notifyOnboarding(nsdInfo.getId().toString(), nsdId, nsdVersion, project.getProjectId(), OperationStatus.SUCCESSFULLY_DONE);
                            } catch (Exception e) {
                                log.error(e.getMessage());
                                log.debug(null, e);
                                manoPlugin.notifyOnboarding(null, nsdId, nsdVersion, project.getProjectId(), OperationStatus.FAILED);
                            }
                        } else {
                            log.info("Project {} - NSD with ID {} and version {} retrieved from MANO with ID {} is already present", project.getProjectId(), nsdId, nsdVersion, manoPlugin.getPluginId());
                            NsdInfo nsdInfo = buildNsdInfo(optionalNsdInfoResource.get());
                            if (nsdInfo.getManoIdToOnboardingStatus().get(manoPlugin.getPluginId()) == null || !nsdInfo.getManoIdToOnboardingStatus().get(manoPlugin.getPluginId()).equals(NsdOnboardingStateType.ONBOARDED)) {
                                try {
                                    optionalNsdInfoResource.get().getUserDefinedData().put(manoPlugin.getPluginId(), "yes");
                                    nsdInfoRepo.saveAndFlush(optionalNsdInfoResource.get());
                                    updateNsdInfoOperationStatus(nsdInfo.getId().toString(), manoPlugin.getPluginId(), OperationStatus.SUCCESSFULLY_DONE, CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION);
                                    manoPlugin.notifyOnboarding(nsdInfo.getId().toString(), nsdId, nsdVersion, project.getProjectId(), OperationStatus.SUCCESSFULLY_DONE);
                                } catch (NotExistingEntityException e) {
                                    log.error(e.getMessage());
                                    log.debug(null, e);
                                    manoPlugin.notifyOnboarding(null, nsdId, nsdVersion, project.getProjectId(), OperationStatus.FAILED);
                                }
                            }
                            nsdInfoResourceList.removeIf(t -> t.getId().equals(optionalNsdInfoResource.get().getId()));
                        }
                    }
                }
                for (NsdInfoResource nsdInfoResource : nsdInfoResourceList) {
                    if(nsdInfoResource.getAcknowledgedOnboardOpConsumers().get(manoPlugin.getPluginId()) != null && nsdInfoResource.getAcknowledgedOnboardOpConsumers().get(manoPlugin.getPluginId()).getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                        log.info("Project {} - NSD with ID {} and version {} no longer present in MANO with ID {}", project.getProjectId(), nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion(), manoPlugin.getPluginId());
                        if (nsdInfoResource.isRetrievedFromMANO()) {
                            try {
                                updateNsdInfoOperationStatus(nsdInfoResource.getId().toString(), manoPlugin.getPluginId(), OperationStatus.RECEIVED, CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION);
                                Optional<NsdInfoResource> targetNsdInfoResource = nsdInfoRepo.findById(nsdInfoResource.getId());
                                if (targetNsdInfoResource.isPresent() && !targetNsdInfoResource.get().getNsdOnboardingState().equals(NsdOnboardingStateType.ONBOARDED)) {
                                    log.info("Project {} - Going to delete NSD with ID {} and version {}", project.getProjectId(), nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion());
                                    NsdInfoModifications request = new NsdInfoModifications();
                                    request.setNsdOperationalState(NsdOperationalStateType.DISABLED);
                                    updateNsdInfo(request, nsdInfoResource.getId().toString(), project.getProjectId(), true);
                                    deleteNsdInfo(nsdInfoResource.getId().toString(), project.getProjectId(), true);
                                    manoPlugin.notifyDelete(nsdInfoResource.getId().toString(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), project.getProjectId(), OperationStatus.SUCCESSFULLY_DONE);
                                }else{
                                    manoPlugin.notifyDelete(nsdInfoResource.getId().toString(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), project.getProjectId(), OperationStatus.SUCCESSFULLY_DONE);
                                    targetNsdInfoResource.get().getUserDefinedData().put(manoPlugin.getPluginId(), "no");
                                }
                            }catch (NotPermittedOperationException e){
                                log.error("Project {} - Failed to delete NSD with ID {} and version {}, it is not deletable", project.getProjectId(), nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion());
                                manoPlugin.notifyDelete(nsdInfoResource.getId().toString(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), project.getProjectId(), OperationStatus.FAILED);
                                log.info("Project {} - Sending NS Onboarding notification for NSD with ID {} and version {} to MANO with ID {}", project.getProjectId(), nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion(), manoPlugin.getPluginId());
                                try {
                                    List<String> nsdFilenames = nsdInfoResource.getNsdFilename();
                                    if (nsdFilenames.size() != 1) {
                                        log.error("Found zero or more than one file for NSD in YAML format. Error");
                                        throw new FailedOperationException("Found more than one file for NSD in YAML format. Error");
                                    }
                                    String nsdFilename = nsdFilenames.get(0);
                                    Resource dtFile = FileSystemStorageService.loadFileAsResource(project.getProjectId(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), nsdFilename, false);
                                    DescriptorTemplate dt = mapper.readValue(dtFile.getFile(), DescriptorTemplate.class);
                                    Map<String, KeyValuePair> includedVnfds = checkVNFPkgs(dt, project.getProjectId());
                                    Map<String, KeyValuePair> includedPnfds = checkPNFDs(dt, project.getProjectId());
                                    List<String> siteOrManoIds = new ArrayList<>();
                                    siteOrManoIds.add(manoPlugin.getPluginId());
                                    NsdOnBoardingNotificationMessage msg =
                                            new NsdOnBoardingNotificationMessage(nsdInfoResource.getId().toString(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), project.getProjectId(), UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageNsdsSubfolder + "/" + project.getProjectId() + "/" + nsdInfoResource.getNsdId() + "/" + nsdInfoResource.getNsdVersion(), PathType.LOCAL.toString()));
                                    msg.setIncludedVnfds(includedVnfds);
                                    msg.setIncludedPnfds(includedPnfds);
                                    notificationManager.sendNsdOnBoardingNotification(msg);
                                } catch (Exception e1) {
                                    log.error(e1.getMessage());
                                    log.debug(null, e1);
                                }
                            } catch (Exception e) {
                                log.error("Project {} - Failed to delete NSD with ID {} and version {}", project.getProjectId(), nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion());
                                log.debug(null, e);
                                manoPlugin.notifyDelete(nsdInfoResource.getId().toString(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), project.getProjectId(), OperationStatus.FAILED);
                            }
                        } else {
                            log.info("Project {} - Sending NS Onboarding notification for NSD with ID {} and version {} to MANO with ID {}", project.getProjectId(), nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion(), manoPlugin.getPluginId());
                            manoPlugin.notifyDelete(nsdInfoResource.getId().toString(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), project.getProjectId(), OperationStatus.FAILED);
                            try{
                                List<String> nsdFilenames = nsdInfoResource.getNsdFilename();
                                if (nsdFilenames.size() != 1) {
                                    log.error("Found zero or more than one file for NSD in YAML format. Error");
                                    throw new FailedOperationException("Found more than one file for NSD in YAML format. Error");
                                }
                                String nsdFilename = nsdFilenames.get(0);
                                Resource dtFile = FileSystemStorageService.loadFileAsResource(project.getProjectId(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), nsdFilename, false);
                                DescriptorTemplate dt = mapper.readValue(dtFile.getFile(), DescriptorTemplate.class);
                                Map<String, KeyValuePair> includedVnfds = checkVNFPkgs(dt, project.getProjectId());
                                Map<String, KeyValuePair> includedPnfds = checkPNFDs(dt, project.getProjectId());
                                List<String> siteOrManoIds = new ArrayList<>();
                                siteOrManoIds.add(manoPlugin.getPluginId());
                                NsdOnBoardingNotificationMessage msg =
                                        new NsdOnBoardingNotificationMessage(nsdInfoResource.getId().toString(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), project.getProjectId(), UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageNsdsSubfolder + "/" + project.getProjectId() + "/" + nsdInfoResource.getNsdId() + "/" + nsdInfoResource.getNsdVersion(), PathType.LOCAL.toString()));
                                msg.setIncludedVnfds(includedVnfds);
                                msg.setIncludedPnfds(includedPnfds);
                                notificationManager.sendNsdOnBoardingNotification(msg);
                            } catch (Exception e) {
                                log.error(e.getMessage());
                                log.debug(null, e);
                            }
                        }
                    }
                }
            }
        }
        log.info("MANO NS synchronization completed");
    }

    public void runtimeNsOnBoarding(NsdOnBoardingNotificationMessage notification){
        List<String> projects = new ArrayList<>();
        if(notification.getProject() == null || notification.getProject().equals("all")){
            List<ProjectResource> projectResourceList = projectRepository.findAll();
            for(ProjectResource projectResource : projectResourceList)
                projects.add(projectResource.getProjectId());
        }else
            projects.add(notification.getProject());

        for(String project : projects){
            Optional<NsdInfoResource> optionalNsdInfoResource = nsdInfoRepo.findByNsdIdAndNsdVersionAndProjectId(UUID.fromString(notification.getNsdId()), notification.getNsdVersion(), project);
            if (!optionalNsdInfoResource.isPresent()) {
                log.info("Project {} - Uploading NSD with ID {} and version {} retrieved from MANO with ID {}", project, notification.getNsdId(), notification.getNsdVersion(), notification.getPluginId());
                CreateNsdInfoRequest request = new CreateNsdInfoRequest();
                KeyValuePairs userDefinedData = new KeyValuePairs();
                userDefinedData.put("isRetrievedFromMANO", "yes");
                userDefinedData.put(notification.getPluginId(), "yes");
                request.setUserDefinedData(userDefinedData);
                try {
                    NsdInfo nsdInfo = createNsdInfo(request, project, true);
                    File nsd;
                    if (notification.getPackagePath().getValue().equals(PathType.LOCAL.toString())) {
                        nsd = new File(notification.getPackagePath().getKey());
                    } else {
                        throw new MethodNotImplementedException("Path Type not currently supported");
                    }
                    MultipartFile multipartFile = Utilities.createMultiPartFromFile(nsd);
                    uploadNsd(nsdInfo.getId().toString(), multipartFile, ContentType.ZIP, false, project);
                    updateNsdInfoOperationStatus(nsdInfo.getId().toString(), notification.getPluginId(), OperationStatus.SUCCESSFULLY_DONE, CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION);
                    notificationManager.sendNsdOnBoardingNotification(new NsdOnBoardingNotificationMessage(nsdInfo.getId().toString(), notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.SUCCESSFULLY_DONE, notification.getPluginId(), null,null));
                } catch (Exception e) {
                    log.error(e.getMessage());
                    log.debug(null, e);
                    try {
                        notificationManager.sendNsdOnBoardingNotification(new NsdOnBoardingNotificationMessage(null, notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId(), null, null));
                    }catch(FailedOperationException e1){
                        log.error(e1.getMessage());
                        log.error(e1.getMessage());
                    }
                }
            }else {
                log.info("Project {} - NSD with ID {} and version {} retrieved from MANO with ID {} is already present", project, notification.getNsdId(), notification.getNsdVersion(), notification.getPluginId());
                try {
                    optionalNsdInfoResource.get().getUserDefinedData().put(notification.getPluginId(), "yes");
                    nsdInfoRepo.saveAndFlush(optionalNsdInfoResource.get());
                    updateNsdInfoOperationStatus(optionalNsdInfoResource.get().getId().toString(), notification.getPluginId(), OperationStatus.SUCCESSFULLY_DONE, CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION);
                    notificationManager.sendNsdOnBoardingNotification(new NsdOnBoardingNotificationMessage(optionalNsdInfoResource.get().getId().toString(), notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.SUCCESSFULLY_DONE, notification.getPluginId(), null,null));
                } catch (Exception e) {
                    log.error(e.getMessage());
                    log.debug(null, e);
                    try {
                        notificationManager.sendNsdOnBoardingNotification(new NsdOnBoardingNotificationMessage(optionalNsdInfoResource.get().getId().toString(), notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId(), null, null));
                    }catch(FailedOperationException e1){
                        log.error(e1.getMessage());
                        log.error(e1.getMessage());
                    }
                }
            }
        }
    }

    public void runtimeNsDeletion(NsdDeletionNotificationMessage notification){
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        List<String> projects = new ArrayList<>();
        if(notification.getProject() == null || notification.getProject().equals("all")){
            List<ProjectResource> projectResourceList = projectRepository.findAll();
            for(ProjectResource projectResource : projectResourceList)
                projects.add(projectResource.getProjectId());
        }else
            projects.add(notification.getProject());

        for(String project : projects) {
            log.info("Project {} - NSD with ID {} and version {} no longer present in MANO with ID {}", project, notification.getNsdId(), notification.getNsdVersion(), notification.getPluginId());
            Optional<NsdInfoResource> optionalNsdInfoResource = nsdInfoRepo.findByNsdIdAndNsdVersionAndProjectId(UUID.fromString(notification.getNsdId()), notification.getNsdVersion(), project);
            if (optionalNsdInfoResource.isPresent()) {
                NsdInfoResource nsdInfoResource = optionalNsdInfoResource.get();
                if (nsdInfoResource.isRetrievedFromMANO()) {
                    try {
                        updateNsdInfoOperationStatus(nsdInfoResource.getId().toString(), notification.getPluginId(), OperationStatus.RECEIVED, CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION);
                        Optional<NsdInfoResource> targetNsdInfoResource = nsdInfoRepo.findById(nsdInfoResource.getId());
                        if (targetNsdInfoResource.isPresent() && !targetNsdInfoResource.get().getNsdOnboardingState().equals(NsdOnboardingStateType.ONBOARDED)) {
                            log.info("Project {} - Going to delete NSD with ID {} and version {}", project, nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion());
                            NsdInfoModifications request = new NsdInfoModifications();
                            request.setNsdOperationalState(NsdOperationalStateType.DISABLED);
                            updateNsdInfo(request, nsdInfoResource.getId().toString(), project, true);
                            deleteNsdInfo(nsdInfoResource.getId().toString(), project, true);
                            notificationManager.sendNsdDeletionNotification(new NsdDeletionNotificationMessage(nsdInfoResource.getId().toString(), notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.SUCCESSFULLY_DONE, notification.getPluginId()));
                        } else {
                            notificationManager.sendNsdDeletionNotification(new NsdDeletionNotificationMessage(nsdInfoResource.getId().toString(), notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.SUCCESSFULLY_DONE, notification.getPluginId()));
                            targetNsdInfoResource.get().getUserDefinedData().put(notification.getPluginId(), "no");
                        }
                    }catch (NotPermittedOperationException e){
                        log.error("Project {} - Failed to delete NSD with ID {} and version {}, it is not deletable", project, nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion());
                        NsdDeletionNotificationMessage msg = new NsdDeletionNotificationMessage(nsdInfoResource.getId().toString(), notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId());
                        log.info("Project {} - Sending NS Onboarding notification for NSD with ID {} and version {} to MANO with ID {}", project, nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion(), notification.getPluginId());
                        try {
                            List<String> nsdFilenames = nsdInfoResource.getNsdFilename();
                            if (nsdFilenames.size() != 1) {
                                log.error("Found zero or more than one file for NSD in YAML format. Error");
                                throw new FailedOperationException("Found more than one file for NSD in YAML format. Error");
                            }
                            String nsdFilename = nsdFilenames.get(0);
                            Resource dtFile = FileSystemStorageService.loadFileAsResource(project, nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), nsdFilename, false);
                            DescriptorTemplate dt = mapper.readValue(dtFile.getFile(), DescriptorTemplate.class);
                            Map<String, KeyValuePair> includedVnfds = checkVNFPkgs(dt, project);
                            Map<String, KeyValuePair> includedPnfds = checkPNFDs(dt, project);
                            List<String> siteOrManoIds = new ArrayList<>();
                            siteOrManoIds.add(notification.getPluginId());
                            NsdOnBoardingNotificationMessage msg1 = new NsdOnBoardingNotificationMessage(nsdInfoResource.getId().toString(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), project, UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageNsdsSubfolder + "/" + project + "/" + nsdInfoResource.getNsdId() + "/" + nsdInfoResource.getNsdVersion(), PathType.LOCAL.toString()));
                            msg1.setIncludedVnfds(includedVnfds);
                            msg1.setIncludedPnfds(includedPnfds);
                            notificationManager.sendNsdDeletionNotification(msg);
                            notificationManager.sendNsdOnBoardingNotification(msg1);
                        } catch (Exception e1) {
                            log.error(e1.getMessage());
                            log.debug(null, e1);
                        }
                    } catch (Exception e) {
                        log.error("Project {} - Failed to delete VNFD with ID {} and version {}", project, nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion());
                        log.debug(null, e);
                        try {
                            notificationManager.sendNsdDeletionNotification(new NsdDeletionNotificationMessage(nsdInfoResource.getId().toString(), notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId()));
                        }catch(FailedOperationException e1){
                            log.error(e1.getMessage());
                            log.error(e1.getMessage());
                        }
                    }
                } else {
                    log.info("Project {} - Sending VNF Onboarding notification for VNFD with ID {} and version {} to MANO with ID {}", project, nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion(), notification.getPluginId());
                    try {
                        notificationManager.sendNsdDeletionNotification(new NsdDeletionNotificationMessage(nsdInfoResource.getId().toString(), notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId()));
                        log.info("Project {} - Sending NS Onboarding notification for NSD with ID {} and version {} to MANO with ID {}", project, nsdInfoResource.getNsdId(), nsdInfoResource.getNsdVersion(), notification.getPluginId());
                        List<String> nsdFilenames = nsdInfoResource.getNsdFilename();
                        if (nsdFilenames.size() != 1) {
                            log.error("Found zero or more than one file for NSD in YAML format. Error");
                            throw new FailedOperationException("Found more than one file for NSD in YAML format. Error");
                        }
                        String nsdFilename = nsdFilenames.get(0);
                        Resource dtFile = FileSystemStorageService.loadFileAsResource(project, nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), nsdFilename, false);
                        DescriptorTemplate dt = mapper.readValue(dtFile.getFile(), DescriptorTemplate.class);
                        Map<String, KeyValuePair> includedVnfds = checkVNFPkgs(dt, project);
                        Map<String, KeyValuePair> includedPnfds = checkPNFDs(dt, project);
                        List<String> siteOrManoIds = new ArrayList<>();
                        siteOrManoIds.add(notification.getPluginId());
                        NsdOnBoardingNotificationMessage msg = new NsdOnBoardingNotificationMessage(nsdInfoResource.getId().toString(), nsdInfoResource.getNsdId().toString(), nsdInfoResource.getNsdVersion(), project, UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageNsdsSubfolder + "/" + project + "/" + nsdInfoResource.getNsdId() + "/" + nsdInfoResource.getNsdVersion(), PathType.LOCAL.toString()));
                        msg.setIncludedVnfds(includedVnfds);
                        msg.setIncludedPnfds(includedPnfds);
                        notificationManager.sendNsdOnBoardingNotification(msg);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        log.debug(null, e);
                    }
                }
            }else{
                try {
                    notificationManager.sendNsdDeletionNotification(new NsdDeletionNotificationMessage(null, notification.getNsdId(), notification.getNsdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId()));
                }catch(FailedOperationException e1){
                    log.error(e1.getMessage());
                    log.error(e1.getMessage());
                }
            }
        }
    }

    protected void updateOperationInfoInConsumersMap(UUID operationId, OperationStatus opStatus, String manoId,
                                                     String nsdInfoId, CatalogueMessageType messageType) {
        Map<String, NotificationResource> manoIdToOpAck = new HashMap<>();
        if (operationIdToConsumersAck.containsKey(operationId.toString())) {
            manoIdToOpAck = operationIdToConsumersAck.get(operationId.toString());
        }
        NotificationResource notificationResource = new NotificationResource(nsdInfoId, messageType, opStatus, PluginType.MANO);
        manoIdToOpAck.put(manoId, notificationResource);

        operationIdToConsumersAck.put(operationId.toString(), manoIdToOpAck);
    }

    private UUID insertOperationInfoInConsumersMap(String nsdInfoId, CatalogueMessageType messageType,
                                                   OperationStatus opStatus, List<String> siteOrManoIds) {
        UUID operationId = UUID.randomUUID();
        log.debug("Updating consumers internal mapping for operationId {}", operationId);
        List<MANO> manos = MANORepository.findAll();
        if(siteOrManoIds != null)
            manos.removeIf(mano -> !siteOrManoIds.contains(mano.getManoId()) && !siteOrManoIds.contains(mano.getManoSite()));
        Map<String, NotificationResource> pluginToOperationState = new HashMap<>();
        for (MANO mano : manos) {
            pluginToOperationState.put(mano.getManoId(), new NotificationResource(nsdInfoId, messageType, opStatus, PluginType.MANO));
        }
        operationIdToConsumersAck.put(operationId.toString(), pluginToOperationState);
        return operationId;
    }

    private List<String> checkWhereOnboardNS(NsdInfoResource nsdInfoResource){
        List<String> availableManos = new ArrayList<>(pluginManger.manoDrivers.keySet());
        Map<String, NotificationResource> onBoardingMap;

        String mano;
        Iterator<String> manosIterator = availableManos.iterator();
        for(UUID vnfInfoId : nsdInfoResource.getVnfPkgIds()){
            Optional<VnfPkgInfoResource> vnfPkgInfoResource = vnfPkgInfoRepository.findById(vnfInfoId);
            if(vnfPkgInfoResource.isPresent()) {
                onBoardingMap = vnfPkgInfoResource.get().getAcknowledgedOnboardOpConsumers();
                while(manosIterator.hasNext()) {
                    mano = manosIterator.next();
                    if (!onBoardingMap.containsKey(mano) || !onBoardingMap.get(mano).getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE))
                        manosIterator.remove();
                }
            }
        }
        /* TODO uncomment when it will be possible to onboard the PNF on MANOs
        manosIterator = availableManos.iterator();
        for(UUID pnfInfoId : nsdInfoResource.getPnfdInfoIds()){
            Optional<PnfdInfoResource> pnfdInfoResource = pnfdInfoRepo.findById(pnfInfoId);
            if(pnfdInfoResource.isPresent()){
                onBoardingMap = pnfdInfoResource.get().getAcknowledgedOnboardOpConsumers();
                while(manosIterator.hasNext()) {
                    mano = manosIterator.next();
                    if (!onBoardingMap.containsKey(mano) || !onBoardingMap.get(mano).getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE))
                        manosIterator.remove();
                }
            }
        }
         */
        manosIterator = availableManos.iterator();
        for(UUID nestedNsdInfoId : nsdInfoResource.getNestedNsdInfoIds()){
            Optional<NsdInfoResource> nestedNsInfoResource = nsdInfoRepo.findById(nestedNsdInfoId);
            if(nestedNsInfoResource.isPresent()){
                onBoardingMap = nestedNsInfoResource.get().getAcknowledgedOnboardOpConsumers();
                while(manosIterator.hasNext()) {
                    mano = manosIterator.next();
                    if (!onBoardingMap.containsKey(mano) || !onBoardingMap.get(mano).getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE))
                        manosIterator.remove();
                }
            }
        }
        return availableManos;
    }

    @Override
    public NsdInfo createNsdInfo(CreateNsdInfoRequest request, String project, boolean isInternalRequest)
            throws FailedOperationException, MalformattedElementException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to create a new NSD info");
        KeyValuePairs kvp = request.getUserDefinedData();
        Map<String, String> targetKvp = new HashMap<>();
        if (kvp != null) {
            for (Map.Entry<String, String> e : kvp.entrySet()) {
                targetKvp.put(e.getKey(), e.getValue());
            }
        }
        NsdInfoResource nsdInfoResource = new NsdInfoResource(targetKvp);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
            try {
                if (isInternalRequest || !keycloakEnabled || checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    nsdInfoResource.setProjectId(project);
                } else {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }
        nsdInfoResource.setRetrievedFromMANO(targetKvp.containsKey("isRetrievedFromMANO") && targetKvp.get("isRetrievedFromMANO").equals("yes"));
        nsdInfoResource.setMultiSite(targetKvp.containsKey("multiSite") && targetKvp.get("multiSite").equals("yes"));

        if(targetKvp.containsKey("NSD_ID") && targetKvp.containsKey("NSD_INVARIANT_ID")){
            NsdIdInvariantIdMapping mapping;
            String nsdId = targetKvp.get("NSD_ID");
            String invariantId = targetKvp.get("NSD_INVARIANT_ID");
            Optional<NsdIdInvariantIdMapping> mappingOptional = nsdIdInvariantIdMappingRepository.findByNsdId(nsdId);
            if(mappingOptional.isPresent()){
                mapping = mappingOptional.get();
                mapping.setInvariantId(invariantId);
            }else {
                mapping = new NsdIdInvariantIdMapping();
                mapping.setNsdId(nsdId);
                mapping.setInvariantId(invariantId);
            }
            nsdIdInvariantIdMappingRepository.saveAndFlush(mapping);
        }

        nsdInfoRepo.saveAndFlush(nsdInfoResource);
        UUID nsdInfoId = nsdInfoResource.getId();
        log.debug("Created NSD info with ID " + nsdInfoId);
        NsdInfo nsdInfo = buildNsdInfo(nsdInfoResource);
        log.debug("Translated internal nsd info resource into nsd info");
        return nsdInfo;
    }

    @Override
    public synchronized void deleteNsdInfo(String nsdInfoId, String project, boolean isInternalRequest)
            throws FailedOperationException, NotExistingEntityException, MalformattedElementException,
            NotPermittedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to delete an NSD info");

        if (nsdInfoId == null)
            throw new MalformattedElementException("Invalid NSD info ID");
        try {
            UUID id = UUID.fromString(nsdInfoId);

            Optional<NsdInfoResource> optional = nsdInfoRepo.findById(id);

            if (optional.isPresent()) {
                log.debug("Found NSD info resource with id: " + nsdInfoId);

                NsdInfoResource nsdInfo = optional.get();

                if (project != null) {
                    Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
                    if (!projectOptional.isPresent()) {
                        log.error("Project with id " + project + " does not exist");
                        throw new FailedOperationException("Project with id " + project + " does not exist");
                    }
                }
                if (project != null && !nsdInfo.getProjectId().equals(project)) {
                    throw new NotAuthorizedOperationException("Specified project differs from NSD info project");
                } else {
                    try {
                        if (!isInternalRequest && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                            throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                        }
                    } catch (NotExistingEntityException e) {
                        throw new NotAuthorizedOperationException(e.getMessage());
                    }
                }

                nsdInfo.isDeletable();

                if(!isInternalRequest && !nsdInfo.getNsdOnboardingState().equals(NsdOnboardingStateType.FAILED) && nsdInfo.isRetrievedFromMANO()){
                    throw new FailedOperationException("Cannot remove NSD info, it has been retrieved from MANO");
                }

                List<UUID> vnfPkgInfoIds = nsdInfo.getVnfPkgIds();

                for (UUID vnfPkgInfoId : vnfPkgInfoIds) {
                    Optional<VnfPkgInfoResource> vnfPkgOptional = vnfPkgInfoRepository.findById(vnfPkgInfoId);

                    if (vnfPkgOptional.isPresent()) {
                        log.debug("Found vnfd with info Id {} while deleting nsd with info Id {}", vnfPkgInfoId, nsdInfoId);
                        VnfPkgInfoResource vnfPkgInfoResource = vnfPkgOptional.get();
                        List<String> parentNsds = vnfPkgInfoResource.getParentNsds();
                        for (Iterator<String> iter = parentNsds.listIterator(); iter.hasNext(); ) {
                            String nsdId = iter.next();
                            if (nsdId.equalsIgnoreCase(nsdInfo.getNsdId().toString())) {
                                iter.remove();
                            }
                        }
                        vnfPkgInfoResource.setParentNsds(parentNsds);
                        vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
                    }
                }

                List<UUID> pnfdInfoIds = nsdInfo.getPnfdInfoIds();

                for (UUID pnfdInfoId : pnfdInfoIds) {
                    Optional<PnfdInfoResource> pnfdOptional = pnfdInfoRepo.findById(pnfdInfoId);

                    if (pnfdOptional.isPresent()) {
                        log.debug("Found pnfd with info Id {} while deleting nsd with info Id {}", pnfdInfoId, nsdInfoId);
                        PnfdInfoResource pnfdInfoResource = pnfdOptional.get();
                        List<String> parentNsds = pnfdInfoResource.getParentNsds();
                        for (Iterator<String> iter = parentNsds.listIterator(); iter.hasNext(); ) {
                            String nsdId = iter.next();
                            if (nsdId.equalsIgnoreCase(nsdInfo.getNsdId().toString())) {
                                iter.remove();
                            }
                        }
                        pnfdInfoResource.setParentNsds(parentNsds);
                        pnfdInfoRepo.saveAndFlush(pnfdInfoResource);
                    }
                }

                List<Appd> appds = appdManagementService.getAssociatedAppD(nsdInfo.getId());
                log.debug("The NSD info can be removed");
                UUID nsdId = nsdInfo.getNsdId();
                if (nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.ONBOARDED
                        || nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.LOCAL_ONBOARDED
                        || nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.PROCESSING) {
                    log.debug("The NSD info is associated to an onboarded NSD. Removing it");

                    // dbWrapper.deleteNsd(nsdId);

                    try {
                        storageService.deleteNsd(project, nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion());
                    } catch (Exception e) {
                        log.error("Unable to delete NSD with nsdId {} from filesystem", nsdInfo.getNsdId().toString());
                        log.error("Details: ", e);
                    }

                    if(!nsdInfo.isRetrievedFromMANO()) {
                        UUID operationId = insertOperationInfoInConsumersMap(nsdInfoId,
                                CatalogueMessageType.NSD_DELETION_NOTIFICATION, OperationStatus.SENT, null);

                        log.debug("NSD {} locally removed. Sending nsdDeletionNotificationMessage to bus", nsdId);
                        NsdDeletionNotificationMessage msg = new NsdDeletionNotificationMessage(nsdInfoId, nsdId.toString(), nsdInfo.getNsdVersion(), project,
                                operationId, ScopeType.LOCAL, OperationStatus.SENT);
                        notificationManager.sendNsdDeletionNotification(msg);
                    }
                }

                nsdInfoRepo.deleteById(id);

                List<NsdIdInvariantIdMapping> mappingList = nsdIdInvariantIdMappingRepository.findByInvariantId(nsdId.toString());
                for(NsdIdInvariantIdMapping mapping : mappingList)
                    nsdIdInvariantIdMappingRepository.delete(mapping);

                //Remove or update AppD if any
                for(Appd appd : appds){
                    Optional<AppPackageInfoResource> appPackageInfoResourceOptional = appPackageInfoResourceRepository.findByAppdIdAndVersionAndProject(appd.getAppDId(), appd.getAppDVersion(), project);
                    if(appPackageInfoResourceOptional.isPresent()) {
                        AppPackageInfoResource appPackageInfoResource = appPackageInfoResourceOptional.get();
                        appPackageInfoResource.setUsageState(UsageState.NOT_IN_USE);
                        appPackageInfoResourceRepository.saveAndFlush(appPackageInfoResource);
                        if (appPackageInfoResource.isDeletionPending()) {
                            try {
                                appdManagementService.deleteAppPackage(appPackageInfoResource.getAppPackageInfoId(), project, true);
                            }catch (Exception e){
                                log.error("Cannot delete the Appd in pending deletion");
                            }
                        }
                    }
                }
                log.debug("Deleted NSD info resource with id: " + nsdInfoId);
            } else {
                log.debug("NSD info resource with id " + nsdInfoId + "not found");
                throw new NotExistingEntityException("NSD info resource with id " + nsdInfoId + "not found");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + nsdInfoId);
            throw new MalformattedElementException("Wrong ID format: " + nsdInfoId);
        }
    }

    @Override
    public Object getNsdFile(String nsdInfoId, boolean isInternalRequest, String project) throws FailedOperationException, NotExistingEntityException,
            MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to retrieve an NSD content for NSD info " + nsdInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);
        if (project != null && !nsdInfo.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from NSD info project");
        } else {
            try {
                if (!isInternalRequest && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if ((!isInternalRequest) && (nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.ONBOARDED
                && nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.LOCAL_ONBOARDED)) {
            log.error("NSD info " + nsdInfoId + " does not have an onboarded NSD yet");
            throw new NotPermittedOperationException("NSD info " + nsdInfoId + " does not have an onboarded NSD yet");
        }
        UUID nsdId = nsdInfo.getNsdId();
        log.debug("Internal NSD ID: " + nsdId);

        // try {
        List<String> nsdFilenames = nsdInfo.getNsdFilename();
        if (nsdFilenames.size() != 1) {
            log.error("Found zero or more than one file for NSD in YAML format. Error");
            throw new FailedOperationException("Found more than one file for NSD in YAML format. Error");
        }
        String nsdFilename = nsdFilenames.get(0);
        return storageService.loadFileAsResource(project, nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion(), nsdFilename, false);
    }

    @Override
    public Object getNsd(String nsdInfoId, boolean isInternalRequest, String project, String accept) throws FailedOperationException, NotExistingEntityException,
            MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to retrieve an NSD content for NSD info " + nsdInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);
        if (project != null && !nsdInfo.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from NSD info project");
        } else {
            try {
                if (!isInternalRequest && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if ((!isInternalRequest) && (nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.ONBOARDED
                && nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.LOCAL_ONBOARDED)) {
            log.error("NSD info " + nsdInfoId + " does not have an onboarded NSD yet");
            throw new NotPermittedOperationException("NSD info " + nsdInfoId + " does not have an onboarded NSD yet");
        }
        UUID nsdId = nsdInfo.getNsdId();
        log.debug("Internal NSD ID: " + nsdId);

        /*
         * DescriptorTemplate nsd = dbWrapper.getNsd(nsdId);
         * log.debug("Got NSD content");
         */

        ContentType ct = ContentType.YAML;
        if (accept.contains("multipart") || accept.contains("zip")) {
            ct = ContentType.ZIP;
        }
        switch (ct) {
            case YAML: {
                // try {
                List<String> nsdFilenames = nsdInfo.getNsdFilename();
                if (nsdFilenames.size() != 1) {
                    log.error("Found zero or more than one file for NSD in YAML format");
                    throw new FailedOperationException("Found more than one file for NSD in YAML format");
                }
                String nsdFilename = nsdFilenames.get(0);
                return storageService.loadFileAsResource(project, nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion(), nsdFilename, false);

                /*
                 * //String nsdString = DescriptorsParser.descriptorTemplateToString(nsd);
                 * //log.debug("NSD content translated into YAML format"); //return nsdString; }
                 * catch (JsonProcessingException e) {
                 * log.error("Error while translating descriptor"); throw new
                 * FailedOperationException("Error while translating descriptor: " +
                 * e.getMessage()); }
                 */
            }
            case ZIP: {
                List<String> nsdFilenames = nsdInfo.getNsdPkgFilename();
                if (nsdFilenames.size() != 1) {
                    log.error("Found zero or more than one file for NSD in YAML format. Error");
                    throw new FailedOperationException("Found more than one file for NSD in YAML format. Error");
                }
                String nsdFilename = nsdFilenames.get(0);
                return storageService.loadFileAsResource(project, nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion(), nsdFilename, false);
            }
            default: {
                log.error("Content type not yet supported");
                throw new MethodNotImplementedException("Content type not yet supported");
            }
        }

    }

    @Override
    public NsdInfo getNsdInfo(String nsdInfoId, String project) throws NotPermittedOperationException, NotExistingEntityException,
            MalformattedElementException, MethodNotImplementedException, NotAuthorizedOperationException, FailedOperationException {
        log.debug("Processing request to get an NSD info");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        NsdInfoResource nsdInfoResource = getNsdInfoResource(nsdInfoId);
        if (project != null && !nsdInfoResource.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from NSD info project");
        } else {
            try {
                if (keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        log.debug("Found NSD info resource with id: " + nsdInfoId);
        NsdInfo nsdInfo = buildNsdInfo(nsdInfoResource);
        log.debug("Built NSD info with id: " + nsdInfoId);
        return nsdInfo;

    }

    private NsdInfoResource getNsdInfoResource(String nsdInfoId)
            throws NotExistingEntityException, MalformattedElementException {
        log.debug("Retrieving internal NSD info resource with ID " + nsdInfoId);
        try {
            UUID id = UUID.fromString(nsdInfoId);

            Optional<NsdInfoResource> optional = nsdInfoRepo.findById(id);

            if (optional.isPresent()) {
                NsdInfoResource nsdInfoResource = optional.get();
                log.debug("Found NSD info resource with id: " + nsdInfoId);
                return nsdInfoResource;
            } else {
                log.debug("NSD info resource with id " + nsdInfoId + " not found");
                throw new NotExistingEntityException("NSD info resource with id " + nsdInfoId + " not found");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + nsdInfoId);
            throw new MalformattedElementException("Wrong ID format: " + nsdInfoId);
        }
    }

    @Override
    public List<NsdInfo> getAllNsdInfos(String project, String extraData, UUID nsdId) throws FailedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to get all NSD infos");
        if (project != null && !project.equals("*")) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        try {
            if (project != null && !project.equals("*") && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
            }
        } catch (NotExistingEntityException e) {
            throw new NotAuthorizedOperationException(e.getMessage());
        }

        if(nsdId != null) {
            Optional<NsdIdInvariantIdMapping> mapping = nsdIdInvariantIdMappingRepository.findByNsdId(nsdId.toString());
            if (mapping.isPresent())
                nsdId = UUID.fromString(mapping.get().getInvariantId());
        }
        
        List<NsdInfoResource> nsdInfoResources;
        if(nsdId == null)
            nsdInfoResources = nsdInfoRepo.findAll();
        else
            nsdInfoResources = nsdInfoRepo.findByNsdId(nsdId);

        List<NsdInfo> nsdInfos = new ArrayList<>();

        for (NsdInfoResource nsdInfoResource : nsdInfoResources) {
            if (project != null && !project.equals("*") && !nsdInfoResource.getProjectId().equals(project)) {
                continue;
            } else {
                NsdInfo nsdInfo = buildNsdInfo(nsdInfoResource);
                if(extraData != null && extraData.equals("manoInfoIds")){
                    Map<String, NotificationResource> onBoardingMap = nsdInfoResource.getAcknowledgedOnboardOpConsumers();
                    List<MANOPlugin> manos = new ArrayList<>(pluginManger.manoDrivers.values());
                    for(MANOPlugin mano : manos){
                        if (onBoardingMap.containsKey(mano.getPluginId()) && onBoardingMap.get(mano.getPluginId()).getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                            String manoInfoId = mano.getManoPkgInfoId(nsdInfoResource.getId().toString());
                            if(manoInfoId != null)
                                nsdInfo.getManoInfoIds().put(mano.getPluginId(), manoInfoId);
                        }
                    }
                }
                nsdInfos.add(nsdInfo);
                log.debug("Added NSD info " + nsdInfoResource.getId());
            }
        }
        return nsdInfos;
    }

    @Override
    public synchronized NsdInfoModifications updateNsdInfo(NsdInfoModifications nsdInfoModifications, String nsdInfoId, String project, boolean isInternalRequest)
            throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, NotAuthorizedOperationException {
        log.debug("Processing request to update NSD info: " + nsdInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);

        if (project != null && !nsdInfo.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from NSD info project");
        } else {
            try {
                if (!isInternalRequest && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if(!isInternalRequest && nsdInfo.isRetrievedFromMANO()){
            throw new FailedOperationException("Cannot update NSD info, it has been retrieved from MANO");
        }

        //TODO add possibility to update onboarding on manos
        if (nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.ONBOARDED
                || nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.LOCAL_ONBOARDED) {
            if (nsdInfoModifications.getNsdOperationalState() != null) {
                if (nsdInfo.getNsdOperationalState() == nsdInfoModifications.getNsdOperationalState()) {
                    log.error("NSD operational state already "
                            + nsdInfo.getNsdOperationalState() + ". Cannot update NSD info");
                    throw new NotPermittedOperationException("NSD operational state already "
                            + nsdInfo.getNsdOperationalState() + ". Cannot update NSD info");
                } else {
                    nsdInfo.setNsdOperationalState(nsdInfoModifications.getNsdOperationalState());
                }
            }
            if (nsdInfoModifications.getUserDefinedData() != null) {
                nsdInfo.setUserDefinedData(nsdInfoModifications.getUserDefinedData());
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);

            try {
                String json = mapper.writeValueAsString(nsdInfoModifications);
                log.debug("Updating nsdInfoResource with nsdInfoModifications: " + json);
            } catch (JsonProcessingException e) {
                log.error("Unable to parse received nsdInfoModifications: " + e.getMessage());
            }
            nsdInfoRepo.saveAndFlush(nsdInfo);
            log.debug("NsdInfoResource successfully updated");
        } else {
            log.error("NSD onboarding state not ONBOARDED. Cannot update NSD info");
            throw new NotPermittedOperationException("NSD onboarding state not ONBOARDED. Cannot update NSD info");
        }
        return nsdInfoModifications;
    }

    @Override
    public synchronized void uploadNsd(String nsdInfoId, MultipartFile nsd, ContentType contentType, boolean isInternalRequest, String project) throws MalformattedElementException, FailedOperationException, NotExistingEntityException, NotPermittedOperationException, MethodNotImplementedException, NotAuthorizedOperationException, AlreadyExistingEntityException {
        log.debug("Processing request to upload NSD content for NSD info " + nsdInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);

        if (project != null && !nsdInfo.getProjectId().equals(project)) {
            nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.FAILED);
            nsdInfoRepo.saveAndFlush(nsdInfo);
            throw new NotAuthorizedOperationException("Specified project differs from NSD info project");
        } else {
            try {
                if (!nsdInfo.isRetrievedFromMANO() && !isInternalRequest && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.FAILED);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.FAILED);
                nsdInfoRepo.saveAndFlush(nsdInfo);
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if (nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.CREATED) {
            log.error("NSD info " + nsdInfoId + " not in CREATED onboarding state");
            nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.FAILED);
            nsdInfoRepo.saveAndFlush(nsdInfo);
            throw new NotPermittedOperationException("NSD info " + nsdInfoId + " not in CREATED onboarding state");
        }

        if(nsdInfo.getUserDefinedData().containsKey("NSD_ID") && nsdInfo.getUserDefinedData().containsKey("NSD_INVARIANT_ID")){
            String nsdId = nsdInfo.getUserDefinedData().get("NSD_ID");
            String invariantId = nsdInfo.getUserDefinedData().get("NSD_INVARIANT_ID");
            List<NsdInfoResource> nsdInfoResourceList = nsdInfoRepo.findByNsdId(UUID.fromString(invariantId));
            if(nsdInfoResourceList.size() == 0){
                Optional<NsdIdInvariantIdMapping> mappingOptional = nsdIdInvariantIdMappingRepository.findByNsdId(nsdId);
                mappingOptional.ifPresent(nsdIdInvariantIdMapping -> nsdIdInvariantIdMappingRepository.delete(nsdIdInvariantIdMapping));
                nsdInfoRepo.delete(nsdInfo);
                throw new NotExistingEntityException("NSD with ID " + invariantId + "not found");
            }else
                nsdInfoRepo.delete(nsdInfo);
            return;
        }

        // convert to File
        File inputFile = null;
        try {
            inputFile = convertToFile(nsd);
        } catch (Exception e) {
            log.error("Error while parsing NSD in zip format: " + e.getMessage());
            nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.FAILED);
            nsdInfoRepo.saveAndFlush(nsdInfo);
            throw new MalformattedElementException("Error while parsing NSD");
        }

        String nsdFilename;
        DescriptorTemplate dt;
        UUID nsdId;

        // pre-set nsdinfo attributes to properly store NSDs
        // UUID nsdId = UUID.randomUUID();

        Map<String, KeyValuePair> nestedNsds;
        Map<String, KeyValuePair> includedVnfds;
        Map<String, KeyValuePair> includedPnfds;
        NsdOnboardingStateType onboardingStateType = NsdOnboardingStateType.UPLOADING;

        CSARInfo csarInfo = null;
        NSNode nsNode;

        switch (contentType) {
            case ZIP: {
                try {
                    log.info("NSD file is in format: zip");

                    checkZipArchive(nsd);

                    // TODO: assuming for now one single file into the zip
                    //MultipartFile nsdMpFile = extractFile(inputFile);
                    // convert to File
                    //File nsdFile = convertToFile(nsdMpFile);
                    //dt = DescriptorsParser.fileToDescriptorTemplate(nsdFile);

                    csarInfo = archiveParser.archiveToCSARInfo(project, nsd, false, true);
                    dt = csarInfo.getMst();

                    //TODO remove
                    Map<String, NSNode> nsNodeMap = dt.getTopologyTemplate().getNSNodes();
                    if(nsNodeMap.size() != 1 && !nsdInfo.isMultiSite())
                        throw new MalformattedElementException("Composite NSs can currently be only multi-site");

                    nsdFilename = csarInfo.getPackageFilename();

                    nestedNsds = checkNestedNSDs(dt, project);
                    includedVnfds = checkVNFPkgs(dt, project);
                    includedPnfds = checkPNFDs(dt, project);

                    String nsdId_string = dt.getMetadata().getDescriptorId();

                    if (!Utilities.isUUID(nsdId_string)) {
                        throw new MalformattedElementException("NSD id not in UUID format");
                    }

                    //NSNode nsNode = dt.getTopologyTemplate().getNSNodes().values().iterator().next();//For the moment assume only one NS node
                    nsNode = getCompositeNsNode(dt);
                    if(nsNode == null)
                        throw new MalformattedElementException("Descriptor ID and version specified into metadata are not aligned with those specified into NSNode");

                    nsdId = UUID.fromString(nsdId_string);

                    Optional<NsdInfoResource> optionalNsdInfoResource = nsdInfoRepo.findByNsdIdAndNsdVersionAndProjectId(nsdId, dt.getMetadata().getVersion(), project);
                    if (optionalNsdInfoResource.isPresent())
                        throw new AlreadyExistingEntityException("An NSD with the same id and version already exists in the project");

                    nsdInfo.setNsdId(nsdId);

                    log.debug("NSD successfully parsed - its content is: \n"
                            + DescriptorsParser.descriptorTemplateToString(dt));

                    // pre-set nsdinfo attributes to properly store NSDs
                    nsdInfo.setNsdVersion(dt.getMetadata().getVersion());

                    //nsdFilename = storageService.storeNsd(nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion(), nsdMpFile);

                    // change contentType to YAML as nsd file is no more zip from now on
                    //contentType = ContentType.YAML;

                    log.debug("NSD file successfully stored");
                    // clean tmp files
                    //if (!nsdFile.delete()) {
                    // log.warn("Could not delete temporary NSD zip content file");
                    //}
                } catch (IOException e) {
                    log.error("Error while parsing NSD in zip format: " + e.getMessage());
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new MalformattedElementException("Error while parsing NSD");
                } catch (NotExistingEntityException e) {
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new NotPermittedOperationException("Unable to onboard NSD because one or more related VNF Pkgs are missing in local storage: " + e.getMessage());
                } catch (MalformattedElementException e) {
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new MalformattedElementException(e.getMessage());
                } catch (NotAuthorizedOperationException e) {
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new NotAuthorizedOperationException(e.getMessage());
                } catch (AlreadyExistingEntityException e) {
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new AlreadyExistingEntityException(e.getMessage());
                } catch (Exception e) {
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    log.error("Error while parsing NSD in zip format: " + e.getMessage());
                    throw new MalformattedElementException("Error while parsing NSD: " + e.getMessage());
                }
                break;
            }
            case YAML: {
                try {
                    log.info("NSD file is in format: yaml");

                    dt = descriptorsParser.fileToDescriptorTemplate(inputFile);

                    //TODO remove
                    Map<String, NSNode> nsNodeMap = dt.getTopologyTemplate().getNSNodes();
                    if(nsNodeMap.size() != 1 && !nsdInfo.isMultiSite())
                        throw new MalformattedElementException("Composite NSs can currently be only multi-site");

                    nestedNsds = checkNestedNSDs(dt, project);
                    includedVnfds = checkVNFPkgs(dt, project);
                    includedPnfds = checkPNFDs(dt, project);

                    String nsdId_string = dt.getMetadata().getDescriptorId();

                    if (!Utilities.isUUID(nsdId_string)) {
                        throw new MalformattedElementException("NSD id not in UUID format");
                    }

                    nsNode = getCompositeNsNode(dt);
                    if(nsNode == null)
                        throw new MalformattedElementException("Descriptor ID and version specified into metadata are not aligned with those specified into NSNode");

                    nsdId = UUID.fromString(nsdId_string);

                    Optional<NsdInfoResource> optionalNsdInfoResource = nsdInfoRepo.findByNsdIdAndNsdVersionAndProjectId(nsdId, dt.getMetadata().getVersion(), project);
                    if (optionalNsdInfoResource.isPresent()) {
                        throw new AlreadyExistingEntityException("An NSD with the same id and version already exists in the project");
                    }

                    nsdInfo.setNsdId(nsdId);

                    log.debug("NSD successfully parsed - its content is: \n"
                            + DescriptorsParser.descriptorTemplateToString(dt));
                    // pre-set nsdinfo attributes to properly store NSDs
                    nsdInfo.setNsdVersion(dt.getMetadata().getVersion());

                    nsdFilename = storageService.storePkg(project, nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion(), nsd, false);

                    log.debug("NSD file successfully stored");

                } catch (IOException e) {
                    log.error("Error while parsing NSD in yaml format: " + e.getMessage());
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new MalformattedElementException("Error while parsing NSD");
                } catch (NotExistingEntityException e) {
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new NotPermittedOperationException("Unable to onboard NSD because one or more related PNFs or VNF Pkgs are missing in local storage: " + e.getMessage());
                } catch (MalformattedElementException e) {
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new MalformattedElementException(e.getMessage());
                } catch (NotAuthorizedOperationException e) {
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new NotAuthorizedOperationException(e.getMessage());
                } catch (AlreadyExistingEntityException e) {
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new AlreadyExistingEntityException(e.getMessage());
                }
                break;
            }

            default: {
                log.error("Unsupported content type: " + contentType.toString());
                onboardingStateType = NsdOnboardingStateType.FAILED;
                nsdInfo.setNsdOnboardingState(onboardingStateType);
                nsdInfoRepo.saveAndFlush(nsdInfo);
                throw new MethodNotImplementedException("Unsupported content type: " + contentType.toString());
            }
        }

        if (nsdFilename == null || dt == null) {
            onboardingStateType = NsdOnboardingStateType.FAILED;
            nsdInfo.setNsdOnboardingState(onboardingStateType);
            nsdInfoRepo.saveAndFlush(nsdInfo);
            throw new FailedOperationException("Invalid internal structures");
        }

        log.debug("Updating NSD info");
        // nsdInfo.setNsdId(nsdId);
        // updated when we will implement also the package uploading
        nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.LOCAL_ONBOARDED);
        nsdInfo.setNsdOperationalState(NsdOperationalStateType.ENABLED);
        nsdInfo.setNsdDesigner(dt.getMetadata().getVendor());
        nsdInfo.setNsdInvariantId(UUID.fromString(dt.getMetadata().getDescriptorId()));

        String nsdName = nsNode.getProperties().getName();

        log.debug("NSD name: " + nsdName);
        nsdInfo.setNsdName(nsdName);
        // nsdInfo.setNsdVersion(dt.getMetadata().getVersion());
        nsdInfo.setContentType(contentType);
        if (csarInfo != null) {
            nsdInfo.addNsdPkgFilename(nsdFilename);
            nsdInfo.addNsdFilename(csarInfo.getDescriptorFilename());
        } else {
            nsdInfo.addNsdFilename(nsdFilename);
        }

        List<UUID> nestedNsdIds = new ArrayList<>();
        for (String nestedNsdId : nestedNsds.keySet()) {
            log.debug("Adding nsdInfo Id {} to nestedNsdInfoIds list in nsdInfo", nestedNsdId);
            nestedNsdIds.add(UUID.fromString(nestedNsdId));
        }
        nsdInfo.setNestedNsdInfoIds(nestedNsdIds);

        List<UUID> vnfPkgIds = new ArrayList<>();
        for (String vnfdInfoId : includedVnfds.keySet()) {
            log.debug("Adding vnfPkgInfo Id {} to vnfPkgs list in nsdInfo", vnfdInfoId);
            vnfPkgIds.add(UUID.fromString(vnfdInfoId));
        }
        nsdInfo.setVnfPkgIds(vnfPkgIds);

        List<UUID> pnfdIds = new ArrayList<>();
        for (String pnfdInfoId : includedPnfds.keySet()) {
            log.debug("Adding pnfdInfo Id {} to pnfs list in nsdInfo", pnfdInfoId);
            pnfdIds.add(UUID.fromString(pnfdInfoId));
        }
        nsdInfo.setPnfdInfoIds(pnfdIds);

        // clean tmp files
        if (!inputFile.delete()) {
            log.warn("Could not delete temporary NSD content file");
        }

        if (isInternalRequest)
            nsdInfo.setPublished(true);
        else
            nsdInfo.setPublished(false);
        
        List<Appd> appds = appdManagementService.getAssociatedAppD(nsdInfo.getId());
        //Update AppD if any
        for(Appd appd : appds){
            Optional<AppPackageInfoResource> appPackageInfoResourceOptional = appPackageInfoResourceRepository.findByAppdIdAndVersionAndProject(appd.getAppDId(), appd.getAppDVersion(), project);
            if(appPackageInfoResourceOptional.isPresent()) {
                AppPackageInfoResource appPackageInfoResource = appPackageInfoResourceOptional.get();
                appPackageInfoResource.setUsageState(UsageState.IN_USE);
                appPackageInfoResourceRepository.saveAndFlush(appPackageInfoResource);
            }
        }

        // send notification over kafka bus
        if(!nsdInfo.isRetrievedFromMANO() && !nsdInfo.isMultiSite()) {
            List<String> manoIds = checkWhereOnboardNS(nsdInfo);
            List<String> siteOrManoIds = new ArrayList<>();
            Map<String, String> userDefinedData = nsdInfo.getUserDefinedData();
            if (userDefinedData == null || userDefinedData.size() == 0)
                siteOrManoIds.addAll(pluginManger.manoDrivers.keySet());
            else
                for (MANOPlugin mano : pluginManger.manoDrivers.values())
                    if ((userDefinedData.containsKey(mano.getPluginId()) && userDefinedData.get(mano.getPluginId()).equals("yes"))
                            || (userDefinedData.containsKey(mano.getMano().getManoSite()) && userDefinedData.get(mano.getMano().getManoSite()).equals("yes")))
                        siteOrManoIds.add(mano.getPluginId());
            if(siteOrManoIds.size() == 0)
                siteOrManoIds.addAll(pluginManger.manoDrivers.keySet());
            siteOrManoIds.removeIf(id -> !manoIds.contains(id));
            if(siteOrManoIds.size() == 0)
                throw new FailedOperationException("No suitable manos found");

            for(MANOPlugin mano : pluginManger.manoDrivers.values())
                if(nsdInfo.getUserDefinedData().remove(mano.getPluginId()) == null)
                    nsdInfo.getUserDefinedData().remove(mano.getMano().getManoSite());

            UUID operationId = insertOperationInfoInConsumersMap(nsdInfoId,
                    CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION, OperationStatus.SENT, siteOrManoIds);
            nsdInfo.setAcknowledgedOnboardOpConsumers(operationIdToConsumersAck.get(operationId.toString()));

            NsdOnBoardingNotificationMessage msg = new NsdOnBoardingNotificationMessage(nsdInfo.getId().toString(), nsdId.toString(), nsdInfo.getNsdVersion(), project,
                    operationId, ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageNsdsSubfolder + "/" + project + "/" + nsdId.toString() + "/" + nsdInfo.getNsdVersion(), PathType.LOCAL.toString()));
            msg.setIncludedVnfds(includedVnfds);
            msg.setIncludedPnfds(includedPnfds);

            // send notification over kafka bus
            notificationManager.sendNsdOnBoardingNotification(msg);
        }
        nsdInfoRepo.saveAndFlush(nsdInfo);
        log.debug("NSD content uploaded and nsdOnBoardingNotification delivered");
    }

    @Override
    public PnfdInfo createPnfdInfo(CreatePnfdInfoRequest request, String project) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to create a new PNFD info");
        KeyValuePairs kvp = request.getUserDefinedData();
        Map<String, String> targetKvp = new HashMap<>();
        if (kvp != null) {
            for (Map.Entry<String, String> e : kvp.entrySet()) {
                targetKvp.put(e.getKey(), e.getValue());
            }
        }
        PnfdInfoResource pnfdInfoResource = new PnfdInfoResource(targetKvp);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
            try {
                if (!keycloakEnabled || checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    pnfdInfoResource.setProjectId(project);
                } else {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }
        pnfdInfoRepo.saveAndFlush(pnfdInfoResource);
        UUID pnfdInfoId = pnfdInfoResource.getId();
        log.debug("Created PNFD info with ID " + pnfdInfoId);
        PnfdInfo pnfdInfo = buildPnfdInfo(pnfdInfoResource);
        log.debug("Translated internal pnfd info resource into pnfd info");
        return pnfdInfo;
    }

    @Override
    public void deletePnfdInfo(String pnfdInfoId, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to delete an PNFD info");

        if (pnfdInfoId == null)
            throw new MalformattedElementException("Invalid PNFD info ID");
        try {
            UUID id = UUID.fromString(pnfdInfoId);

            Optional<PnfdInfoResource> optional = pnfdInfoRepo.findById(id);

            if (optional.isPresent()) {
                log.debug("Found PNFD info resource with id: " + pnfdInfoId);

                PnfdInfoResource pnfdInfo = optional.get();
                if (project != null) {
                    Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
                    if (!projectOptional.isPresent()) {
                        log.error("Project with id " + project + " does not exist");
                        throw new FailedOperationException("Project with id " + project + " does not exist");
                    }
                }
                if (project != null && !pnfdInfo.getProjectId().equals(project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                } else {
                    try {
                        if (keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                            throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                        }
                    } catch (NotExistingEntityException e) {
                        throw new NotAuthorizedOperationException(e.getMessage());
                    }
                }

                pnfdInfo.isDeletable();

                log.debug("The PNFD info can be removed");
                if (pnfdInfo.getPnfdOnboardingState() == PnfdOnboardingStateType.ONBOARDED
                        || pnfdInfo.getPnfdOnboardingState() == PnfdOnboardingStateType.LOCAL_ONBOARDED
                        || pnfdInfo.getPnfdOnboardingState() == PnfdOnboardingStateType.PROCESSING) {
                    log.debug("The PNFD info is associated to an onboarded PNFD. Removing it");
                    UUID pnfdId = pnfdInfo.getPnfdId();

                    try {
                        storageService.deleteNsd(project, pnfdInfo.getPnfdId().toString(), pnfdInfo.getPnfdVersion());
                    } catch (Exception e) {
                        log.error("Unable to delete PNFD with nsdId {} from fylesystem", pnfdInfo.getPnfdId().toString());
                        log.error("Details: ", e);
                    }

                    UUID operationId = insertOperationInfoInConsumersMap(pnfdInfoId,
                            CatalogueMessageType.PNFD_DELETION_NOTIFICATION, OperationStatus.SENT, null);
                    log.debug("PNFD {} locally removed. Sending nsdDeletionNotificationMessage to bus", pnfdId);
                    PnfdDeletionNotificationMessage msg = new PnfdDeletionNotificationMessage(pnfdInfoId, pnfdId.toString(), pnfdInfo.getPnfdVersion(), project,
                            operationId, ScopeType.LOCAL, OperationStatus.SENT);
                    notificationManager.sendPnfdDeletionNotification(msg);
                }

                pnfdInfoRepo.deleteById(id);
                log.debug("Deleted PNFD info resource with id: " + pnfdInfoId);
            } else {
                log.debug("PNFD info resource with id " + pnfdInfoId + "not found");
                throw new NotExistingEntityException("PNFD info resource with id " + pnfdInfoId + "not found");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + pnfdInfoId);
            throw new MalformattedElementException("Wrong ID format: " + pnfdInfoId);
        }
    }

    @Override
    public PnfdInfoModifications updatePnfdInfo(PnfdInfoModifications pnfdInfoModifications, String pnfdInfoId, String project) throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException {
        return null;
    }

    @Override
    public Object getPnfd(String pnfdInfoId, boolean isInternalRequest, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to retrieve an PNFD content for PNFD info " + pnfdInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }

        PnfdInfoResource pnfdInfo = getPnfdInfoResource(pnfdInfoId);

        if (project != null && !pnfdInfo.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from PNFD info project");
        } else {
            try {
                if (!isInternalRequest && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if ((!isInternalRequest) && (pnfdInfo.getPnfdOnboardingState() != PnfdOnboardingStateType.ONBOARDED
                && pnfdInfo.getPnfdOnboardingState() != PnfdOnboardingStateType.LOCAL_ONBOARDED)) {
            log.error("PNFD info " + pnfdInfoId + " does not have an onboarded PNFD yet");
            throw new NotPermittedOperationException("PNFD info " + pnfdInfoId + " does not have an onboarded PNFD yet");
        }
        UUID pnfdId = pnfdInfo.getPnfdId();
        log.debug("Internal PNFD ID: " + pnfdId);

        ContentType ct = pnfdInfo.getContentType();
        switch (ct) {
            case YAML: {
                // try {
                List<String> pnfdFilenames = pnfdInfo.getPnfdFilename();
                if (pnfdFilenames.size() != 1) {
                    log.error("Found zero or more than one file for PNFD in YAML format. Error");
                    throw new FailedOperationException("Found more than one file for PNFD in YAML format. Error");
                }
                String pnfdFilename = pnfdFilenames.get(0);
                Resource pnfd = storageService.loadFileAsResource(project, pnfdInfo.getPnfdId().toString(), pnfdInfo.getPnfdVersion(), pnfdFilename, false);

                return pnfd;
            }

            default: {
                log.error("Content type not yet supported");
                throw new MethodNotImplementedException("Content type not yet supported");
            }
        }
    }

    @Override
    public PnfdInfo getPnfdInfo(String pnfdInfoId, String project) throws FailedOperationException, NotPermittedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to get an PNFD info");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        PnfdInfoResource pnfdInfoResource = getPnfdInfoResource(pnfdInfoId);

        if (project != null && !pnfdInfoResource.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from PNFD info project");
        } else {
            try {
                if (keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        log.debug("Found PNFD info resource with id: " + pnfdInfoId);
        PnfdInfo pnfdInfo = buildPnfdInfo(pnfdInfoResource);
        log.debug("Built PNFD info with id: " + pnfdInfoId);
        return pnfdInfo;
    }

    @Override
    public List<PnfdInfo> getAllPnfdInfos(String project, UUID pnfdId) throws FailedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to get all PNFD infos");
        if (project != null && !project.equals("*")) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        try {
            if (project != null && !project.equals("*") && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
            }
        } catch (NotExistingEntityException e) {
            throw new NotAuthorizedOperationException(e.getMessage());
        }

        List<PnfdInfoResource> pnfdInfoResources;
        if(pnfdId == null)
            pnfdInfoResources = pnfdInfoRepo.findAll();
        else
            pnfdInfoResources = pnfdInfoRepo.findByPnfdId(pnfdId);

        List<PnfdInfo> pnfdInfos = new ArrayList<>();

        for (PnfdInfoResource pnfdInfoResource : pnfdInfoResources) {
            if (project != null && !project.equals("*") && !pnfdInfoResource.getProjectId().equals(project)) {
                continue;
            } else {
                PnfdInfo pnfdInfo = buildPnfdInfo(pnfdInfoResource);
                pnfdInfos.add(pnfdInfo);
                log.debug("Added Pnfd info " + pnfdInfoResource.getId());
            }
        }
        return pnfdInfos;
    }

    @Override
    public void uploadPnfd(String pnfdInfoId, MultipartFile pnfd, ContentType contentType, boolean isInternalRequest, String project) throws MalformattedElementException, NotExistingEntityException, NotPermittedOperationException, FailedOperationException, MethodNotImplementedException, NotAuthorizedOperationException, AlreadyExistingEntityException {
        log.debug("Processing request to upload PNFD content for PNFD info " + pnfdInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        PnfdInfoResource pnfdInfo = getPnfdInfoResource(pnfdInfoId);

        if (project != null && !pnfdInfo.getProjectId().equals(project)) {
            pnfdInfo.setPnfdOnboardingState(PnfdOnboardingStateType.FAILED);
            pnfdInfoRepo.saveAndFlush(pnfdInfo);
            throw new NotAuthorizedOperationException("Specified project differs from PNFD info project");
        } else {
            try {
                if (!isInternalRequest && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    pnfdInfo.setPnfdOnboardingState(PnfdOnboardingStateType.FAILED);
                    pnfdInfoRepo.saveAndFlush(pnfdInfo);
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project with id" + project);
                }
            } catch (NotExistingEntityException e) {
                pnfdInfo.setPnfdOnboardingState(PnfdOnboardingStateType.FAILED);
                pnfdInfoRepo.saveAndFlush(pnfdInfo);
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if (pnfdInfo.getPnfdOnboardingState() != PnfdOnboardingStateType.CREATED) {
            log.error("PNFD info " + pnfdInfoId + " not in CREATED onboarding state");
            pnfdInfo.setPnfdOnboardingState(PnfdOnboardingStateType.FAILED);
            pnfdInfoRepo.saveAndFlush(pnfdInfo);
            throw new NotPermittedOperationException("PNFD info " + pnfdInfoId + " not in CREATED onboarding state");
        }

        // convert to File
        File inputFile = null;
        try {
            inputFile = convertToFile(pnfd);
        } catch (Exception e) {
            log.error("Error while parsing PNFD in zip format: " + e.getMessage());
            pnfdInfo.setPnfdOnboardingState(PnfdOnboardingStateType.FAILED);
            pnfdInfoRepo.saveAndFlush(pnfdInfo);
            throw new MalformattedElementException("Error while parsing PNFD");
        }

        String pnfdFilename;
        DescriptorTemplate dt;
        UUID pnfdId;

        PnfdOnboardingStateType onboardingStateType = PnfdOnboardingStateType.UPLOADING;

        switch (contentType) {
            case ZIP: {
                try {
                    log.info("PNFD file is in format: zip");

                    // TODO: assuming for now one single file into the zip
                    MultipartFile pnfdMpFile = extractFile(inputFile);
                    // convert to File
                    File pnfdFile = convertToFile(pnfdMpFile);

                    dt = descriptorsParser.fileToDescriptorTemplate(pnfdFile);

                    String pnfdId_string = dt.getMetadata().getDescriptorId();

                    if (!Utilities.isUUID(pnfdId_string)) {
                        throw new MalformattedElementException("PNFD id not in UUID format");
                    }

                    pnfdId = UUID.fromString(pnfdId_string);

                    Optional<PnfdInfoResource> optionalPnfdInfoResource = pnfdInfoRepo.findByPnfdIdAndPnfdVersionAndProjectId(pnfdId, dt.getMetadata().getVersion(), project);
                    if (optionalPnfdInfoResource.isPresent()) {
                        throw new AlreadyExistingEntityException("A PNFD with the same id and version already exists in the project");
                    }

                    pnfdInfo.setPnfdId(pnfdId);

                    log.debug("PNFD successfully parsed - its content is: \n"
                            + DescriptorsParser.descriptorTemplateToString(dt));

                    // pre-set pnfdInfo attributes to properly store PNFDs
                    pnfdInfo.setPnfdVersion(dt.getMetadata().getVersion());

                    pnfdFilename = storageService.storePkg(project, pnfdInfo.getPnfdId().toString(), pnfdInfo.getPnfdVersion(), pnfdMpFile, false);

                    // change contentType to YAML as nsd file is no more zip from now on
                    contentType = ContentType.YAML;

                    log.debug("PNFD file successfully stored");
                    // clean tmp files
                    if (!pnfdFile.delete()) {
                        log.warn("Could not delete temporary PNFD zip content file");
                    }
                } catch (IOException e) {
                    log.error("Error while parsing PNFD in zip format: " + e.getMessage());
                    onboardingStateType = PnfdOnboardingStateType.FAILED;
                    pnfdInfo.setPnfdOnboardingState(onboardingStateType);
                    pnfdInfoRepo.saveAndFlush(pnfdInfo);
                    throw new MalformattedElementException("Error while parsing PNFD");
                } catch (MalformattedElementException e) {
                    log.error("Error while parsing PNFD in zip format: " + e.getMessage());
                    onboardingStateType = PnfdOnboardingStateType.FAILED;
                    pnfdInfo.setPnfdOnboardingState(onboardingStateType);
                    pnfdInfoRepo.saveAndFlush(pnfdInfo);
                    throw new MalformattedElementException("Error while parsing PNFD");
                } catch (FailedOperationException e) {
                    log.error("Error while storing PNFD in zip format: " + e.getMessage());
                    onboardingStateType = PnfdOnboardingStateType.FAILED;
                    pnfdInfo.setPnfdOnboardingState(onboardingStateType);
                    pnfdInfoRepo.saveAndFlush(pnfdInfo);
                    throw new FailedOperationException("Error while storing PNFD");
                } catch (AlreadyExistingEntityException e) {
                    onboardingStateType = PnfdOnboardingStateType.FAILED;
                    pnfdInfo.setPnfdOnboardingState(onboardingStateType);
                    pnfdInfoRepo.saveAndFlush(pnfdInfo);
                    throw new AlreadyExistingEntityException(e.getMessage());
                } catch (Exception e) {
                    log.error("Error while storing PNFD in zip format: " + e.getMessage());
                    onboardingStateType = PnfdOnboardingStateType.FAILED;
                    pnfdInfo.setPnfdOnboardingState(onboardingStateType);
                    pnfdInfoRepo.saveAndFlush(pnfdInfo);
                    throw new FailedOperationException("Error while storing PNFD");
                }
                break;
            }
            case YAML: {
                try {
                    log.info("PNFD file is in format: yaml");

                    dt = descriptorsParser.fileToDescriptorTemplate(inputFile);

                    String pnfdId_string = dt.getMetadata().getDescriptorId();

                    if (!Utilities.isUUID(pnfdId_string)) {
                        throw new MalformattedElementException("PNFD id not in UUID format");
                    }

                    pnfdId = UUID.fromString(pnfdId_string);

                    Optional<PnfdInfoResource> optionalPnfdInfoResource = pnfdInfoRepo.findByPnfdIdAndPnfdVersionAndProjectId(pnfdId, dt.getMetadata().getVersion(), project);
                    if (optionalPnfdInfoResource.isPresent()) {
                        throw new AlreadyExistingEntityException("A PNFD with the same id and version already exists in project");
                    }

                    pnfdInfo.setPnfdId(pnfdId);

                    log.debug("PNFD successfully parsed - its content is: \n"
                            + DescriptorsParser.descriptorTemplateToString(dt));
                    // pre-set pnfdInfo attributes to properly store PNFDs
                    pnfdInfo.setPnfdVersion(dt.getMetadata().getVersion());

                    pnfdFilename = storageService.storePkg(project, pnfdInfo.getPnfdId().toString(), pnfdInfo.getPnfdVersion(), pnfd, false);

                    log.debug("PNFD file successfully stored");

                } catch (IOException e) {
                    log.error("Error while parsing PNFD in yaml format: " + e.getMessage());
                    onboardingStateType = PnfdOnboardingStateType.FAILED;
                    pnfdInfo.setPnfdOnboardingState(onboardingStateType);
                    pnfdInfoRepo.saveAndFlush(pnfdInfo);
                    throw new MalformattedElementException("Error while parsing PNFD");
                } catch (MalformattedElementException e) {
                    log.error("Error while parsing PNFD in yaml format: " + e.getMessage());
                    onboardingStateType = PnfdOnboardingStateType.FAILED;
                    pnfdInfo.setPnfdOnboardingState(onboardingStateType);
                    pnfdInfoRepo.saveAndFlush(pnfdInfo);
                    throw new MalformattedElementException("Error while parsing PNFD");
                } catch (AlreadyExistingEntityException e) {
                    onboardingStateType = PnfdOnboardingStateType.FAILED;
                    pnfdInfo.setPnfdOnboardingState(onboardingStateType);
                    pnfdInfoRepo.saveAndFlush(pnfdInfo);
                    throw new AlreadyExistingEntityException(e.getMessage());
                }
                break;
            }

            default: {
                log.error("Unsupported content type: " + contentType.toString());
                onboardingStateType = PnfdOnboardingStateType.FAILED;
                pnfdInfo.setPnfdOnboardingState(onboardingStateType);
                pnfdInfoRepo.saveAndFlush(pnfdInfo);
                throw new MethodNotImplementedException("Unsupported content type: " + contentType.toString());
            }
        }

        if (pnfdFilename == null || dt == null) {
            onboardingStateType = PnfdOnboardingStateType.FAILED;
            pnfdInfo.setPnfdOnboardingState(onboardingStateType);
            pnfdInfoRepo.saveAndFlush(pnfdInfo);
            throw new FailedOperationException("Invalid internal structures");
        }

        log.debug("Updating PNFD info");
        pnfdInfo.setPnfdOnboardingState(PnfdOnboardingStateType.LOCAL_ONBOARDED);
        pnfdInfo.setPnfdProvider(dt.getMetadata().getVendor());
        pnfdInfo.setPnfdInvariantId(UUID.fromString(dt.getMetadata().getDescriptorId()));
        Map<String, PNFNode> pnfNodes = dt.getTopologyTemplate().getPNFNodes();
        String pnfdName = "";
        if (pnfNodes.size() == 1) {
            for (Entry<String, PNFNode> pnfNode : pnfNodes.entrySet()) {
                pnfdName = pnfNode.getValue().getProperties().getName();
            }
        } else {
            Map<String, String> subMapsProperties = dt.getTopologyTemplate().getSubstituitionMappings().getProperties();
            for (Map.Entry<String, String> entry : subMapsProperties.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("name")) {
                    pnfdName = entry.getValue();
                }
            }
        }

        log.debug("PNFD name: " + pnfdName);
        pnfdInfo.setPnfdName(pnfdName);
        pnfdInfo.setContentType(contentType);
        pnfdInfo.addPnfdFilename(pnfdFilename);

        // clean tmp files
        if (!inputFile.delete()) {
            log.warn("Could not delete temporary PNFD content file");
        }

        List<String> siteOrManoIds = new ArrayList<>();
        Map<String, String> userDefinedData = pnfdInfo.getUserDefinedData();
        if(userDefinedData == null || userDefinedData.size() == 0)
            siteOrManoIds.addAll(pluginManger.manoDrivers.keySet());
        else
            for(MANOPlugin mano : pluginManger.manoDrivers.values())
                if((userDefinedData.containsKey(mano.getPluginId()) && userDefinedData.get(mano.getPluginId()).equals("yes"))
                        || (userDefinedData.containsKey(mano.getMano().getManoSite()) && userDefinedData.get(mano.getMano().getManoSite()).equals("yes")))
                    siteOrManoIds.add(mano.getPluginId());
        if(siteOrManoIds.size() == 0)
            siteOrManoIds.addAll(pluginManger.manoDrivers.keySet());

        for(MANOPlugin mano : pluginManger.manoDrivers.values())
            if(pnfdInfo.getUserDefinedData().remove(mano.getPluginId()) == null)
                pnfdInfo.getUserDefinedData().remove(mano.getMano().getManoSite());

        UUID operationId = insertOperationInfoInConsumersMap(pnfdInfoId,
                CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION, OperationStatus.SENT, siteOrManoIds);
        pnfdInfo.setAcknowledgedOnboardOpConsumers(operationIdToConsumersAck.get(operationId.toString()));

        if (isInternalRequest)
            pnfdInfo.setPublished(true);
        else
            pnfdInfo.setPublished(false);

        pnfdInfoRepo.saveAndFlush(pnfdInfo);
        log.debug("PNFD info updated");

        //TODO modify
        PnfdOnBoardingNotificationMessage msg = new PnfdOnBoardingNotificationMessage(pnfdInfo.getId().toString(), pnfdId.toString(), dt.getMetadata().getVersion(), project,
                operationId, ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageNsdsSubfolder + "/" + project + "/" + pnfdInfo.getPnfdId().toString() + "/" + pnfdInfo.getPnfdVersion(), PathType.LOCAL.toString()));

        // send notification over kafka bus
        notificationManager.sendPnfdOnBoardingNotification(msg);

        log.debug("NSD content uploaded and nsdOnBoardingNotification delivered");
    }

    private PnfdInfoResource getPnfdInfoResource(String pnfdInfoId)
            throws NotExistingEntityException, MalformattedElementException {
        log.debug("Retrieving internal PNFD info resource with ID " + pnfdInfoId);
        try {
            UUID id = UUID.fromString(pnfdInfoId);

            Optional<PnfdInfoResource> optional = pnfdInfoRepo.findById(id);

            if (optional.isPresent()) {
                PnfdInfoResource pnfdInfoResource = optional.get();
                log.debug("Found PNFD info resource with id: " + pnfdInfoId);
                return pnfdInfoResource;
            } else {
                log.debug("PNFD info resource with id " + pnfdInfoId + " not found");
                throw new NotExistingEntityException("PNFD info resource with id " + pnfdInfoId + " not found");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + pnfdInfoId);
            throw new MalformattedElementException("Wrong ID format: " + pnfdInfoId);
        }
    }

    public NSNode getCompositeNsNode(DescriptorTemplate dt) throws MalformattedElementException{
        Map<String, NSNode> nsNodeMap = dt.getTopologyTemplate().getNSNodes();
        for(NSNode nsNode : nsNodeMap.values())
            if(nsNode.getProperties().getDescriptorId().equals(dt.getMetadata().getDescriptorId())
                    && nsNode.getProperties().getVersion().equals(dt.getMetadata().getVersion()))
                    return nsNode;
        return null;
    }

    public Map<String, KeyValuePair> checkNestedNSDs(DescriptorTemplate nsd, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, IOException, NotPermittedOperationException, NotAuthorizedOperationException {

        log.debug("Checking nested NS availability for NSD " + nsd.getMetadata().getDescriptorId() + " with version " + nsd.getMetadata().getVersion());
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        Map<String, NSNode> nsNodes = nsd.getTopologyTemplate().getNSNodes();

        Map<String, KeyValuePair> nestedNsds = new HashMap<>();

        for (Map.Entry<String, NSNode> nsNodeEntry : nsNodes.entrySet()) {

            String nsdId = nsNodeEntry.getValue().getProperties().getDescriptorId();
            String version = nsNodeEntry.getValue().getProperties().getVersion();

            if(nsdId.equals(nsd.getMetadata().getDescriptorId()) && version.equals(nsd.getMetadata().getVersion()))
                continue;

            Optional<NsdInfoResource> optional = nsdInfoRepo.findByNsdIdAndNsdVersionAndProjectId(UUID.fromString(nsdId), version, project);
            if (!optional.isPresent()) {
                throw new NotExistingEntityException("Nested NSD for nsdId " + nsdId + "and version " + version + " not find in project " + project);
            }

            NsdInfoResource nsdInfoResource = optional.get();
            List<String> nsdFileNames = nsdInfoResource.getNsdFilename();
            if (nsdFileNames.size() != 1) {
                log.error("Found zero or more than one file for NSD in YAML format. Error");
                throw new FailedOperationException("Found more than one file for NSD in YAML format. Error");
            }
            String fileName = nsdFileNames.get(0);

            log.debug("Searching NSD {} with nsdId {} and version {} in project {}", fileName, nsdId, version, project);
            File nsd_file = storageService.loadFileAsResource(project, nsdId, version, fileName, false).getFile();
            DescriptorTemplate nestedNsd = descriptorsParser.fileToDescriptorTemplate(nsd_file);

            log.debug("Nested NSD successfully parsed - its content is: \n"
                    + DescriptorsParser.descriptorTemplateToString(nestedNsd));

            if (!nestedNsds.containsKey(nsdInfoResource.getId().toString()))
                nestedNsds.put(nsdInfoResource.getId().toString(), new KeyValuePair(rootDir + ConfigurationParameters.storageNsdsSubfolder + "/" + project + "/" + nsdId + "/" + version, PathType.LOCAL.toString()));
        }

        return nestedNsds;
    }

    public Map<String, KeyValuePair> checkVNFPkgs(DescriptorTemplate nsd, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, IOException, NotPermittedOperationException, NotAuthorizedOperationException {

        log.debug("Checking VNF Pkgs availability for NSD " + nsd.getMetadata().getDescriptorId() + " with version " + nsd.getMetadata().getVersion());
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        Map<String, VNFNode> vnfNodes = nsd.getTopologyTemplate().getVNFNodes();
        //Map<String, DescriptorTemplate> vnfds = new HashMap<>();
        Map<String, KeyValuePair> includedVnfds = new HashMap<>();

        List<VnfPkgInfoResource> vnfPkgInfoResources = new ArrayList<>();

        for (Map.Entry<String, VNFNode> vnfNodeEntry : vnfNodes.entrySet()) {

            String vnfdId = vnfNodeEntry.getValue().getProperties().getDescriptorId();
            String version = vnfNodeEntry.getValue().getProperties().getDescriptorVersion();

            Optional<VnfPkgInfoResource> optional = vnfPkgInfoRepository.findByVnfdIdAndVnfdVersionAndProjectId(UUID.fromString(vnfdId), version, project);
            if (!optional.isPresent()) {
                throw new NotExistingEntityException("VNFD filename for vnfdId " + vnfdId + "and version " + version + " not find in project " + project);
            }

            VnfPkgInfoResource vnfPkgInfoResource = optional.get();
            String fileName = vnfPkgInfoResource.getVnfdFilename();

            log.debug("Searching VNFD {} with vnfdId {} and version {} in project {}", fileName, vnfdId, version, project);
            File vnfd_file = storageService.loadFileAsResource(project, vnfdId, version, fileName, true).getFile();
            DescriptorTemplate vnfd = descriptorsParser.fileToDescriptorTemplate(vnfd_file);

            log.debug("VNFD successfully parsed - its content is: \n"
                    + DescriptorsParser.descriptorTemplateToString(vnfd));

            if (!includedVnfds.containsKey(vnfPkgInfoResource.getId().toString())) {
                includedVnfds.put(vnfPkgInfoResource.getId().toString(), new KeyValuePair(rootDir + ConfigurationParameters.storageVnfpkgsSubfolder + "/" + project + "/" + vnfdId + "/" + version, PathType.LOCAL.toString()));
                vnfPkgInfoResources.add(vnfPkgInfoResource);
            }

            if (includedVnfds.isEmpty()) {
                throw new NotPermittedOperationException("VNFDs listed in NSD are not available in Catalogue's storage");
            }
        }

        for (VnfPkgInfoResource resource : vnfPkgInfoResources) {
            List<String> parentNsds = resource.getParentNsds();
            parentNsds.add(nsd.getMetadata().getDescriptorId());
            resource.setParentNsds(parentNsds);
            vnfPkgInfoRepository.saveAndFlush(resource);
        }

        return includedVnfds;
    }

    public Map<String, KeyValuePair> checkPNFDs(DescriptorTemplate nsd, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, IOException, NotPermittedOperationException, NotAuthorizedOperationException {

        log.debug("Checking PNFDs availability for NSD " + nsd.getMetadata().getDescriptorId() + " with version " + nsd.getMetadata().getVersion());

        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }

        Map<String, PNFNode> pnfNodes = nsd.getTopologyTemplate().getPNFNodes();
        Map<String, KeyValuePair> includedPnfds = new HashMap<>();

        List<PnfdInfoResource> pnfdInfoResources = new ArrayList<>();

        for (Map.Entry<String, PNFNode> pnfNodeEntry : pnfNodes.entrySet()) {

            String pnfdId = pnfNodeEntry.getValue().getProperties().getDescriptorId();
            String version = pnfNodeEntry.getValue().getProperties().getVersion();

            Optional<PnfdInfoResource> optional = pnfdInfoRepo.findByPnfdIdAndPnfdVersionAndProjectId(UUID.fromString(pnfdId), version, project);
            if (!optional.isPresent()) {
                throw new NotExistingEntityException("PNFD filename for pnfdId " + pnfdId + " and version " + version + " not find in project " + project);
            }
            PnfdInfoResource pnfdInfoResource = optional.get();
            String fileName = pnfdInfoResource.getPnfdFilename().get(0);

            log.debug("Searching PNFD {} with pnfdId {} and version {}", fileName, pnfdId, version);
            File pnfd_file = storageService.loadFileAsResource(project, pnfdId, version, fileName, false).getFile();
            DescriptorTemplate pnfd = descriptorsParser.fileToDescriptorTemplate(pnfd_file);

            log.debug("PNFD successfully parsed - its content is: \n"
                    + DescriptorsParser.descriptorTemplateToString(pnfd));

            if (!includedPnfds.containsKey(pnfdInfoResource.getId().toString())) {
                includedPnfds.put(pnfdInfoResource.getId().toString(), new KeyValuePair(rootDir + ConfigurationParameters.storageNsdsSubfolder + "/" + pnfdId + "/" + version, PathType.LOCAL.toString()));
                pnfdInfoResources.add(pnfdInfoResource);
            }

            if (includedPnfds.isEmpty()) {
                throw new NotPermittedOperationException("PNFDs listed in NSD are not available in Catalogue's storage");
            }
        }

        for (PnfdInfoResource resource : pnfdInfoResources) {
            List<String> parentNsds = resource.getParentNsds();
            parentNsds.add(nsd.getMetadata().getDescriptorId());
            resource.setParentNsds(parentNsds);
            pnfdInfoRepo.saveAndFlush(resource);
        }

        return includedPnfds;
    }

    public synchronized void updateNsdInfoOperationStatus(String nsdInfoId, String manoId, OperationStatus opStatus,
                                                          CatalogueMessageType messageType) throws NotExistingEntityException {
        log.debug("Retrieving nsdInfoResource {} from DB for updating with onboarding status info for plugin {}",
                nsdInfoId, manoId);
        Optional<NsdInfoResource> optionalNsdInfoResource = nsdInfoRepo.findById(UUID.fromString(nsdInfoId));

        if (optionalNsdInfoResource.isPresent()) {
            try {
                NsdInfoResource nsdInfoResource = optionalNsdInfoResource.get();

                Map<String, NotificationResource> ackMap = new HashMap<>();
                if (nsdInfoResource.getAcknowledgedOnboardOpConsumers() != null) {
                    ackMap = nsdInfoResource.getAcknowledgedOnboardOpConsumers();
                }
                ackMap.put(manoId, new NotificationResource(nsdInfoId, messageType, opStatus, PluginType.MANO));
                nsdInfoResource.setAcknowledgedOnboardOpConsumers(ackMap);

                if (messageType == CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION) {
                    if(!optionalNsdInfoResource.get().isRetrievedFromMANO()) {
                        if (opStatus.equals(OperationStatus.SUCCESSFULLY_DONE))
                            optionalNsdInfoResource.get().getUserDefinedData().put(manoId, "yes");
                        else
                            optionalNsdInfoResource.get().getUserDefinedData().put(manoId, "no");
                    }
                    log.debug("Checking NSD with nsdInfoId {} onboarding state", nsdInfoId);
                    nsdInfoResource.setNsdOnboardingState(checkNsdOnboardingState(nsdInfoId, ackMap));
                }

                log.debug("Updating NsdInfoResource {} with onboardingState {}", nsdInfoId,
                        nsdInfoResource.getNsdOnboardingState());
                nsdInfoRepo.saveAndFlush(nsdInfoResource);
            } catch (Exception e) {
                log.error("Error while updating NsdInfoResource with nsdInfoId: " + nsdInfoId);
                log.error("Details: ", e);
            }
        } else {
            throw new NotExistingEntityException("NsdInfoResource " + nsdInfoId + " not present in DB");
        }
    }

    private NsdOnboardingStateType checkNsdOnboardingState(String nsdInfoId, Map<String, NotificationResource> ackMap) {

        for (Entry<String, NotificationResource> entry : ackMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION && entry.getValue().getPluginType() == PluginType.MANO) {
                if (entry.getValue().getOpStatus() == OperationStatus.SUCCESSFULLY_DONE) {
                    log.info("NSD with nsdInfoId {} is onboarded in at least one MANO", nsdInfoId);
                    return NsdOnboardingStateType.ONBOARDED;
                }
                /*
                    // TODO: Decide how to handle MANO onboarding failures.
                    return NsdOnboardingStateType.LOCAL_ONBOARDED;
                } else if (entry.getValue().getOpStatus() == OperationStatus.SENT
                        || entry.getValue().getOpStatus() == OperationStatus.RECEIVED
                        || entry.getValue().getOpStatus() == OperationStatus.PROCESSING) {
                    log.debug("NSD with nsdInfoId {} onboarding still in progress for mano with manoId {}");
                    return NsdOnboardingStateType.LOCAL_ONBOARDED;
                } */
            }
        }
        log.debug("NSD with nsdInfoId " + nsdInfoId + " is not onboarded in any MANO");
        return NsdOnboardingStateType.LOCAL_ONBOARDED;
    }

    private NsdInfo buildNsdInfo(NsdInfoResource nsdInfoResource) {
        log.debug("Building NSD info from internal repo");
        NsdInfo nsdInfo = new NsdInfo();
        nsdInfo.setId(nsdInfoResource.getId());
        NsdLinksType links = new NsdLinksType();
        links.setSelf("/nsd/v1/ns_descriptors/" + nsdInfoResource.getId());
        links.setNsdContent("/nsd/v1/ns_descriptors/" + nsdInfoResource.getId() + "/nsd_content");
        nsdInfo.setLinks(links);
        nsdInfo.setNestedNsdInfoIds(nsdInfoResource.getNestedNsdInfoIds());
        nsdInfo.setNsdDesigner(nsdInfoResource.getNsdDesigner());
        nsdInfo.setNsdId(nsdInfoResource.getNsdId());
        nsdInfo.setNsdInvariantId(nsdInfoResource.getNsdInvariantId());
        nsdInfo.setNsdName(nsdInfoResource.getNsdName());
        nsdInfo.setNsdOnboardingState(nsdInfoResource.getNsdOnboardingState());
        nsdInfo.setNsdOperationalState(nsdInfoResource.getNsdOperationalState());
        nsdInfo.setNsdUsageState(nsdInfoResource.getNsdUsageState());
        nsdInfo.setNsdVersion(nsdInfoResource.getNsdVersion());
        nsdInfo.setOnboardingFailureDetails(null);
        nsdInfo.setPnfdInfoIds(nsdInfoResource.getPnfdInfoIds());
        KeyValuePairs kvp = new KeyValuePairs();
        kvp.putAll(nsdInfoResource.getUserDefinedData());
        nsdInfo.setUserDefinedData(kvp);
        nsdInfo.setVnfPkgIds(nsdInfoResource.getVnfPkgIds());
        nsdInfo.setProjectId(nsdInfoResource.getProjectId());

        Map<String, NotificationResource> acksMap = nsdInfoResource.getAcknowledgedOnboardOpConsumers();
        Map<String, NsdOnboardingStateType> manoIdToOnboardingStatus = new HashMap<>();
        for (Entry<String, NotificationResource> entry : acksMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION) {
                NsdOnboardingStateType nsdOnboardingStateType = NsdOnboardingStateType.UPLOADING;
                switch (entry.getValue().getOpStatus()) {
                    case SENT:
                        nsdOnboardingStateType = NsdOnboardingStateType.UPLOADING;
                        break;
                    case RECEIVED:
                        nsdOnboardingStateType = NsdOnboardingStateType.SKIPPED;
                        break;
                    case PROCESSING:
                        nsdOnboardingStateType = NsdOnboardingStateType.PROCESSING;
                        break;
                    case FAILED:
                        nsdOnboardingStateType = NsdOnboardingStateType.FAILED;
                        break;
                    case SUCCESSFULLY_DONE:
                        nsdOnboardingStateType = NsdOnboardingStateType.ONBOARDED;
                        break;
                }
                if (entry.getValue().getPluginType() == PluginType.MANO) {
                    manoIdToOnboardingStatus.putIfAbsent(entry.getKey(), nsdOnboardingStateType);
                }
            }
        }

        nsdInfo.setManoIdToOnboardingStatus(manoIdToOnboardingStatus);

        if (nsdInfoResource.isPublished()) {
            nsdInfo.setC2cOnboardingState(C2COnboardingStateType.PUBLISHED);
        } else {
            nsdInfo.setC2cOnboardingState(C2COnboardingStateType.UNPUBLISHED);
        }

        return nsdInfo;
    }

    public synchronized void updatePnfdInfoOperationStatus(String pnfdInfoId, String manoId, OperationStatus opStatus,
                                                           CatalogueMessageType messageType) throws NotExistingEntityException {

        log.debug("Retrieving pnfdInfoResource {} from DB for updating with onboarding status info for plugin {}",
                pnfdInfoId, manoId);
        Optional<PnfdInfoResource> optionalPnfdInfoResource = pnfdInfoRepo.findById(UUID.fromString(pnfdInfoId));

        if (optionalPnfdInfoResource.isPresent()) {
            try {
                PnfdInfoResource pnfdInfoResource = optionalPnfdInfoResource.get();

                Map<String, NotificationResource> ackMap = new HashMap<>();
                if (pnfdInfoResource.getAcknowledgedOnboardOpConsumers() != null) {
                    ackMap = pnfdInfoResource.getAcknowledgedOnboardOpConsumers();
                }
                ackMap.put(manoId, new NotificationResource(pnfdInfoId, messageType, opStatus, PluginType.MANO));
                pnfdInfoResource.setAcknowledgedOnboardOpConsumers(ackMap);

                if (messageType == CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION) {
                    if (opStatus.equals(OperationStatus.SUCCESSFULLY_DONE))
                        optionalPnfdInfoResource.get().getUserDefinedData().put(manoId, "yes");
                    else
                        optionalPnfdInfoResource.get().getUserDefinedData().put(manoId, "no");
                    log.debug("Checking PNFD with pnfdInfoId {} onboarding state", pnfdInfoId);
                    pnfdInfoResource.setPnfdOnboardingState(checkPnfdOnboardingState(pnfdInfoId, ackMap));
                }

                log.debug("Updating PnfdInfoResource {} with onboardingState {}", pnfdInfoId,
                        pnfdInfoResource.getPnfdOnboardingState());
                pnfdInfoRepo.saveAndFlush(pnfdInfoResource);
            } catch (Exception e) {
                log.error("Error while updating PnfdInfoResource with pnfdInfoId: " + pnfdInfoId);
                log.error("Details: ", e);
            }
        } else {
            throw new NotExistingEntityException("PnfdInfoResource " + pnfdInfoId + " not present in DB");
        }
    }

    private PnfdOnboardingStateType checkPnfdOnboardingState(String pnfdInfoId, Map<String, NotificationResource> ackMap) {

        for (Entry<String, NotificationResource> entry : ackMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION
                    && entry.getValue().getPluginType() == PluginType.MANO) {
                if (entry.getValue().getOpStatus() == OperationStatus.SUCCESSFULLY_DONE) {
                    log.error("PNFD with pnfdInfoId {} is onboarded in at least one MANO", pnfdInfoId);
                    return PnfdOnboardingStateType.ONBOARDED;
                } /*else if (entry.getValue().getOpStatus() == OperationStatus.SENT
                        || entry.getValue().getOpStatus() == OperationStatus.RECEIVED
                        || entry.getValue().getOpStatus() == OperationStatus.PROCESSING) {
                    log.debug("PNFD with pnfdInfoId {} onboarding still in progress for mano with manoId {}");
                    return PnfdOnboardingStateType.LOCAL_ONBOARDED;
                }*/
            }
        }
        log.debug("PNFD with pnfdInfoId " + pnfdInfoId + " is not onboarded in any MANO");
        return PnfdOnboardingStateType.LOCAL_ONBOARDED;
    }

    private PnfdInfo buildPnfdInfo(PnfdInfoResource pnfdInfoResource) {
        log.debug("Building PNFD info from internal repo");
        PnfdInfo pnfdInfo = new PnfdInfo();
        pnfdInfo.setId(pnfdInfoResource.getId());
        PnfdLinksType links = new PnfdLinksType();
        links.setSelf("/nsd/v1/pnf_descriptors/" + pnfdInfoResource.getId());
        links.setPnfdContent("/nsd/v1/pnf_descriptors/" + pnfdInfoResource.getId() + "/pnfd_content");
        pnfdInfo.setLinks(links);
        pnfdInfo.setPnfdProvider(pnfdInfoResource.getPnfdProvider());
        pnfdInfo.setPnfdId(pnfdInfoResource.getPnfdId());
        pnfdInfo.setPnfdInvariantId(pnfdInfoResource.getPnfdInvariantId());
        pnfdInfo.setPnfdName(pnfdInfoResource.getPnfdName());
        pnfdInfo.setPnfdOnboardingState(pnfdInfoResource.getPnfdOnboardingState());
        pnfdInfo.setPnfdUsageState(pnfdInfoResource.getPnfdUsageState());
        pnfdInfo.setPnfdVersion(pnfdInfoResource.getPnfdVersion());
        pnfdInfo.setOnboardingFailureDetails(null);
        KeyValuePairs kvp = new KeyValuePairs();
        kvp.putAll(pnfdInfoResource.getUserDefinedData());
        pnfdInfo.setUserDefinedData(kvp);
        pnfdInfo.setProjectId(pnfdInfoResource.getProjectId());

        Map<String, NotificationResource> acksMap = pnfdInfoResource.getAcknowledgedOnboardOpConsumers();
        Map<String, PnfdOnboardingStateType> manoIdToOnboardingStatus = new HashMap<>();

        for (Entry<String, NotificationResource> entry : acksMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION) {
                PnfdOnboardingStateType pnfdOnboardingStateType = PnfdOnboardingStateType.UPLOADING;
                switch (entry.getValue().getOpStatus()) {
                    case SENT:
                        pnfdOnboardingStateType = PnfdOnboardingStateType.UPLOADING;
                        break;
                    case RECEIVED:
                        pnfdOnboardingStateType = PnfdOnboardingStateType.SKIPPED;
                        break;
                    case PROCESSING:
                        pnfdOnboardingStateType = PnfdOnboardingStateType.PROCESSING;
                        break;
                    case FAILED:
                        pnfdOnboardingStateType = PnfdOnboardingStateType.FAILED;
                        break;
                    case SUCCESSFULLY_DONE:
                        pnfdOnboardingStateType = PnfdOnboardingStateType.ONBOARDED;
                }
                if (entry.getValue().getPluginType() == PluginType.MANO) {
                    manoIdToOnboardingStatus.putIfAbsent(entry.getKey(), pnfdOnboardingStateType);
                }
            }
        }

        pnfdInfo.setManoIdToOnboardingStatus(manoIdToOnboardingStatus);

        if (pnfdInfoResource.isPublished()) {
            pnfdInfo.setC2cOnboardingState(C2COnboardingStateType.PUBLISHED);
        } else {
            pnfdInfo.setC2cOnboardingState(C2COnboardingStateType.UNPUBLISHED);
        }

        return pnfdInfo;
    }
}