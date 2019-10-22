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
import it.nextworks.nfvmano.catalogue.auth.AuthUtilities;
import it.nextworks.nfvmano.catalogue.auth.projectmanagement.ProjectResource;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.PnfdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.PnfdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.common.ConfigurationParameters;
import it.nextworks.nfvmano.catalogue.engine.elements.ContentType;
import it.nextworks.nfvmano.catalogue.engine.resources.NotificationResource;
import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.catalogue.engine.resources.PnfdInfoResource;
import it.nextworks.nfvmano.catalogue.engine.resources.VnfPkgInfoResource;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import it.nextworks.nfvmano.catalogue.plugins.PluginsManager;
import it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.C2COnboardingStateType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.PluginType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.repos.MANORepository;
import it.nextworks.nfvmano.catalogue.repos.*;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.ArchiveParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.elements.CSARInfo;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.pnfd.nodes.PNF.PNFNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
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
    private PnfdInfoRepository pnfdInfoRepo;

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

    }

    public void runtimeNsOnBoarding(NsdOnBoardingNotificationMessage notification){

    }

    public void runtimeNsDeletion(NsdDeletionNotificationMessage notification){

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
    public NsdInfo createNsdInfo(CreateNsdInfoRequest request, String project)
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
                if (!keycloakEnabled || checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    nsdInfoResource.setProjectId(project);
                } else {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }
        nsdInfoRepo.saveAndFlush(nsdInfoResource);
        UUID nsdInfoId = nsdInfoResource.getId();
        log.debug("Created NSD info with ID " + nsdInfoId);
        NsdInfo nsdInfo = buildNsdInfo(nsdInfoResource);
        log.debug("Translated internal nsd info resource into nsd info");
        return nsdInfo;
    }

    @Override
    public synchronized void deleteNsdInfo(String nsdInfoId, String project)
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
                        if (keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                            throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                        }
                    } catch (NotExistingEntityException e) {
                        throw new NotAuthorizedOperationException(e.getMessage());
                    }
                }

                nsdInfo.isDeletable();

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

                log.debug("The NSD info can be removed");
                if (nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.ONBOARDED
                        || nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.LOCAL_ONBOARDED
                        || nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.PROCESSING) {
                    log.debug("The NSD info is associated to an onboarded NSD. Removing it");
                    UUID nsdId = nsdInfo.getNsdId();
                    // dbWrapper.deleteNsd(nsdId);

                    try {
                        storageService.deleteNsd(project, nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion());
                    } catch (Exception e) {
                        log.error("Unable to delete NSD with nsdId {} from fylesystem", nsdInfo.getNsdId().toString());
                        log.error("Details: ", e);
                    }

                    UUID operationId = insertOperationInfoInConsumersMap(nsdInfoId,
                            CatalogueMessageType.NSD_DELETION_NOTIFICATION, OperationStatus.SENT, null);

                    log.debug("NSD {} locally removed. Sending nsdDeletionNotificationMessage to bus", nsdId);
                    NsdDeletionNotificationMessage msg = new NsdDeletionNotificationMessage(nsdInfoId, nsdId.toString(), nsdInfo.getNsdVersion(), project,
                            operationId, ScopeType.LOCAL, OperationStatus.SENT);
                    notificationManager.sendNsdDeletionNotification(msg);
                }

                nsdInfoRepo.deleteById(id);
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
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
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
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
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
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
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
    public List<NsdInfo> getAllNsdInfos(String project) throws FailedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to get all NSD infos");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        try {
            if (keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
            }
        } catch (NotExistingEntityException e) {
            throw new NotAuthorizedOperationException(e.getMessage());
        }


        List<NsdInfoResource> nsdInfoResources = nsdInfoRepo.findAll();
        List<NsdInfo> nsdInfos = new ArrayList<>();

        for (NsdInfoResource nsdInfoResource : nsdInfoResources) {
            if (project != null && !nsdInfoResource.getProjectId().equals(project)) {
                continue;
            } else {
                NsdInfo nsdInfo = buildNsdInfo(nsdInfoResource);
                nsdInfos.add(nsdInfo);
                log.debug("Added NSD info " + nsdInfoResource.getId());
            }
        }
        return nsdInfos;
    }

    @Override
    public synchronized NsdInfoModifications updateNsdInfo(NsdInfoModifications nsdInfoModifications, String nsdInfoId, String project)
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
                if (keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                }
            } catch (NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

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
                if (!isInternalRequest && keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.FAILED);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
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

        Map<String, KeyValuePair> includedVnfds;
        Map<String, KeyValuePair> includedPnfds;
        NsdOnboardingStateType onboardingStateType = NsdOnboardingStateType.UPLOADING;

        CSARInfo csarInfo = null;

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
                    nsdFilename = csarInfo.getPackageFilename();

                    includedVnfds = checkVNFPkgs(dt, project);
                    includedPnfds = checkPNFDs(dt, project);

                    String nsdId_string = dt.getMetadata().getDescriptorId();

                    if (!Utilities.isUUID(nsdId_string)) {
                        throw new MalformattedElementException("NSD id not in UUID format");
                    }

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
                    throw new MalformattedElementException("Unable to onboard NSD because VNFDs info are missing or malformed: " + e.getMessage());
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

                    includedVnfds = checkVNFPkgs(dt, project);
                    includedPnfds = checkPNFDs(dt, project);

                    String nsdId_string = dt.getMetadata().getDescriptorId();

                    if (!Utilities.isUUID(nsdId_string)) {
                        throw new MalformattedElementException("NSD id not in UUID format");
                    }

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
                    throw new MalformattedElementException("Unable to onboard NSD because VNFDs info are missing or malformed: " + e.getMessage());
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
        Map<String, NSNode> nsNodes = dt.getTopologyTemplate().getNSNodes();
        String nsdName = "";
        if (nsNodes.size() == 1) {
            for (Entry<String, NSNode> nsNode : nsNodes.entrySet()) {
                nsdName = nsNode.getValue().getProperties().getName();
            }
        } else {
            Map<String, String> subMapsProperties = dt.getTopologyTemplate().getSubstituitionMappings().getProperties();
            for (Map.Entry<String, String> entry : subMapsProperties.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("name")) {
                    nsdName = entry.getValue();
                }
            }
        }

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

        List<UUID> vnfPkgIds = new ArrayList<>();
        for (String vnfdInfoId : includedVnfds.keySet()) {
            log.debug("Adding vnfPkgInfo Id {} to vnfPkgs list in nsdInfo", vnfdInfoId);
            log.debug("Adding vnfPkgInfo Id {} to vnfPkgs list in nsdInfo", vnfdInfoId);
            vnfPkgIds.add(UUID.fromString(vnfdInfoId));
        }
        nsdInfo.setVnfPkgIds(vnfPkgIds);

        List<UUID> pnfdIds = new ArrayList<>();
        for (String pnfdInfoId : includedPnfds.keySet()) {
            log.debug("Adding pnfdInfo Id {} to pnfs list in nsdInfo", pnfdInfoId);
            log.debug("Adding pnfdInfo Id {} to pnfs list in nsdInfo", pnfdInfoId);
            pnfdIds.add(UUID.fromString(pnfdInfoId));
        }
        nsdInfo.setPnfdInfoIds(pnfdIds);

        // clean tmp files
        if (!inputFile.delete()) {
            log.warn("Could not delete temporary NSD content file");
        }

        List<String> manoIds = checkWhereOnboardNS(nsdInfo);

        List<String> siteOrManoIds = new ArrayList<>();
        Map<String, String> userDefinedData = nsdInfo.getUserDefinedData();
        if(userDefinedData == null || userDefinedData.size() == 0)
            siteOrManoIds.addAll(pluginManger.manoDrivers.keySet());
        else
            for(MANOPlugin mano : pluginManger.manoDrivers.values())
                if((userDefinedData.containsKey(mano.getPluginId()) && userDefinedData.get(mano.getPluginId()).equals("yes"))
                        || (userDefinedData.containsKey(mano.getMano().getManoSite()) && userDefinedData.get(mano.getMano().getManoSite()).equals("yes")))
                    siteOrManoIds.add(mano.getPluginId());

        siteOrManoIds.removeIf(id -> !manoIds.contains(id));

        // send notification over kafka bus
        UUID operationId = insertOperationInfoInConsumersMap(nsdInfoId,
                CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION, OperationStatus.SENT, siteOrManoIds);
        nsdInfo.setAcknowledgedOnboardOpConsumers(operationIdToConsumersAck.get(operationId.toString()));

        if (isInternalRequest)
            nsdInfo.setPublished(true);
        else
            nsdInfo.setPublished(false);

        nsdInfoRepo.saveAndFlush(nsdInfo);
        log.debug("NSD info updated");

        NsdOnBoardingNotificationMessage msg = new NsdOnBoardingNotificationMessage(nsdInfo.getId().toString(), nsdId.toString(), nsdInfo.getNsdVersion(), project,
                operationId, ScopeType.LOCAL, OperationStatus.SENT, siteOrManoIds, new KeyValuePair(rootDir + ConfigurationParameters.storageNsdsSubfolder + "/" + project + "/" + nsdId.toString() + "/" + nsdInfo.getNsdVersion(), PathType.LOCAL.toString()));
        msg.setIncludedVnfds(includedVnfds);
        msg.setIncludedPnfds(includedPnfds);
        // send notification over kafka bus
        notificationManager.sendNsdOnBoardingNotification(msg);

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
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
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
                    throw new NotAuthorizedOperationException("Specified project differs from PNFD info project");
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
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
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
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
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
    public List<PnfdInfo> getAllPnfdInfos(String project) throws FailedOperationException, MethodNotImplementedException, NotAuthorizedOperationException {
        log.debug("Processing request to get all PNFD infos");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new FailedOperationException("Project with id " + project + " does not exist");
            }
        }
        try {
            if (keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
            }
        } catch (NotExistingEntityException e) {
            throw new NotAuthorizedOperationException(e.getMessage());
        }

        List<PnfdInfoResource> pnfdInfoResources = pnfdInfoRepo.findAll();
        List<PnfdInfo> pnfdInfos = new ArrayList<>();

        for (PnfdInfoResource pnfdInfoResource : pnfdInfoResources) {
            if (project != null && !pnfdInfoResource.getProjectId().equals(project)) {
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
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
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
                    log.error("NSD with nsdInfoId {} is onboarded in at least one MANO", nsdInfoId);
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