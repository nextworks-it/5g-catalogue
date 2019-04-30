package it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementInterface;
import it.nextworks.nfvmano.catalogue.engine.VnfPackageManagementInterface;
import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.catalogue.engine.resources.PnfdInfoResource;
import it.nextworks.nfvmano.catalogue.engine.resources.VnfPkgInfoResource;
import it.nextworks.nfvmano.catalogue.messages.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.CreateVnfPkgInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPkgInfo;
import it.nextworks.nfvmano.catalogue.plugins.KafkaConnector;
import it.nextworks.nfvmano.catalogue.plugins.Plugin;
import it.nextworks.nfvmano.catalogue.plugins.PluginType;
import it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.api.nsd.DefaultApi;
import it.nextworks.nfvmano.catalogue.repos.NsdInfoRepository;
import it.nextworks.nfvmano.catalogue.repos.PnfdInfoRepository;
import it.nextworks.nfvmano.catalogue.repos.VnfPkgInfoRepository;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class CataloguePlugin extends Plugin
        implements NsdNotificationsConsumerInterface, VnfPkgNotificationsConsumerInterface {

    private static final Logger log = LoggerFactory.getLogger(CataloguePlugin.class);

    private Catalogue catalogue;
    protected KafkaConnector connector;
    protected NsdManagementInterface nsdService;
    protected VnfPackageManagementInterface vnfdService;
    protected DefaultApi nsdApi;
    protected it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.api.vnf.DefaultApi vnfdApi;
    protected NsdInfoRepository nsdInfoRepository;
    protected PnfdInfoRepository pnfdInfoRepository;
    protected VnfPkgInfoRepository vnfPkgInfoRepository;
    protected KafkaTemplate<String, String> kafkaTemplate;
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

                    sendNotification(new NsdOnBoardingNotificationMessage(notification.getNsdInfoId(), notification.getNsdId(),
                            notification.getOperationId(), ScopeType.C2C, OperationStatus.SUCCESSFULLY_DONE,
                            catalogue.getCatalogueId()));

                    /*HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                    MultiValueMap<String, Object> body
                            = new LinkedMultiValueMap<>();
                    body.add("nsd", nsd);

                    HttpEntity<MultiValueMap<String, Object>> requestEntity
                            = new HttpEntity<>(body, headers);

                    String serverUrl = "";

                    RestTemplate restTemplate = new RestTemplate();
                    ResponseEntity<String> response = restTemplate
                            .postForEntity(serverUrl, requestEntity, String.class);*/
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
                Optional<PnfdInfoResource> nsdInfoResourceOptional = pnfdInfoRepository.findById(UUID.fromString(notification.getPnfdInfoId()));

                if (nsdInfoResourceOptional.isPresent()) {
                    PnfdInfoResource nsdInfoResource = nsdInfoResourceOptional.get();

                    CreatePnfdInfoRequest request = new CreatePnfdInfoRequest();
                    if (nsdInfoResource.getUserDefinedData() != null) {
                        KeyValuePairs keyValuePairs = new KeyValuePairs();
                        keyValuePairs.putAll(nsdInfoResource.getUserDefinedData());
                        request.setUserDefinedData(keyValuePairs);
                    }

                    File pnfd = ((Resource) nsdService.getPnfd(notification.getPnfdInfoId(), true)).getFile();

                    PnfdInfo pnfdInfo;
                    try { pnfdInfo = nsdApi.createPNFDInfo(request);
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
                    try { vnfPkgInfo = vnfdApi.createVNFPkgInfo(request);
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

    private MultipartFile createMultiPartFromFile(File file, String contentType) throws IOException {

        byte[] content = null;
        try {
            content = Files.readAllBytes(file.toPath());
        } catch (final IOException e) {
        }
        MultipartFile multipartFile = new MockMultipartFile("file",
                file.getName(), contentType, content);
        return multipartFile;
    }

    @Override
    public void init() {
        connector.init();
    }

    public Catalogue getCatalogue() {
        return catalogue;
    }
}
