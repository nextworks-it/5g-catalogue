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
import it.nextworks.nfvmano.catalogue.engine.resources.PnfdInfoResource;
import it.nextworks.nfvmano.catalogue.engine.resources.VnfPkgInfoResource;
import it.nextworks.nfvmano.catalogue.messages.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import it.nextworks.nfvmano.catalogue.plugins.PluginType;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.repos.*;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.pnfd.nodes.PNF.PNFNode;
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
    private PnfdInfoRepository pnfdInfoRepo;

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
        NotificationResource notificationResource = new NotificationResource(nsdInfoId, messageType, opStatus, PluginType.MANO);
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
            pluginToOperationState.put(mano.getManoId(), new NotificationResource(nsdInfoId, messageType, opStatus, PluginType.MANO));

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

                log.debug("The NSD info can be removed.");
                if (nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.ONBOARDED
                        || nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.LOCAL_ONBOARDED
                        || nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.PROCESSING) {
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
        if ((!isInternalRequest) && (nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.ONBOARDED
                && nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.LOCAL_ONBOARDED)) {
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

        if (nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.ONBOARDED
                || nsdInfo.getNsdOnboardingState() == NsdOnboardingStateType.LOCAL_ONBOARDED) {
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

        String nsdFilename;
        DescriptorTemplate dt;
        UUID nsdId;

        // pre-set nsdinfo attributes to properly store NSDs
        // UUID nsdId = UUID.randomUUID();

        List<String> includedVnfds;
        List<String> includedPnfds;
        NsdOnboardingStateType onboardingStateType = NsdOnboardingStateType.UPLOADING;

        switch (contentType) {
            case ZIP: {
                try {
                    log.info("NSD file is in format: zip");

                    // TODO: assuming for now one single file into the zip
                    MultipartFile nsdMpFile = extractFile(inputFile);
                    // convert to File
                    File nsdFile = convertToFile(nsdMpFile);

                    dt = DescriptorsParser.fileToDescriptorTemplate(nsdFile);

                    includedVnfds = checkVNFPkgs(dt);
                    includedPnfds = checkPNFDs(dt);

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
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new MalformattedElementException("Error while parsing NSD.");
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

                    dt = DescriptorsParser.fileToDescriptorTemplate(inputFile);

                    includedVnfds = checkVNFPkgs(dt);
                    includedPnfds = checkPNFDs(dt);

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
                    onboardingStateType = NsdOnboardingStateType.FAILED;
                    nsdInfo.setNsdOnboardingState(onboardingStateType);
                    nsdInfoRepo.saveAndFlush(nsdInfo);
                    throw new MalformattedElementException("Error while parsing NSD.");
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
        // TODO: here it is actually onboarded only locally and just in the DB. To be
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
        nsdInfo.addNsdFilename(nsdFilename);

        List<UUID> vnfPkgIds = new ArrayList<>();
        for (String vnfdInfoId : includedVnfds) {
            log.debug("Adding vnfPkgInfo Id {} to vnfPkgs list in nsdInfo", vnfdInfoId);
            log.debug("Adding vnfPkgInfo Id {} to vnfPkgs list in nsdInfo", vnfdInfoId);
            vnfPkgIds.add(UUID.fromString(vnfdInfoId));
        }
        nsdInfo.setVnfPkgIds(vnfPkgIds);

        List<UUID> pnfdIds = new ArrayList<>();
        for (String pnfdInfoId : includedPnfds) {
            log.debug("Adding pnfdInfo Id {} to pnfs list in nsdInfo", pnfdInfoId);
            log.debug("Adding pnfdInfo Id {} to pnfs list in nsdInfo", pnfdInfoId);
            pnfdIds.add(UUID.fromString(pnfdInfoId));
        }
        nsdInfo.setPnfdInfoIds(pnfdIds);

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
        msg.setIncludedPnfds(includedPnfds);

        // send notification over kafka bus
        notificationManager.sendNsdOnBoardingNotification(msg);

        log.debug("NSD content uploaded and nsdOnBoardingNotification delivered");
    }

    @Override
    public PnfdInfo createPnfdInfo(CreatePnfdInfoRequest request) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException {
        log.debug("Processing request to create a new PNFD info.");
        KeyValuePairs kvp = request.getUserDefinedData();
        Map<String, String> targetKvp = new HashMap<>();
        if (kvp != null) {
            for (Map.Entry<String, String> e : kvp.entrySet()) {
                targetKvp.put(e.getKey(), e.getValue());
            }
        }
        PnfdInfoResource pnfdInfoResource = new PnfdInfoResource(targetKvp);
        pnfdInfoRepo.saveAndFlush(pnfdInfoResource);
        UUID pnfdInfoId = pnfdInfoResource.getId();
        log.debug("Created PNFD info with ID " + pnfdInfoId);
        PnfdInfo pnfdInfo = buildPnfdInfo(pnfdInfoResource);
        log.debug("Translated internal pnfd info resource into pnfd info");
        return pnfdInfo;
    }

    @Override
    public void deletePnfdInfo(String pnfdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
        log.debug("Processing request to delete an PNFD info");

        if (pnfdInfoId == null)
            throw new MalformattedElementException("Invalid PNFD info ID");
        try {
            UUID id = UUID.fromString(pnfdInfoId);

            Optional<PnfdInfoResource> optional = pnfdInfoRepo.findById(id);

            if (optional.isPresent()) {
                log.debug("Found PNFD info resource with id: " + pnfdInfoId);

                PnfdInfoResource pnfdInfo = optional.get();
                pnfdInfo.isDeletable();

                log.debug("The PNFD info can be removed.");
                if (pnfdInfo.getPnfdOnboardingState() == PnfdOnboardingStateType.ONBOARDED
                        || pnfdInfo.getPnfdOnboardingState() == PnfdOnboardingStateType.LOCAL_ONBOARDED
                        || pnfdInfo.getPnfdOnboardingState() == PnfdOnboardingStateType.PROCESSING) {
                    log.debug("The PNFD info is associated to an onboarded PNFD. Removing it");
                    UUID pnfdId = pnfdInfo.getPnfdId();

                    try {
                        storageService.deleteNsd(pnfdInfo.getPnfdId().toString(), pnfdInfo.getPnfdVersion());
                    } catch (Exception e) {
                        log.error("Unable to delete PNFD with nsdId {} from fylesystem", pnfdInfo.getPnfdId().toString());
                        log.error("Details: ", e);
                    }

                    UUID operationId = insertOperationInfoInConsumersMap(pnfdInfoId,
                            CatalogueMessageType.PNFD_DELETION_NOTIFICATION, OperationStatus.SENT);

                    log.debug("PNFD {} locally removed. Sending nsdDeletionNotificationMessage to bus", pnfdId);
                    PnfdDeletionNotificationMessage msg = new PnfdDeletionNotificationMessage(pnfdInfoId, pnfdId.toString(),
                            operationId, ScopeType.LOCAL, OperationStatus.SENT, null);
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
    public PnfdInfoModifications updatePnfdInfo(PnfdInfoModifications pnfdInfoModifications, String pnfdInfoId) throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException {
        return null;
    }

    @Override
    public Object getPnfd(String pnfdInfoId, boolean isInternalRequest) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
        log.debug("Processing request to retrieve an PNFD content for PNFD info " + pnfdInfoId);
        PnfdInfoResource pnfdInfo = getPnfdInfoResource(pnfdInfoId);
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
                return storageService.loadNsdAsResource(pnfdInfo.getPnfdId().toString(), pnfdInfo.getPnfdVersion(), pnfdFilename);
            }

            default: {
                log.error("Content type not yet supported");
                throw new MethodNotImplementedException("Content type not yet supported");
            }
        }
    }

    @Override
    public PnfdInfo getPnfdInfo(String pnfdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException {
        log.debug("Processing request to get an PNFD info");
        PnfdInfoResource pnfdInfoResource = getPnfdInfoResource(pnfdInfoId);
        log.debug("Found PNFD info resource with id: " + pnfdInfoId);
        PnfdInfo pnfdInfo = buildPnfdInfo(pnfdInfoResource);
        log.debug("Built PNFD info with id: " + pnfdInfoId);
        return pnfdInfo;
    }

    @Override
    public List<PnfdInfo> getAllPnfdInfos() throws FailedOperationException, MethodNotImplementedException {
        log.debug("Processing request to get all PNFD infos");

        List<PnfdInfoResource> pnfdInfoResources = pnfdInfoRepo.findAll();
        List<PnfdInfo> pnfdInfos = new ArrayList<>();

        for (PnfdInfoResource pnfdInfoResource : pnfdInfoResources) {
            PnfdInfo pnfdInfo = buildPnfdInfo(pnfdInfoResource);
            pnfdInfos.add(pnfdInfo);
            log.debug("Added Pnfd info " + pnfdInfoResource.getId());
        }
        return pnfdInfos;
    }

    @Override
    public void uploadPnfd(String pnfdInfoId, MultipartFile pnfd, ContentType contentType) throws MalformattedElementException, NotExistingEntityException, NotPermittedOperationException, FailedOperationException, MethodNotImplementedException {
        log.debug("Processing request to upload PNFD content for PNFD info " + pnfdInfoId);

        PnfdInfoResource pnfdInfo = getPnfdInfoResource(pnfdInfoId);

        if (pnfdInfo.getPnfdOnboardingState() != PnfdOnboardingStateType.CREATED) {
            log.error("PNFD info " + pnfdInfoId + " not in CREATED onboarding state.");
            throw new NotPermittedOperationException("PNFD info " + pnfdInfoId + " not in CREATED onboarding state.");
        }

        // convert to File
        File inputFile = null;
        try {
            inputFile = convertToFile(pnfd);
        } catch (Exception e) {
            log.error("Error while parsing PNFD in zip format: " + e.getMessage());
            throw new MalformattedElementException("Error while parsing PNFD.");
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

                    dt = DescriptorsParser.fileToDescriptorTemplate(pnfdFile);

                    pnfdId = UUID.fromString(dt.getMetadata().getDescriptorId());
                    pnfdInfo.setPnfdId(pnfdId);

                    log.debug("PNFD successfully parsed - its content is: \n"
                            + DescriptorsParser.descriptorTemplateToString(dt));

                    // pre-set pnfdInfo attributes to properly store PNFDs
                    pnfdInfo.setPnfdVersion(dt.getMetadata().getVersion());

                    pnfdFilename = storageService.storeNsd(pnfdInfo.getPnfdId().toString(), pnfdInfo.getPnfdVersion(), pnfdMpFile);

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

                    dt = DescriptorsParser.fileToDescriptorTemplate(inputFile);

                    pnfdId = UUID.fromString(dt.getMetadata().getDescriptorId());
                    pnfdInfo.setPnfdId(pnfdId);

                    log.debug("PNFD successfully parsed - its content is: \n"
                            + DescriptorsParser.descriptorTemplateToString(dt));
                    // pre-set pnfdInfo attributes to properly store PNFDs
                    pnfdInfo.setPnfdVersion(dt.getMetadata().getVersion());

                    pnfdFilename = storageService.storeNsd(pnfdInfo.getPnfdId().toString(), pnfdInfo.getPnfdVersion(), pnfd);

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

        // TODO: request to Policy Manager for retrieving the MANO Plugins list,
        // now all plugins are expected to be consumers

        UUID operationId = insertOperationInfoInConsumersMap(pnfdInfoId,
                CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION, OperationStatus.SENT);
        pnfdInfo.setAcknowledgedOnboardOpConsumers(operationIdToConsumersAck.get(operationId.toString()));

        pnfdInfoRepo.saveAndFlush(pnfdInfo);
        log.debug("PNFD info updated");

        PnfdOnBoardingNotificationMessage msg = new PnfdOnBoardingNotificationMessage(pnfdInfo.getId().toString(), pnfdId.toString(),
                operationId, ScopeType.LOCAL, OperationStatus.SENT);

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
                log.debug("PNFD info resource with id " + pnfdInfoId + " not found.");
                throw new NotExistingEntityException("PNFD info resource with id " + pnfdInfoId + " not found.");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + pnfdInfoId);
            throw new MalformattedElementException("Wrong ID format: " + pnfdInfoId);
        }
    }

    public List<String> checkVNFPkgs(DescriptorTemplate nsd) throws NotExistingEntityException, MalformattedElementException, IOException, NotPermittedOperationException {

        log.debug("Checking VNF Pkgs availability for NSD " + nsd.getMetadata().getDescriptorId() + " with version " + nsd.getMetadata().getVersion());

        Map<String, VNFNode> vnfNodes = nsd.getTopologyTemplate().getVNFNodes();
        //Map<String, DescriptorTemplate> vnfds = new HashMap<>();
        List<String> includedVnfds = new ArrayList<>();

        List<VnfPkgInfoResource> vnfPkgInfoResources = new ArrayList<>();

        for (Map.Entry<String, VNFNode> vnfNodeEntry : vnfNodes.entrySet()) {

            String vnfdId = vnfNodeEntry.getValue().getProperties().getDescriptorId();
            String version = vnfNodeEntry.getValue().getProperties().getDescriptorVersion();

            Optional<VnfPkgInfoResource> optional = vnfPkgInfoRepository.findByVnfdId(UUID.fromString(vnfdId));
            VnfPkgInfoResource vnfPkgInfoResource;
            if (optional.isPresent()) {
                vnfPkgInfoResource = optional.get();
            } else {
                throw new NotExistingEntityException("VNFD filename for vnfdId " + vnfdId + " not find in DB");
            }
            String fileName = vnfPkgInfoResource.getVnfdFilename();

            log.debug("Searching VNFD {} with vnfdId {} and version {}", fileName, vnfdId, version);
            File vnfd_file = storageService.loadVnfdAsResource(vnfdId, version, fileName).getFile();
            DescriptorTemplate vnfd = DescriptorsParser.fileToDescriptorTemplate(vnfd_file);

            log.debug("VNFD successfully parsed - its content is: \n"
                    + DescriptorsParser.descriptorTemplateToString(vnfd));

            if (!includedVnfds.contains(vnfPkgInfoResource.getId().toString())) {
                includedVnfds.add(vnfPkgInfoResource.getId().toString());
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

    public List<String> checkPNFDs(DescriptorTemplate nsd) throws NotExistingEntityException, MalformattedElementException, IOException, NotPermittedOperationException {

        log.debug("Checking PNFDs availability for NSD " + nsd.getMetadata().getDescriptorId() + " with version " + nsd.getMetadata().getVersion());

        Map<String, PNFNode> pnfNodes = nsd.getTopologyTemplate().getPNFNodes();
        List<String> includedPnfds = new ArrayList<>();

        List<PnfdInfoResource> pnfdInfoResources = new ArrayList<>();

        for (Map.Entry<String, PNFNode> pnfNodeEntry : pnfNodes.entrySet()) {

            String pnfdId = pnfNodeEntry.getValue().getProperties().getDescriptorId();
            String version = pnfNodeEntry.getValue().getProperties().getVersion();

            Optional<PnfdInfoResource> optional = pnfdInfoRepo.findByPnfdId(UUID.fromString(pnfdId));
            PnfdInfoResource pnfdInfoResource;
            if (optional.isPresent()) {
                pnfdInfoResource = optional.get();
            } else {
                throw new NotExistingEntityException("PNFD filename for pnfdId " + pnfdId + " not find in DB");
            }
            String fileName = pnfdInfoResource.getPnfdFilename().get(0);

            log.debug("Searching PNFD {} with pnfdId {} and version {}", fileName, pnfdId, version);
            File pnfd_file = storageService.loadNsdAsResource(pnfdId, version, fileName).getFile();
            DescriptorTemplate pnfd = DescriptorsParser.fileToDescriptorTemplate(pnfd_file);

            log.debug("PNFD successfully parsed - its content is: \n"
                    + DescriptorsParser.descriptorTemplateToString(pnfd));

            if (!includedPnfds.contains(pnfdInfoResource.getId().toString())) {
                includedPnfds.add(pnfdInfoResource.getId().toString());
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
                ackMap.put(manoId, new NotificationResource(nsdInfoId, messageType, opStatus, PluginType.MANO));
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
            if (entry.getValue().getOperation() == CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION && entry.getValue().getPluginType() == PluginType.MANO) {
                if (entry.getValue().getOpStatus() == OperationStatus.FAILED) {
                    log.error("NSD with nsdInfoId {} onboarding failed for mano with manoId {}.", nsdInfoId,
                            entry.getKey());

                    // TODO: Decide how to handle MANO onboarding failures.
                    return NsdOnboardingStateType.LOCAL_ONBOARDED;
                } else if (entry.getValue().getOpStatus() == OperationStatus.SENT
                        || entry.getValue().getOpStatus() == OperationStatus.RECEIVED
                        || entry.getValue().getOpStatus() == OperationStatus.PROCESSING) {
                    log.debug("NSD with nsdInfoId {} onboarding still in progress for mano with manoId {}.");
                    return NsdOnboardingStateType.LOCAL_ONBOARDED;
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

        Map<String, NotificationResource> acksMap = nsdInfoResource.getAcknowledgedOnboardOpConsumers();
        Map<String, NsdOnboardingStateType> manoIdToOnboardingStatus = new HashMap<>();
        Map<String, NsdOnboardingStateType> c2cOnboardingStatus = new HashMap<>();
        for (Entry<String, NotificationResource> entry : acksMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION) {
                NsdOnboardingStateType nsdOnboardingStateType = NsdOnboardingStateType.UPLOADING;
                switch (entry.getValue().getOpStatus()) {
                    case SENT:
                        nsdOnboardingStateType = NsdOnboardingStateType.UPLOADING;
                        break;
                    case RECEIVED:
                        nsdOnboardingStateType = NsdOnboardingStateType.PROCESSING;
                        break;
                    case PROCESSING:
                        nsdOnboardingStateType = NsdOnboardingStateType.PROCESSING;
                        break;
                    case FAILED:
                        nsdOnboardingStateType = NsdOnboardingStateType.FAILED;
                        break;
                    case SUCCESSFULLY_DONE:
                        nsdOnboardingStateType = NsdOnboardingStateType.ONBOARDED;
                }
                switch (entry.getValue().getPluginType()) {
                    case MANO:
                        manoIdToOnboardingStatus.putIfAbsent(entry.getKey(), nsdOnboardingStateType);
                        break;
                    case C2C:
                        c2cOnboardingStatus.putIfAbsent(entry.getKey(), nsdOnboardingStateType);
                }

            }
        }

        nsdInfo.setManoIdToOnboardingStatus(manoIdToOnboardingStatus);
        nsdInfo.setC2cOnboardingStatus(c2cOnboardingStatus);

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
            if (entry.getValue().getOperation() == CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION && entry.getValue().getPluginType() == PluginType.MANO) {
                if (entry.getValue().getOpStatus() == OperationStatus.FAILED) {
                    log.error("PNFD with pnfdInfoId {} onboarding failed for mano with manoId {}", pnfdInfoId,
                            entry.getKey());

                    // TODO: Decide how to handle MANO onboarding failures.
                    return PnfdOnboardingStateType.LOCAL_ONBOARDED;
                } else if (entry.getValue().getOpStatus() == OperationStatus.SENT
                        || entry.getValue().getOpStatus() == OperationStatus.RECEIVED
                        || entry.getValue().getOpStatus() == OperationStatus.PROCESSING) {
                    log.debug("PNFD with pnfdInfoId {} onboarding still in progress for mano with manoId {}");
                    return PnfdOnboardingStateType.LOCAL_ONBOARDED;
                }
            }
        }
        log.debug("PNFD with pnfdInfoId " + pnfdInfoId + " successfully onboarded by all expected consumers");
        return PnfdOnboardingStateType.ONBOARDED;
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

        Map<String, NotificationResource> acksMap = pnfdInfoResource.getAcknowledgedOnboardOpConsumers();
        Map<String, PnfdOnboardingStateType> manoIdToOnboardingStatus = new HashMap<>();
        Map<String, PnfdOnboardingStateType> c2cOnboardingStatus = new HashMap<>();
        for (Entry<String, NotificationResource> entry : acksMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION) {
                PnfdOnboardingStateType pnfdOnboardingStateType = PnfdOnboardingStateType.UPLOADING;
                switch (entry.getValue().getOpStatus()) {
                    case SENT:
                        pnfdOnboardingStateType = PnfdOnboardingStateType.UPLOADING;
                        break;
                    case RECEIVED:
                        pnfdOnboardingStateType = PnfdOnboardingStateType.PROCESSING;
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
                switch (entry.getValue().getPluginType()) {
                    case MANO:
                        manoIdToOnboardingStatus.putIfAbsent(entry.getKey(), pnfdOnboardingStateType);
                        break;
                    case C2C:
                        c2cOnboardingStatus.putIfAbsent(entry.getKey(), pnfdOnboardingStateType);
                }
            }
        }

        pnfdInfo.setManoIdToOnboardingStatus(manoIdToOnboardingStatus);

        return pnfdInfo;
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
    private MultipartFile extractFile(File file) throws IOException {

        MultipartFile archived = null;

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
                // it is the NSD/PNFD
                log.debug("File inside zip: " + entry.getName());

                if (archived != null) {
                    log.error("Archive validation failed: multiple NSDs/PNFDs in the zip");
                    throw new IOException("Multiple NSDs/PNFDs in the zip");
                }
                InputStream zipIn = zipFile.getInputStream(entry);
                archived = new MockMultipartFile(entry.getName(), entry.getName(), null, zipIn);
                // found (ASSUME one single .yaml in the zip)
                break;
            }
        }
        if (archived == null) {
            throw new IOException("The zip archive does not contain NSD/PNFD file.");
        }
        zipFile.close();
        return archived;
    }
}
