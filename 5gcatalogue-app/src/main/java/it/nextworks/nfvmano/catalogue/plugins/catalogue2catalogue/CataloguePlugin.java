package it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementInterface;
import it.nextworks.nfvmano.catalogue.engine.VnfPackageManagementInterface;
import it.nextworks.nfvmano.catalogue.engine.resources.NotificationResource;
import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.catalogue.engine.resources.PnfdInfoResource;
import it.nextworks.nfvmano.catalogue.engine.resources.VnfPkgInfoResource;
import it.nextworks.nfvmano.catalogue.messages.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.CreateVnfPkgInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.PackageOnboardingStateType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPkgInfo;
import it.nextworks.nfvmano.catalogue.plugins.KafkaConnector;
import it.nextworks.nfvmano.catalogue.plugins.Plugin;
import it.nextworks.nfvmano.catalogue.plugins.PluginType;
import it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.api.nsd.DefaultApi;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.catalogue.repos.NsdInfoRepository;
import it.nextworks.nfvmano.catalogue.repos.PnfdInfoRepository;
import it.nextworks.nfvmano.catalogue.repos.VnfPkgInfoRepository;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;

public class CataloguePlugin extends Plugin
        implements NsdNotificationsConsumerInterface, VnfPkgNotificationsConsumerInterface {

    private static final Logger log = LoggerFactory.getLogger(CataloguePlugin.class);
    protected KafkaConnector connector;
    protected NsdManagementInterface nsdService;
    protected VnfPackageManagementInterface vnfdService;
    protected DefaultApi nsdApi;
    protected it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.api.vnf.DefaultApi vnfdApi;
    protected NsdInfoRepository nsdInfoRepository;
    protected PnfdInfoRepository pnfdInfoRepository;
    protected VnfPkgInfoRepository vnfPkgInfoRepository;
    protected KafkaTemplate<String, String> kafkaTemplate;
    private Catalogue catalogue;
    private String remoteTopic;

    public CataloguePlugin(String pluginId, PluginType pluginType) {
        super(pluginId, pluginType);
    }

    public CataloguePlugin(String pluginId, PluginType pluginType, Catalogue catalogue, String kafkaBootstrapServers,
                           NsdManagementInterface nsdService,
                           VnfPackageManagementInterface vnfdService,
                           DefaultApi nsdApi,
                           it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.api.vnf.DefaultApi vnfdApi,
                           NsdInfoRepository nsdInfoRepository,
                           PnfdInfoRepository pnfdInfoRepository,
                           VnfPkgInfoRepository vnfPkgInfoRepository,
                           String localTopic,
                           String remoteTopic,
                           KafkaTemplate<String, String> kafkaTemplate) {
        super(pluginId, pluginType);
        this.catalogue = catalogue;
        this.nsdService = nsdService;
        this.vnfdService = vnfdService;
        this.nsdApi = nsdApi;
        this.vnfdApi = vnfdApi;
        this.nsdInfoRepository = nsdInfoRepository;
        this.pnfdInfoRepository = pnfdInfoRepository;
        this.vnfPkgInfoRepository = vnfPkgInfoRepository;
        this.remoteTopic = remoteTopic;
        String connectorID = "5GCATALOGUE_" + catalogue.getCatalogueId(); // assuming it's unique among Catalogues
        Map<CatalogueMessageType, Consumer<CatalogueMessage>> functor = new HashMap<>();
        functor.put(
                CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION,
                msg -> {
                    NsdOnBoardingNotificationMessage castMsg = (NsdOnBoardingNotificationMessage) msg;
                    try {
                        acceptNsdOnBoardingNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.NSD_CHANGE_NOTIFICATION,
                msg -> {
                    NsdChangeNotificationMessage castMsg = (NsdChangeNotificationMessage) msg;
                    try {
                        acceptNsdChangeNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.NSD_DELETION_NOTIFICATION,
                msg -> {
                    NsdDeletionNotificationMessage castMsg = (NsdDeletionNotificationMessage) msg;
                    try {
                        acceptNsdDeletionNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION,
                msg -> {
                    PnfdOnBoardingNotificationMessage castMsg = (PnfdOnBoardingNotificationMessage) msg;
                    try {
                        acceptPnfdOnBoardingNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.PNFD_DELETION_NOTIFICATION,
                msg -> {
                    PnfdDeletionNotificationMessage castMsg = (PnfdDeletionNotificationMessage) msg;
                    try {
                        acceptPnfdDeletionNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION,
                msg -> {
                    VnfPkgOnBoardingNotificationMessage castMsg = (VnfPkgOnBoardingNotificationMessage) msg;
                    try {
                        acceptVnfPkgOnBoardingNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.VNFPKG_CHANGE_NOTIFICATION,
                msg -> {
                    VnfPkgChangeNotificationMessage castMsg = (VnfPkgChangeNotificationMessage) msg;
                    try {
                        acceptVnfPkgChangeNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.VNFPKG_DELETION_NOTIFICATION,
                msg -> {
                    VnfPkgDeletionNotificationMessage castMsg = (VnfPkgDeletionNotificationMessage) msg;
                    try {
                        acceptVnfPkgDeletionNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        connector = KafkaConnector.Builder()
                .setBeanId(connectorID)
                .setKafkaBootstrapServers(kafkaBootstrapServers)
                .setKafkaGroupId(connectorID)
                .addTopic(localTopic)
                .setFunctor(functor)
                .build();
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received NSD onboarding notification.");
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.C2C) {
            try {
                Optional<NsdInfoResource> nsdInfoResourceOptional = nsdInfoRepository.findById(UUID.fromString(notification.getNsdInfoId()));

                if (nsdInfoResourceOptional.isPresent()) {
                    NsdInfoResource nsdInfoResource = nsdInfoResourceOptional.get();

                    CreateNsdInfoRequest request = new CreateNsdInfoRequest();
                    if (nsdInfoResource.getUserDefinedData() != null) {
                        KeyValuePairs keyValuePairs = new KeyValuePairs();
                        keyValuePairs.putAll(nsdInfoResource.getUserDefinedData());
                        request.setUserDefinedData(keyValuePairs);
                    }

                    File nsd = ((Resource) nsdService.getNsd(notification.getNsdInfoId(), true)).getFile();

                    NsdInfo nsdInfo;
                    try {
                        nsdInfo = nsdApi.createNsdInfo(request);
                        log.debug("Created nsdInfo with id: " + nsdInfo.getId());
                    } catch (RestClientException e1) {
                        log.error("Unable to create a new NsdInfo resource on public 5G Catalogue with id {}: {}", catalogue.getCatalogueId(), e1.getMessage());
                        throw new RestClientException("Unable to create a new NsdInfo resource on public 5G Catalogue with id " + catalogue.getCatalogueId());
                    }

                    try {
                        log.debug("Creating MultipartFile...");
                        MultipartFile multipartFile = this.createMultiPartFromFile(nsd, "multipart/form-data");
                        log.debug("Trying to push NSD to public 5G Catalogue with id {}", catalogue.getCatalogueId());
                        nsdApi.uploadNSD(nsdInfo.getId().toString(), multipartFile, "multipart/form-data");
                        log.debug("NSD has been pushed successfully to 5G Catalogue with id " + catalogue.getCatalogueId());
                    } catch (RestClientException e2) {
                        log.error("Something went wrong while pushing the NSD to the public 5G Catalogue with id {}: {}", catalogue.getCatalogueId(), e2.getMessage());
                        nsdApi.deleteNSDInfo(nsdInfo.getId().toString());
                        throw new RestClientException("Something went wrong while pushing the NSD to the public 5G Catalogue with id  " + catalogue.getCatalogueId() + ": " + e2.getMessage());
                    }

                    Optional<NsdInfoResource> nsdInfoResourceOptional1 = nsdInfoRepository.findById(UUID.fromString(notification.getNsdInfoId()));

                    if (nsdInfoResourceOptional1.isPresent()) {
                        NsdInfoResource nsdInfoResource1 = nsdInfoResourceOptional1.get();

                        nsdInfoResource1.setPublished(true);
                        nsdInfoRepository.saveAndFlush(nsdInfoResource1);
                    }

                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.C2C, OperationStatus.SUCCESSFULLY_DONE,
                            catalogue.getCatalogueId()));
                }

            } catch (Exception e) {
                log.error("Could not onboard Nsd: {}", e.getMessage());
                log.debug("Error details: ", e);
                sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                        notification.getOperationId(), ScopeType.C2C, OperationStatus.FAILED,
                        catalogue.getCatalogueId()));
            }
        }
    }

    @Override
    public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received NSD change notification.");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received NSD deletion notification.");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptPnfdOnBoardingNotification(PnfdOnBoardingNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received PNFD onboarding notification.");
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.C2C) {
            try {
                Optional<PnfdInfoResource> pnfdInfoResourceOptional = pnfdInfoRepository.findById(UUID.fromString(notification.getPnfdInfoId()));

                if (pnfdInfoResourceOptional.isPresent()) {
                    PnfdInfoResource pnfdInfoResource = pnfdInfoResourceOptional.get();

                    CreatePnfdInfoRequest request = new CreatePnfdInfoRequest();
                    if (pnfdInfoResource.getUserDefinedData() != null) {
                        KeyValuePairs keyValuePairs = new KeyValuePairs();
                        keyValuePairs.putAll(pnfdInfoResource.getUserDefinedData());
                        request.setUserDefinedData(keyValuePairs);
                    }

                    File pnfd = ((Resource) nsdService.getPnfd(notification.getPnfdInfoId(), true)).getFile();

                    PnfdInfo pnfdInfo;
                    try {
                        pnfdInfo = nsdApi.createPNFDInfo(request);
                        log.debug("Created pnfdInfo with id: " + pnfdInfo.getId());
                    } catch (RestClientException e1) {
                        log.error("Unable to create a new PnfdInfo resource on public 5G Catalogue with id {}: {}", catalogue.getCatalogueId(), e1.getMessage());
                        throw new RestClientException("Unable to create a new PnfdInfo resource on public 5G Catalogue with id " + catalogue.getCatalogueId());
                    }

                    try {
                        log.debug("Creating MultipartFile...");
                        MultipartFile multipartFile = this.createMultiPartFromFile(pnfd, "multipart/form-data");
                        log.debug("Trying to push PNFD to public 5G Catalogue with id {}", catalogue.getCatalogueId());
                        nsdApi.uploadPNFD(pnfdInfo.getId().toString(), multipartFile, "multipart/form-data");
                        log.debug("PNFD has been pushed successfully to 5G Catalogue with id " + catalogue.getCatalogueId());
                    } catch (RestClientException e2) {
                        log.error("Something went wrong while pushing the PNFD to the public 5G Catalogue with id {}: {}", catalogue.getCatalogueId(), e2.getMessage());
                        nsdApi.deletePNFDInfo(pnfdInfo.getId().toString());
                        throw new RestClientException("Something went wrong while pushing the PNFD to the public 5G Catalogue with id  " + catalogue.getCatalogueId() + ": " + e2.getMessage());
                    }

                    Optional<PnfdInfoResource> pnfdInfoResourceOptional1 = pnfdInfoRepository.findById(UUID.fromString(notification.getPnfdInfoId()));

                    if (pnfdInfoResourceOptional1.isPresent()) {
                        PnfdInfoResource pnfdInfoResource1 = pnfdInfoResourceOptional1.get();

                        pnfdInfoResource1.setPublished(true);
                        pnfdInfoRepository.saveAndFlush(pnfdInfoResource1);
                    }

                    sendNotification(new PnfdOnBoardingNotificationMessage(notification.getPnfdInfoId(), notification.getPnfdId(),
                            notification.getOperationId(), ScopeType.C2C, OperationStatus.SUCCESSFULLY_DONE,
                            catalogue.getCatalogueId()));
                }

            } catch (Exception e) {
                log.error("Could not onboard Pnfd: {}", e.getMessage());
                log.debug("Error details: ", e);
                sendNotification(new PnfdOnBoardingNotificationMessage(notification.getPnfdInfoId(), notification.getPnfdId(),
                        notification.getOperationId(), ScopeType.C2C, OperationStatus.FAILED,
                        catalogue.getCatalogueId()));
            }
        }
    }

    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received PNFD deletion notification.");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received VNF Pkg onboarding notification.");
        log.debug("Body: {}", notification);
        if (notification.getScope() == ScopeType.C2C) {
            try {
                Optional<VnfPkgInfoResource> vnfPkgInfoResourceOptional = vnfPkgInfoRepository.findById(UUID.fromString(notification.getVnfPkgInfoId()));

                if (vnfPkgInfoResourceOptional.isPresent()) {
                    VnfPkgInfoResource vnfPkgInfoResource = vnfPkgInfoResourceOptional.get();

                    CreateVnfPkgInfoRequest request = new CreateVnfPkgInfoRequest();
                    if (vnfPkgInfoResource.getUserDefinedData() != null) {
                        KeyValuePairs keyValuePairs = new KeyValuePairs();
                        keyValuePairs.putAll(vnfPkgInfoResource.getUserDefinedData());
                        request.setUserDefinedData(keyValuePairs);
                    }

                    File vnfPkg = ((Resource) vnfdService.getVnfPkg(notification.getVnfPkgInfoId(), true)).getFile();

                    VnfPkgInfo vnfPkgInfo;
                    try {
                        vnfPkgInfo = vnfdApi.createVNFPkgInfo(request);
                        log.debug("Created vnfdInfo with id: " + vnfPkgInfo.getId());
                    } catch (RestClientException e1) {
                        log.error("Unable to create a new PnfdInfo resource on public 5G Catalogue with id {}: {}", catalogue.getCatalogueId(), e1.getMessage());
                        throw new RestClientException("Unable to create a new PnfdInfo resource on public 5G Catalogue with id " + catalogue.getCatalogueId());
                    }

                    try {
                        log.debug("Creating MultipartFile...");
                        MultipartFile multipartFile = this.createMultiPartFromFile(vnfPkg, "multipart/form-data");
                        log.debug("Trying to push VNF Pkg to public 5G Catalogue with id {}", catalogue.getCatalogueId());
                        vnfdApi.uploadVNFPkg(vnfPkgInfo.getId().toString(), multipartFile, "multipart/form-data");
                        log.debug("VNF Pkg has been pushed successfully to 5G Catalogue with id " + catalogue.getCatalogueId());
                    } catch (RestClientException e2) {
                        log.error("Something went wrong while pushing the VNF Pkg to the public 5G Catalogue with id {}: {}", catalogue.getCatalogueId(), e2.getMessage());
                        vnfdApi.deleteVNFPkgInfo(vnfPkgInfo.getId().toString());
                        throw new RestClientException("Something went wrong while pushing the VNF Pkg to the public 5G Catalogue with id  " + catalogue.getCatalogueId() + ": " + e2.getMessage());
                    }

                    Optional<VnfPkgInfoResource> vnfPkgInfoResourceOptional1 = vnfPkgInfoRepository.findById(UUID.fromString(notification.getVnfPkgInfoId()));

                    if (vnfPkgInfoResourceOptional1.isPresent()) {
                        VnfPkgInfoResource vnfPkgInfoResource1 = vnfPkgInfoResourceOptional1.get();

                        vnfPkgInfoResource1.setPublished(true);
                        vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource1);
                    }

                    sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                            notification.getOperationId(), ScopeType.C2C, OperationStatus.SUCCESSFULLY_DONE,
                            catalogue.getCatalogueId()));
                }

            } catch (Exception e) {
                log.error("Could not onboard Vnf Pkg: {}", e.getMessage());
                log.debug("Error details: ", e);
                sendNotification(new VnfPkgOnBoardingNotificationMessage(notification.getVnfPkgInfoId(), notification.getVnfdId(),
                        notification.getOperationId(), ScopeType.C2C, OperationStatus.FAILED,
                        catalogue.getCatalogueId()));
            }
        }
    }

    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received VNF Pkg change notification.");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received VNF Pkg deletion notification.");
        log.debug("Body: {}", notification);
    }

    public void sendNotification(CatalogueMessage msg) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        try {
            String json = mapper.writeValueAsString(msg);
            kafkaTemplate.send(remoteTopic, json);
        } catch (JsonProcessingException e) {
            log.error("Could not notify main engine: {}", e.getMessage());
            log.debug("Error details: ", e);
        }
    }

    private void loadPublicCatalogueContent() throws FailedOperationException {
        log.debug("Loading all VNF Pkg, PNFDs and NSDs from public 5G Catalogue...");
        List<VnfPkgInfo> vnfPkgInfos = vnfdApi.getVNFPkgsInfo();
        List<PnfdInfo> pnfdInfos = nsdApi.getPNFDsInfo();
        List<NsdInfo> nsdInfos = nsdApi.getNSDsInfo();

        for (VnfPkgInfo vnfPkgInfo : vnfPkgInfos) {
            Optional<VnfPkgInfoResource> vnfPkgInfoResource = vnfPkgInfoRepository.findByVnfdIdAndVnfdVersion(vnfPkgInfo.getVnfdId(), vnfPkgInfo.getVnfdVersion());
            if (vnfPkgInfoResource.isPresent()) {
                continue;
            } else {
                VnfPkgInfoResource vnfPkgTargetResource = buildVnfPkgInfoResource(vnfPkgInfo);
                VnfPkgInfoResource createdVnfPkgInfo = vnfPkgInfoRepository.saveAndFlush(vnfPkgTargetResource);

                Resource obj;
                File targetFile;
                try {
                    obj = (Resource) vnfdApi.getVNFPkg(vnfPkgInfo.getId().toString(), null);
                    InputStream inputStream = obj.getInputStream();
                    String fileName = obj.getFilename();
                    log.debug("Extracted file with filename: " + fileName);
                    targetFile = new File("src/main/resources/package.zip");

                    java.nio.file.Files.copy(
                            inputStream,
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);

                    IOUtils.closeQuietly(inputStream);

                } catch (RestClientException e1) {
                    log.error("Error when trying to get VNF Pkg with vnfPkgInfo  " + vnfPkgInfo.getId().toString() + ". Error: " + e1.getMessage());
                    try {
                        vnfdService.deleteVnfPkgInfo(createdVnfPkgInfo.getId().toString());
                    } catch (Exception e) {
                        log.error("Unable to delete vnfPkgInfo "+ createdVnfPkgInfo.getId().toString() + " after failure while loading VNF Pkg: " + e.getMessage());
                    }
                    throw new RestClientException("Error when trying to get VNF Pkg with vnfPkgInfo  " + vnfPkgInfo.getId().toString() + ". Error: " + e1.getMessage());
                } catch (IOException e) {
                    log.error("Error while getting VNF Pkg file: " + e.getMessage());
                    throw new FailedOperationException("Error while getting VNF Pkg file: " + e.getMessage());
                }

                MultipartFile multipartFile = createMultiPartFromFile(targetFile, "multipart/form-data");
                try {
                    vnfdService.uploadVnfPkg(vnfPkgTargetResource.getId().toString(), multipartFile, ContentType.ZIP, true);
                } catch (Exception e) {
                    log.error("Error while uploading VNF Pkg with vnfPkgInfoId " + vnfPkgTargetResource.getId().toString() + ": " + e.getMessage());
                    throw new FailedOperationException("Error while uploading VNF Pkg with vnfPkgInfoId " + vnfPkgTargetResource.getId().toString() + ": " + e.getMessage());
                }

                boolean success = FileUtils.deleteQuietly(targetFile);

                /*Optional<VnfPkgInfoResource> optionalVnfPkgInfoResource = vnfPkgInfoRepository.findByVnfdIdAndVnfdVersion(vnfPkgInfo.getVnfdId(), vnfPkgInfo.getVnfdVersion());
                if (optionalVnfPkgInfoResource.isPresent()) {
                    VnfPkgInfoResource vnfPkgInfoResource1 = optionalVnfPkgInfoResource.get();
                    vnfPkgInfoResource1.setPublished(true);
                    vnfPkgInfoRepository.saveAndFlush(vnfPkgInfoResource1);
                }*/
            }
        }

        for (PnfdInfo pnfdInfo : pnfdInfos) {
            Optional<PnfdInfoResource> pnfdInfoResource = pnfdInfoRepository.findByPnfdIdAndPnfdVersion(pnfdInfo.getPnfdId(), pnfdInfo.getPnfdVersion());
            if (pnfdInfoResource.isPresent()) {
                continue;
            } else {
                PnfdInfoResource pnfdTargetResource = buildPnfdInfoResource(pnfdInfo);
                PnfdInfoResource createdPnfdInfo = pnfdInfoRepository.saveAndFlush(pnfdTargetResource);

                Resource obj;
                File targetFile;
                try {
                    obj = (Resource) nsdApi.getPNFD(pnfdInfo.getId().toString());
                    InputStream inputStream = obj.getInputStream();
                    String fileName = obj.getFilename();
                    log.debug("Extracted file with filename: " + fileName);
                    targetFile = new File("src/main/resources/descriptor.yaml");

                    java.nio.file.Files.copy(
                            inputStream,
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);

                    IOUtils.closeQuietly(inputStream);

                } catch (RestClientException e1) {
                    log.error("Error when trying to get PNFD with pnfdInfo  " + pnfdInfo.getId().toString() + ". Error: " + e1.getMessage());
                    try {
                        nsdService.deletePnfdInfo(createdPnfdInfo.getId().toString());
                    } catch (Exception e) {
                        log.error("Unable to delete pnfdInfo "+ createdPnfdInfo.getId().toString() + " after failure while loading PNFD: " + e.getMessage());
                    }
                    throw new RestClientException("Error when trying to get PNFD with pnfdInfo  " + pnfdInfo.getId().toString() + ". Error: " + e1.getMessage());
                } catch (IOException e) {
                    log.error("Error while getting PNFD file: " + e.getMessage());
                    throw new FailedOperationException("Error while getting PNFD file: " + e.getMessage());
                }

                MultipartFile multipartFile = createMultiPartFromFile(targetFile, "application/yaml");
                try {
                    nsdService.uploadPnfd(pnfdTargetResource.getId().toString(), multipartFile, ContentType.YAML, true);
                } catch (Exception e) {
                    log.error("Error while uploading PNFD with pnfdInfoId " + pnfdTargetResource.getId().toString() + ": " + e.getMessage());
                    throw new FailedOperationException("Error while uploading PNFD with pnfdInfoId " + pnfdTargetResource.getId().toString() + ": " + e.getMessage());
                }

                boolean success = FileUtils.deleteQuietly(targetFile);

                /*Optional<PnfdInfoResource> optionalPnfdInfoResource = pnfdInfoRepository.findByPnfdIdAndPnfdVersion(pnfdInfo.getPnfdId(), pnfdInfo.getPnfdVersion());
                if (optionalPnfdInfoResource.isPresent()) {
                    PnfdInfoResource pnfdInfoResource1 = optionalPnfdInfoResource.get();
                    pnfdInfoResource1.setPublished(true);
                    pnfdInfoRepository.saveAndFlush(pnfdInfoResource1);
                }*/
            }
        }

        for (NsdInfo nsdInfo : nsdInfos) {
            Optional<NsdInfoResource> nsdInfoResource = nsdInfoRepository.findByNsdIdAndNsdVersion(nsdInfo.getNsdId(), nsdInfo.getNsdVersion());
            if (nsdInfoResource.isPresent()) {
                continue;
            } else {
                NsdInfoResource nsdTargetResource = buildNsdInfoResource(nsdInfo);
                NsdInfoResource createdNsdInfo = nsdInfoRepository.saveAndFlush(nsdTargetResource);

                Resource obj;
                File targetFile;
                try {
                    obj = (Resource) nsdApi.getNSD(nsdInfo.getId().toString(), null);
                    InputStream inputStream = obj.getInputStream();

                    targetFile = new File("src/main/resources/descriptor.yaml");

                    java.nio.file.Files.copy(
                            inputStream,
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);

                    IOUtils.closeQuietly(inputStream);
                } catch (RestClientException e1) {
                    log.error("Error when trying to get NSD with nsdInfo  " + nsdInfo.getId().toString() + ". Error: " + e1.getMessage());
                    try {
                        nsdService.deleteNsdInfo(createdNsdInfo.getId().toString());
                    } catch (Exception e) {
                        log.error("Unable to delete nsdInfo "+ createdNsdInfo.getId().toString() + " after failure while loading NSD: " + e.getMessage());
                    }
                    throw new RestClientException("Error when trying to get NSD with nsdInfo  " + nsdInfo.getId().toString() + ". Error: " + e1.getMessage());
                } catch (IOException e) {
                    log.error("Error while getting NSD file: " + e.getMessage());
                    throw new FailedOperationException("Error while getting NSD file: " + e.getMessage());
                }

                MultipartFile multipartFile = createMultiPartFromFile(targetFile, "application/yaml");
                try {
                    nsdService.uploadNsd(nsdTargetResource.getId().toString(), multipartFile, ContentType.YAML, true);
                } catch (Exception e) {
                    log.error("Error while uploading NSD with nsdInfoId " + nsdTargetResource.getId().toString() + ": " + e.getMessage());
                    throw new FailedOperationException("Error while uploading NSD with nsdInfoId " + nsdTargetResource.getId().toString() + ": " + e.getMessage());
                }

                boolean success = FileUtils.deleteQuietly(targetFile);

                /*Optional<NsdInfoResource> optionalNsdInfoResource1 = nsdInfoRepository.findByNsdIdAndNsdVersion(nsdInfo.getNsdId(), nsdInfo.getNsdVersion());
                if (optionalNsdInfoResource1.isPresent()) {
                    NsdInfoResource nsdInfoResource1 = optionalNsdInfoResource1.get();
                    nsdInfoResource1.setPublished(true);
                    nsdInfoRepository.saveAndFlush(nsdInfoResource1);
                }*/
            }
        }
    }

    private PnfdInfoResource buildPnfdInfoResource(PnfdInfo pnfdInfo) {

        return new PnfdInfoResource(
                pnfdInfo.getPnfdId(),
                pnfdInfo.getPnfdName(),
                pnfdInfo.getPnfdVersion(),
                pnfdInfo.getPnfdProvider(),
                pnfdInfo.getPnfdInvariantId(),
                PnfdOnboardingStateType.CREATED,
                pnfdInfo.getPnfdUsageState(),
                pnfdInfo.getUserDefinedData()
        );
    }

    private VnfPkgInfoResource buildVnfPkgInfoResource(VnfPkgInfo vnfPkgInfo) {

        return new VnfPkgInfoResource(
                vnfPkgInfo.getVnfdId(),
                vnfPkgInfo.getVnfProvider(),
                vnfPkgInfo.getVnfProductName(),
                vnfPkgInfo.getVnfSoftwareVersion(),
                vnfPkgInfo.getVnfdVersion(),
                PackageOnboardingStateType.CREATED,
                vnfPkgInfo.getOperationalState(),
                vnfPkgInfo.getUsageState(),
                vnfPkgInfo.getUserDefinedData()
        );
    }

    private NsdInfoResource buildNsdInfoResource(NsdInfo nsdInfo) {

        return new NsdInfoResource(
                nsdInfo.getNsdId(),
                null,
                null,
                null,
                NsdOnboardingStateType.CREATED,
                nsdInfo.getNsdOperationalState(),
                nsdInfo.getNsdUsageState(),
                nsdInfo.getNsdName(),
                nsdInfo.getNsdVersion(),
                nsdInfo.getNsdDesigner(),
                nsdInfo.getNsdInvariantId(),
                nsdInfo.getUserDefinedData()
        );
    }

    private MultipartFile createMultiPartFromFile(File file, String contentType) {

        byte[] content = null;
        try {
            content = Files.readAllBytes(file.toPath());
        } catch (final IOException e) {
        }
        MultipartFile multipartFile = new MockMultipartFile("file",
                file.getName(), contentType, content);
        return multipartFile;
    }

    private File convertToFile(MultipartFile multipart) throws Exception {
        File convFile = new File(multipart.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(multipart.getBytes());
        fos.close();
        return convFile;
    }


    @Override
    public void init() {
        connector.init();

        try {
            loadPublicCatalogueContent();
        } catch (FailedOperationException e) {
            log.error("Failure while loading contents from public 5G Catalogue " + catalogue.getCatalogueId());
        }
    }

    public Catalogue getCatalogue() {
        return catalogue;
    }
}
