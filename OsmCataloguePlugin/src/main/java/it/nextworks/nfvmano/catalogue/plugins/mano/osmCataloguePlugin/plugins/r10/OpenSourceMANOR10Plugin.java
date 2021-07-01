package it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.plugins.r10;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.*;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.PathType;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.PluginOperationalState;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.osm.OSM;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.common.OsmArchiveBuilder;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.common.Utilities;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.elements.OsmTranslationInformation;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.repos.OsmInfoObjectRepository;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.repos.TranslationInformationRepository;
import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.translators.SolToOsmTranslator;
import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import it.nextworks.nfvmano.libs.descriptors.sol006.Vnfd;
import it.nextworks.nfvmano.libs.osmr4PlusClient.OSMr4PlusClient;
import it.nextworks.nfvmano.libs.osmr4PlusClient.utilities.OSMHttpResponse;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.osmManagement.IdObject;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.osmManagement.OsmInfoObject;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.osmManagement.OsmObjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OpenSourceMANOR10Plugin extends MANOPlugin {

    private static final Logger log = LoggerFactory.getLogger(OpenSourceMANOR10Plugin.class);

    private Path osmDirPath;
    private File osmDir;
    private Path tmpDirPath;
    private final File logo;
    private final OSM osm;
    private OSMr4PlusClient osmClient;
    private OsmInfoObjectRepository osmInfoObjectRepository;
    private TranslationInformationRepository translationInformationRepository;
    private long syncPeriod;
    private boolean useVimNetworkName;

    public OpenSourceMANOR10Plugin(MANOType manoType,
                                   MANO mano,
                                   String kafkaBootstrapServers,
                                   String localTopic,
                                   String remoteTopic,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   boolean manoSync,
                                   OsmInfoObjectRepository osmInfoObjectRepository,
                                   TranslationInformationRepository translationInformationRepository,
                                   Path osmDirPath,
                                   Path logoPath,
                                   long syncPeriod,
                                   Path tmpDir,
                                   boolean useVimNetworkName) {

        super(manoType, mano, kafkaBootstrapServers, localTopic, remoteTopic, kafkaTemplate, manoSync);

        if(manoType != MANOType.OSMR10)
            throw new IllegalArgumentException("OSM R10 requires an OSM R10 type MANO");

        osm = (OSM) mano;
        this.osmInfoObjectRepository = osmInfoObjectRepository;
        this.translationInformationRepository = translationInformationRepository;
        this.osmDirPath = osmDirPath;
        this.logo = new File(logoPath.toUri());
        this.syncPeriod = syncPeriod;
        this.tmpDirPath = tmpDir;
        this.useVimNetworkName = useVimNetworkName;
    }

    @Override
    public void init() {
        super.init();

        try {
            osmDirPath = Paths.get(osmDirPath.toString(), "/" + osm.getManoId());
            Files.createDirectories(osmDirPath);
            osmDir = osmDirPath.toFile();
        } catch (IOException e) {
            log.error("Could not initialize tmp directory: " + e.getMessage());
        }

        initOsmConnection();
    }

    private void initOsmConnection() {
        osmClient = new OSMr4PlusClient(osm.getIpAddress(), osm.getPort(),
                osm.getUsername(), osm.getPassword(), osm.getProject());
        log.info("{} - OSM R10 instance addr {}:{}, user {}, project {} connected",
                osm.getManoId(), osm.getIpAddress(), osm.getPort(), osm.getUsername(), osm.getProject());

        if(isManoSync()) {
            log.info("{} - Starting runtime synchronization, sync period every {} minutes", osm.getManoId(), syncPeriod);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable syncTask = this::RuntimeSynchronization;
            scheduler.scheduleAtFixedRate(syncTask, syncPeriod, syncPeriod, TimeUnit.MINUTES);
        }
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) throws MethodNotImplementedException {

    }

    @Override
    public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) throws MethodNotImplementedException {

    }

    @Override
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) throws MethodNotImplementedException {

    }

    @Override
    public void acceptPnfdOnBoardingNotification(PnfdOnBoardingNotificationMessage notification) throws MethodNotImplementedException {

    }

    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotificationMessage notification) throws MethodNotImplementedException {

    }

    private String getOsmDescriptorId(String catDescriptorId, String descriptorVersion) {
        if(catDescriptorId == null)
            throw new IllegalArgumentException("The catalog descriptor ID specified cannot be null.");
        if(descriptorVersion == null)
            throw new IllegalArgumentException("The descriptor version specified cannot be null.");

        List<OsmTranslationInformation> translationInformationList = translationInformationRepository
                .findByCatDescriptorIdAndDescriptorVersionAndOsmManoId(catDescriptorId, descriptorVersion, osm.getManoId());
        if(translationInformationList.size() == 0)
            return null;

        return translationInformationList.get(0).getOsmDescriptorId();
    }

    private boolean isTheFirstTranslationInformationEntry(String catDescriptorId) {
        if (catDescriptorId == null)
            throw new IllegalArgumentException("The catalog descriptor ID specified cannot be null.");

        List<OsmTranslationInformation> translationInformationList =
                translationInformationRepository.findByCatDescriptorIdAndOsmManoId(catDescriptorId, osm.getManoId());
        if(translationInformationList.size() != 0) {
            for (OsmTranslationInformation translationInformation : translationInformationList)
                if (translationInformation.getCatDescriptorId().equals(translationInformation.getOsmDescriptorId()))
                    return false;
        }

        return true;
    }

    private static <T> List<T> parseResponse(OSMHttpResponse httpResponse, String opId, Class<T> clazz)
            throws FailedOperationException {
        List<T> objList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        if ((httpResponse.getCode() >= 300) || (httpResponse.getCode() < 200)) {
            log.error("Unexpected response code {}: {}. OpId: {}", httpResponse.getCode(), httpResponse.getMessage(), opId);
            log.debug("Response content: {}", httpResponse.getContent());
            throw new FailedOperationException(String.format("Unexpected code from MANO: %s", httpResponse.getCode()));
        }
        // else, code is 2XX
        if (clazz == null)
            return null;
        if (httpResponse.getFilePath() != null)
            objList.add(clazz.cast(httpResponse.getFilePath().toFile()));
        else
            try {
                Class<T[]> arrayClass = (Class<T[]>) Class.forName("[L" + clazz.getName() + ";");
                objList = Arrays.asList(mapper.readValue(httpResponse.getContent(), arrayClass));
            } catch (Exception e) {
                log.error("Could not obtain objects form response: {}", e.getMessage());
                e.printStackTrace();
                throw new FailedOperationException("Could not obtain objects from MANO response");
            }

        return objList;
    }

    private String onBoardVnfPackage(File file, String opId) throws FailedOperationException {
        log.info("{} - Onboarding vnf package, opId: {}", osm.getManoId(), opId);

        OSMHttpResponse httpResponse = osmClient.createVnfPackage();
        IdObject content = parseResponse(httpResponse, opId, IdObject.class).get(0);
        String osmVnfdInfoId = content.getId();

        httpResponse = osmClient.uploadVnfPackageContent(osmVnfdInfoId, file);
        try {
            parseResponse(httpResponse, opId, null);
        } catch(FailedOperationException e){
            osmClient.deleteVnfPackage(osmVnfdInfoId);
            throw new FailedOperationException(e);
        }

        httpResponse = osmClient.getVnfPackageInfo(osmVnfdInfoId);
        OsmInfoObject osmInfoObject = parseResponse(httpResponse, opId, OsmInfoObject.class).get(0);
        osmInfoObject.setEpoch(Instant.now().getEpochSecond());
        osmInfoObject.setOsmId(osm.getManoId());
        osmInfoObject.setType(OsmObjectType.VNF);
        osmInfoObjectRepository.saveAndFlush(osmInfoObject);

        log.info("{} - Package onboarding successful. OpId: {}", osm.getManoId(), opId);
        return osmVnfdInfoId;
    }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) throws MethodNotImplementedException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        try {
            log.debug("RECEIVED MESSAGE: " + mapper.writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            log.error("Unable to parse received vnfPkgOnboardingNotificationMessage: " + e.getMessage());
        }

        String vnfPkgInfoId = notification.getVnfPkgInfoId();
        boolean forceOnboard = false;
        if(vnfPkgInfoId.endsWith("_update")) {
            forceOnboard = true;
            vnfPkgInfoId = vnfPkgInfoId.replace("_update", "");
        }

        String manoId  = osm.getManoId();
        String vnfdId  = notification.getVnfdId();
        String version = notification.getVnfdVersion();
        String project = notification.getProject();

        if(notification.getScope() == ScopeType.LOCAL) {
            log.info("{} - Received Vnfd onboarding notification for Vnfd with ID {} and version {} for project {}",
                    manoId, vnfdId, version, project);

            if(Utilities.isTargetMano(notification.getSiteOrManoIds(), osm) &&
                    this.getPluginOperationalState() == PluginOperationalState.ENABLED) {

                try {
                    String packagePath = notification.getPackagePath().getKey();
                    File descriptor;

                    if (notification.getPackagePath().getValue().equals(PathType.LOCAL.toString())) {

                        Set<String> metadataFileNames = Utilities.listFiles(packagePath + "/TOSCA-Metadata/");
                        String metadataFileName;

                        Optional<String> opt = metadataFileNames.stream()
                                .filter(name -> name.endsWith(".meta")).findFirst();
                        if(!opt.isPresent())
                            throw new MalformattedElementException("Metadata file not found in " + packagePath +
                                    "/TOSCA-Metadata/" + " for Vnfd with ID " + vnfdId + " and version " + version +
                                    " for project " + project + ".");
                        metadataFileName = opt.get();

                        File metadata = new File(packagePath + "/TOSCA-Metadata/" + metadataFileName);
                        descriptor = new File(packagePath + "/" + Utilities.getMainServiceTemplateFromMetadata(metadata));
                    } else
                        throw new MethodNotImplementedException("Path Type not currently supported");

                    if(descriptor.getName().endsWith(".yaml"))
                        mapper = new ObjectMapper(new YAMLFactory());

                    Vnfd tmp = mapper.readValue(descriptor, Vnfd.class);
                    if(!tmp.getId().equals(vnfdId) || !tmp.getVersion().equals(version))
                        throw new MalformattedElementException("The VNFD ID and version specified in the notification " +
                                "(" + vnfdId + ", " + version + ") do not match those of the stored descriptor " +
                                "(" + tmp.getId() + ", " + tmp.getVersion() + ").");
                    SolToOsmTranslator.OsmVnfdSol006Wrapper vnfd = SolToOsmTranslator.generateVnfDescriptor(tmp);

                    String osmDescriptorId = getOsmDescriptorId(vnfdId, version);
                    if(osmDescriptorId == null)
                        osmDescriptorId = vnfdId;

                    String osmInfoObjectId;
                    Optional<OsmInfoObject> osmInfoObjectOptional = osmInfoObjectRepository
                            .findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, version, manoId);
                    if(!osmInfoObjectOptional.isPresent() || forceOnboard) {

                        if(isTheFirstTranslationInformationEntry(vnfdId))
                            osmDescriptorId = vnfdId;
                        else {
                            osmDescriptorId = UUID.randomUUID().toString();
                            vnfd.getVnfd().setId(osmDescriptorId);
                        }

                        mapper = new ObjectMapper(new YAMLFactory());
                        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                        log.debug("Translated VNFD: " + mapper.writeValueAsString(vnfd));

                        Set<String> fileNames = Utilities.listFiles(packagePath);
                        String manifestPath = fileNames.stream().filter(name -> name.endsWith(".mf")).findFirst().get();
                        File mf = new File(packagePath + "/" + manifestPath);

                        String cloudInitPath = Utilities.getCloudInitFromManifest(mf);
                        File cloudInit = null;
                        if (cloudInitPath != null)
                            cloudInit = new File(packagePath + "/" + cloudInitPath);
                        else
                            log.debug("{} - No cloud-init file found for VNF with ID {} and version {}", manoId, vnfdId, version);

                        String monitoringPath = Utilities.getMonitoringFromManifest(mf);
                        File monitoring = null;
                        if (monitoringPath != null)
                            monitoring = new File(packagePath + "/" + monitoringPath);
                        else
                            log.debug("{} - No monitoring file found for VNF with ID {} and version {}", manoId, vnfdId, version);

                        File archive = new OsmArchiveBuilder(osmDir, logo)
                                .makeNewArchive(vnfd, "Generated by NXW Catalogue", cloudInit, monitoring);
                        osmInfoObjectId = onBoardVnfPackage(archive, notification.getOperationId().toString());
                        log.info("{} - Successfully uploaded Vnfd with ID {} and version {} for project {}", manoId, vnfdId, version, project);
                    }
                    else
                        osmInfoObjectId = osmInfoObjectOptional.get().getId();

                    translationInformationRepository.saveAndFlush(new OsmTranslationInformation(vnfPkgInfoId,
                            osmInfoObjectId, vnfdId, osmDescriptorId, version, manoId));
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(vnfPkgInfoId, vnfdId, version, project,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            manoId, null,null));
                } catch (Exception e) {
                    log.error("{} - Could not onboard Vnfd: {}", manoId, e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(vnfPkgInfoId, vnfdId, version, project,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            manoId, null,null));
                }
            } else {
                if(this.getPluginOperationalState() == PluginOperationalState.DISABLED ||
                        this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - VNF Pkg onboarding skipped", manoId);
                    sendNotification(new VnfPkgOnBoardingNotificationMessage(vnfPkgInfoId, vnfdId, version, project,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            manoId, null,null));
                }
            }
        } else if(notification.getScope() == ScopeType.SYNC) {
            log.info("{} - Received Sync Pkg onboarding notification for Vnfd with ID {} and version {} for project {} : {}",
                    manoId, vnfdId, version, project, notification.getOpStatus().toString());
            if(notification.getPluginId().equals(manoId)) {
                String osmDescriptorId = getOsmDescriptorId(vnfdId, version);
                if (osmDescriptorId == null)
                    osmDescriptorId = vnfdId;

                Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository
                        .findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, version, manoId);
                if (osmInfoObject.isPresent()) {
                    if (notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE))
                        translationInformationRepository.saveAndFlush(new OsmTranslationInformation(vnfPkgInfoId,
                                osmInfoObject.get().getId(), vnfdId, osmInfoObject.get().getDescriptorId(), version, manoId));
                }
            }
        }
    }

    private String getOsmInfoId(String catInfoId) {
        if(catInfoId == null)
            throw new IllegalArgumentException("The catalog descriptor ID specified cannot be null.");

        Optional<OsmTranslationInformation> optionalTranslationInformation =
                translationInformationRepository.findByCatInfoIdAndOsmManoId(catInfoId, osm.getManoId());
        if(!optionalTranslationInformation.isPresent())
            return null;

        return optionalTranslationInformation.get().getOsmInfoId();
    }

    private boolean deleteTranslationInformationEntry(String catInfoId) {
        if(catInfoId == null)
            throw new IllegalArgumentException("The catalog descriptor ID specified cannot be null.");

        Optional<OsmTranslationInformation> optionalTranslationInformation =
                translationInformationRepository.findByCatInfoIdAndOsmManoId(catInfoId, osm.getManoId());
        if(!optionalTranslationInformation.isPresent())
            return false;

        translationInformationRepository.delete(optionalTranslationInformation.get());
        return true;
    }

    private boolean translationInformationContainsOsmInfoId(String osmInfoId) {
        if(osmInfoId == null)
            throw new IllegalArgumentException("The OSM info ID specified cannot be null.");

        List<OsmTranslationInformation> translationInformationList = translationInformationRepository.findByOsmInfoIdAndOsmManoId(osmInfoId, osm.getManoId());
        return translationInformationList.size() != 0;
    }

    private void deleteVnfd(String vnfdInfoId, String opId) throws FailedOperationException {
        log.info("{} - Deleting vnfd {}, opId: {}", osm.getManoId(), vnfdInfoId, opId);

        OSMHttpResponse httpResponse = osmClient.deleteVnfPackage(vnfdInfoId);
        parseResponse(httpResponse, opId, null);

        OsmInfoObject osmInfoObject =  osmInfoObjectRepository.findById(vnfdInfoId).get();
        String descriptorId = osmInfoObject.getDescriptorId();
        osmInfoObjectRepository.delete(osmInfoObject);

        Utilities.deleteDir(new File(osmDir + "/" + descriptorId));
        Utilities.deleteDir(new File(osmDir + "/" + descriptorId + ".tar.gz"));

        log.info("{} - Vnfd deleting successful. OpId: {}", osm.getManoId(), opId);
    }

    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) throws MethodNotImplementedException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        try {
            log.debug("RECEIVED MESSAGE: " + mapper.writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            log.error("Unable to parse received vnfPkgChangeNotificationMessage: " + e.getMessage());
        }

        String manoId       = osm.getManoId();
        String vnfPkgInfoId = notification.getVnfPkgInfoId();
        String vnfdId       = notification.getVnfdId();
        String version      = notification.getVnfdVersion();
        String project      = notification.getProject();

        if(notification.getScope() == ScopeType.LOCAL) {
            log.info("{} - Received VNF Pkg change notification for Vnfd with ID {} and version {} for project {}",
                    manoId, vnfdId, version, project);

            if(Utilities.isTargetMano(notification.getSiteOrManoIds(), osm) &&
                    this.getPluginOperationalState() == PluginOperationalState.ENABLED) {

                try {
                    String osmInfoPkgId = getOsmInfoId(vnfPkgInfoId);
                    if(osmInfoPkgId == null)
                        throw new FailedOperationException("Could not find the corresponding Info ID in OSM");
                    if(!deleteTranslationInformationEntry(vnfPkgInfoId))
                        throw new FailedOperationException("Could not delete the specified entry");
                    if(!translationInformationContainsOsmInfoId(osmInfoPkgId))
                        deleteVnfd(osmInfoPkgId, notification.getOperationId().toString());
                    log.info("{} - Successfully deleted Vnfd with ID {} and version {} for project {}", manoId, vnfdId, version, project);

                    String packagePath = notification.getPackagePath().getKey();
                    File descriptor;

                    if (notification.getPackagePath().getValue().equals(PathType.LOCAL.toString())) {

                        Set<String> files = Utilities.listFiles(packagePath);
                        if(files.size() != 1) {
                            Set<String> metadataFileNames = Utilities.listFiles(packagePath + "/TOSCA-Metadata/");
                            String metadataFileName;

                            Optional<String> opt = metadataFileNames.stream()
                                    .filter(name -> name.endsWith(".meta")).findFirst();
                            if(!opt.isPresent())
                                throw new MalformattedElementException("Metadata file not found in " + packagePath +
                                        "/TOSCA-Metadata/" + " for Vnfd with ID " + vnfdId + " and version " + version +
                                        " for project " + project + ".");
                            metadataFileName = opt.get();

                            File metadata = new File(packagePath + "/TOSCA-Metadata/" + metadataFileName);
                            descriptor = new File(packagePath + "/" + Utilities.getMainServiceTemplateFromMetadata(metadata));
                        } else {
                            String filename = files.iterator().next();
                            if(filename.endsWith(".yaml") || filename.endsWith("yml") || filename.endsWith("json"))
                                descriptor = new File(packagePath + "/" + filename);
                            else
                                throw new MalformattedElementException("Descriptor files not found");
                        }
                    } else
                        throw new MethodNotImplementedException("Path Type not currently supported");

                    log.info("{} - Onboarding Vnfd with ID {} and version {} for project {}", manoId, vnfdId, version, project);

                    if(descriptor.getName().endsWith(".yaml"))
                        mapper = new ObjectMapper(new YAMLFactory());

                    Vnfd tmp = mapper.readValue(descriptor, Vnfd.class);
                    if(!tmp.getId().equals(vnfdId) || !tmp.getVersion().equals(version))
                        throw new MalformattedElementException("The VNFD ID and version specified in the notification " +
                                "(" + vnfdId + ", " + version + ") do not match those of the stored descriptor " +
                                "(" + tmp.getId() + ", " + tmp.getVersion() + ").");
                    SolToOsmTranslator.OsmVnfdSol006Wrapper vnfd = SolToOsmTranslator.generateVnfDescriptor(tmp);

                    String osmDescriptorId = getOsmDescriptorId(vnfdId, version);
                    if(osmDescriptorId == null)
                        osmDescriptorId = vnfdId;

                    String osmInfoObjectId;
                    Optional<OsmInfoObject> osmInfoObjectOptional = osmInfoObjectRepository
                            .findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, version, manoId);
                    if(!osmInfoObjectOptional.isPresent()) {

                        if(isTheFirstTranslationInformationEntry(vnfdId))
                            osmDescriptorId = vnfdId;
                        else {
                            osmDescriptorId = UUID.randomUUID().toString();
                            vnfd.getVnfd().setId(osmDescriptorId);
                        }

                        mapper = new ObjectMapper(new YAMLFactory());
                        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                        log.debug("Translated VNFD: " + mapper.writeValueAsString(vnfd));

                        Set<String> fileNames = Utilities.listFiles(packagePath);
                        String manifestPath = fileNames.stream().filter(name -> name.endsWith(".mf")).findFirst().get();
                        File mf = new File(packagePath + "/" + manifestPath);

                        String cloudInitPath = Utilities.getCloudInitFromManifest(mf);
                        File cloudInit = null;
                        if (cloudInitPath != null)
                            cloudInit = new File(packagePath + "/" + cloudInitPath);
                        else
                            log.debug("{} - No cloud-init file found for VNF with ID {} and version {}", manoId, vnfdId, version);

                        String monitoringPath = Utilities.getMonitoringFromManifest(mf);
                        File monitoring = null;
                        if (monitoringPath != null)
                            monitoring = new File(packagePath + "/" + monitoringPath);
                        else
                            log.debug("{} - No monitoring file found for VNF with ID {} and version {}", manoId, vnfdId, version);

                        File archive = new OsmArchiveBuilder(osmDir, logo)
                                .makeNewArchive(vnfd, "Generated by NXW Catalogue", cloudInit, monitoring);
                        osmInfoObjectId = onBoardVnfPackage(archive, notification.getOperationId().toString());
                        log.info("{} - Successfully uploaded Vnfd with ID {} and version {} for project {}", manoId, vnfdId, version, project);
                    }
                    else
                        osmInfoObjectId = osmInfoObjectOptional.get().getId();

                    translationInformationRepository.saveAndFlush(new OsmTranslationInformation(vnfPkgInfoId,
                            osmInfoObjectId, vnfdId, osmDescriptorId, version, manoId));
                    sendNotification(new VnfPkgChangeNotificationMessage(vnfPkgInfoId, vnfdId, version, project,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            manoId, null,null));
                } catch (Exception e) {
                    log.error("{} - Could not change Vnfd: {}", manoId, e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new VnfPkgChangeNotificationMessage(vnfPkgInfoId, vnfdId, version, project,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            manoId, null,null));
                }
            } else {
                if(this.getPluginOperationalState() == PluginOperationalState.DISABLED ||
                        this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - VNF Pkg change skipped", manoId);
                    sendNotification(new VnfPkgChangeNotificationMessage(vnfPkgInfoId, vnfdId, version, project,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            manoId, null,null));
                }
            }
        } else if(notification.getScope() == ScopeType.SYNC) {
            log.info("{} - Received Sync Pkg change notification for Vnfd with ID {} and version {} for project {} : {}",
                    manoId, vnfdId, version, project, notification.getOpStatus().toString());
            if(notification.getPluginId().equals(manoId)) {
                String osmDescriptorId = getOsmDescriptorId(vnfdId, version);
                if(osmDescriptorId == null)
                    osmDescriptorId = vnfdId;

                Optional<OsmInfoObject> osmInfoObject = osmInfoObjectRepository
                        .findByDescriptorIdAndVersionAndOsmId(osmDescriptorId, version, manoId);
                if(osmInfoObject.isPresent()) {
                    if (notification.getOpStatus().equals(OperationStatus.SUCCESSFULLY_DONE)) {
                        Optional<OsmTranslationInformation> translationInformationOptional =
                                translationInformationRepository.findByCatInfoIdAndOsmManoId(vnfPkgInfoId, manoId);
                        translationInformationOptional.ifPresent(osmTranslationInformation ->
                                translationInformationRepository.delete(osmTranslationInformation));

                        translationInformationRepository.saveAndFlush(new OsmTranslationInformation(vnfPkgInfoId,
                                osmInfoObject.get().getId(), vnfdId, osmInfoObject.get().getDescriptorId(), version, manoId));
                    }
                }
            }
        }
    }

    private boolean translationInformationContainsCatInfoId(String catInfoId) {
        if(catInfoId == null)
            throw new IllegalArgumentException("The catalog descriptor ID specified cannot be null.");

        Optional<OsmTranslationInformation> optionalTranslationInformation = translationInformationRepository.findByCatInfoIdAndOsmManoId(catInfoId, osm.getManoId());
        return optionalTranslationInformation.isPresent();
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) throws MethodNotImplementedException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        try {
            log.debug("RECEIVED MESSAGE: " + mapper.writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            log.error("Unable to parse received vnfPkgChangeNotificationMessage: " + e.getMessage());
        }

        String manoId       = osm.getManoId();
        String vnfPkgInfoId = notification.getVnfPkgInfoId();
        String vnfdId       = notification.getVnfdId();
        String version      = notification.getVnfdVersion();
        String project      = notification.getProject();

        if(notification.getScope() == ScopeType.LOCAL) {
            log.info("{} - Received VNFD deletion notification for Vnfd with ID {} and version {} for project {}",
                    manoId, vnfdId, version, project);

            if(Utilities.isTargetMano(notification.getSiteOrManoIds(), osm) &&
                    this.getPluginOperationalState() == PluginOperationalState.ENABLED &&
                    translationInformationContainsCatInfoId(vnfPkgInfoId)) {

                try {
                    String osmInfoPkgId = getOsmInfoId(vnfPkgInfoId);
                    if(osmInfoPkgId == null)
                        throw new FailedOperationException("Could not find the corresponding Info ID in OSM");
                    if(!deleteTranslationInformationEntry(vnfPkgInfoId))
                        throw new FailedOperationException("Could not delete the specified entry");
                    if(!translationInformationContainsOsmInfoId(osmInfoPkgId))
                        deleteVnfd(osmInfoPkgId, notification.getOperationId().toString());
                    log.info("{} - Successfully deleted Vnfd with ID {} and version {} for project {}", manoId, vnfdId, version, project);

                    sendNotification(new VnfPkgDeletionNotificationMessage(vnfPkgInfoId, vnfdId, version, project,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.SUCCESSFULLY_DONE,
                            manoId, null));
                } catch (Exception e) {
                    log.error("{} - Could not delete Vnfd: {}", manoId, e.getMessage());
                    log.debug("Error details: ", e);
                    sendNotification(new VnfPkgDeletionNotificationMessage(vnfPkgInfoId, vnfdId, version, project,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.FAILED,
                            manoId, null));
                }
            } else {
                if(this.getPluginOperationalState() == PluginOperationalState.DISABLED ||
                        this.getPluginOperationalState() == PluginOperationalState.DELETING) {
                    log.debug("{} - VNF Pkg deletion skipped", manoId);
                    sendNotification(new VnfPkgDeletionNotificationMessage(vnfPkgInfoId, vnfdId, version, project,
                            notification.getOperationId(), ScopeType.REMOTE, OperationStatus.RECEIVED,
                            manoId, null));
                }
            }
        } else if(notification.getScope() == ScopeType.SYNC)
            log.info("{} - Received Sync Pkg deletion notification for Vnfd with ID {} and version {} for project {} : {}",
                    manoId, vnfdId, version, project, notification.getOpStatus().toString());
    }

    @Override
    public Map<String, List<String>> getAllVnfd(String project) {
        return null;
    }

    @Override
    public Map<String, List<String>> getAllNsd(String project) {
        return null;
    }

    @Override
    public KeyValuePair getTranslatedPkgPath(String descriptorId, String descriptorVersion, String project) {
        return null;
    }

    @Override
    public void notifyOnboarding(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus) {

    }

    @Override
    public void notifyDelete(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus) {

    }

    @Override
    public String getManoPkgInfoId(String catalogueInfoId) {
        return null;
    }

    @Override
    public void RuntimeSynchronization() {

    }
}
