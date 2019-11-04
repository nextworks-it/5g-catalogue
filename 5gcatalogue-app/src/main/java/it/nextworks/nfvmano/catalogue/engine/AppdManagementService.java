package it.nextworks.nfvmano.catalogue.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextworks.nfvmano.catalogue.auth.AuthUtilities;
import it.nextworks.nfvmano.catalogue.auth.projectmanagement.ProjectResource;
import it.nextworks.nfvmano.catalogue.common.FileUtilities;
import it.nextworks.nfvmano.catalogue.repos.ProjectRepository;
import it.nextworks.nfvmano.catalogue.repos.UserRepository;
import it.nextworks.nfvmano.catalogue.repos.mec0102.*;
import it.nextworks.nfvmano.libs.common.exceptions.NotAuthorizedOperationException;
import it.nextworks.nfvmano.libs.ifa.catalogues.interfaces.MecAppPackageManagementConsumerInterface;
import it.nextworks.nfvmano.libs.ifa.catalogues.interfaces.elements.AppPackageInfo;

import it.nextworks.nfvmano.libs.ifa.catalogues.interfaces.messages.*;
import it.nextworks.nfvmano.libs.ifa.common.elements.Filter;
import it.nextworks.nfvmano.libs.ifa.common.enums.OperationalState;
import it.nextworks.nfvmano.libs.ifa.common.enums.UsageState;
import it.nextworks.nfvmano.libs.ifa.common.exceptions.*;
import it.nextworks.nfvmano.libs.ifa.common.messages.GeneralizedQueryRequest;
import it.nextworks.nfvmano.libs.ifa.common.messages.SubscribeRequest;
import it.nextworks.nfvmano.libs.ifa.descriptors.appd.*;
import it.nextworks.nfvmano.libs.ifa.descriptors.common.elements.VirtualComputeDesc;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects;

/**
 * This service manages the external requests for MEC App Package management
 * and interacts with the internal repositories of the APPD catalog.
 * Its methods are invoked by the AppdManagementRestController class.
 *
 * @author nextworks
 *
 */
@Service
public class AppdManagementService implements AppdManagementInterface {

    private static final Logger log = LoggerFactory.getLogger(AppdManagementService.class);

    @Autowired
    private AppPackageInfoRepository appPackageInfoRepository;

    @Autowired
    private AppdRepository appdRepository;

    @Autowired
    private VirtualComputeDescriptorRepository vcdRepository;

    @Autowired
    private AppExternalCpdRepository appExternalCpdRepository;

    @Autowired
    private MecServiceDependencyRepository mecServiceDependencyRepository;

    @Autowired
    private TransportDependencyRepository transportDependencyRepository;

    @Autowired
    private TransportDescriptorRepository transportDescriptorRepository;

    @Autowired
    private SecurityInfoRepository securityInfoRepository;

    @Autowired
    private MecServiceDescriptorRepository mecServiceDescriptorRepository;

    @Autowired
    private MecServiceTransportRepository mecServiceTransportRepository;

    @Autowired
    private TrafficRuleDescriptorRepository trafficRuleDescriptorRepository;

    @Autowired
    private TrafficFilterRepository trafficFilterRepository;

    @Autowired
    private MeAppInterfaceDescriptorRepository meAppInterfaceDescriptorRepository;

    /*
    @Autowired
    AppPackageManagementSubscriptionRepository appPackageManagementSubscriptionRepository;
    */

