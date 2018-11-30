package it.nextworks.nfvmano.catalogue.engine;

import it.nextworks.nfvmano.catalogue.engine.resources.NotificationResource;
import it.nextworks.nfvmano.catalogue.engine.resources.VnfPkgInfoResource;
import it.nextworks.nfvmano.catalogue.messages.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.KeyValuePairs;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOnboardingStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOperationalStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.*;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANORepository;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.catalogue.repos.VnfPkgInfoRepository;
import it.nextworks.nfvmano.catalogue.storage.StorageServiceInterface;
import it.nextworks.nfvmano.catalogue.translators.tosca.ArchiveParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class VnfPackageManagementService implements  VnfPackageManagementInterface {

    private static final Logger log = LoggerFactory.getLogger(VnfPackageManagementService.class);

    @Autowired
    private VnfPkgInfoRepository vnfPkgInfoRepository;

    @Autowired
    private StorageServiceInterface storageService;

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
        NotificationResource notificationResource = new NotificationResource(nsdInfoId, messageType, opStatus);
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
            pluginToOperationState.put(mano.getManoId(), new NotificationResource(vnfPkgInfoId, messageType, opStatus));

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

    }

    @Override
    public VnfPkgInfoModifications updateVnfPkgInfo(VnfPkgInfoModifications vnfPkgInfoModifications, String vnfPkgInfoId) throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException {
        return null;
    }

    @Override
    public Object getVnfPkg(String vnfPkgInfoId, boolean isInternalRequest) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
        return null;
    }

    @Override
    public VnfPkgInfo getVnfPkgInfo(String vnfPkgInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException {
        return null;
    }

    @Override
    public List<VnfPkgInfo> getAllVnfPkgInfos() throws FailedOperationException, MethodNotImplementedException {
        return null;
    }

    @Override
    public void uploadVnfPkg(String vnfPkgInfoId, MultipartFile vnfPkg, ContentType contentType) throws FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
        log.debug("Processing request to upload VNF Pkg content for NSD info " + vnfPkgInfoId);
        VnfPkgInfoResource vnfPkgInfoResource = getVnfPkgInfoResource(vnfPkgInfoId);

        if (vnfPkgInfoResource.getOnboardingState() != PackageOnboardingStateType.CREATED) {
            log.error("VNF Pkg info " + vnfPkgInfoId + " not in CREATED onboarding state.");
            throw new NotPermittedOperationException("VNF Pkg info " + vnfPkgInfoId + " not in CREATED onboarding state.");
        }

        String vnfPkgFilename = null;
        DescriptorTemplate dt = null;
        UUID vnfdId = null;

        if (contentType != ContentType.ZIP) {
            log.error("VNF Pkg upload request with wrong content-type.");
            throw new MalformattedElementException("VNF Pkg " + vnfPkgInfoId + " upload request with wrong content-type.");
        }

        try {
            dt = archiveParser.archiveToMainDescriptor(vnfPkg);

            vnfdId = UUID.fromString(dt.getMetadata().getDescriptorId());
            vnfPkgInfoResource.setVnfdId(vnfdId);
            log.debug("VNFD in Pkg successfully parsed - its content is: \n"
                    + DescriptorsParser.descriptorTemplateToString(dt));

            vnfPkgInfoResource.setVnfdVersion(dt.getMetadata().getVersion());
            vnfPkgInfoResource.setVnfProvider(dt.getMetadata().getVendor());

            vnfPkgFilename = storageService.storeVnfPkg(vnfPkgInfoResource, vnfPkg);

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
        vnfPkgInfoResource.setOnboardingState(PackageOnboardingStateType.PROCESSING);
        vnfPkgInfoResource.setOperationalState(PackageOperationalStateType.ENABLED);
        vnfPkgInfoResource.setVnfProvider(dt.getMetadata().getVendor());
        vnfPkgInfoResource.setVnfdVersion(dt.getMetadata().getVersion());

        Map<String, VNFNode> vnfNodes = dt.getTopologyTemplate().getVNFNodes();
        String vnfName = "";
        if (vnfNodes.size() == 1) {
            for (Map.Entry<String, VNFNode> vnfNode : vnfNodes.entrySet()) {
                vnfName = vnfNode.getValue().getProperties().getProductName();
            }
        }

        log.debug("VNFD name: " + vnfName);
        vnfPkgInfoResource.setVnfProductName(vnfName);
        vnfPkgInfoResource.setContentType(contentType);
        vnfPkgInfoResource.setVnfPkgFilename(vnfPkgFilename);

        // TODO: request to Policy Manager for retrieving the MANO Plugins list,
        // now all plugins are expected to be consumers

        UUID operationId = insertOperationInfoInConsumersMap(vnfPkgInfoId,
                CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION, OperationStatus.SENT);
        vnfPkgInfoResource.setAcknowledgedOnboardOpConsumers(operationIdToConsumersAck.get(operationId.toString()));

        vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource);
        log.debug("VNF PKG info updated");

        // send notification over kafka bus
        //notificationManager.sendVnfPkgOnBoardingNotification(vnfPkgInfoResource.getId().toString(), vnfdId.toString(), operationId);

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
}
