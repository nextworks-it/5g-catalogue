package it.nextworks.nfvmano.catalogue.engine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.engine.resources.NotificationResource;
import it.nextworks.nfvmano.catalogue.engine.resources.VnfPkgInfoResource;
import it.nextworks.nfvmano.catalogue.messages.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.messages.ScopeType;
import it.nextworks.nfvmano.catalogue.messages.VnfPkgDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.VnfPkgOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.KeyValuePairs;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.*;
import it.nextworks.nfvmano.catalogue.plugins.PluginType;
import it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.C2COnboardingStateType;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.catalogue.repos.MANORepository;
import it.nextworks.nfvmano.catalogue.repos.VnfPkgInfoRepository;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.ArchiveParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.elements.CSARInfo;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;

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
    private MANORepository MANORepository;

    @Autowired
    private ArchiveParser archiveParser;

    private Map<String, Map<String, NotificationResource>> operationIdToConsumersAck = new HashMap<>();

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
                                                   OperationStatus opStatus) {
        UUID operationId = UUID.randomUUID();
        log.debug("Updating consumers internal mapping for operationId {}", operationId);
        List<MANO> manos = MANORepository.findAll();
        Map<String, NotificationResource> pluginToOperationState = new HashMap<>();
        for (MANO mano : manos) {
            pluginToOperationState.put(mano.getManoId(), new NotificationResource(vnfPkgInfoId, messageType, opStatus, PluginType.MANO));

        }
        operationIdToConsumersAck.put(operationId.toString(), pluginToOperationState);
        return operationId;
    }

    @Override
    public VnfPkgInfo createVnfPkgInfo(CreateVnfPkgInfoRequest request) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException {
        log.debug("Processing request to create a new VNF Pkg info.");
        KeyValuePairs kvp = request.getUserDefinedData();
        Map<String, String> targetKvp = new HashMap<>();
        if (kvp != null) {
            for (Map.Entry<String, String> e : kvp.entrySet()) {
                targetKvp.put(e.getKey(), e.getValue());
            }
        }
        VnfPkgInfoResource vnfPkgInfoResource = new VnfPkgInfoResource(targetKvp);
        vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
        UUID vnfPkgInfoId = vnfPkgInfoResource.getId();
        log.debug("Created VNF Pkg info with ID " + vnfPkgInfoId);
        VnfPkgInfo vnfPkgInfo = buildVnfPkgInfo(vnfPkgInfoResource);
        log.debug("Translated internal VNF Pkg info resource into VNF Pkg info");
        return vnfPkgInfo;
    }

    @Override
    public void deleteVnfPkgInfo(String vnfPkgInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
        log.debug("Processing request to delete an VNF Pkg info.");

        if (vnfPkgInfoId == null)
            throw new MalformattedElementException("Invalid VNF Pkg info ID.");

        try {
            UUID id = UUID.fromString(vnfPkgInfoId);

            Optional<VnfPkgInfoResource> optional = vnfPkgInfoRepository.findById(id);

            if (optional.isPresent()) {
                log.debug("Found VNF Pkg info resource with id: " + vnfPkgInfoId);

                VnfPkgInfoResource vnfPkgInfoResource = optional.get();
                vnfPkgInfoResource.isDeletable();

                log.debug("The VNF Pkg info can be removed.");
                if (vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.ONBOARDED
                        || vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.LOCAL_ONBOARDED
                        || vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.PROCESSING) {
                    log.debug("The VNF Pkg info is associated to an onboarded VNF Pkg. Removing it.");
                    UUID vnfdId = vnfPkgInfoResource.getVnfdId();

                    try {
                        storageService.deleteVnfPkg(vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion());
                    } catch (Exception e) {
                        log.error("Unable to delete VNF Pkg with vnfdId {} from fylesystem.", vnfPkgInfoResource.getVnfdId().toString());
                        log.error("Details: ", e);
                    }

                    UUID operationId = insertOperationInfoInConsumersMap(vnfPkgInfoId,
                            CatalogueMessageType.NSD_DELETION_NOTIFICATION, OperationStatus.SENT);

                    log.debug("VNF Pkg {} locally removed. Sending vnfPkgDeletionNotificationMessage to bus.", vnfdId);

                    VnfPkgDeletionNotificationMessage msg = new VnfPkgDeletionNotificationMessage(vnfPkgInfoId, vnfdId.toString(), operationId, ScopeType.LOCAL, OperationStatus.SENT);
                    msg.setVnfName(vnfPkgInfoResource.getVnfProductName());
                    notificationManager.sendVnfPkgDeletionNotification(msg);
                }

                vnfPkgInfoRepository.deleteById(id);
                log.debug("Deleted VNF Pkg info resource with id: " + vnfPkgInfoId);
            } else {
                log.debug("VNF Pkg info resource with id " + vnfPkgInfoId + "not found.");
                throw new NotExistingEntityException("VNF Pkg info resource with id " + vnfPkgInfoId + "not found.");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + vnfPkgInfoId);
            throw new MalformattedElementException("Wrong ID format: " + vnfPkgInfoId);
        }

    }

    @Override
    public VnfPkgInfoModifications updateVnfPkgInfo(VnfPkgInfoModifications vnfPkgInfoModifications, String vnfPkgInfoId) throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException {
        log.debug("Processing request to update VNF Pkg info: " + vnfPkgInfoId);
        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);

        if (vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.ONBOARDED
                || vnfPkgInfoResource.getOnboardingState() == PackageOnboardingStateType.LOCAL_ONBOARDED) {
            if (vnfPkgInfoModifications.getOperationalState() != null) {
                if (vnfPkgInfoResource.getOperationalState() == vnfPkgInfoModifications.getOperationalState()) {
                    log.error("VNF Pkg operational state already "
                            + vnfPkgInfoResource.getOperationalState() + ". Cannot update VNF Pkg info.");
                    throw new NotPermittedOperationException("VNF Pkg operational state already "
                            + vnfPkgInfoResource.getOperationalState() + ". Cannot update VNF Pkg info.");
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
            log.debug("VnfPkgInfoResource successfully updated.");
        } else {
            log.error("VNF Pkg onboarding state not ONBOARDED. Cannot update VNF Pkg info.");
            throw new NotPermittedOperationException("VNF Pkg onboarding state not ONBOARDED/LOCAL_ONBOARDED. Cannot update VNF Pkg info.");
        }
        return vnfPkgInfoModifications;
    }

    @Override
    public Object getVnfd(String vnfPkgInfoId, boolean isInternalRequest) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException {
        log.debug("Processing request to retrieve a VNFD content for VNF Pkg info " + vnfPkgInfoId);

        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);
        if ((!isInternalRequest) && (vnfPkgInfoResource.getOnboardingState() != PackageOnboardingStateType.ONBOARDED
                && vnfPkgInfoResource.getOnboardingState() != PackageOnboardingStateType.LOCAL_ONBOARDED)) {
            log.error("VNF Pkg info " + vnfPkgInfoId + " does not have an onboarded VNFD yet");
            throw new NotPermittedOperationException("VNF Pkg info " + vnfPkgInfoId + " does not have an onboarded VNFD yet");
        }
        UUID vnfdId = vnfPkgInfoResource.getVnfdId();
        log.debug("Internal VNFD ID: " + vnfdId);


        String vnfdFilename = vnfPkgInfoResource.getVnfdFilename();
        if (vnfdFilename == null) {
            log.error("Found zero file for VNFD in YAML format. Error.");
            throw new FailedOperationException("Found zero file for VNFD in YAML format. Error.");
        }

        return storageService.loadVnfdAsResource(vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), vnfdFilename);
    }

    @Override
    public Object getVnfPkg(String vnfPkgInfoId, boolean isInternalRequest) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
        log.debug("Processing request to retrieve a VNF Pkg content for VNF Pkg info " + vnfPkgInfoId);

        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);
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
                    log.error("Found zero file for VNF Pkg in ZIP format. Error.");
                    throw new FailedOperationException("Found zero file for VNF Pkg in ZIP format. Error.");
                }

                return storageService.loadVnfPkgAsResource(vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), vnfPkgFilename);
            }

            default: {
                log.error("Content type not yet supported.");
                throw new MethodNotImplementedException("Content type not yet supported.");
            }
        }
    }

    @Override
    public VnfPkgInfo getVnfPkgInfo(String vnfPkgInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException {
        log.debug("Processing request to get a VNF Pkg info.");
        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);
        log.debug("Found VNF Pkg info resource with id: " + vnfPkgInfoId);
        VnfPkgInfo vnfPkgInfo = buildVnfPkgInfo(vnfPkgInfoResource);
        log.debug("Built VNF Pkg info with id: " + vnfPkgInfoId);
        return vnfPkgInfo;
    }

    @Override
    public List<VnfPkgInfo> getAllVnfPkgInfos() throws FailedOperationException, MethodNotImplementedException {
        log.debug("Processing request to get all VNF Pkg infos.");

        List<VnfPkgInfoResource> vnfPkgInfoResources = vnfPkgInfoRepository.findAll();
        List<VnfPkgInfo> vnfPkgInfos = new ArrayList<>();

        for (VnfPkgInfoResource vnfPkgInfoResource : vnfPkgInfoResources) {
            VnfPkgInfo vnfPkgInfo = buildVnfPkgInfo(vnfPkgInfoResource);
            vnfPkgInfos.add(vnfPkgInfo);
            log.debug("Added VNF Pkg info " + vnfPkgInfoResource.getId());
        }
        return vnfPkgInfos;
    }

    @Override
    public void uploadVnfPkg(String vnfPkgInfoId, MultipartFile vnfPkg, ContentType contentType, boolean isInternalRequest) throws FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
        log.debug("Processing request to upload VNF Pkg content for VNFD info " + vnfPkgInfoId);
        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);

        if (vnfPkgInfoResource.getOnboardingState() != PackageOnboardingStateType.CREATED) {
            log.error("VNF Pkg info " + vnfPkgInfoId + " not in CREATED onboarding state.");
            throw new NotPermittedOperationException("VNF Pkg info " + vnfPkgInfoId + " not in CREATED onboarding state.");
        }

        String vnfPkgFilename = null;
        DescriptorTemplate dt = null;
        CSARInfo csarInfo = null;
        UUID vnfdId = null;

        if (contentType != ContentType.ZIP) {
            log.error("VNF Pkg upload request with wrong content-type.");
            throw new MalformattedElementException("VNF Pkg " + vnfPkgInfoId + " upload request with wrong content-type.");
        }

        checkZipArchive(vnfPkg);

        try {
            csarInfo = archiveParser.archiveToMainDescriptor(vnfPkg);
            dt = csarInfo.getMst();
            vnfPkgFilename = csarInfo.getVnfPkgFilename();

            vnfdId = UUID.fromString(dt.getMetadata().getDescriptorId());
            vnfPkgInfoResource.setVnfdId(vnfdId);
            log.debug("VNFD in Pkg successfully parsed - its content is: \n"
                    + DescriptorsParser.descriptorTemplateToString(dt));

            vnfPkgInfoResource.setVnfdVersion(dt.getMetadata().getVersion());
            vnfPkgInfoResource.setVnfProvider(dt.getMetadata().getVendor());

            //vnfPkgFilename = storageService.storeVnfPkg(vnfPkgInfoResource.getVnfdId().toString(), vnfPkgInfoResource.getVnfdVersion(), vnfPkg);

            log.debug("VNF Pkg file successfully stored: " + vnfPkgFilename);
        } catch (IOException e) {
            log.error("Error while parsing VNF Pkg: " + e.getMessage());
            throw new MalformattedElementException("Error while parsing VNF Pkg.");
        } catch (MalformattedElementException e) {
            log.error("Error while parsing VNF Pkg, not aligned with CSAR format: " + e.getMessage());
            throw new MalformattedElementException("Error while parsing VNF Pkg, not aligned with CSAR format.");
        }

        if (vnfPkgFilename == null || dt == null) {
            throw new FailedOperationException("Invalid internal structures.");
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
        vnfPkgInfoResource.setVnfdFilename(csarInfo.getVnfdFilename());
        vnfPkgInfoResource.setMetaFilename(csarInfo.getMetaFilename());
        vnfPkgInfoResource.setManifestFilename(csarInfo.getMfFilename());

        // TODO: request to Policy Manager for retrieving the MANO Plugins list,
        // now all plugins are expected to be consumers

        UUID operationId = insertOperationInfoInConsumersMap(vnfPkgInfoId,
                CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION, OperationStatus.SENT);
        vnfPkgInfoResource.setAcknowledgedOnboardOpConsumers(operationIdToConsumersAck.get(operationId.toString()));

        if (isInternalRequest)
            vnfPkgInfoResource.setPublished(true);
        else
            vnfPkgInfoResource.setPublished(false);

        vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
        log.debug("VNF PKG info updated");

        // send notification over kafka bus
        VnfPkgOnBoardingNotificationMessage msg =
                new VnfPkgOnBoardingNotificationMessage(vnfPkgInfoId, vnfdId.toString(), operationId, ScopeType.LOCAL, OperationStatus.SENT);
        msg.setCsarInfo(csarInfo);
        notificationManager.sendVnfPkgOnBoardingNotification(msg);

        log.debug("VNF Pkg content uploaded and vnfPkgOnBoardingNotification delivered");
    }

    private VnfPkgInfo buildVnfPkgInfo(VnfPkgInfoResource vnfPkgInfoResource) {
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
                        pkgOnboardingStateType = PackageOnboardingStateType.PROCESSING;
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
                log.debug("VNF Pkg info resource with id " + vnfPkgInfoId + " not found.");
                throw new NotExistingEntityException("VNF Pkg info resource with id " + vnfPkgInfoId + " not found.");
            }
        } catch (IllegalArgumentException e) {
            log.error("Wrong ID format: " + vnfPkgInfoId);
            throw new MalformattedElementException("Wrong ID format: " + vnfPkgInfoId);
        }
    }

    private File convertToFile(MultipartFile multipart) throws Exception {
        File convFile = new File(multipart.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(multipart.getBytes());
        fos.close();
        return convFile;
    }

    public void updateVnfPkgInfoOperationStatus(String vnfPkgInfoId, String pluginId, OperationStatus opStatus, CatalogueMessageType type) throws NotExistingEntityException {
        log.debug("Retrieving vnfPkgInfoResource {} from DB for updating with onboarding status info for plugin {}.",
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
                    log.debug("Checking VNF Pkg with vnfPkgInfoId {} onboarding state.", vnfPkgInfoId);
                    vnfPkgInfoResource.setOnboardingState(checkVnfPkgOnboardingState(vnfPkgInfoId, ackMap));
                }

                log.debug("Updating VnfPkgInfoResource {} with onboardingState {}.", vnfPkgInfoId,
                        vnfPkgInfoResource.getOnboardingState());
                vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
            } catch (Exception e) {
                log.error("Error while updating VnfPkgInfoResource with vnfPkgInfoId: " + vnfPkgInfoId);
                log.error("Details: ", e);
            }
        } else {
            throw new NotExistingEntityException("VnfPkgInfoResource " + vnfPkgInfoId + " not present in DB.");
        }
    }

    private PackageOnboardingStateType checkVnfPkgOnboardingState(String vnfPkgInfoId, Map<String, NotificationResource> ackMap) {

        for (Map.Entry<String, NotificationResource> entry : ackMap.entrySet()) {
            if (entry.getValue().getOperation() == CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION && entry.getValue().getPluginType() == PluginType.MANO) {
                if (entry.getValue().getOpStatus() == OperationStatus.FAILED) {
                    log.error("VNF Pkg with vnfPkgInfoId {} onboarding failed for mano with manoId {}.", vnfPkgInfoId,
                            entry.getKey());

                    // TODO: Decide how to handle MANO onboarding failures.
                    return PackageOnboardingStateType.LOCAL_ONBOARDED;
                } else if (entry.getValue().getOpStatus() == OperationStatus.SENT
                        || entry.getValue().getOpStatus() == OperationStatus.RECEIVED
                        || entry.getValue().getOpStatus() == OperationStatus.PROCESSING) {
                    log.debug("VNF Pkg with vnfPkgInfoId {} onboarding still in progress for mano with manoId {}.");
                    return PackageOnboardingStateType.LOCAL_ONBOARDED;
                }
            }
        }
        log.debug("VNF Pkg with vnfPkgInfoId " + vnfPkgInfoId + " successfully onboarded by all expected consumers.");
        return PackageOnboardingStateType.ONBOARDED;
    }

    private void checkZipArchive(MultipartFile vnfPkg) throws FailedOperationException {

        byte[] bytes = new byte[0];
        try {
            bytes = vnfPkg.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStream is = new ByteArrayInputStream(bytes);

        ZipInputStream zis = new ZipInputStream(is);

        try {
            zis.getNextEntry();
        } catch (IOException e) {
            throw new FailedOperationException("CSAR Archive is corrupted: " + e.getMessage());
        }

        try {
            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            throw new FailedOperationException("CSAR Archive is corrupted: " + e.getMessage());
        }
    }
}