    @Autowired
    private FileUtilities fileUtilities;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    public AppdManagementService() { }

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    @Override
    public File fetchOnboardedApplicationPackage(String onboardedAppPkgId)
            throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        throw new MethodNotImplementedException();
    }

    @Override
    public QueryOnBoadedAppPkgInfoResponse queryApplicationPackage(GeneralizedQueryRequest request, String project)
            throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException, NotAuthorizedOperationException {
        log.debug("Received MEC application package query.");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new MalformattedElementException("Project with id " + project + " does not exist");
            }
        }
        try {
            if (project != null && keycloakEnabled && !it.nextworks.nfvmano.catalogue.engine.Utilities.checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
            }
        } catch (it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException e) {
            throw new NotAuthorizedOperationException(e.getMessage());
        }
        request.isValid();

        //At the moment the only filters accepted are:
        //1. App package info ID
        //APP_PACKAGE_INFO_ID
        //2. APPD ID && version
        //APPD_ID
        //APPD_VERSION
        //No attribute selector is supported at the moment

        Filter filter = request.getFilter();
        List<String> attributeSelector = request.getAttributeSelector();

        List<AppPackageInfo> pkgList = new ArrayList<>();

        if ((attributeSelector == null) || (attributeSelector.isEmpty())) {
            Map<String, String> fp = filter.getParameters();
            if (fp.size() == 1 && fp.containsKey("APP_PACKAGE_INFO_ID")) {
                String pkgId = fp.get("APP_PACKAGE_INFO_ID");
                AppPackageInfo pkg = findAppPackageInfo(pkgId);
                AppPackageInfo p = fillAppPackageInfoDetails(pkg);
                pkgList.add(p);
                log.debug("Added MEC package info found in DB");
            } else if (fp.size() == 2 && fp.containsKey("APPD_ID") && fp.containsKey("APPD_VERSION")) {
                String appdId = fp.get("APPD_ID");
                String version = fp.get("APPD_VERSION");
                AppPackageInfo pkg = findAppPackageInfo(appdId, version);
                AppPackageInfo p = fillAppPackageInfoDetails(pkg);
                pkgList.add(p);
                log.debug("Added MEC package info found in DB");
            } else if (fp.isEmpty()) {
                log.debug("Query with empty filter. Returning all the elements");
                List<AppPackageInfo> pkgs = appPackageInfoRepository.findAll();
                for (AppPackageInfo pkg : pkgs) {
                    AppPackageInfo p = fillAppPackageInfoDetails(pkg);
                    pkgList.add(p);
                }
                log.debug("Added all MEC package infos found in DB");
            }
        } else {
            log.error("Received query MEC App package with attribute selector. Not supported at the moment.");
            throw new MethodNotImplementedException("Received query MEC App package with attribute selector. Not supported at the moment.");
        }
        //TODO filter per project
        return new QueryOnBoadedAppPkgInfoResponse(pkgList);
    }

    @Override
    public String subscribeMecAppPackageInfo(SubscribeRequest request, MecAppPackageManagementConsumerInterface consumer)
            throws MethodNotImplementedException, MalformattedElementException, FailedOperationException, NotAuthorizedOperationException{
        throw new MethodNotImplementedException("Method not implemented");
    }

    @Override
    public void unsubscribeMecAppPackageInfo(String subscriptionId)
            throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException, NotAuthorizedOperationException {
        throw new MethodNotImplementedException("Method not implemented");
    }
    /*
    @Override
    public String subscribeMecAppPackageInfo(SubscribeRequest request, MecAppPackageManagementConsumerInterface consumer)
            throws MethodNotImplementedException, MalformattedElementException, FailedOperationException {
        log.debug("Received subscribe request for MEC App package notifications.");
        request.isValid();
        AppdManagementConsumer abstractConsumer = (AppdManagementConsumer) consumer;
        switch (abstractConsumer.getType()) {
            case REST: {
                log.debug("Subscription for a REST consumer");
                AppdManagementRestConsumer restConsumer = (AppdManagementRestConsumer) abstractConsumer;
                AppPackageManagementSubscription subscription = new AppPackageManagementSubscription(restConsumer);
                appPackageManagementSubscriptionRepository.saveAndFlush(subscription);
                String subscriptionId = subscription.getId().toString();
                log.debug("Stored MEC AppD management subscription with ID " + subscriptionId);
                return subscriptionId;
            }

            default: {
                log.error("MEC App package management consumer type not supported.");
                throw new FailedOperationException("MEC App package management consumer type not supported.");
            }
        }
    }

    @Override
    public void unsubscribeMecAppPackageInfo(String subscriptionId)
            throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        log.debug("Received request to remove AppD Management subscription with ID " + subscriptionId);
        if (subscriptionId == null) throw new MalformattedElementException("Received request with null ID.");
        Optional<AppPackageManagementSubscription> subOpt = appPackageManagementSubscriptionRepository.findById(Long.parseLong(subscriptionId));
        if (subOpt.isPresent()) {
            AppPackageManagementSubscription sub = subOpt.get();
            appPackageManagementSubscriptionRepository.delete(sub);
            log.debug("Subscription removed.");
        } else {
            log.warn("Not existing AppD Management subscription. ID: " + subscriptionId + ". Impossible to remove it.");
            throw new NotExistingEntityException("Not existing AppD Management subscription. ID: " + subscriptionId + ". Impossible to remove it.");
        }
    }
    */

    @Override
    public OnboardAppPackageResponse onboardAppPackage(OnboardAppPackageRequest request, String project)
            throws MethodNotImplementedException, AlreadyExistingEntityException, FailedOperationException,
            MalformattedElementException, NotAuthorizedOperationException {
        log.debug("Received on-board MEC application package request.");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new MalformattedElementException("Project with id " + project + " does not exist");
            }
            try {
                if (keycloakEnabled && !checkUserProjects(userRepository, AuthUtilities.getUserNameFromJWT(), project)) {
                    throw new NotAuthorizedOperationException("Current user cannot access to the specified project");
                }
            } catch (it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException e) {
                throw new NotAuthorizedOperationException(e.getMessage());
            }
        }

        request.isValid();

        //Retrieve the appD
        String mecAppPackageUrl = request.getAppPackagePath();
        Appd appd;

        try {
            appd = readAppdFromMecAppPackage(mecAppPackageUrl);
            log.debug("AppD retrieved.");
        } catch (Exception e) {
            log.error("Error while getting AppD: " + e.getMessage());
            throw new FailedOperationException("Onboarding failed due to an error in retrieving the AppD.");
        }

        //Check if the appD is well formatted
        try {
            appd.isValid();
        } catch (MalformattedElementException e) {
            log.error("The AppD is malformatted! " + e.getMessage());
            throw new FailedOperationException("Onboarding failed because the retrieved AppD is malformatted.");
        }

        String version = appd.getAppDVersion();
        if (!(version.equals(request.getVersion()))) {
            log.error("AppD version and version in the request are not matching. Onboarding failed.");
            throw new FailedOperationException("Onboarding failed because of mismatching versions.");
        }

        String appdId = appd.getAppDId();
        //TODO create AppPackageInfoResource che ingloba appPackageInfo + progetto  e creare repo e aggiungere ad Id e versione anche il progetto
        //TODO add security check in all other following methods
        //TODO controllare che dopo le modifiche sui progetti (non più obbligatorio), tutto sia a posto
        //TODO controllare anche che funzioni la variabile /tmp/catalogue e che quindi tutto venga salvato li dentro (quando si fa retrieve da osm)
        //check if a MEC application package with the same AppD ID and the same version already exists
        if (appPackageInfoRepository.findByAppdIdAndVersion(appdId, version).isPresent()) {
            log.error("MEC Application Package with ID " + appdId + " and version " + version + " already existing. Impossible to on-board a new one.");
            throw new AlreadyExistingEntityException("MEC Application Package with ID " + appdId + " and version " + version + " already existing. Impossible to on-board a new one.");
        }

        //check if a MEC AppD with same ID, provider and version already exists
        if (appdRepository.findByAppDIdAndAppDVersionAndAppProvider(appdId, version, appd.getAppProvider()).isPresent()) {
            log.error("MEC AppD with AppD ID " + appdId + ", provider " + appd.getAppProvider() + " and version " + version + " already existing in DB. Impossible to create a new one.");
            throw new AlreadyExistingEntityException("MEC AppD with AppD ID " + appdId + ", provider " + appd.getAppProvider() + " and version " + version + " already existing in DB. Impossible to create a new one.");
        }

        Long appPackageInfoId = storeAppPackageInfo(request, appd);
        String appPackageInfoIdString = String.valueOf(appPackageInfoId);

        try {
            Long storedAppdId = storeAppd(appd);
            String storedAppdIdString = String.valueOf(storedAppdId);

            log.debug("Successful MEC application package on-boarding. App package ID: " + String.valueOf(appPackageInfoId) + " - Internal AppD ID: " + storedAppdIdString);
            //notify(new AppPackageOnBoardingNotification(appPackageInfoIdString, storedAppdIdString));

            return new OnboardAppPackageResponse(appPackageInfoIdString, storedAppdIdString);

        } catch (AlreadyExistingEntityException e) {
            log.error("Failed MEC AppD saving - overlapping elements");
            appPackageInfoRepository.deleteById(appPackageInfoId);
            log.debug("App package info deleted");
            throw new AlreadyExistingEntityException(e.getMessage());
        } catch (Exception e) {
            log.error("Failed MEC AppD saving - generic error");
            appPackageInfoRepository.deleteById(appPackageInfoId);
            log.debug("App package info deleted");
            throw new FailedOperationException(e.getMessage());
        }

    }

    @Override
    public synchronized void enableAppPackage(String onboardedAppPkgId, String project) throws MethodNotImplementedException,
            NotExistingEntityException, FailedOperationException, MalformattedElementException, NotAuthorizedOperationException {
        log.debug("Received request to enable MEC app package with ID " + onboardedAppPkgId);
        if (onboardedAppPkgId == null) throw new MalformattedElementException("Received request with null ID.");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new MalformattedElementException("Project with id " + project + " does not exist");
            }
        }
        AppPackageInfo pkg = findAppPackageInfo(onboardedAppPkgId);
        if (pkg.getOperationalState() == OperationalState.ENABLED) {
            log.error("MEC app package " + onboardedAppPkgId + " already enabled.");
            throw new FailedOperationException("MEC app package " + onboardedAppPkgId + " already enabled.");
        }
        if (pkg.isDeletionPending()) {
            log.error("MEC app package " + onboardedAppPkgId + " under deletion. Impossible to enable.");
            throw new FailedOperationException("MEC app package " + onboardedAppPkgId + " under deletion. Impossible to enable");
        }
        pkg.setOperationalState(OperationalState.ENABLED);
        appPackageInfoRepository.saveAndFlush(pkg);
        log.debug("MEC app package " + onboardedAppPkgId + " enabled.");
        //notify(new AppPackageStateChangeNotification(onboardedAppPkgId, AppdChangeType.OPERATIONAL_STATE_CHANGE, OperationalState.ENABLED, false));
    }

    @Override
    public synchronized void disableAppPackage(String onboardedAppPkgId, String project) throws MethodNotImplementedException,
            NotExistingEntityException, FailedOperationException, MalformattedElementException, NotAuthorizedOperationException {
        log.debug("Received request to disable MEC app package with ID " + onboardedAppPkgId);
        if (onboardedAppPkgId == null) throw new MalformattedElementException("Received request with null ID.");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new MalformattedElementException("Project with id " + project + " does not exist");
            }
        }
        AppPackageInfo pkg = findAppPackageInfo(onboardedAppPkgId);
        if (pkg.getOperationalState() == OperationalState.DISABLED) {
            log.error("MEC app package " + onboardedAppPkgId + " already disabled.");
            throw new FailedOperationException("MEC app package " + onboardedAppPkgId + " already disabled.");
        }
        if (pkg.isDeletionPending()) {
            log.error("MEC app package " + onboardedAppPkgId + " under deletion. Impossible to disable.");
            throw new FailedOperationException("MEC app package " + onboardedAppPkgId + " under deletion. Impossible to disable");
        }
        pkg.setOperationalState(OperationalState.DISABLED);
        appPackageInfoRepository.saveAndFlush(pkg);
        log.debug("MEC app package " + onboardedAppPkgId + " enabled.");
        //notify(new AppPackageStateChangeNotification(onboardedAppPkgId, AppdChangeType.OPERATIONAL_STATE_CHANGE, OperationalState.DISABLED, false));
    }

    @Override
    public synchronized void deleteAppPackage(String onboardedAppPkgId, String project) throws MethodNotImplementedException,
            NotExistingEntityException, FailedOperationException, MalformattedElementException, NotAuthorizedOperationException {
        log.debug("Received request to delete MEC app package with ID " + onboardedAppPkgId);
        if (onboardedAppPkgId == null) throw new MalformattedElementException("Received request with null ID.");
        if (project != null) {
            Optional<ProjectResource> projectOptional = projectRepository.findByProjectId(project);
            if (!projectOptional.isPresent()) {
                log.error("Project with id " + project + " does not exist");
                throw new MalformattedElementException("Project with id " + project + " does not exist");
            }
        }
        AppPackageInfo pkg = findAppPackageInfo(onboardedAppPkgId);
        if (pkg.getUsageState() == UsageState.IN_USE) {
            log.debug("MEC App package still in use. It will be removed later on. Setting it to pending delete");
            pkg.setDeletionPending(true);
            appPackageInfoRepository.save(pkg);
            //notify(new AppPackageStateChangeNotification(onboardedAppPkgId, AppdChangeType.APP_PACKAGE_IN_DELETION_PENDING, pkg.getOperationalState(), true));
        } else {
            appPackageInfoRepository.delete(pkg);
            log.debug("MEC App pacakge " + onboardedAppPkgId + " removed from DB.");
            //notify(new AppPackageStateChangeNotification(onboardedAppPkgId, AppdChangeType.APP_PACKAGE_DELETION, pkg.getOperationalState(), false));
        }
    }

    @Override
    public void abortAppPackageDeletion(String onboardedAppPkgId) throws MethodNotImplementedException,
            NotExistingEntityException, FailedOperationException, MalformattedElementException, NotAuthorizedOperationException {
        throw new MethodNotImplementedException();
    }

    /**
     * Method used to send notifications about new app package on-boarding to the active subscribers.
     *
     * @param notification notification to be sent to the subscribers
     *//*
    private void notify(AppPackageOnBoardingNotification notification) {
        //TODO: manage filters
        log.debug("Going to notify MEC app package onboarding to all the subscribers");
        List<AppPackageManagementSubscription> subscriptions = appPackageManagementSubscriptionRepository.findAll();
        for (AppPackageManagementSubscription s : subscriptions) {
            AppdManagementRestConsumer consumer = s.getConsumer();
            consumer.notify(notification);
        }
        log.debug("MEC app package onboarding notified to all the subscribers");
    }

    *//**
     * Method used to send notifications about app package state change to the active subscribers.
     *
     * @param notification notification to be sent to the subscribers
     *//*
    private void notify(AppPackageStateChangeNotification notification) {
        //TODO: manage filters
        log.debug("Going to notify MEC app package state change to all the subscribers");
        List<AppPackageManagementSubscription> subscriptions = appPackageManagementSubscriptionRepository.findAll();
        for (AppPackageManagementSubscription s : subscriptions) {
            AppdManagementRestConsumer consumer = s.getConsumer();
            consumer.notify(notification);
        }
        log.debug("MEC app package state change notified to all the subscribers");
    }*/

    /**
     * Method to retrieve a MEC app package info with the given package info ID from the repository
     *
     * @param appPackageInfoId ID of the MEC app package info
     * @return the MEC app package info
     * @throws NotExistingEntityException if the package info is not present in the repository
     */
    private AppPackageInfo findAppPackageInfo(String appPackageInfoId) throws NotExistingEntityException {
        log.debug("Searching MEC App package info with ID " + appPackageInfoId);
        Optional<AppPackageInfo> appPackageInfoOpt = appPackageInfoRepository.findByAppPackageInfoId(appPackageInfoId);
        if (appPackageInfoOpt.isPresent()) {
            log.debug("MEC App package info with ID " + appPackageInfoId + " found.");
            return appPackageInfoOpt.get();
        } else {
            log.warn("MEC App package info with ID " + appPackageInfoId + " not found.");
            throw new NotExistingEntityException("MEC App package info with ID " + appPackageInfoId + " not found.");
        }
    }

    /**
     * Method to retrieve a MEC app package info with the given Appd ID and version from the repository
     *
     * @param appdId ID of the MEC app package App Descriptor
     * @param version version of the MEC app package
     * @return the MEC app package info
     * @throws NotExistingEntityException if the package info is not present in the repository
     */
    private AppPackageInfo findAppPackageInfo(String appdId, String version) throws NotExistingEntityException {
        log.debug("Searching MEC App package info with AppD ID " + appdId + " and version " + version);
        Optional<AppPackageInfo> appPackageInfoOpt = appPackageInfoRepository.findByAppdIdAndVersion(appdId, version);
        if (appPackageInfoOpt.isPresent()) {
            log.debug("MEC App package info with App ID " + appdId + " and version " + version + " found.");
            return appPackageInfoOpt.get();
        } else {
            log.warn("MEC App package info with App ID " + appdId + " and version " + version + " not found.");
            throw new NotExistingEntityException("MEC App package info with App ID " + appdId + " and version " + version + " not found.");
        }
    }

    /**
     * Method used to complete an app package info with details included in other DB tables (e.g. the appD).
     *
     * @param input orginal app package info to be completed
     * @return the completed app package info
     * @throws NotExistingEntityException if the required information are not found
     */
    private AppPackageInfo fillAppPackageInfoDetails(AppPackageInfo input) throws NotExistingEntityException {
        String appdId = input.getAppdId();
        String version = input.getVersion();
        String provider = input.getProvider();
        log.debug("Completing app package info with appD with ID " + appdId + " and version " + version);
        Optional<Appd> appdOpt = appdRepository.findByAppDIdAndAppDVersionAndAppProvider(appdId, version, provider);
        if (appdOpt.isPresent()) {
            log.debug("AppD found.");
            input.setAppd(appdOpt.get());
            return input;
        } else {
            log.warn("AppD with ID " + appdId + " and version " + version + " not found.");
            throw new NotExistingEntityException("AppD with ID " + appdId + " and version " + version + " not found.");
        }
    }

    /**
     * Method to download the MEC APP package and retrieve the APPD.
     * The MEC APP package must be a .tar file and must include a .json file with the APPD in its root.
     *
     * @param appPackagePath URL to download the MEC APP package
     * @return the AppD in the MEC APP package
     * @throws IOException           in case of a generic IO exception
     * @throws MalformedURLException if the APP package URL is malformed
     * @throws IllegalStateException if the action cannot be executed
     * @throws FileNotFoundException if the file is not found
     * @throws ArchiveException      is the archive is malformatted
     */
    private Appd readAppdFromMecAppPackage(String appPackagePath) throws IOException, MalformedURLException, IllegalStateException, FileNotFoundException, ArchiveException {
        log.debug("Getting MEC APP package");
        String downloadedFile = fileUtilities.downloadFile(appPackagePath);

        log.debug("Retrieving APPD from APP package");
        String folder = fileUtilities.extractFile(downloadedFile);
        File jsonFile = fileUtilities.findJsonFileInDir(folder);
        Charset encoding = null;
        String json = FileUtils.readFileToString(jsonFile, encoding);
        log.debug("APPD json: \n" + json);

        ObjectMapper mapper = new ObjectMapper();
        Appd appd = (Appd) mapper.readValue(json, Appd.class);
        log.debug("AppD correctly parsed.");

        log.debug("Cleaning local directory");
        fileUtilities.removeFileAndFolder(downloadedFile, folder);

        return appd;
    }

    //Private methods to store app package info and appd

    private synchronized Long storeAppPackageInfo(OnboardAppPackageRequest request, Appd appd) {
        log.debug("Storing MEC App package info.");
        AppPackageInfo appPackageInfo = new AppPackageInfo(null, //appPackageInfoId
                appd.getAppDId(),
                appd.getAppDVersion(),
                appd.getAppProvider(),
                request.getName(),
                null, //appd,
                OperationalState.ENABLED,
                UsageState.NOT_IN_USE,
                false);

        appPackageInfoRepository.saveAndFlush(appPackageInfo);

        AppPackageInfo createdPackage = appPackageInfoRepository.findByAppdIdAndVersion(appd.getAppDId(), appd.getAppDVersion()).get();
        Long id = createdPackage.getId();
        log.debug("Added MEC package info with AppD ID " + appd.getAppDId() + ", version " + appd.getAppDVersion() + " and provider " + request.getProvider() + ". Internal ID assigned to the APP package: " + id);

        String pkgId = String.valueOf(id);
        createdPackage.setAppPackageInfoId(pkgId);
        appPackageInfoRepository.saveAndFlush(createdPackage);
        log.debug("Stored MEC App package info.");

        return id;
    }

    private synchronized Long storeAppd(Appd appd) throws AlreadyExistingEntityException, FailedOperationException {
        log.debug("Storing Appd.");

        Appd output = new Appd(appd.getAppDId(),
                appd.getAppName(),
                appd.getAppProvider(),
                appd.getAppSoftVersion(),
                appd.getAppDVersion(),
                appd.getMecVersion(),
                appd.getAppInfoName(),
                appd.getAppDescription(),
                appd.getSwImageDescriptor(),
                appd.getVirtualStorageDescriptor(),
                appd.getAppFeatureRequired(),
                appd.getAppFeatureOptional(),
                appd.getAppDNSRule(),
                appd.getAppLatency(),
                appd.getTerminateAppInstanceOpConfig(),
                appd.getChangeAppInstanceStateOpConfig());

        appdRepository.saveAndFlush(output);

        Appd createdAppD = appdRepository.findByAppDIdAndAppDVersionAndAppProvider(appd.getAppDId(), appd.getAppDVersion(), appd.getAppProvider()).get();
        Long id = createdAppD.getId();
        log.debug("Added MEC AppD with AppD ID " + appd.getAppDId() + ", version " + appd.getAppDVersion() + " and provider " + appd.getAppProvider() + ". Internal ID assigned to the VNFD: " + id);

        try {
            storeVirtualComputeDescriptor(appd, output);
            storeAppExternalCpd(appd, output);
            storeAppServiceRequiredDependency(appd, output);
            storeAppServiceOptionalDependency(appd, output);
            storeAppServiceProduced(appd, output);
            storeTransportDependency(appd, output);
            storeTrafficRuleDescriptor(appd, output);
            log.debug("Completed AppD loading.");
            return id;
        } catch (AlreadyExistingEntityException e) {
            log.error("Error while storing the elements of the AppD since one of the element already existed: " + e.getMessage());
            appdRepository.deleteById(id);
            log.debug("AppD with ID " + id + " removed from DB.");
            throw new AlreadyExistingEntityException(e.getMessage());
        } catch (Exception e) {
            log.error("General error while storing the elements of the AppD: " + e.getMessage());
            appdRepository.deleteById(id);
            log.debug("AppD with ID " + id + " removed from DB.");
            throw new FailedOperationException(e.getMessage());
        }
    }

    private void storeVirtualComputeDescriptor(Appd input, Appd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing AppD Virtual compute descriptor");

        String appdId = output.getAppDId();
        String version = output.getAppDVersion();

        VirtualComputeDesc vcd = input.getVirtualComputeDescriptor();
        String vcdId = vcd.getVirtualComputeDescId();
        if (vcdRepository.findByVirtualComputeDescIdAndAppdAppDIdAndAppdAppDVersion(vcdId, appdId, version).isPresent()) {
            log.error("VCD " + vcdId + " already existing. Impossible to load a new one");
            throw new AlreadyExistingEntityException("VCD " + vcdId + " already existing.");
        }
        VirtualComputeDesc target = new VirtualComputeDesc(output, vcdId, vcd.getLogicalNode(), vcd.getRequestAdditionalCapabilities(),
                vcd.getVirtualCpu(), vcd.getVirtualMemory());
        vcdRepository.saveAndFlush(target);
        log.debug("Stored AppD Virtual compute descriptor " + vcdId);
    }

    private void storeAppExternalCpd(Appd input, Appd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing AppD external CP descriptors");

        String appdId = output.getAppDId();
        String version = output.getAppDVersion();

        List<AppExternalCpd> cps = input.getAppExtCpd();
        for (AppExternalCpd cp : cps) {
            String cpdId = cp.getCpdId();
            if (appExternalCpdRepository.findByCpdIdAndAppdAppDIdAndAppdAppDVersion(cpdId, appdId, version).isPresent()) {
                log.error("Appd external CP " + cpdId + " already existing. Impossible to load a new one");
                throw new AlreadyExistingEntityException("Appd external CP " + cpdId + " already existing.");
            }
            AppExternalCpd target = new AppExternalCpd(output, cpdId, cp.getLayerProtocol(), cp.getCpRole(), cp.getDescription(),
                    cp.getAddressData(), cp.getVirtualNetworkInterfaceRequirements());

            appExternalCpdRepository.saveAndFlush(target);
            log.debug("Stored MEC app external CP " + cpdId);
        }

        log.debug("All MEC app external CPs have been stored.");
    }

    private void storeAppServiceRequiredDependency(Appd input, Appd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing AppD required service dependencies");

        String appdId = output.getAppDId();
        String version = output.getAppDVersion();

        List<MecServiceDependency> sds = input.getAppServiceRequired();
        for (MecServiceDependency sd : sds) {
            String serName = sd.getSerName();
            if (mecServiceDependencyRepository.findBySerNameAndAppdRequiredAppDIdAndAppdRequiredAppDVersion(serName, appdId, version).isPresent()) {
                log.error("AppD required service dependency " + serName + " already existing for app  " + appdId + " . Impossible to create a new one.");
                throw new AlreadyExistingEntityException("AppD required service dependency " + serName + " already existing for app  " + appdId + " . Impossible to create a new one.");
            }
            MecServiceDependency target = new MecServiceDependency(output, null, serName, sd.getSerCategory(), sd.getServiceVersion(), sd.getRequestedPermissions());
            mecServiceDependencyRepository.saveAndFlush(target);

            storeTransportDependencies(target, null, sd.getSerTransportDependencies());

            log.debug("Required MEC service dependency has been stored.");
        }
        log.debug("All the AppD required service dependencies have been stored");
    }

    private void storeAppServiceOptionalDependency(Appd input, Appd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing AppD optional service dependencies");

        String appdId = output.getAppDId();
        String version = output.getAppDVersion();

        List<MecServiceDependency> sds = input.getAppServiceRequired();
        for (MecServiceDependency sd : sds) {
            String serName = sd.getSerName();
            if (mecServiceDependencyRepository.findBySerNameAndAppdOptionalAppDIdAndAppdOptionalAppDVersion(serName, appdId, version).isPresent()) {
                log.error("AppD optional service dependency " + serName + " already existing for app  " + appdId + " . Impossible to create a new one.");
                throw new AlreadyExistingEntityException("AppD optional service dependency " + serName + " already existing for app  " + appdId + " . Impossible to create a new one.");
            }
            MecServiceDependency target = new MecServiceDependency(null, output, serName, sd.getSerCategory(), sd.getServiceVersion(), sd.getRequestedPermissions());
            mecServiceDependencyRepository.saveAndFlush(target);

            storeTransportDependencies(target, null, sd.getSerTransportDependencies());

            log.debug("Optional MEC service dependency has been stored.");
        }
        log.debug("All the AppD optional service dependencies have been stored");
    }

    private void storeTransportDependencies(MecServiceDependency targetMecService, Appd targetAppd, List<TransportDependency> input) {
        for (TransportDependency tDep : input) {
            TransportDependency tDepTarget;
            if (targetMecService != null)
                tDepTarget = new TransportDependency(targetMecService, tDep.getSerializers(), tDep.getLabels());
            else
                tDepTarget = new TransportDependency(targetAppd, tDep.getSerializers(), tDep.getLabels());
            transportDependencyRepository.saveAndFlush(tDepTarget);

            TransportDescriptor tDes = tDep.getTransport();
            TransportDescriptor tDesTarget = new TransportDescriptor(tDepTarget, tDes.getType(), tDes.getProtocol(), tDes.getVersion());
            transportDescriptorRepository.saveAndFlush(tDesTarget);

            SecurityInfo sInfo = tDes.getSecurity();
            SecurityInfo sInfoTarget = new SecurityInfo(tDesTarget, sInfo.getExtensions(), sInfo.getGrantTypes(), sInfo.getTokenEndpoint());
            securityInfoRepository.saveAndFlush(sInfoTarget);
            log.debug("Security info within transport descriptor has been stored.");

            log.debug("Transport descriptor within transport dependency has been stored.");

            log.debug("Transport dependency has been stored.");
        }
        log.debug("All transport dependencies have been stored.");
    }

    private void storeAppServiceProduced(Appd input, Appd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing AppD produced service");

        String appdId = output.getAppDId();
        String version = output.getAppDVersion();

        List<MecServiceDescriptor> msds = input.getAppServiceProduced();
        for (MecServiceDescriptor msd : msds) {
            String serName = msd.getSerName();
            if (mecServiceDescriptorRepository.findBySerNameAndAppdAppDIdAndAppdAppDVersion(serName, appdId, version).isPresent()) {
                log.error("AppD produced service " + serName + " already existing for app  " + appdId + " . Impossible to create a new one.");
                throw new AlreadyExistingEntityException("AppD produced service " + serName + " already existing for app  " + appdId + " . Impossible to create a new one.");
            }

            MecServiceDescriptor target = new MecServiceDescriptor(output, serName, msd.getSerCategory(), msd.getSerVersion());
            mecServiceDescriptorRepository.saveAndFlush(target);

            List<MecServiceTransport> msts = msd.getTransportsSupported();
            for (MecServiceTransport mst : msts) {
                MecServiceTransport targetMst = new MecServiceTransport(target, mst.getSerializers());
                mecServiceTransportRepository.saveAndFlush(targetMst);

                TransportDescriptor td = mst.getTransport();
                TransportDescriptor tDesTarget = new TransportDescriptor(targetMst, td.getType(), td.getProtocol(), td.getVersion());
                transportDescriptorRepository.saveAndFlush(tDesTarget);

                SecurityInfo sInfo = td.getSecurity();
                SecurityInfo sInfoTarget = new SecurityInfo(tDesTarget, sInfo.getExtensions(), sInfo.getGrantTypes(), sInfo.getTokenEndpoint());
                securityInfoRepository.saveAndFlush(sInfoTarget);
                log.debug("Security info within transport descriptor has been stored.");

                log.debug("Transport descriptor within MEC service transport has been stored.");

                log.debug("MEC service tranport stored.");
            }
            log.debug("All MEC service transports have been stored.");
        }
        log.debug("All produced services have been stored.");
    }

    private void storeTransportDependency(Appd input, Appd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing AppD transport dependencies");
        storeTransportDependencies(null, output, input.getTransportDependencies());
    }

    private void storeTrafficRuleDescriptor(Appd input, Appd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing AppD traffic rule descriptors");

        String appdId = output.getAppDId();
        String version = output.getAppDVersion();

        List<TrafficRuleDescriptor> trds = input.getAppTrafficRule();
        for (TrafficRuleDescriptor trd : trds) {
            String ruleId = trd.getTrafficRuleId();
            if (trafficRuleDescriptorRepository.findByTrafficRuleIdAndAppdAppDIdAndAppdAppDVersion(ruleId, appdId, version).isPresent()) {
                log.error("Traffic rule " + ruleId + " already existing for app " + appdId + ". Impossible to create a new one.");
                throw new AlreadyExistingEntityException("Traffic rule " + ruleId + " already existing for app " + appdId + ". Impossible to create a new one.");
            }

            TrafficRuleDescriptor target = new TrafficRuleDescriptor(output, ruleId, trd.getFilterType(), trd.getPriority(), trd.getAction());
            trafficRuleDescriptorRepository.saveAndFlush(target);

            List<TrafficFilter> trafficFilters = trd.getTrafficFilter();
            for (TrafficFilter tf : trafficFilters) {
                TrafficFilter targetFilter = new TrafficFilter(target, tf.getSrcAddress(), tf.getDstAddress(), tf.getSrcPort(), tf.getDstPort(), tf.getProtocol(),
                        tf.getToken(), tf.getSrcTunnelAddress(), tf.getTgtTunnelAddress(), tf.getSrcTunnelAddress(), tf.getDstTunnelPort(), tf.getqCI(), tf.getdSCP(), tf.gettC());
                trafficFilterRepository.saveAndFlush(targetFilter);
                log.debug("Traffic filter stored");
            }
            log.debug("All the traffic filters have been stored.");

            List<MeAppInterfaceDescriptor> dstInterfaces = trd.getDstInterface();
            for (MeAppInterfaceDescriptor maid : dstInterfaces) {
                MeAppInterfaceDescriptor targetIf = new MeAppInterfaceDescriptor(target, maid.getInterfaceType(), maid.getTunnelInfo(), maid.getSrcMACAddress(), maid.getDstMACAddress(), maid.getDstIPAddress());
                meAppInterfaceDescriptorRepository.saveAndFlush(targetIf);
                log.debug("ME Application interface descriptor stored.");
            }
            log.debug("All the ME app interface descriptors have been stored.");

            log.debug("Traffic rule descriptor with rule ID " + ruleId + " stored.");
        }
        log.debug("All the traffic rule descriptors have been stored.");
    }

}
