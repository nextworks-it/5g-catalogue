package it.nextworks.nfvmano.catalogue.engine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.auth.AuthUtilities;
import it.nextworks.nfvmano.catalogue.auth.projectmanagement.ProjectResource;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.VnfPkgDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.VnfPkgOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.common.ConfigurationParameters;
import it.nextworks.nfvmano.catalogue.engine.elements.ContentType;
import it.nextworks.nfvmano.catalogue.engine.resources.NotificationResource;
import it.nextworks.nfvmano.catalogue.engine.resources.VnfPkgInfoResource;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.KeyValuePairs;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.*;
import it.nextworks.nfvmano.catalogue.plugins.PluginsManager;
import it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.C2COnboardingStateType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.PluginType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.repos.MANORepository;
import it.nextworks.nfvmano.catalogue.repos.ProjectRepository;
import it.nextworks.nfvmano.catalogue.repos.UserRepository;
import it.nextworks.nfvmano.catalogue.repos.VnfPkgInfoRepository;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.ArchiveParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.elements.CSARInfo;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import org.junit.internal.runners.statements.Fail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class VnfPackageManagementService implements VnfPackageManagementInterface {

    private static final Logger log = LoggerFactory.getLogger(VnfPackageManagementService.class);

    @Autowired
    private VnfPkgInfoRepository vnfPkgInfoRepository;

    @Autowired
    private FileSystemStorageService storageService;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private NsdManagementService nsdMgmtService;

    @Autowired
    private MANORepository MANORepository;

    @Autowired
    private ArchiveParser archiveParser;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PluginsManager pluginManger;

    private Map<String, Map<String, NotificationResource>> operationIdToConsumersAck = new HashMap<>();

    @Value("${catalogue.storageRootDir}")
    private String rootDir;

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    @Value("${mano.startup.sync}")
    private boolean startupSync;

    @PostConstruct
    private void startSync(){
        if(startupSync) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable syncTask = this::startupSync;
            scheduler.schedule(syncTask, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void startupSync(){
        log.info("Starting MANO VNF synchronization");
        for (MANOPlugin manoPlugin : pluginManger.manoDrivers.values()) {
            List<ProjectResource> projectResourceList = projectRepository.findAll();
            for (ProjectResource project : projectResourceList) {
                Map<String, String> vnfFromManoList = manoPlugin.getAllVnfd(project.getProjectId());
                if (vnfFromManoList == null) {
                    log.error("Project " + project.getProjectId() + " - Cannot retrieve VNFDs list from MANO with ID " + manoPlugin.getPluginId());
                    continue;
                }
                List<VnfPkgInfoResource> vnfPkgInfoResourceList = vnfPkgInfoRepository.findByProjectId(project.getProjectId());
                for (Map.Entry<String, String> vnfFromMano : vnfFromManoList.entrySet()) {
                    String vnfdId = vnfFromMano.getKey();
                    String vnfdVersion = vnfFromMano.getValue();
                    if(!Utilities.isUUID(vnfdId)){
                        log.error("Project " + project.getProjectId() + " - Cannot retrieve VNFD with ID " + vnfdId + " and version " + vnfdVersion + " from MANO with ID " + manoPlugin.getPluginId() + ", ID is not in UUID format");
                        continue;
                    }
                    Optional<VnfPkgInfoResource> optionalVnfPkgInfoResource = vnfPkgInfoRepository.findByVnfdIdAndVnfdVersionAndProjectId(UUID.fromString(vnfdId), vnfdVersion, project.getProjectId());
                    if (!optionalVnfPkgInfoResource.isPresent()) {
                        if(manoPlugin.getManoType().toString().startsWith("OSM")){//Avoid sync among projects of VNFs uploaded from NBI, OSM plugin doesn't handle projects
                            boolean isRetrievedFromMano = true;
                            List<VnfPkgInfoResource> infoResources = vnfPkgInfoRepository.findByVnfdIdAndVnfdVersion(UUID.fromString(vnfdId), vnfdVersion);
                            for(VnfPkgInfoResource infoResource : infoResources)
                                if (!infoResource.isRetrievedFromMANO()) {
                                    isRetrievedFromMano = false;
                                    break;
                                }
                            if(!isRetrievedFromMano)
                                continue;
                        }
                        log.info("Project {} - Uploading VNFD with ID {} and version {} retrieved from MANO with ID {}", project.getProjectId(), vnfdId, vnfdVersion, manoPlugin.getPluginId());
                        KeyValuePair vnfPath = manoPlugin.getTranslatedPkgPath(vnfdId, vnfdVersion, project.getProjectId());
                        if (vnfPath == null) {
                            log.error("Project " + project.getProjectId() + " - Cannot retrieve VNFD with ID " + vnfdId + " and version " + vnfdVersion + " from MANO with ID " + manoPlugin.getPluginId());
                            continue;
                        }
                        CreateVnfPkgInfoRequest request = new CreateVnfPkgInfoRequest();
                        KeyValuePairs userDefinedData = new KeyValuePairs();
                        userDefinedData.put("isRetrievedFromMANO", "yes");
                        userDefinedData.put(manoPlugin.getPluginId(), "yes");
                        request.setUserDefinedData(userDefinedData);
                        try {
                            VnfPkgInfo vnfPkgInfo = createVnfPkgInfo(request, project.getProjectId(), true);
                            File vnfPkg = null;
                            if (vnfPath.getValue().equals(PathType.LOCAL.toString())) {
                                vnfPkg = new File(vnfPath.getKey());
                            } else {
                                log.error("Path Type not currently supported");
                                continue;
                            }
                            MultipartFile multipartFile = Utilities.createMultiPartFromFile(vnfPkg);
                            uploadVnfPkg(vnfPkgInfo.getId().toString(), multipartFile, ContentType.ZIP, false, project.getProjectId());
                            updateVnfPkgInfoOperationStatus(vnfPkgInfo.getId().toString(), manoPlugin.getPluginId(), OperationStatus.SUCCESSFULLY_DONE, CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION);
                            manoPlugin.notifyOnboarding(vnfPkgInfo.getId().toString(), vnfdId, vnfdVersion, project.getProjectId(), OperationStatus.SUCCESSFULLY_DONE);
                        } catch (Exception e) {
                            log.error(e.getMessage());
                            log.debug(null, e);
                            manoPlugin.notifyOnboarding(null, vnfdId, vnfdVersion, project.getProjectId(), OperationStatus.FAILED);
                        }
                    } else {
                        log.info("Project {} - VNFD with ID {} and version {} retrieved from MANO with ID {} is already present", project.getProjectId(), vnfdId, vnfdVersion, manoPlugin.getPluginId());
                        VnfPkgInfo vnfPkgInfo = buildVnfPkgInfo(optionalVnfPkgInfoResource.get());
                        if (vnfPkgInfo.getManoIdToOnboardingStatus().get(manoPlugin.getPluginId()) == null || !vnfPkgInfo.getManoIdToOnboardingStatus().get(manoPlugin.getPluginId()).equals(PackageOnboardingStateType.ONBOARDED)) {
                            try {
                                optionalVnfPkgInfoResource.get().getUserDefinedData().put(manoPlugin.getPluginId(), "yes");
                                vnfPkgInfoRepository.saveAndFlush(optionalVnfPkgInfoResource.get());
                                updateVnfPkgInfoOperationStatus(vnfPkgInfo.getId().toString(), manoPlugin.getPluginId(), OperationStatus.SUCCESSFULLY_DONE, CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION);
                                manoPlugin.notifyOnboarding(vnfPkgInfo.getId().toString(), vnfdId, vnfdVersion, project.getProjectId(), OperationStatus.SUCCESSFULLY_DONE);
                            } catch (NotExistingEntityException e) {
                                log.error(e.getMessage());
                                log.debug(null, e);
                                manoPlugin.notifyOnboarding(null, vnfdId, vnfdVersion, project.getProjectId(), OperationStatus.FAILED);
                            }
                        }
                        vnfPkgInfoResourceList.removeIf(t -> t.getId().equals(optionalVnfPkgInfoResource.get().getId()));
                    }
                }
                for (VnfPkgInfoResource vnfPkgInfoResource : vnfPkgInfoResourceList) {
                    if(vnfPkgInfoResource.getAcknowledgedOnboardOpConsumers().get(manoPlugin.getPluginId()) != null && vnfPkgInfoResource.getAcknowledgedOnboardOpConsumers().get(manoPlugin.getPluginId()).getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                        log.info("Project {} - VNFD with ID {} and version {} no longer present in MANO with ID {}", project.getProjectId(), vnfPkgInfoResource.getVnfdId(), vnfPkgInfoResource.getVnfdVersion(), manoPlugin.getPluginId());
                        if (vnfPkgInfoResource.isRetrievedFromMANO()) {
                            try {
                                updateVnfPkgInfoOperationStatus(vnfPkgInfoResource.getId().toString(), manoPlugin.getPluginId(), OperationStatus.RECEIVED, CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION);
                                Optional<VnfPkgInfoResource> targetVnfPkgInfoResource = vnfPkgInfoRepository.findById(vnfPkgInfoResource.getId());
                                if (targetVnfPkgInfoResource.isPresent() && !targetVnfPkgInfoResource.get().getOnboardingState().equals(PackageOnboardingStateType.ONBOARDED)) {
                                    log.info("Project {} - Going to delete VNFD with ID {} and version {}", project.getProjectId(), vnfPkgInfoResource.getVnfdId(), vnfPkgInfoResource.getVnfdVersion());
                                    VnfPkgInfoModifications request = new VnfPkgInfoModifications();
                                    request.setOperationalState(PackageOperationalStateType.DISABLED);
                                    updateVnfPkgInfo(request, vnfPkgInfoResource.getId().toString(), project.getProjectId(), true);
                                    deleteVnfPkgInfo(vnfPkgInfoResource.getId().toString(), project.getProjectId(), true);
                                    manoPlugin.notifyDelete(vnfPkgInfoResource.getId().toString(), vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), project.getProjectId(), OperationStatus.SUCCESSFULLY_DONE);
                                }else{
                                    manoPlugin.notifyDelete(vnfPkgInfoResource.getId().toString(), vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), project.getProjectId(), OperationStatus.SUCCESSFULLY_DONE);
                                }
                            }catch (NotPermittedOperationException e){
                                log.error("Project {} - Failed to delete VNFD with ID {} and version {}, it is not deletable", project.getProjectId(), vnfPkgInfoResource.getVnfdId(), vnfPkgInfoResource.getVnfdVersion());
                                manoPlugin.notifyDelete(vnfPkgInfoResource.getId().toString(), vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), project.getProjectId(), OperationStatus.FAILED);
                                List<String> siteOrManoIds = new ArrayList<>();
                                siteOrManoIds.add(manoPlugin.getPluginId());
                                VnfPkgOnBoardingNotificationMessage msg =
                                        new VnfPkgOnBoardingNotificationMessage(vnfPkgInfoResource.getId().toString(), vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), project.getProjectId(), UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageVnfpkgsSubfolder + "/" + project.getProjectId() + "/" + vnfPkgInfoResource.getVnfdId() + "/" + vnfPkgInfoResource.getVnfdVersion(), PathType.LOCAL.toString()));
                                try {
                                    notificationManager.sendVnfPkgOnBoardingNotification(msg);
                                } catch (FailedOperationException e1) {
                                    log.error(e1.getMessage());
                                    log.debug(null, e1);
                                }
                            } catch (Exception e) {
                                log.error("Project {} - Failed to delete VNFD with ID {} and version {}", project.getProjectId(), vnfPkgInfoResource.getVnfdId(), vnfPkgInfoResource.getVnfdVersion());
                                log.debug(null, e);
                                manoPlugin.notifyDelete(vnfPkgInfoResource.getId().toString(), vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), project.getProjectId(), OperationStatus.FAILED);
                            }
                        } else {
                            log.info("Project {} - Sending VNF Onboarding notification for VNFD with ID {} and version {} to MANO with ID {}", project.getProjectId(), vnfPkgInfoResource.getVnfdId(), vnfPkgInfoResource.getVnfdVersion(), manoPlugin.getPluginId());
                            manoPlugin.notifyDelete(vnfPkgInfoResource.getId().toString(), vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), project.getProjectId(), OperationStatus.FAILED);
                            List<String> siteOrManoIds = new ArrayList<>();
                            siteOrManoIds.add(manoPlugin.getPluginId());
                            VnfPkgOnBoardingNotificationMessage msg =
                                    new VnfPkgOnBoardingNotificationMessage(vnfPkgInfoResource.getId().toString(), vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), project.getProjectId(), UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageVnfpkgsSubfolder + "/" + project.getProjectId() + "/" + vnfPkgInfoResource.getVnfdId() + "/" + vnfPkgInfoResource.getVnfdVersion(), PathType.LOCAL.toString()));
                            try {
                                notificationManager.sendVnfPkgOnBoardingNotification(msg);
                            } catch (FailedOperationException e) {
                                log.error(e.getMessage());
                                log.debug(null, e);
                            }
                        }
                    }
                }
            }
        }
        log.info("MANO VNF synchronization completed");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable syncTask = () -> nsdMgmtService.startupSync();
        scheduler.schedule(syncTask, 1, TimeUnit.SECONDS);
    }

    public void runtimeVnfOnBoarding(VnfPkgOnBoardingNotificationMessage notification){
        List<String> projects = new ArrayList<>();
        if(notification.getProject() == null || notification.getProject().equals("all")){
            List<ProjectResource> projectResourceList = projectRepository.findAll();
            for(ProjectResource projectResource : projectResourceList)
                projects.add(projectResource.getProjectId());
        }else
            projects.add(notification.getProject());

        for(String project : projects){
            Optional<VnfPkgInfoResource> optionalVnfPkgInfoResource = vnfPkgInfoRepository.findByVnfdIdAndVnfdVersionAndProjectId(UUID.fromString(notification.getVnfdId()), notification.getVnfdVersion(), project);
            if (!optionalVnfPkgInfoResource.isPresent()) {
                log.info("Project {} - Uploading VNFD with ID {} and version {} retrieved from MANO with ID {}", project, notification.getVnfdId(), notification.getVnfdVersion(), notification.getPluginId());
                CreateVnfPkgInfoRequest request = new CreateVnfPkgInfoRequest();
                KeyValuePairs userDefinedData = new KeyValuePairs();
                userDefinedData.put("isRetrievedFromMANO", "yes");
                userDefinedData.put(notification.getPluginId(), "yes");
                request.setUserDefinedData(userDefinedData);
                try {
                    VnfPkgInfo vnfPkgInfo = createVnfPkgInfo(request, project, true);
                    File vnfPkg;
                    if (notification.getPackagePath().getValue().equals(PathType.LOCAL.toString())) {
                        vnfPkg = new File(notification.getPackagePath().getKey());
                    } else {
                        throw new MethodNotImplementedException("Path Type not currently supported");
                    }
                    MultipartFile multipartFile = Utilities.createMultiPartFromFile(vnfPkg);
                    uploadVnfPkg(vnfPkgInfo.getId().toString(), multipartFile, ContentType.ZIP, false, project);
                    updateVnfPkgInfoOperationStatus(vnfPkgInfo.getId().toString(), notification.getPluginId(), OperationStatus.SUCCESSFULLY_DONE, CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION);
                    notificationManager.sendVnfPkgOnBoardingNotification(new VnfPkgOnBoardingNotificationMessage(vnfPkgInfo.getId().toString(), notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.SUCCESSFULLY_DONE, notification.getPluginId(), null,null));
                } catch (Exception e) {
                    log.error(e.getMessage());
                    log.debug(null, e);
                    try {
                        notificationManager.sendVnfPkgOnBoardingNotification(new VnfPkgOnBoardingNotificationMessage(null, notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId(), null, null));
                    }catch(FailedOperationException e1){
                        log.error(e1.getMessage());
                        log.error(e1.getMessage());
                    }
                }
            }else {
                log.info("Project {} - VNFD with ID {} and version {} retrieved from MANO with ID {} is already present", project, notification.getVnfdId(), notification.getVnfdVersion(), notification.getPluginId());
                try {
                    optionalVnfPkgInfoResource.get().getUserDefinedData().put(notification.getPluginId(), "yes");
                    vnfPkgInfoRepository.saveAndFlush(optionalVnfPkgInfoResource.get());
                    updateVnfPkgInfoOperationStatus(optionalVnfPkgInfoResource.get().getId().toString(), notification.getPluginId(), OperationStatus.SUCCESSFULLY_DONE, CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION);
                    notificationManager.sendVnfPkgOnBoardingNotification(new VnfPkgOnBoardingNotificationMessage(optionalVnfPkgInfoResource.get().getId().toString(), notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.SUCCESSFULLY_DONE, notification.getPluginId(), null,null));
                } catch (Exception e) {
                    log.error(e.getMessage());
                    log.debug(null, e);
                    try {
                        notificationManager.sendVnfPkgOnBoardingNotification(new VnfPkgOnBoardingNotificationMessage(optionalVnfPkgInfoResource.get().getId().toString(), notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId(), null, null));
                    }catch(FailedOperationException e1){
                        log.error(e1.getMessage());
                        log.error(e1.getMessage());
                    }
                }
            }
        }
    }

    public void runtimeVnfDeletion(VnfPkgDeletionNotificationMessage notification){
        List<String> projects = new ArrayList<>();
        if(notification.getProject() == null || notification.getProject().equals("all")){
            List<ProjectResource> projectResourceList = projectRepository.findAll();
            for(ProjectResource projectResource : projectResourceList)
                projects.add(projectResource.getProjectId());
        }else
            projects.add(notification.getProject());

        for(String project : projects) {
            log.info("Project {} - VNFD with ID {} and version {} no longer present in MANO with ID {}", project, notification.getVnfdId(), notification.getVnfdVersion(), notification.getPluginId());
            Optional<VnfPkgInfoResource> optionalVnfPkgInfoResource = vnfPkgInfoRepository.findByVnfdIdAndVnfdVersionAndProjectId(UUID.fromString(notification.getVnfdId()), notification.getVnfdVersion(), project);
            if (optionalVnfPkgInfoResource.isPresent()) {
                VnfPkgInfoResource vnfPkgInfoResource = optionalVnfPkgInfoResource.get();
                if (vnfPkgInfoResource.isRetrievedFromMANO()) {
                    try {
                        updateVnfPkgInfoOperationStatus(vnfPkgInfoResource.getId().toString(), notification.getPluginId(), OperationStatus.RECEIVED, CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION);
                        Optional<VnfPkgInfoResource> targetVnfPkgInfoResource = vnfPkgInfoRepository.findById(vnfPkgInfoResource.getId());
                        if (targetVnfPkgInfoResource.isPresent() && !targetVnfPkgInfoResource.get().getOnboardingState().equals(PackageOnboardingStateType.ONBOARDED)) {
                            log.info("Project {} - Going to delete VNFD with ID {} and version {}", project, vnfPkgInfoResource.getVnfdId(), vnfPkgInfoResource.getVnfdVersion());
                            VnfPkgInfoModifications request = new VnfPkgInfoModifications();
                            request.setOperationalState(PackageOperationalStateType.DISABLED);
                            updateVnfPkgInfo(request, vnfPkgInfoResource.getId().toString(), project, true);
                            deleteVnfPkgInfo(vnfPkgInfoResource.getId().toString(), project, true);
                            notificationManager.sendVnfPkgDeletionNotification(new VnfPkgDeletionNotificationMessage(vnfPkgInfoResource.getId().toString(), notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.SUCCESSFULLY_DONE, notification.getPluginId()));
                        } else {
                            notificationManager.sendVnfPkgDeletionNotification(new VnfPkgDeletionNotificationMessage(vnfPkgInfoResource.getId().toString(), notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.SUCCESSFULLY_DONE, notification.getPluginId()));
                        }
                    }catch (NotPermittedOperationException e){
                        log.error("Project {} - Failed to delete VNFD with ID {} and version {}, it is not deletable", project, vnfPkgInfoResource.getVnfdId(), vnfPkgInfoResource.getVnfdVersion());
                        VnfPkgDeletionNotificationMessage msg = new VnfPkgDeletionNotificationMessage(vnfPkgInfoResource.getId().toString(), notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId());
                        List<String> siteOrManoIds = new ArrayList<>();
                        siteOrManoIds.add(notification.getPluginId());
                        VnfPkgOnBoardingNotificationMessage msg1 = new VnfPkgOnBoardingNotificationMessage(vnfPkgInfoResource.getId().toString(), vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), project, UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageVnfpkgsSubfolder + "/" + project + "/" + vnfPkgInfoResource.getVnfdId() + "/" + vnfPkgInfoResource.getVnfdVersion(), PathType.LOCAL.toString()));
                        try {
                            notificationManager.sendVnfPkgDeletionNotification(msg);
                            notificationManager.sendVnfPkgOnBoardingNotification(msg1);
                        } catch (FailedOperationException e1) {
                            log.error(e1.getMessage());
                            log.debug(null, e1);
                        }
                    } catch (Exception e) {
                        log.error("Project {} - Failed to delete VNFD with ID {} and version {}", project, vnfPkgInfoResource.getVnfdId(), vnfPkgInfoResource.getVnfdVersion());
                        log.debug(null, e);
                        try {
                            notificationManager.sendVnfPkgDeletionNotification(new VnfPkgDeletionNotificationMessage(vnfPkgInfoResource.getId().toString(), notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId()));
                        }catch(FailedOperationException e1){
                            log.error(e1.getMessage());
                            log.error(e1.getMessage());
                        }
                    }
                } else {
                    log.info("Project {} - Sending VNF Onboarding notification for VNFD with ID {} and version {} to MANO with ID {}", project, vnfPkgInfoResource.getVnfdId(), vnfPkgInfoResource.getVnfdVersion(), notification.getPluginId());
                    try {
                        notificationManager.sendVnfPkgDeletionNotification(new VnfPkgDeletionNotificationMessage(vnfPkgInfoResource.getId().toString(), notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId()));
                        List<String> siteOrManoIds = new ArrayList<>();
                        siteOrManoIds.add(notification.getPluginId());
                        notificationManager.sendVnfPkgOnBoardingNotification(new VnfPkgOnBoardingNotificationMessage(vnfPkgInfoResource.getId().toString(), vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), project, UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageVnfpkgsSubfolder + "/" + project + "/" + vnfPkgInfoResource.getVnfdId() + "/" + vnfPkgInfoResource.getVnfdVersion(), PathType.LOCAL.toString())));
                    } catch (FailedOperationException e) {
                        log.error(e.getMessage());
                        log.debug(null, e);
                    }
                }
            }else{
                try {
                    notificationManager.sendVnfPkgDeletionNotification(new VnfPkgDeletionNotificationMessage(null, notification.getVnfdId(), notification.getVnfdVersion(), project, notification.getOperationId(), ScopeType.SYNC, OperationStatus.FAILED, notification.getPluginId()));
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

    private UUID insertOperationInfoInConsumersMap(String vnfPkgInfoId, CatalogueMessageType messageType,
                                                   OperationStatus opStatus, List<String> siteOrManoIds) {
        UUID operationId = UUID.randomUUID();
        log.debug("Updating consumers internal mapping for operationId {}", operationId);
        List<MANO> manos = MANORepository.findAll();
        if(siteOrManoIds != null)
            manos.removeIf(mano -> !siteOrManoIds.contains(mano.getManoId()) && !siteOrManoIds.contains(mano.getManoSite()));
        Map<String, NotificationResource> pluginToOperationState = new HashMap<>();
        for (MANO mano : manos) {
            pluginToOperationState.put(mano.getManoId(), new NotificationResource(vnfPkgInfoId, messageType, opStatus, PluginType.MANO));
        }
        operationIdToConsumersAck.put(operationId.toString(), pluginToOperationState);
        return operationId;
    }

    @Override
    public VnfPkgInfo createVnfPkgInfo(CreateVnfPkgInfoRequest request, String project, boolean isInternalRequest) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException, NotPermittedOperationException, NotAuthorizedOperationException {
        log.debug("Processing request to create a new VNF Pkg info");
        KeyValuePairs kvp = request.getUserDefinedData();
        Map<String, String> targetKvp = new HashMap<>();
        if (kvp != null) {
            for (Map.Entry<String, String> e : kvp.entrySet()) {
                targetKvp.put(e.getKey(), e.getValue());
            }
        }
        VnfPkgInfoResource vnfPkgInfoResource = new VnfPkgInfoResource(targetKvp);
        if (project != null) {
            try {
                Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
                if (!projectOptional.isPresent()) {
                    log.error("Project with id " + project + " does not exist");
                    throw new FailedOperationException("Project with id " + project + " does not exist");
                }
                if (isInternalRequest || !keycloakEnabled || it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    vnfPkgInfoResource.setProjectId(project);
                } else {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }
        if(targetKvp.containsKey("isRetrievedFromMANO") && targetKvp.get("isRetrievedFromMANO").equals("yes"))
            vnfPkgInfoResource.setRetrievedFromMANO(true);
        else
            vnfPkgInfoResource.setRetrievedFromMANO(false);
        vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
        UUID vnfPkgInfoId = vnfPkgInfoResource.getId();
        log.debug("Created VNF Pkg info with ID " + vnfPkgInfoId);
        VnfPkgInfo vnfPkgInfo = buildVnfPkgInfo(vnfPkgInfoResource);
        log.debug("Translated internal VNF Pkg info resource into VNF Pkg info");
        return vnfPkgInfo;
    }

    @Override
    public void deleteVnfPkgInfo(String vnfPkgInfoId, String project, boolean isInternalRequest) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to delete an VNF Pkg info");

        if (vnfPkgInfoId == null)
            throw new MalformattedElementException("Invalid VNF Pkg info ID");

        try {
            UUID id = UUID.fromString(vnfPkgInfoId);

            Optional<VnfPkgInfoResource> optional = vnfPkgInfoRepository.findById(id);

            if (optional.isPresent()) {
                log.debug("Found VNF Pkg info resource with id: " + vnfPkgInfoId);

                VnfPkgInfoResource vnfPkgInfoResource = optional.get();
                if (project != null) {
                    Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
                    if (!projectOptional.isPresent()) {
                        log.error("Project with id " + project + " does not exist");
                        throw new FailedOperationException("Project with id " + project + " does not exist");
                    }
                }
                if (project != null && !vnfPkgInfoResource.getProjectId().equals(project)) {
                    throw new NotAuthorizedOperationException("Specified project differs from VNF Pkg info project");
                } else {
                    try {
                        if (!isInternalRequest && keycloakEnabled && !it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                            throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                        }
                    } catch (NotExistingEntityException e) {
                        throw new NotAuthorizedOperationException(e.getMessage());
                    }
                }

                vnfPkgInfoResource.isDeletable();

                if(!isInternalRequest && vnfPkgInfoResource.isRetrievedFromMANO()){
                    throw new FailedOperationException("Cannot remove VNF Pkg info, it has been retrieved from MANO");
                }

                log.debug("The VNF Pkg info can be removed");
                if (vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.ONBOARDED
                        || vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.LOCAL_ONBOARDED
                        || vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.PROCESSING) {
                    log.debug("The VNF Pkg info is associated to an onboarded VNF Pkg. Removing it");
                    UUID vnfdId = vnfPkgInfoResource.getVnfdId();

                    try {
                        storageService.deleteVnfPkg(project, vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion());
                    } catch (Exception e) {
                        log.error("Unable to delete VNF Pkg with vnfdId {} from filesystem", vnfPkgInfoResource.getVnfdId().toString());
                        log.error("Details: ", e);
                    }

                    UUID operationId;
                    if(!vnfPkgInfoResource.isRetrievedFromMANO()) {
                        operationId = insertOperationInfoInConsumersMap(vnfPkgInfoId,
                                CatalogueMessageType.NSD_DELETION_NOTIFICATION, OperationStatus.SENT, null);
                        VnfPkgDeletionNotificationMessage msg = new VnfPkgDeletionNotificationMessage(vnfPkgInfoId, vnfdId.toString(), vnfPkgInfoResource.getVnfdVersion(), project, operationId, ScopeType.LOCAL, OperationStatus.SENT);
                        msg.setVnfName(vnfPkgInfoResource.getVnfProductName());
                        notificationManager.sendVnfPkgDeletionNotification(msg);
                    }

                    log.debug("VNF Pkg {} locally removed. Sending vnfPkgDeletionNotificationMessage to bus", vnfdId);
                }

                vnfPkgInfoRepository.deleteById(id);
                log.debug("Deleted VNF Pkg info resource with id: " + vnfPkgInfoId);
            } else {
                log.debug("VNF Pkg info resource with id " + vnfPkgInfoId + "not found");
                throw new NotExistingEntityException("VNF Pkg info resource with id " + vnfPkgInfoId + "not found");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + vnfPkgInfoId);
            throw new MalformattedElementException("Wrong ID format: " + vnfPkgInfoId);
        }

    }

    @Override
    public VnfPkgInfoModifications updateVnfPkgInfo(VnfPkgInfoModifications vnfPkgInfoModifications, String vnfPkgInfoId, String project, boolean isInternalRequest) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, NotAuthorizedOperationException {
        log.debug("Processing request to update VNF Pkg info: " + vnfPkgInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);

        if (project != null && !vnfPkgInfoResource.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from VNF Pkg info project");
        } else {
            try {
                if (!isInternalRequest && keycloakEnabled && !it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if(!isInternalRequest && vnfPkgInfoResource.isRetrievedFromMANO()){
            throw new FailedOperationException("Cannot update VNF Pkg info, it has been retrieved from MANO");
        }

        //TODO add possibility to update onboarding on manos
        if (vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.ONBOARDED
                || vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.LOCAL_ONBOARDED) {
            if (vnfPkgInfoModifications.getOperationalState() != null) {
                if (vnfPkgInfoResource.getOperationalState() == vnfPkgInfoModifications.getOperationalState()) {
                    log.error("VNF Pkg operational state already "
                            + vnfPkgInfoResource.getOperationalState() + ". Cannot update VNF Pkg info");
                    throw new NotPermittedOperationException("VNF Pkg operational state already "
                            + vnfPkgInfoResource.getOperationalState() + ". Cannot update VNF Pkg info");
                } else {
                    vnfPkgInfoResource.setOperationalState(vnfPkgInfoModifications.getOperationalState());
                }
            }
            if (vnfPkgInfoModifications.getUserDefinedData() != null) {
                vnfPkgInfoResource.setUserDefinedData(vnfPkgInfoModifications.getUserDefinedData());
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

            try {
                String json = mapper.writeValueAsString(vnfPkgInfoModifications);
                log.debug("Updating vnfPkgInfoResource with vnfPkgInfoModifications: " + json);
            } catch (JsonProcessingException e) {
                log.error("Unable to parse received vnfPkgInfoModifications: " + e.getMessage());
            }
            vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
            log.debug("VnfPkgInfoResource successfully updated");
        } else {
            log.error("VNF Pkg onboarding state not ONBOARDED. Cannot update VNF Pkg info");
            throw new NotPermittedOperationException("VNF Pkg onboarding state not ONBOARDED/LOCAL_ONBOARDED. Cannot update VNF Pkg info");
        }
        return vnfPkgInfoModifications;
    }

    @Override
    public Object getVnfd(String vnfPkgInfoId, boolean isInternalRequest, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, NotAuthorizedOperationException {
        log.debug("Processing request to retrieve a VNFD content for VNF Pkg info " + vnfPkgInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);

        if (!isInternalRequest && project != null && !vnfPkgInfoResource.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from VNF Pkg info project");
        } else {
            try {
                if (!isInternalRequest && keycloakEnabled && !it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if ((!isInternalRequest) && (vnfPkgInfoResource.getOnboardingState() != PackageOnboardingStateType.ONBOARDED
                && vnfPkgInfoResource.getOnboardingState() != PackageOnboardingStateType.LOCAL_ONBOARDED)) {
            log.error("VNF Pkg info " + vnfPkgInfoId + " does not have an onboarded VNFD yet");
            throw new NotPermittedOperationException("VNF Pkg info " + vnfPkgInfoId + " does not have an onboarded VNFD yet");
        }
        UUID vnfdId = vnfPkgInfoResource.getVnfdId();
        log.debug("Internal VNFD ID: " + vnfdId);


        String vnfdFilename = vnfPkgInfoResource.getVnfdFilename();
        if (vnfdFilename == null) {
            log.error("Found zero file for VNFD in YAML format");
            throw new FailedOperationException("Found zero file for VNFD in YAML format");
        }

        return storageService.loadFileAsResource(project, vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), vnfdFilename, true);
    }

    @Override
    public Object getVnfPkg(String vnfPkgInfoId, boolean isInternalRequest, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to retrieve a VNF Pkg content for VNF Pkg info " + vnfPkgInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);

        if (project != null && !vnfPkgInfoResource.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from VNF Pkg info project");
        } else {
            try {
                if (!isInternalRequest && keycloakEnabled && !it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if ((!isInternalRequest) && (vnfPkgInfoResource.getOnboardingState() != PackageOnboardingStateType.ONBOARDED
                && vnfPkgInfoResource.getOnboardingState() != PackageOnboardingStateType.LOCAL_ONBOARDED)) {
            log.error("VNF Pkg info " + vnfPkgInfoId + " does not have an onboarded VNF Pkg yet");
            throw new NotPermittedOperationException("VNF Pkg info " + vnfPkgInfoId + " does not have an onboarded VNF Pkg yet");
        }
        UUID vnfdId = vnfPkgInfoResource.getVnfdId();
        log.debug("Internal VNFD ID: " + vnfdId);

        ContentType ct = vnfPkgInfoResource.getContentType();
        switch (ct) {
            case ZIP: {
                // try {
                String vnfPkgFilename = vnfPkgInfoResource.getVnfPkgFilename();
                if (vnfPkgFilename == null) {
                    log.error("Found zero file for VNF Pkg in ZIP format");
                    throw new FailedOperationException("Found zero file for VNF Pkg in ZIP format");
                }

                return storageService.loadFileAsResource(project, vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), vnfPkgFilename, true);
            }

            default: {
                log.error("Content type not yet supported");
                throw new MethodNotImplementedException("Content type not yet supported");
            }
        }
    }

    @Override
    public VnfPkgInfo getVnfPkgInfo(String vnfPkgInfoId, String project) throws FailedOperationException, NotPermittedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to get a VNF Pkg info");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);

        if (project != null && !vnfPkgInfoResource.getProjectId().equals(project)) {
            throw new NotAuthorizedOperationException("Specified project differs from VNF Pkg info project");
        } else {
            try {
                if (keycloakEnabled && !it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        log.debug("Found VNF Pkg info resource with id: " + vnfPkgInfoId);
        VnfPkgInfo vnfPkgInfo = buildVnfPkgInfo(vnfPkgInfoResource);
        log.debug("Built VNF Pkg info with id: " + vnfPkgInfoId);
        return vnfPkgInfo;
    }

    @Override
    public List<VnfPkgInfo> getAllVnfPkgInfos(String project, UUID vnfdId) throws FailedOperationException, MethodNotImplementedException, NotPermittedOperationException, NotAuthorizedOperationException {
        log.debug("Processing request to get all VNF Pkg infos");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        try {
            if (keycloakEnabled && !it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
            }
        } catch (NotExistingEntityException e) {
            throw new NotAuthorizedOperationException(e.getMessage());
        }

        List<VnfPkgInfoResource> vnfPkgInfoResources;
        if(vnfdId == null)
            vnfPkgInfoResources = vnfPkgInfoRepository.findAll();
        else
            vnfPkgInfoResources = vnfPkgInfoRepository.findByVnfdId(vnfdId);

        List<VnfPkgInfo> vnfPkgInfos = new ArrayList<>();

        for (VnfPkgInfoResource vnfPkgInfoResource : vnfPkgInfoResources) {
            if (project != null && !vnfPkgInfoResource.getProjectId().equals(project)) {
                continue;
            } else {
                VnfPkgInfo vnfPkgInfo = buildVnfPkgInfo(vnfPkgInfoResource);
                vnfPkgInfos.add(vnfPkgInfo);
                log.debug("Added VNF Pkg info " + vnfPkgInfoResource.getId());
            }
        }
        return vnfPkgInfos;
    }

    @Override
    public void uploadVnfPkg(String vnfPkgInfoId, MultipartFile vnfPkg, ContentType contentType, boolean isInternalRequest, String project) throws FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to upload VNF Pkg content for VNFD info " + vnfPkgInfoId);
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);

        if (project != null && !vnfPkgInfoResource.getProjectId().equals(project)) {
            vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.FAILED);
            vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
            throw new NotAuthorizedOperationException("Specified project differs from VNF Pkg info project");
        } else {
            try {
                if (!vnfPkgInfoResource.isRetrievedFromMANO() && !isInternalRequest && keycloakEnabled && !it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.FAILED);
                    vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                }
            } catch (NotExistingEntityException e) {
                vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.FAILED);
                vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        if (vnfPkgInfoResource.getOnboardingState() != PackageOnboardingStateType.CREATED) {
            log.error("VNF Pkg info " + vnfPkgInfoId + " not in CREATED onboarding state");
            vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.FAILED);
            vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
            throw new NotPermittedOperationException("VNF Pkg info " + vnfPkgInfoId + " not in CREATED onboarding state");
        }

        String vnfPkgFilename = null;
        DescriptorTemplate dt = null;
        CSARInfo csarInfo = null;
        UUID vnfdId = null;

        if (contentType != ContentType.ZIP) {
            log.error("VNF Pkg upload request with wrong content-type");
            vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.FAILED);
            vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
            throw new MalformattedElementException("VNF Pkg " + vnfPkgInfoId + " upload request with wrong content-type");
        }

        it.nextworks.nfvmano.catalogue.engine.Utilities.checkZipArchive(vnfPkg);

        try {
            csarInfo = archiveParser.archiveToCSARInfo(project, vnfPkg, true, true);
            dt = csarInfo.getMst();
            vnfPkgFilename = csarInfo.getPackageFilename();

            String vnfPkgId_string = dt.getMetadata().getDescriptorId();

            if (!Utilities.isUUID(vnfPkgId_string)) {
                throw new MalformattedElementException("VNFD id not in UUID format");
            }

            vnfdId = UUID.fromString(vnfPkgId_string);

            Optional<VnfPkgInfoResource> optionalVnfPkgInfoResource = vnfPkgInfoRepository.findByVnfdIdAndVnfdVersionAndProjectId(vnfdId, dt.getMetadata().getVersion(), project);
            if (optionalVnfPkgInfoResource.isPresent()) {
                throw new AlreadyExistingEntityException("A VNFD with the same id and version already exists in the project");
            }

            vnfPkgInfoResource.setVnfdId(vnfdId);
            log.debug("VNFD in Pkg successfully parsed - its content is: \n"
                    + DescriptorsParser.descriptorTemplateToString(dt));

            vnfPkgInfoResource.setVnfdVersion(dt.getMetadata().getVersion());
            vnfPkgInfoResource.setVnfProvider(dt.getMetadata().getVendor());

            //vnfPkgFilename = storageService.storeVnfPkg(vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), vnfPkg);

            log.debug("VNF Pkg file successfully stored: " + vnfPkgFilename);
        } catch (IOException e) {
            log.error("Error while parsing VNF Pkg: " + e.getMessage());
            vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.FAILED);
            vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
            throw new MalformattedElementException("Error while parsing VNF Pkg");
        } catch (MalformattedElementException e) {
            log.error("Error while parsing VNF Pkg, not aligned with CSAR format: " + e.getMessage());
            vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.FAILED);
            vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
            throw new MalformattedElementException("Error while parsing VNF Pkg, not aligned with CSAR format");
        } catch (AlreadyExistingEntityException e) {
            vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.FAILED);
            vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
            throw new AlreadyExistingEntityException(e.getMessage());
        }

        if (vnfPkgFilename == null || dt == null) {
            throw new FailedOperationException("Invalid internal structures");
        }

        log.debug("Updating VNF Pkg info");
        vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.LOCAL_ONBOARDED);
        vnfPkgInfoResource.setOperationalState(PackageOperationalStateType.ENABLED);
        vnfPkgInfoResource.setVnfProvider(dt.getMetadata().getVendor());
        vnfPkgInfoResource.setVnfdVersion(dt.getMetadata().getVersion());

        Map<String, VNFNode> vnfNodes = dt.getTopologyTemplate().getVNFNodes();
        String vnfName = "";
        if (vnfNodes.size() == 1) {
            for (Map.Entry<String, VNFNode> vnfNode : vnfNodes.entrySet()) {
                vnfName = vnfNode.getValue().getProperties().getProductName();
            }
        } else {
            Map<String, String> subMapsProperties = dt.getTopologyTemplate().getSubstituitionMappings().getProperties();
            for (Map.Entry<String, String> entry : subMapsProperties.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("productName")) {
                    vnfName = entry.getValue();
                }
            }
        }

        log.debug("VNFD name: " + vnfName);
        vnfPkgInfoResource.setVnfProductName(vnfName);
        vnfPkgInfoResource.setContentType(contentType);
        vnfPkgInfoResource.setVnfPkgFilename(vnfPkgFilename);
        vnfPkgInfoResource.setVnfdFilename(csarInfo.getDescriptorFilename());
        vnfPkgInfoResource.setMetaFilename(csarInfo.getMetaFilename());
        vnfPkgInfoResource.setManifestFilename(csarInfo.getMfFilename());

        if (isInternalRequest)
            vnfPkgInfoResource.setPublished(true);
        else
            vnfPkgInfoResource.setPublished(false);

        vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
        log.debug("VNF PKG info updated");

        // send notification over kafka bus
        if(!vnfPkgInfoResource.isRetrievedFromMANO()) {
            List<String> siteOrManoIds = new ArrayList<>();
            Map<String, String> userDefinedData = vnfPkgInfoResource.getUserDefinedData();
            if(userDefinedData == null || userDefinedData.size() == 0)
                siteOrManoIds.addAll(pluginManger.manoDrivers.keySet());
            else
                for(MANOPlugin mano : pluginManger.manoDrivers.values())
                    if((userDefinedData.containsKey(mano.getPluginId()) && userDefinedData.get(mano.getPluginId()).equals("yes"))
                        || (userDefinedData.containsKey(mano.getMano().getManoSite()) && userDefinedData.get(mano.getMano().getManoSite()).equals("yes")))
                        siteOrManoIds.add(mano.getPluginId());

            UUID operationId = insertOperationInfoInConsumersMap(vnfPkgInfoId,
                    CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION, OperationStatus.SENT, siteOrManoIds);
            vnfPkgInfoResource.setAcknowledgedOnboardOpConsumers(operationIdToConsumersAck.get(operationId.toString()));
            VnfPkgOnBoardingNotificationMessage msg =
                    new VnfPkgOnBoardingNotificationMessage(vnfPkgInfoId, vnfdId.toString(), dt.getMetadata().getVersion(), project, operationId, ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageVnfpkgsSubfolder + "/" + project + "/" + vnfdId.toString() + "/" + vnfPkgInfoResource.getVnfdVersion(), PathType.LOCAL.toString()));
            notificationManager.sendVnfPkgOnBoardingNotification(msg);
        }

        log.debug("VNF Pkg content uploaded and vnfPkgOnBoardingNotification delivered");
    }

    public VnfPkgInfo buildVnfPkgInfo(VnfPkgInfoResource vnfPkgInfoResource) {
        log.debug("Building VNF Pkg info from internal repo");
        VnfPkgInfo vnfPkgInfo = new VnfPkgInfo();
        vnfPkgInfo.setId(vnfPkgInfoResource.getId());
        VnfPkgLinksType links = new VnfPkgLinksType();
        links.setSelf("/vnfpkgm/v1/vnf_packages/" + vnfPkgInfoResource.getId());
        links.setVnfd("/vnfpkgm/v1/vnf_packages/" + vnfPkgInfoResource.getId() + "/vnfd");
        links.setPackageContent("/vnfpkgm/v1/vnf_packages/" + vnfPkgInfoResource.getId() + "/package_content");
        vnfPkgInfo.setLinks(links);
        vnfPkgInfo.setChecksum(vnfPkgInfoResource.getChecksum());
        vnfPkgInfo.setOnboardingState(vnfPkgInfoResource.getOnboardingState());
        vnfPkgInfo.setOperationalState(vnfPkgInfoResource.getOperationalState());
        vnfPkgInfo.setUsageState(vnfPkgInfoResource.getUsageState());
        KeyValuePairs kvp = new KeyValuePairs();
        kvp.putAll(vnfPkgInfoResource.getUserDefinedData());
        vnfPkgInfo.setUserDefinedData(kvp);
        vnfPkgInfo.setVnfdId(vnfPkgInfoResource.getVnfdId());
        vnfPkgInfo.setVnfdVersion(vnfPkgInfoResource.getVnfdVersion());
        vnfPkgInfo.setVnfProductName(vnfPkgInfoResource.getVnfProductName());
        vnfPkgInfo.setVnfProvider(vnfPkgInfoResource.getVnfProvider());
        vnfPkgInfo.setVnfSoftwareVersion(vnfPkgInfoResource.getVnfSoftwareVersion());
        vnfPkgInfo.setProjectId(vnfPkgInfoResource.getProjectId());

        Map<String, NotificationResource> acksMap = vnfPkgInfoResource.getAcknowledgedOnboardOpConsumers();
        Map<String, PackageOnboardingStateType> manoIdToOnboardingStatus = new HashMap<>();
        for (Map.Entry<String, NotificationResource> entry : acksMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION) {
                PackageOnboardingStateType pkgOnboardingStateType = PackageOnboardingStateType.UPLOADING;
                switch (entry.getValue().getOpStatus()) {
                    case SENT:
                        pkgOnboardingStateType = PackageOnboardingStateType.UPLOADING;
                        break;
                    case RECEIVED:
                        pkgOnboardingStateType = PackageOnboardingStateType.SKIPPED;
                        break;
                    case PROCESSING:
                        pkgOnboardingStateType = PackageOnboardingStateType.PROCESSING;
                        break;
                    case FAILED:
                        pkgOnboardingStateType = PackageOnboardingStateType.FAILED;
                        break;
                    case SUCCESSFULLY_DONE:
                        pkgOnboardingStateType = PackageOnboardingStateType.ONBOARDED;
                }
                if (entry.getValue().getPluginType() == PluginType.MANO) {
                    manoIdToOnboardingStatus.putIfAbsent(entry.getKey(), pkgOnboardingStateType);
                }
            }
        }

        vnfPkgInfo.setManoIdToOnboardingStatus(manoIdToOnboardingStatus);

        if (vnfPkgInfoResource.isPublished()) {
            vnfPkgInfo.setC2cOnboardingState(C2COnboardingStateType.PUBLISHED);
        } else {
            vnfPkgInfo.setC2cOnboardingState(C2COnboardingStateType.UNPUBLISHED);
        }

        return vnfPkgInfo;
    }

    private VnfPkgInfoResource getVnfPkgInfoResource(String vnfPkgInfoId) throws MalformattedElementException, NotExistingEntityException {
        log.debug("Retrieving internal VNF Pkg info resource with ID " + vnfPkgInfoId);
        try {
            UUID id = UUID.fromString(vnfPkgInfoId);

            Optional<VnfPkgInfoResource> optional = vnfPkgInfoRepository.findById(id);

            if (optional.isPresent()) {
                VnfPkgInfoResource vnfPkgInfoResource = optional.get();
                log.debug("Found VNF Pkg info resource with id: " + vnfPkgInfoId);
                return vnfPkgInfoResource;
            } else {
                log.debug("VNF Pkg info resource with id " + vnfPkgInfoId + " not found");
                throw new NotExistingEntityException("VNF Pkg info resource with id " + vnfPkgInfoId + " not found");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + vnfPkgInfoId);
            throw new MalformattedElementException("Wrong ID format: " + vnfPkgInfoId);
        }
    }

    public void updateVnfPkgInfoOperationStatus(String vnfPkgInfoId, String pluginId, OperationStatus opStatus, CatalogueMessageType type) throws NotExistingEntityException {
        log.debug("Retrieving vnfPkgInfoResource {} from DB for updating with onboarding status info for plugin {}",
                vnfPkgInfoId, pluginId);
        Optional<VnfPkgInfoResource> optionalVnfPkgInfoResource = vnfPkgInfoRepository.findById(UUID.fromString(vnfPkgInfoId));

        if (optionalVnfPkgInfoResource.isPresent()) {
            try {
                VnfPkgInfoResource vnfPkgInfoResource = optionalVnfPkgInfoResource.get();

                Map<String, NotificationResource> ackMap = new HashMap<>();
                if (vnfPkgInfoResource.getAcknowledgedOnboardOpConsumers() != null) {
                    ackMap = vnfPkgInfoResource.getAcknowledgedOnboardOpConsumers();
                }
                ackMap.put(pluginId, new NotificationResource(vnfPkgInfoId, type, opStatus, PluginType.MANO));
                vnfPkgInfoResource.setAcknowledgedOnboardOpConsumers(ackMap);

                if (type == CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION) {
                    log.debug("Checking VNF Pkg with vnfPkgInfoId {} onboarding state", vnfPkgInfoId);
                    vnfPkgInfoResource.setOnboardingState(checkVnfPkgOnboardingState(vnfPkgInfoId, ackMap));
                }

                log.debug("Updating VnfPkgInfoResource {} with onboardingState {}", vnfPkgInfoId,
                        vnfPkgInfoResource.getOnboardingState());
                vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
            } catch (Exception e) {
                log.error("Error while updating VnfPkgInfoResource with vnfPkgInfoId: " + vnfPkgInfoId);
                log.error("Details: ", e);
            }
        } else {
            throw new NotExistingEntityException("VnfPkgInfoResource " + vnfPkgInfoId + " not present in DB");
        }
    }

    private PackageOnboardingStateType checkVnfPkgOnboardingState(String vnfPkgInfoId, Map<String, NotificationResource> ackMap) {

        for (Map.Entry<String, NotificationResource> entry : ackMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION && entry.getValue().getPluginType() == PluginType.MANO) {
                if(entry.getValue().getOpStatus() == OperationStatus.SUCCESSFULLY_DONE) {
                    log.debug("VNF Pkg with vnfPkgInfoId " + vnfPkgInfoId + " is onboarded in at least one MANO");
                    return PackageOnboardingStateType.ONBOARDED;
                }
                /*
                if (entry.getValue().getOpStatus() == OperationStatus.FAILED) {
                    log.error("VNF Pkg with vnfPkgInfoId {} onboarding failed for mano with manoId {}", vnfPkgInfoId,
                            entry.getKey());

                    // TODO: Decide how to handle MANO onboarding failures.
                    return PackageOnboardingStateType.LOCAL_ONBOARDED;
                } else if (entry.getValue().getOpStatus() == OperationStatus.SENT
                        || entry.getValue().getOpStatus() == OperationStatus.RECEIVED
                        || entry.getValue().getOpStatus() == OperationStatus.PROCESSING) {
                    log.debug("VNF Pkg with vnfPkgInfoId {} onboarding still in progress for mano with manoId {}", vnfPkgInfoId, entry.getKey());
                    return PackageOnboardingStateType.LOCAL_ONBOARDED;
                }

                 */
            }
        }
        log.debug("VNF Pkg with vnfPkgInfoId " + vnfPkgInfoId + " is not onboarded in any MANO");
        return PackageOnboardingStateType.LOCAL_ONBOARDED;
    }
}
