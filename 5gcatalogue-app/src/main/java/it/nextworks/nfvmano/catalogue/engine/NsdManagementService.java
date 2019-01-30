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
import it.nextworks.nfvmano.catalogue.engine.resources.NotificationResource;
import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.catalogue.engine.resources.VnfPkgInfoResource;
import it.nextworks.nfvmano.catalogue.messages.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.ScopeType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANORepository;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.catalogue.repos.NsdInfoRepository;
import it.nextworks.nfvmano.catalogue.repos.VnfPkgInfoRepository;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class NsdManagementService implements NsdManagementInterface {

    private static final Logger log = LoggerFactory.getLogger(NsdManagementService.class);

    @Autowired
    private NsdInfoRepository nsdInfoRepo;

    @Autowired
    private FileSystemStorageService storageService;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private MANORepository MANORepository;

    @Autowired
    private VnfPkgInfoRepository vnfPkgInfoRepository;

    private Map<String, Map<String, NotificationResource>> operationIdToConsumersAck = new HashMap<>();

    public NsdManagementService() {
    }

    protected void updateOperationInfoInConsumersMap(UUID operationId, OperationStatus opStatus, String manoId,
                                                     String nsdInfoId, CatalogueMessageType messageType) {
        Map<String, NotificationResource> manoIdToOpAck = new HashMap<>();
        if (operationIdToConsumersAck.containsKey(operationId.toString())) {
            manoIdToOpAck = operationIdToConsumersAck.get(operationId.toString());
        }
        NotificationResource notificationResource = new NotificationResource(nsdInfoId, messageType, opStatus);
        manoIdToOpAck.put(manoId, notificationResource);

        operationIdToConsumersAck.put(operationId.toString(), manoIdToOpAck);
    }

    private UUID insertOperationInfoInConsumersMap(String nsdInfoId, CatalogueMessageType messageType,
                                                   OperationStatus opStatus) {
        UUID operationId = UUID.randomUUID();
        log.debug("Updating consumers internal mapping for operationId {}", operationId);
        List<MANO> manos = MANORepository.findAll();
        Map<String, NotificationResource> pluginToOperationState = new HashMap<>();
        for (MANO mano : manos) {
            pluginToOperationState.put(mano.getManoId(), new NotificationResource(nsdInfoId, messageType, opStatus));

        }
        operationIdToConsumersAck.put(operationId.toString(), pluginToOperationState);
        return operationId;
    }

    @Override
    public NsdInfo createNsdInfo(CreateNsdInfoRequest request)
            throws FailedOperationException, MalformattedElementException, MethodNotImplementedException {
        log.debug("Processing request to create a new NSD info.");
        KeyValuePairs kvp = request.getUserDefinedData();
        Map<String, String> targetKvp = new HashMap<>();
        if (kvp != null) {
            for (Map.Entry<String, String> e : kvp.entrySet()) {
                targetKvp.put(e.getKey(), e.getValue());
            }
        }
        NsdInfoResource nsdInfoResource = new NsdInfoResource(targetKvp);
        nsdInfoRepo.saveAndFlush(nsdInfoResource);
        UUID nsdInfoId = nsdInfoResource.getId();
        log.debug("Created NSD info with ID " + nsdInfoId);
        NsdInfo nsdInfo = buildNsdInfo(nsdInfoResource);
        log.debug("Translated internal nsd info resource into nsd info");
        return nsdInfo;
    }

    @Override
    public synchronized void deleteNsdInfo(String nsdInfoId)
            throws FailedOperationException, NotExistingEntityException, MalformattedElementException,
            NotPermittedOperationException, MethodNotImplementedException {
        log.debug("Processing request to delete an NSD info.");

        if (nsdInfoId == null)
            throw new MalformattedElementException("Invalid NSD info ID.");

        try {
            UUID id = UUID.fromString(nsdInfoId);

            Optional<NsdInfoResource> optional = nsdInfoRepo.findById(id);

            if (optional.isPresent()) {
                log.debug("Found NSD info resource with id: " + nsdInfoId);

                NsdInfoResource nsdInfo = optional.get();
                nsdInfo.isDeletable();

                log.debug("The NSD info can be removed.");
                if (nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.ONBOARDED) {
                    log.debug("The NSD info is associated to an onboarded NSD. Removing it.");
                    UUID nsdId = nsdInfo.getNsdId();
                    // dbWrapper.deleteNsd(nsdId);

                    try {
                        storageService.deleteNsd(nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion());
                    } catch (Exception e) {
                        log.error("Unable to delete NSD with nsdId {} from fylesystem.", nsdInfo.getNsdId().toString());
                        log.error("Details: ", e);
                    }

                    UUID operationId = insertOperationInfoInConsumersMap(nsdInfoId,
                            CatalogueMessageType.NSD_DELETION_NOTIFICATION, OperationStatus.SENT);

                    log.debug("NSD {} locally removed. Sending nsdDeletionNotificationMessage to bus.", nsdId);
                    NsdDeletionNotificationMessage msg = new NsdDeletionNotificationMessage(nsdInfoId, nsdId.toString(),
                            operationId, ScopeType.LOCAL, OperationStatus.SENT, null);
                    notificationManager.sendNsdDeletionNotification(msg);
                }

                nsdInfoRepo.deleteById(id);
                log.debug("Deleted NSD info resource with id: " + nsdInfoId);
            } else {
                log.debug("NSD info resource with id " + nsdInfoId + "not found.");
                throw new NotExistingEntityException("NSD info resource with id " + nsdInfoId + "not found.");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + nsdInfoId);
            throw new MalformattedElementException("Wrong ID format: " + nsdInfoId);
        }

    }

    @Override
    public Object getNsd(String nsdInfoId, boolean isInternalRequest) throws FailedOperationException, NotExistingEntityException,
            MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
        log.debug("Processing request to retrieve an NSD content for NSD info " + nsdInfoId);
        NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);
        if ((!isInternalRequest) && (nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.ONBOARDED)) {
            log.error("NSD info " + nsdInfoId + " does not have an onboarded NSD yet");
            throw new NotPermittedOperationException("NSD info " + nsdInfoId + " does not have an onboarded NSD yet");
        }
        UUID nsdId = nsdInfo.getNsdId();
        log.debug("Internal NSD ID: " + nsdId);

        /*
         * DescriptorTemplate nsd = dbWrapper.getNsd(nsdId);
         * log.debug("Got NSD content.");
         */

        ContentType ct = nsdInfo.getContentType();
        switch (ct) {
            case YAML: {
                // try {
                List<String> nsdFilenames = nsdInfo.getNsdFilename();
                if (nsdFilenames.size() != 1) {
                    log.error("Found zero or more than one file for NSD in YAML format. Error.");
                    throw new FailedOperationException("Found more than one file for NSD in YAML format. Error.");
                }
                String nsdFilename = nsdFilenames.get(0);
                return storageService.loadNsdAsResource(nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion(), nsdFilename);

                /*
                 * //String nsdString = DescriptorsParser.descriptorTemplateToString(nsd);
                 * //log.debug("NSD content translated into YAML format"); //return nsdString; }
                 * catch (JsonProcessingException e) {
                 * log.error("Error while translating descriptor"); throw new
                 * FailedOperationException("Error while translating descriptor: " +
                 * e.getMessage()); }
                 */
            }

            default: {
                log.error("Content type not yet supported.");
                throw new MethodNotImplementedException("Content type not yet supported.");
            }
        }

    }

    @Override
    public NsdInfo getNsdInfo(String nsdInfoId) throws FailedOperationException, NotExistingEntityException,
            MalformattedElementException, MethodNotImplementedException {
        log.debug("Processing request to get an NSD info.");
        NsdInfoResource nsdInfoResource = getNsdInfoResource(nsdInfoId);
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
                log.debug("NSD info resource with id " + nsdInfoId + " not found.");
                throw new NotExistingEntityException("NSD info resource with id " + nsdInfoId + " not found.");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + nsdInfoId);
            throw new MalformattedElementException("Wrong ID format: " + nsdInfoId);
        }
    }

    @Override
    public List<NsdInfo> getAllNsdInfos() throws FailedOperationException, MethodNotImplementedException {
        log.debug("Processing request to get all NSD infos.");

        List<NsdInfoResource> nsdInfoResources = nsdInfoRepo.findAll();
        List<NsdInfo> nsdInfos = new ArrayList<>();

        for (NsdInfoResource nsdInfoResource : nsdInfoResources) {
            NsdInfo nsdInfo = buildNsdInfo(nsdInfoResource);
            nsdInfos.add(nsdInfo);
            log.debug("Added NSD info " + nsdInfoResource.getId());
        }
        return nsdInfos;
    }

    @Override
    public synchronized NsdInfoModifications updateNsdInfo(NsdInfoModifications nsdInfoModifications, String nsdInfoId)
            throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException {
        log.debug("Processing request to update NSD info: " + nsdInfoId);
        NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);

        if (nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.ONBOARDED) {
            if (nsdInfoModifications.getNsdOperationalState() != null) {
                if (nsdInfo.getNsdOperationalState() == nsdInfoModifications.getNsdOperationalState()) {
                    log.error("NSD operational state already "
                            + nsdInfo.getNsdOperationalState() + ". Cannot update NSD info.");
                    throw new NotPermittedOperationException("NSD operational state already "
                            + nsdInfo.getNsdOperationalState() + ". Cannot update NSD info.");
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
            log.debug("NsdInfoResource successfully updated.");
        } else {
            log.error("NSD onboarding state not ONBOARDED. Cannot update NSD info.");
            throw new NotPermittedOperationException("NSD onboarding state not ONBOARDED. Cannot update NSD info.");
        }
        return nsdInfoModifications;
    }

    @Override
    public synchronized void uploadNsd(String nsdInfoId, MultipartFile nsd, ContentType contentType) throws MalformattedElementException, FailedOperationException, NotExistingEntityException, NotPermittedOperationException, MethodNotImplementedException {

        log.debug("Processing request to upload NSD content for NSD info " + nsdInfoId);

        NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);

        if (nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.CREATED) {
            log.error("NSD info " + nsdInfoId + " not in CREATED onboarding state.");
            throw new NotPermittedOperationException("NSD info " + nsdInfoId + " not in CREATED onboarding state.");
        }

        // convert to File
        File inputFile = null;
        try {
            inputFile = convertToFile(nsd);
        } catch (Exception e) {
            log.error("Error while parsing NSD in zip format: " + e.getMessage());
            throw new MalformattedElementException("Error while parsing NSD.");
        }

        String nsdFilename = null;
        DescriptorTemplate dt = null;
        UUID nsdId = null;

        // pre-set nsdinfo attributes to properly store NSDs
        // UUID nsdId = UUID.randomUUID();

        List<DescriptorTemplate> includedVnfds = new ArrayList<>();

        switch (contentType) {
            case ZIP: {
                try {
                    log.info("NSD file is in format: zip");

                    // TODO: assuming for now one single file into the zip
                    MultipartFile nsdMpFile = extractNsdFile(inputFile);
                    // convert to File
                    File nsdFile = convertToFile(nsdMpFile);

                    dt = DescriptorsParser.fileToDescriptorTemplate(nsdFile);

                    includedVnfds = checkVNFPkgs(dt);

                    nsdId = UUID.fromString(dt.getMetadata().getDescriptorId());
                    nsdInfo.setNsdId(nsdId);

                    log.debug("NSD successfully parsed - its content is: \n"
                            + DescriptorsParser.descriptorTemplateToString(dt));

                    // pre-set nsdinfo attributes to properly store NSDs
                    nsdInfo.setNsdVersion(dt.getMetadata().getVersion());

                    nsdFilename = storageService.storeNsd(nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion(), nsdMpFile);

                    // change contentType to YAML as nsd file is no more zip from now on
                    contentType = ContentType.YAML;

                    log.debug("NSD file successfully stored");
                    // clean tmp files
                    if (!nsdFile.delete()) {
                        log.warn("Could not delete temporary NSD zip content file");
                    }
                } catch (IOException e) {
                    log.error("Error while parsing NSD in zip format: " + e.getMessage());
                    throw new MalformattedElementException("Error while parsing NSD.");
                } catch (NotExistingEntityException e) {
                    throw new NotPermittedOperationException("Unable to onboard NSD because one or more related VNF Pkgs are missing in local storage: " + e.getMessage());
                } catch (MalformattedElementException e) {
                    throw new MalformattedElementException("Unable to onboard NSD because VNFDs info are missing or malformed: " + e.getMessage());
                } catch (Exception e) {
                    log.error("Error while parsing NSD in zip format: " + e.getMessage());
                    throw new MalformattedElementException("Error while parsing NSD.");
                }
                break;
            }
            case YAML: {
                try {
                    log.info("NSD file is in format: yaml");

                    dt = DescriptorsParser.fileToDescriptorTemplate(inputFile);

                    includedVnfds = checkVNFPkgs(dt);

                    nsdId = UUID.fromString(dt.getMetadata().getDescriptorId());
                    nsdInfo.setNsdId(nsdId);

                    log.debug("NSD successfully parsed - its content is: \n"
                            + DescriptorsParser.descriptorTemplateToString(dt));
                    // pre-set nsdinfo attributes to properly store NSDs
                    nsdInfo.setNsdVersion(dt.getMetadata().getVersion());

                    nsdFilename = storageService.storeNsd(nsdInfo.getNsdId().toString(), nsdInfo.getNsdVersion(), nsd);

                    log.debug("NSD file successfully stored");

                } catch (IOException e) {
                    log.error("Error while parsing NSD in yaml format: " + e.getMessage());
                    throw new MalformattedElementException("Error while parsing NSD.");
                } catch (NotExistingEntityException e) {
                    throw new NotPermittedOperationException("Unable to onboard NSD because one or more related VNF Pkgs are missing in local storage: " + e.getMessage());
                } catch (MalformattedElementException e) {
                    throw new MalformattedElementException("Unable to onboard NSD because VNFDs info are missing or malformed: " + e.getMessage());
                }
                break;
            }

            default: {
                log.error("Unsupported content type: " + contentType.toString());
                throw new MethodNotImplementedException("Unsupported content type: " + contentType.toString());
            }
        }

        if (nsdFilename == null || dt == null) {
            throw new FailedOperationException("Invalid internal structures");
        }

        log.debug("Updating NSD info");
        // nsdInfo.setNsdId(nsdId);
        // TODO: here it is actually onboarded only locally and just in the DB. To be
        // updated when we will implement also the package uploading
        nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.PROCESSING);
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
        nsdInfo.addNsdFilename(nsdFilename);

        // clean tmp files
        if (!inputFile.delete()) {
            log.warn("Could not delete temporary NSD content file");
        }

        // TODO: request to Policy Manager for retrieving the MANO Plugins list,
        // now all plugins are expected to be consumers

        UUID operationId = insertOperationInfoInConsumersMap(nsdInfoId,
                CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION, OperationStatus.SENT);
        nsdInfo.setAcknowledgedOnboardOpConsumers(operationIdToConsumersAck.get(operationId.toString()));

        nsdInfoRepo.saveAndFlush(nsdInfo);
        log.debug("NSD info updated");

        NsdOnBoardingNotificationMessage msg = new NsdOnBoardingNotificationMessage(nsdInfo.getId().toString(), nsdId.toString(),
                operationId, ScopeType.LOCAL, OperationStatus.SENT);
        msg.setIncludedVnfds(includedVnfds);

        // send notification over kafka bus
        notificationManager.sendNsdOnBoardingNotification(msg);

        log.debug("NSD content uploaded and nsdOnBoardingNotification delivered");
    }

    public List<DescriptorTemplate> checkVNFPkgs(DescriptorTemplate nsd) throws NotExistingEntityException, MalformattedElementException, IOException {

        log.debug("Checking VNF Pkgs availability for NSD " + nsd.getMetadata().getDescriptorId() + " with version " + nsd.getMetadata().getVersion());

        Map<String, VNFNode> vnfNodes = nsd.getTopologyTemplate().getVNFNodes();
        //Map<String, DescriptorTemplate> vnfds = new HashMap<>();
        List<DescriptorTemplate> includedVnfds = new ArrayList<>();

        for (Map.Entry<String, VNFNode> vnfNodeEntry : vnfNodes.entrySet()) {

            String vnfdId = vnfNodeEntry.getValue().getProperties().getDescriptorId();
            String version = vnfNodeEntry.getValue().getProperties().getDescriptorVersion();

            Optional<VnfPkgInfoResource> optional = vnfPkgInfoRepository.findByVnfdId(UUID.fromString(vnfdId));
            VnfPkgInfoResource vnfPkgInfoResource = null;
            if (optional.isPresent()) {
                vnfPkgInfoResource = optional.get();
            } else {
                throw new NotExistingEntityException("VNFD filename for vnfdId " + vnfdId + " not find in DB");
            }
            String fileName = vnfPkgInfoResource.getVnfdFilename();

            log.debug("Searching VNFD {} with vnfdId {} and version {}", fileName, vnfdId, version);
            File vnfd_file = storageService.loadVnfdAsResource(vnfdId, version, fileName).getFile();
            DescriptorTemplate vnfd = DescriptorsParser.fileToDescriptorTemplate(vnfd_file);
            //vnfds.putIfAbsent(vnfd.getMetadata().getDescriptorId(), vnfd);
            if (!includedVnfds.contains(vnfd)) {
                includedVnfds.add(vnfd);
            }
        }

        return includedVnfds;

    }

    public synchronized void updateNsdInfoOperationStatus(String nsdInfoId, String manoId, OperationStatus opStatus,
                                                          CatalogueMessageType messageType) throws NotExistingEntityException {

        log.debug("Retrieving nsdInfoResource {} from DB for updating with onboarding status info for plugin {}.",
                nsdInfoId, manoId);
        Optional<NsdInfoResource> optionalNsdInfoResource = nsdInfoRepo.findById(UUID.fromString(nsdInfoId));

        if (optionalNsdInfoResource.isPresent()) {
            try {
                NsdInfoResource nsdInfoResource = optionalNsdInfoResource.get();

                Map<String, NotificationResource> ackMap = new HashMap<>();
                if (nsdInfoResource.getAcknowledgedOnboardOpConsumers() != null) {
                    ackMap = nsdInfoResource.getAcknowledgedOnboardOpConsumers();
                }
                ackMap.put(manoId, new NotificationResource(nsdInfoId, messageType, opStatus));
                nsdInfoResource.setAcknowledgedOnboardOpConsumers(ackMap);

                if (messageType == CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION) {
                    log.debug("Checking NSD with nsdInfoId {} onboarding state.", nsdInfoId);
                    nsdInfoResource.setNsdOnboardingState(checkNsdOnboardingState(nsdInfoId, ackMap));
                }

                log.debug("Updating NsdInfoResource {} with onboardingState {}.", nsdInfoId,
                        nsdInfoResource.getNsdOnboardingState());
                nsdInfoRepo.saveAndFlush(nsdInfoResource);
            } catch (Exception e) {
                log.error("Error while updating NsdInfoResource with nsdInfoId: " + nsdInfoId);
                log.error("Details: ", e);
            }
        } else {
            throw new NotExistingEntityException("NsdInfoResource " + nsdInfoId + " not present in DB.");
        }
    }

    private NsdOnboardingStateType checkNsdOnboardingState(String nsdInfoId, Map<String, NotificationResource> ackMap) {

        for (Entry<String, NotificationResource> entry : ackMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION) {
                if (entry.getValue().getOpStatus() == OperationStatus.FAILED) {
                    log.error("NSD with nsdInfoId {} onboarding failed for mano with manoId {}.", nsdInfoId,
                            entry.getKey());

                    // TODO: Decide how to handle MANO onboarding failures.
                    return NsdOnboardingStateType.PROCESSING;
                } else if (entry.getValue().getOpStatus() == OperationStatus.SENT
                        || entry.getValue().getOpStatus() == OperationStatus.RECEIVED
                        || entry.getValue().getOpStatus() == OperationStatus.PROCESSING) {
                    log.debug("NSD with nsdInfoId {} onboarding still in progress for mano with manoId {}.");
                    return NsdOnboardingStateType.PROCESSING;
                }
            }
        }
        log.debug("NSD with nsdInfoId " + nsdInfoId + " successfully onboarded by all expected consumers.");
        return NsdOnboardingStateType.ONBOARDED;
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
        return nsdInfo;
    }

    private File convertToFile(MultipartFile multipart) throws Exception {
        File convFile = new File(multipart.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(multipart.getBytes());
        fos.close();
        return convFile;
    }

    @SuppressWarnings("resource")
    private MultipartFile extractNsdFile(File file) throws IOException {

        MultipartFile archivedNsd = null;

        ZipFile zipFile = new ZipFile(file);
        if (zipFile.size() == 0) {
            throw new IOException("The zip archive does not contain any entries.");
        }
        if (zipFile.size() > 1) {
            throw new IOException("The zip archive contains more than one entry.");
        }
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".yaml")) {
                // it is the NSD
                log.debug("File inside zip: " + entry.getName());

                if (archivedNsd != null) {
                    log.error("Archive validation failed: multiple NSDs in the zip");
                    throw new IOException("Multiple NSDs in the zip");
                }
                InputStream zipIn = zipFile.getInputStream(entry);
                archivedNsd = new MockMultipartFile(entry.getName(), entry.getName(), null, zipIn);
                // found (ASSUME one single .yaml in the zip)
                break;
            }
        }
        if (archivedNsd == null) {
            throw new IOException("The zip archive does not contain nsd file.");
        }
        zipFile.close();
        return archivedNsd;
    }
}
