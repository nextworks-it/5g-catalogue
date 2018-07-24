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
package it.nextworks.nfvmano.catalogue.repos.vnfpackages;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextworks.nfvmano.libs.catalogues.interfaces.VnfPackageManagementConsumerInterface;
import it.nextworks.nfvmano.libs.catalogues.interfaces.VnfPackageManagementProviderInterface;
import it.nextworks.nfvmano.libs.catalogues.interfaces.enums.VnfPackageChangeType;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.*;
import it.nextworks.nfvmano.libs.common.elements.Filter;
import it.nextworks.nfvmano.libs.common.enums.OperationalState;
import it.nextworks.nfvmano.libs.common.enums.UsageState;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.common.messages.GeneralizedQueryRequest;
import it.nextworks.nfvmano.libs.common.messages.SubscribeRequest;
import it.nextworks.nfvmano.libs.descriptors.common.elements.*;
import it.nextworks.nfvmano.libs.descriptors.onboardedvnfpackage.OnboardedVnfPkgInfo;
import it.nextworks.nfvmano.libs.descriptors.onboardedvnfpackage.VnfPackageArtifactInformation;
import it.nextworks.nfvmano.libs.descriptors.vnfd.*;
import it.nextworks.nfvmano.catalogue.common.FileUtilities;
import it.nextworks.nfvmano.catalogue.repos.nsds.repositories.RuleRepository;
import it.nextworks.nfvmano.catalogue.repos.nsds.repositories.VirtualLinkDfRepository;
import it.nextworks.nfvmano.catalogue.repos.nsds.repositories.VirtualLinkProfileRepository;
import it.nextworks.nfvmano.catalogue.repos.vnfpackages.repositories.*;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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


/**
 * This service manages the external requests for VNF package management
 * and interacts with the internal repositories of the VNFD catalog.
 * Its methods are invoked by the VnfPackageManagementController class.
 *
 * @author nextworks
 */
@Service
public class VnfPackageManagementService implements VnfPackageManagementProviderInterface {

    private static final Logger log = LoggerFactory.getLogger(VnfPackageManagementService.class);

    @Autowired
    VnfPackageInfoRepository vnfPackageInfoRepository;

    @Autowired
    VnfPackageSoftwareImageRepository vpSoftwareImageRepository;

    @Autowired
    VnfdRepository vnfdRepository;

    @Autowired
    SoftwareImageInformationRepository swImageInfoRepository;

    @Autowired
    VnfdVduRepository vduRepository;

    @Autowired
    VirtualComputeDescriptorRepository vcdRepository;

    @Autowired
    VnfVirtualLinkDescriptorRepository vnfVldRepository;

    @Autowired
    VnfExtCpdRepository vnfExtCpdRepository;

    @Autowired
    VnfDfRepository vnfDfRepository;

    @Autowired
    VnfInfoModifiableAttributesRepository vnfInfoModifiableAttributesRepository;
    
    @Autowired
    VnfConfigurablePropertiesRepository vnfConfigurablePropertiesRepository;

    @Autowired
    LifeCycleManagementScriptRepository lcmScriptRepository;

    @Autowired
    VnfdElementGroupRepository vnfdElementGroupRepository;

    @Autowired
    RuleRepository ruleRepository;

    @Autowired
    VnfdVduProfileRepository vduProfileRepository;

    @Autowired
    VirtualLinkProfileRepository vlProfileRepository;

    @Autowired
    InstantiationLevelRepository instantiationLevelRepository;

    @Autowired
    VnfLcmOperationsConfigurationRepository vnfLcmOperationsConfigurationRepository;

    @Autowired
    InstantiateVnfOpConfigRepository instantiateVnfOpConfigRepository;

    @Autowired
    ScaleVnfOpConfigRepository scaleVnfOpConfigRepository;

    @Autowired
    ScaleVnfToLevelOpConfigRepository scaleVnfToLevelOpConfigRepository;

    @Autowired
    HealVnfOpConfigRepository healVnfOpConfigRepository;
    
    @Autowired
    ChangeVnfFlavourOpConfigRepository changeVnfFlavourOpConfigRepository;
    
    @Autowired
    ChangeExtVnfConnectivityOpConfigRepository changeExtVnfConnectivityOpConfigRepository;

    @Autowired
    VirtualLinkDfRepository vldDfRepository;

    @Autowired
    VnfVduCpdRepository vnfVduCpdRepository;

    @Autowired
    VnfPackageManagementSubscriptionRepository vnfpManagementSubscriptionRepository;

    @Autowired
    FileUtilities fileUtilities;

    public VnfPackageManagementService() { }

    /**
     * This operation will on-board a VNF Package in the NFVO.
     *
     * @param request on-board request
     * @return on-board response
     * @throws MethodNotImplementedException  if the method is not implemented
     * @throws AlreadyExistingEntityException if the VNF package is already existing in the NFVO nfvmano
     * @throws FailedOperationException       if the operation fails internally in the NFVO
     */
    @Override
    public OnBoardVnfPackageResponse onBoardVnfPackage(OnBoardVnfPackageRequest request)
            throws MethodNotImplementedException, AlreadyExistingEntityException, FailedOperationException {
        log.debug("Received request to onboard a new VNF package");

        //check if a VNF package with same name, provider and version already exists
        if (vnfPackageInfoRepository.findByVnfProductNameAndVnfSoftwareVersionAndVnfProvider(request.getName(), request.getVersion(), request.getProvider()).isPresent()) {
            log.error("VNF Package with name " + request.getName() + ", version " + request.getVersion() + " and provider " + request.getProvider() + " already existing in DB. Impossible to create a new one.");
            throw new AlreadyExistingEntityException("VNF Package with name " + request.getName() + ", version " + request.getVersion() + " and provider " + request.getProvider() + " already existing in DB. Impossible to create a new one.");
        }

        String vnfPackagePath = request.getVnfPackagePath();

        Vnfd vnfd;

        try {
            vnfd = readVnfdFromVnfPackage(vnfPackagePath);
            log.debug("VNFD retrieved.");
        } catch (Exception e) {
            log.error("Error while getting VNFD: " + e.getMessage());
            throw new FailedOperationException("Onboarding failed due to an error in retrieving the VNFD.");
        }

        try {
            vnfd.isValid();
        } catch (MalformattedElementException e) {
            log.error("The VNFD is malformatted! " + e.getMessage());
            throw new FailedOperationException("Onboarding failed because the retrieved VNFD is malformatted.");
        }

        //check if a VNFD with same ID, provider and version already exists
        if (vnfdRepository.findByVnfdIdAndVnfProviderAndVnfdVersion(vnfd.getVnfdId(), vnfd.getVnfProvider(), vnfd.getVnfdVersion()).isPresent()) {
            log.error("VNFD with VNFD ID " + vnfd.getVnfdId() + ", provider " + vnfd.getVnfProvider() + " and version " + vnfd.getVnfdVersion() + " already existing in DB. Impossible to create a new one.");
            throw new AlreadyExistingEntityException("VNFD with VNFD ID " + vnfd.getVnfdId() + ", provider " + vnfd.getVnfProvider() + " and version " + vnfd.getVnfdVersion() + " already existing in DB. Impossible to create a new one.");
        }

        Long vnfPackageInfoId = storeVnfPackageInfo(request, vnfd);
        String vnfPackageInfoIdString = String.valueOf(vnfPackageInfoId);
        String vnfdId;

        try {
            vnfdId = storeVnfd(vnfd, vnfPackageInfoIdString);
        } catch (AlreadyExistingEntityException e) {
            log.error("Failed VNFD saving - overlapping elements");
            vnfPackageInfoRepository.deleteById(vnfPackageInfoId);
            log.debug("VNF package info deleted");
            throw new AlreadyExistingEntityException(e.getMessage());
        } catch (Exception e) {
            log.error("Failed VNFD saving - generic error");
            vnfPackageInfoRepository.deleteById(vnfPackageInfoId);
            log.debug("VNF package info deleted");
            throw new FailedOperationException(e.getMessage());
        }

        log.debug("Successful VNF package on-boarding. VNF package ID: " + vnfPackageInfoIdString + " - Internal VNFD ID: " + vnfdId);
        notify(new VnfPackageOnboardingNotification(vnfPackageInfoIdString, vnfdId));
        return new OnBoardVnfPackageResponse(vnfPackageInfoIdString, vnfdId);
    }

    /**
     * This operation will enable a previously disabled VNF Package instance,
     * allowing again its use for instantiation of new VNF with this package.
     *
     * @param request enable request
     * @throws MethodNotImplementedException if the method is not implemented
     * @throws NotExistingEntityException    if the VNF package info does not exist
     * @throws FailedOperationException      if the operation fails internally in the NFVO
     */
    @Override
    public synchronized void enableVnfPackage(EnableVnfPackageRequest request)
            throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException {
        String pkgId = request.getOnboardedVnfPkgInfoId();
        log.debug("Received request to enable VNF package " + pkgId);
        OnboardedVnfPkgInfo pkg = findVnfPackage(pkgId);
        if (pkg.getOperationalState() == OperationalState.ENABLED) {
            log.error("VNF package " + pkgId + " already enabled.");
            throw new FailedOperationException("VNF package " + pkgId + " already enabled.");
        }
        if (pkg.isDeletionPending()) {
            log.error("VNF package " + pkgId + " under deletion. Impossible to enable.");
            throw new FailedOperationException("VNF package " + pkgId + " under deletion. Impossible to enable");
        }
        pkg.setOperationalState(OperationalState.ENABLED);
        vnfPackageInfoRepository.saveAndFlush(pkg);
        log.debug("VNF package " + pkgId + " enabled.");

        notify(new VnfPackageChangeNotification(pkgId, pkg.getVnfdId(), VnfPackageChangeType.OPERATIONAL_STATE_CHANGE, OperationalState.ENABLED, false));
    }

    /**
     * This operation will disable a previously enabled VNF Package instance,
     * preventing further use for instantiation of new VNFs with this package
     * (unless and until the VNF Package is re-enabled).
     *
     * @param request enable request
     * @throws MethodNotImplementedException if the method is not implemented
     * @throws NotExistingEntityException    if the VNF package info does not exist
     * @throws FailedOperationException      if the operation fails internally in the NFVO
     */
    @Override
    public synchronized void disableVnfPackage(DisableVnfPackageRequest request)
            throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException {
        String pkgId = request.getOnboardedVnfPkgInfoId();
        log.debug("Received request to disable VNF package " + pkgId);
        OnboardedVnfPkgInfo pkg = findVnfPackage(pkgId);
        if (pkg.getOperationalState() == OperationalState.DISABLED) {
            log.error("VNF package " + pkgId + " already disabled.");
            throw new FailedOperationException("VNF package " + pkgId + " already disabled.");
        }
        if (pkg.isDeletionPending()) {
            log.error("VNF package " + pkgId + " under deletion. Impossible to disable.");
            throw new FailedOperationException("VNF package " + pkgId + " under deletion. Impossible to disable");
        }
        pkg.setOperationalState(OperationalState.DISABLED);
        vnfPackageInfoRepository.saveAndFlush(pkg);
        log.debug("VNF package " + pkgId + " disabled.");

        notify(new VnfPackageChangeNotification(pkgId, pkg.getVnfdId(), VnfPackageChangeType.OPERATIONAL_STATE_CHANGE, OperationalState.DISABLED, false));
    }

    /**
     * This operation will delete a VNF Package.
     * A VNF Package can only be deleted once there are no VNFs using it.
     * A deletion pending VNF Package can no longer be enabled, disabled or updated.
     * It is not possible to instantiate VNFs using a VNF Package in the "deletion pending" state.
     *
     * @param request delete request
     * @throws MethodNotImplementedException if the method is not implemented
     * @throws NotExistingEntityException    if the VNF package does not exist
     * @throws FailedOperationException      if the operation fails internally in the NFVO
     */
    @Override
    public synchronized void deleteVnfPackage(DeleteVnfPackageRequest request)
            throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException {
        String pkgId = request.getOnboardedVnfPkgInfoId();
        log.debug("Received request to delete VNF package " + pkgId);
        OnboardedVnfPkgInfo pkg = findVnfPackage(pkgId);
        if (pkg.getUsageState() == UsageState.IN_USE) {
            log.debug("VNF package still in use. It will be removed later on. Setting it to pending delete");
            pkg.setDeletionPending(true);
            vnfPackageInfoRepository.save(pkg);
            notify(new VnfPackageChangeNotification(pkgId, pkg.getVnfdId(), VnfPackageChangeType.VNF_PACKAGE_IN_DELETION_PENDING, pkg.getOperationalState(), true));
        } else {
            vnfPackageInfoRepository.delete(pkg);
            log.debug("VNF package " + pkgId + " removed from DB.");
            notify(new VnfPackageChangeNotification(pkgId, pkg.getVnfdId(), VnfPackageChangeType.VNF_PACKAGE_DELETION, pkg.getOperationalState(), false));
        }
    }

    /**
     * This operation will enable the OSS/BSS to query from the NFVO
     * for information it has stored about one or more VNF Packages.
     *
     * @param request query
     * @return query response
     * @throws MethodNotImplementedException if the method is not implemented
     * @throws NotExistingEntityException    if the VNF package info is not found in the NFVO nfvmano
     */
    @Override
    public QueryOnBoardedVnfPkgInfoResponse queryVnfPackageInfo(GeneralizedQueryRequest request)
            throws MethodNotImplementedException, NotExistingEntityException {
        log.debug("Received query on boarded VNF package info request");

        //At the moment the only filters accepted are:
        //1. VNF package ID
        //VNF_PACKAGE_ID
        //2. Product name && sw version && VNF provider
        //VNF_PACKAGE_PRODUCT_NAME
        //VNF_PACKAGE_SW_VERSION
        //VNF_PACKAGE_PROVIDER
        //VNFD_ID
        //No attribute selector is supported at the moment

        Filter filter = request.getFilter();
        List<String> attributeSelector = request.getAttributeSelector();

        List<OnboardedVnfPkgInfo> pkgList = new ArrayList<>();

        if ((attributeSelector == null) || (attributeSelector.isEmpty())) {
            Map<String, String> fp = filter.getParameters();
            if (fp.size() == 1 && fp.containsKey("VNF_PACKAGE_ID")) {
                String pkgId = fp.get("VNF_PACKAGE_ID");
                OnboardedVnfPkgInfo pkg = findVnfPackage(pkgId);
                pkgList.add(pkg);
            } else if (fp.size() == 1 && fp.containsKey("VNFD_ID")) {
            	String vnfdId = fp.get("VNFD_ID");
            	OnboardedVnfPkgInfo pkg = getOnboardedVnfPkgInfoFromVnfd(vnfdId);
            	pkgList.add(pkg);
            } else if (fp.size() == 3 && fp.containsKey("VNF_PACKAGE_PRODUCT_NAME") && fp.containsKey("VNF_PACKAGE_SW_VERSION") && fp.containsKey("VNF_PACKAGE_PROVIDER")) {
                String name = fp.get("VNF_PACKAGE_PRODUCT_NAME");
                String version = fp.get("VNF_PACKAGE_SW_VERSION");
                String provider = fp.get("VNF_PACKAGE_PROVIDER");
                OnboardedVnfPkgInfo pkg = findVnfPackage(name, version, provider);
                pkgList.add(pkg);
            } else if (fp.isEmpty()) {
                log.debug("Query with empty filter. Returning all the elements");
                List<OnboardedVnfPkgInfo> vnfPkgInfos = vnfPackageInfoRepository.findAll();
                pkgList.addAll(vnfPkgInfos);
                log.debug("Added all VNF package infos found in DB");
            } else {
                log.error("Received query VNF package with not supported filter.");
                throw new MethodNotImplementedException("Received query VNF package with not supported filter.");
            }
        } else {
            log.error("Received query VNF package with attribute selector. Not supported at the moment.");
            throw new MethodNotImplementedException("Received query VNF package with attribute selector. Not supported at the moment.");
        }
        QueryOnBoardedVnfPkgInfoResponse response = new QueryOnBoardedVnfPkgInfoResponse(pkgList);
        return response;
    }

    /**
     * This operation enables the OSS/BSS to subscribe with a filter
     * for the notifications related to on-boarding of VNF Packages
     * and changes of VNF Packages sent by the NFVO.
     *
     * @param request  subscription request
     * @param consumer subscriber
     * @return subscription response
     * @throws MethodNotImplementedException if the method is not implemented
     */
    @Override
    public String subscribeVnfPackageInfo(SubscribeRequest request, VnfPackageManagementConsumerInterface consumer)
            throws MethodNotImplementedException, MalformattedElementException, FailedOperationException {
        log.debug("Received subscribe request for VNF package notifications");

        VnfPackageManagementConsumer abstractConsumer = (VnfPackageManagementConsumer) consumer;
        switch (abstractConsumer.getType()) {
            case REST: {
                log.debug("Subscription for a REST consumer");
                VnfPackageManagementRestConsumer restConsumer = (VnfPackageManagementRestConsumer) abstractConsumer;
                VnfPackageManagementSubscription subscription = new VnfPackageManagementSubscription(restConsumer);
                this.vnfpManagementSubscriptionRepository.saveAndFlush(subscription);
                log.debug("VNF package management subscription done: ID " + subscription.getId().toString());
                return subscription.getId().toString();
            }

            default: {
                log.error("VNF package management consumer type not supported.");
                throw new FailedOperationException("VNF package management consumer type not supported.");
            }
        }
    }


    /**
     * Method to remove a previous subscription
     *
     * @param subscriptionId ID of the subscription to be removed
     * @throws MethodNotImplementedException if the method is not implemented
     * @throws NotExistingEntityException    if the VNF package is not found in the NFVO nfvmano
     * @throws MalformattedElementException if the request is malformatted
     */
    @Override
    public void unsubscribeVnfPackageInfo(String subscriptionId) throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
    	if (subscriptionId == null) throw new MalformattedElementException("Null subscription ID");
        log.debug("Received unsubscribe for VNF package management information: ID " + subscriptionId);
        Optional<VnfPackageManagementSubscription> subscriptionOpt = vnfpManagementSubscriptionRepository.findById(Long.parseLong(subscriptionId));
        if (subscriptionOpt.isPresent()) {
            VnfPackageManagementSubscription subscription = subscriptionOpt.get();
            vnfpManagementSubscriptionRepository.delete(subscription);
            log.debug("Subscription removed");
        } else {
            log.error("VNF package management subscription " + subscriptionId + " not found. Impossible to remove");
            throw new NotExistingEntityException("VNF package management subscription " + subscriptionId + " not found. Impossible to remove");
        }
    }

    /**
     * This operation enables the OSS to fetch a whole on-boarded VNF Package.
     * The package is addressed using an identifier of information held by the
     * NFVO about the specific on-boarded VNF Package.
     *
     * @param onboardedVnfPkgInfoId Identifier of information held by the NFVO about the specific on-boarded VNF Package. This identifier was allocated by the NFVO.
     * @return the VNF package
     * @throws MethodNotImplementedException if the method is not implemented
     * @throws NotExistingEntityException    if the VNF package is not found in the NFVO nfvmano
     * @throws MalformattedElementException if the request is malformatted
     */
    @Override
    public File fetchOnboardedVnfPackage(String onboardedVnfPkgInfoId) 
			throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        //TODO:
        throw new MethodNotImplementedException();
    }

    /**
     * This operation enables the OSS/BSS to fetch selected artifacts
     * contained in an on-boarded VNF package.
     *
     * @param request fetch VNF package artifacts request
     * @return response with the requested artifacts
     * @throws MethodNotImplementedException if the method is not implemented
     * @throws NotExistingEntityException    if the VNF package is not found in the NFVO nfvmano
     * @throws MalformattedElementException if the request is malformatted
     */
    @Override
    public List<File> fetchOnboardedVnfPackageArtifacts(FetchOnboardedVnfPackageArtifactsRequest request)
			throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        //TODO:
        throw new MethodNotImplementedException();
    }

    /**
     * This operation enables the OSS to abort the deletion
     * of a VNF Package that is in deletion pending state.
     *
     * @param onboardedVnfPkgInfoId Identifier of the onboarded VNF Package of which the deletion is requested to be aborted.
     * @throws MethodNotImplementedException if the method is not implemented
     * @throws NotExistingEntityException    if the VNF package is not found in the NFVO nfvmano
     * @throws FailedOperationException      if the operation fails internally within the NFVO
     */
    @Override
    public void abortVnfPackageDeletion(String onboardedVnfPkgInfoId)
            throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        //TODO:
        throw new MethodNotImplementedException();
    }
    
    /**
	 * This operation enables the consumer to query information about subscriptions.
	 * TODO: still to be defined the format of the request
	 * 
	 * REF IFA 007 v2.3.1 - 6.2.9
	 * 
	 * @param request subscription query
	 * @throws MethodNotImplementedException if the method is not implemented
	 * @throws NotExistingEntityException if the subscription does not exist
	 * @throws FailedOperationException if the operation fails
	 * @throws MalformattedElementException if the request is malformatted
	 */
    @Override
	public void queryVnfPackageSubscription(GeneralizedQueryRequest request) 
			throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
		//TODO:
        throw new MethodNotImplementedException();
	}


    public synchronized void notifyVnfInstanceCreation(String vnfPackageId, String vnfId) throws NotExistingEntityException {
        log.debug("Received notification for a new VNF instance associated to VNF package " + vnfPackageId);
        OnboardedVnfPkgInfo pkg = findVnfPackage(vnfPackageId);
        if (pkg.getUsageState() == UsageState.NOT_IN_USE) pkg.setUsageState(UsageState.IN_USE);
        pkg.addVnfInstance(vnfId);
        vnfPackageInfoRepository.saveAndFlush(pkg);
        log.debug("Updated VNF package info " + vnfPackageId + " for the instantiation of the new VNF instance " + vnfId);
    }

    public synchronized void notifyVnfInstanceDeletion(String vnfPackageId, String vnfId) throws NotExistingEntityException {
        log.debug("Received notification for the deletion of a VNF instance associated to VNF package " + vnfPackageId);
        OnboardedVnfPkgInfo pkg = findVnfPackage(vnfPackageId);
        pkg.removeVnfInstance(vnfId);
        if (pkg.getVnfId().isEmpty()) pkg.setUsageState(UsageState.NOT_IN_USE);
        vnfPackageInfoRepository.saveAndFlush(pkg);
        log.debug("Updated VNF package info " + vnfPackageId + " for the deletion of the VNF instance " + vnfId);

        if ((pkg.getUsageState() == (UsageState.NOT_IN_USE)) && (pkg.isDeletionPending())) {
            log.debug("The VNF package info can be now removed");
            vnfPackageInfoRepository.delete(pkg);
            log.debug("VNF pacakge " + vnfPackageId + " removed from DB.");
            notify(new VnfPackageChangeNotification(vnfPackageId, pkg.getVnfdId(), VnfPackageChangeType.VNF_PACKAGE_DELETION, pkg.getOperationalState(), false));
        }
    }

    /**
     * Method to retrieve a VNF package from the VNFD ID
     * 
     * @param vnfdId VNFD ID
     * @return the VNF package with the given VNFD ID
     * @throws NotExistingEntityException if the VNF package is not found
     */
    public OnboardedVnfPkgInfo getOnboardedVnfPkgInfoFromVnfd(String vnfdId) throws NotExistingEntityException {
    	log.debug("Finding OnboardedVnfPkgInfo from VnfdId: " + vnfdId);
    	Optional<OnboardedVnfPkgInfo> pkg = vnfPackageInfoRepository.findByVnfdId(vnfdId);
    	if (pkg.isPresent()) return pkg.get();
    	else throw new NotExistingEntityException("VNF package for VNFD " + vnfdId + " not found in DB");
    }
    
    /**
     * Method to retrieve the VNFDs with a given VNFD ID.
     * Here the VNFD ID is the one assigned by the provider.
     *
     * @param vnfdId VNFD ID
     * @return the list of VNFD with the given VNFD ID
     * @throws NotExistingEntityException if no VNFD is found
     */
    public List<Vnfd> getVnfd(String vnfdId) throws NotExistingEntityException {
        List<Vnfd> vnfds = vnfdRepository.findByVnfdId(vnfdId);
        if ((vnfds == null) || (vnfds.isEmpty())) {
            throw new NotExistingEntityException("No VNFDs found with VNFD ID " + vnfdId);
        }
        return vnfds;
    }

    private void notify(VnfPackageOnboardingNotification notification) {
        //TODO: manage filters
        log.debug("Going to notify VNF package onboarding to all the subscribers");
        List<VnfPackageManagementSubscription> subscriptions = vnfpManagementSubscriptionRepository.findAll();
        for (VnfPackageManagementSubscription s : subscriptions) {
            VnfPackageManagementRestConsumer consumer = s.getConsumer();
            consumer.notify(notification);
        }
        log.debug("VNF package onboarding notified to all the subscribers");
    }

    private void notify(VnfPackageChangeNotification notification) {
        //TODO: manage filters
        log.debug("Going to notify VNF package change to all the subscribers");
        List<VnfPackageManagementSubscription> subscriptions = vnfpManagementSubscriptionRepository.findAll();
        for (VnfPackageManagementSubscription s : subscriptions) {
            VnfPackageManagementRestConsumer consumer = s.getConsumer();
            consumer.notify(notification);
        }
        log.debug("VNF package change notified to all the subscribers");
    }

    private OnboardedVnfPkgInfo findVnfPackage(String pkgId) throws NotExistingEntityException {
        log.debug("Searching VNF package info with ID " + pkgId);
        Optional<OnboardedVnfPkgInfo> pkgOpt = vnfPackageInfoRepository.findByOnboardedVnfPkgInfoId(pkgId);
        if (pkgOpt.isPresent()) {
            return pkgOpt.get();
        } else {
            log.error("VNF package " + pkgId + " not found in DB.");
            throw new NotExistingEntityException("VNF package " + pkgId + " not found in DB.");
        }
    }

    private OnboardedVnfPkgInfo findVnfPackage(String name, String version, String provider) throws NotExistingEntityException {
        Optional<OnboardedVnfPkgInfo> pkgOpt = vnfPackageInfoRepository.findByVnfProductNameAndVnfSoftwareVersionAndVnfProvider(name, version, provider);
        if (pkgOpt.isPresent()) {
            return pkgOpt.get();
        } else {
            log.error("VNF package with name " + name + ", version " + version + " and provider " + provider + " not found in DB.");
            throw new NotExistingEntityException("VNF package with name " + name + ", version " + version + " and provider " + provider + " not found in DB.");
        }
    }
    
    

    /**
     * Method to download the VNF package and retrieve the VNFD.
     * The VNF package must be a .tar file and must include a .json file with the VNFD in its root.
     *
     * @param vnfPackagePath URL to download the VNF package
     * @return the VNFD in the VNF package
     * @throws IOException           in case of a generic IO exception
     * @throws MalformedURLException if the VNF package URL is malformed
     * @throws IllegalStateException if the action cannot be executed
     * @throws FileNotFoundException if the file is not found
     * @throws ArchiveException      is the archive is malformatted
     */
    private Vnfd readVnfdFromVnfPackage(String vnfPackagePath) throws IOException, MalformedURLException, IllegalStateException, FileNotFoundException, ArchiveException {
        log.debug("Getting VNF package");
        String downloadedFile = fileUtilities.downloadFile(vnfPackagePath);

        log.debug("Retrieving VNFD from VNF package");
        String folder = fileUtilities.extractFile(downloadedFile);
        File jsonFile = fileUtilities.findJsonFileInDir(folder);
        Charset encoding = null;
        String json = FileUtils.readFileToString(jsonFile, encoding);
        log.debug("VNFD json: \n" + json);

        ObjectMapper mapper = new ObjectMapper();
        Vnfd vnfd = (Vnfd) mapper.readValue(json, Vnfd.class);
        log.debug("VNFD correctly parsed.");

        log.debug("Cleaning local directory");
        fileUtilities.removeFileAndFolder(downloadedFile, folder);

        return vnfd;
    }

    private synchronized Long storeVnfPackageInfo(OnBoardVnfPackageRequest request, Vnfd vnfd) {
        log.debug("Storing VNF package info.");
        OnboardedVnfPkgInfo pkg = new OnboardedVnfPkgInfo(null,        //onboardedVnfPkgInfoId
                vnfd.getVnfdId(),
                request.getProvider(),
                request.getName(),
                request.getVersion(),
                vnfd.getVnfdVersion(),
                request.getChecksum(),
                new ArrayList<VnfPackageArtifactInformation>(),
                OperationalState.ENABLED,
                UsageState.NOT_IN_USE,
                false,
                request.getUserDefinedData());

        vnfPackageInfoRepository.saveAndFlush(pkg);

        OnboardedVnfPkgInfo createdPkg = vnfPackageInfoRepository.findByVnfProductNameAndVnfSoftwareVersionAndVnfProvider(request.getName(), request.getVersion(), request.getProvider()).get();
        Long id = createdPkg.getId();
        log.debug("Added VNF package info with name " + request.getName() + ", version " + request.getVersion() + " and provider " + request.getProvider() + ". Internal ID assigned to the VNF package: " + id);

        String pkgId = String.valueOf(id);
        createdPkg.setOnboardedVnfPkgInfoId(pkgId);
        vnfPackageInfoRepository.saveAndFlush(createdPkg);

        return id;
    }

    private synchronized String storeVnfd(Vnfd vnfd, String vnfPackageInfoId) throws NotExistingEntityException, AlreadyExistingEntityException, Exception {
        log.debug("Storing VNFD.");

        Optional<OnboardedVnfPkgInfo> pkgOpt = vnfPackageInfoRepository.findByOnboardedVnfPkgInfoId(vnfPackageInfoId);
        if (pkgOpt.isPresent()) {
        	Vnfd output = new Vnfd(pkgOpt.get(), vnfd.getVnfdId(), vnfd.getVnfProvider(), vnfd.getVnfProductName(),
                    vnfd.getVnfSoftwareVersion(), vnfd.getVnfdVersion(), vnfd.getVnfProductInfoName(),
                    vnfd.getVnfProductInfoDescription(), vnfd.getVnfmInfo(), vnfd.getLocalizationLanguage(), vnfd.getDefaultLocalizationLanguage(),
                    vnfd.getVirtualStorageDesc(), vnfd.getVnfIndicator());

            vnfdRepository.saveAndFlush(output);

            Vnfd createdVnfd = vnfdRepository.findByVnfdIdAndVnfProviderAndVnfdVersion(vnfd.getVnfdId(), vnfd.getVnfProvider(), vnfd.getVnfdVersion()).get();
            Long id = createdVnfd.getId();
            log.debug("Added VNFD with VNFD ID " + vnfd.getVnfdId() + ", version " + vnfd.getVnfdVersion() + " and provider " + vnfd.getVnfProvider() + ". Internal ID assigned to the VNFD: " + id);

            try {
                storeVdus(vnfd, output);
                storeVirtualComputeDescriptors(vnfd, output);
                storeVnfVlds(vnfd, output);
                storeVnfExtCpds(vnfd, output);
                storeVnfDeploymentFlavours(vnfd, output);
                storeConfigurableProperties(vnfd, output);
                storeModifiableAttributes(vnfd, output);
                storeLcmScripts(vnfd, output);
                storeVnfdElementGroups(vnfd, output);
                storeRules(vnfd, output);
                log.debug("Completed VNFD loading.");
                return String.valueOf(id);
            } catch (AlreadyExistingEntityException e) {
                log.error("Error while storing the elements of the VNFD since one of the element already existed: " + e.getMessage());
                vnfdRepository.deleteById(id);
                log.debug("VNFD with ID " + id + " removed from DB.");
                throw new AlreadyExistingEntityException(e.getMessage());
            } catch (Exception e) {
                log.error("General error while storing the elements of the VNFD: " + e.getMessage());
                vnfdRepository.deleteById(id);
                log.debug("VNFD with ID " + id + " removed from DB.");
                throw new Exception(e.getMessage());
            }
        } else {
            log.error("VNF package info " + vnfPackageInfoId + " not found in DB.");
            throw new NotExistingEntityException("VNF package info not found.");
        }
    }

    private void storeVdus(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing VDUs");
        String vnfdId = output.getVnfdId();
        String version = output.getVnfdVersion();
        String vnfProvider = output.getVnfProvider();

        List<Vdu> vdus = input.getVdu();
        for (Vdu vdu : vdus) {
            String vduId = vdu.getVduId();
            if (vduRepository.findByVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersionAndVduId(vnfdId, vnfProvider, version, vduId).isPresent()) {
                log.error("VDU " + vduId + " already existing. Impossible to load a new one.");
                throw new AlreadyExistingEntityException("VDU " + vduId + " already existing.");
            }
            Vdu target = new Vdu(output, vduId, vdu.getVduName(), vdu.getDescription(), vdu.getVirtualComputeDesc(), vdu.getVirtualStorageDesc(), vdu.getBootOrder(),
                    vdu.getSwImageDesc(), vdu.getNfviConstraint(), vdu.getMonitoringParameter(), vdu.getConfigurableProperties());
            vduRepository.saveAndFlush(target);
            log.debug("Stored VDU " + vduId);

            List<VduCpd> cps = vdu.getIntCpd();
            for (VduCpd cp : cps) {
                String cpdId = cp.getCpdId();
                if (vnfVduCpdRepository.findByCpdIdAndVduVduIdAndVduVnfdVnfdIdAndVduVnfdVnfProviderAndVduVnfdVnfdVersion(cpdId, vduId, vnfdId, vnfProvider, version).isPresent()) {
                    log.error("CP " + cpdId + " already existing for VDU " + vduId + ". Impossible to load a new one");
                    throw new AlreadyExistingEntityException("CP " + cpdId + " already existing for VDU " + vduId);
                }
                VduCpd targetCp = new VduCpd(target, cpdId, cp.getLayerProtocol(), cp.getCpRole(),
                        cp.getDescription(), cp.getAddressData(), cp.getIntVirtualLinkDesc(), cp.getBitrateRequirement(),
                        cp.getVirtualNetworkInterfaceRequirements());
                vnfVduCpdRepository.saveAndFlush(targetCp);
                log.debug("Stored CP " + cpdId + " in VDU " + vduId);
            }
        }
    }

    private void storeVirtualComputeDescriptors(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing Virtual Compute Descriptor");
        String vnfdId = output.getVnfdId();
        String version = output.getVnfdVersion();
        String vnfProvider = output.getVnfProvider();

        List<VirtualComputeDesc> vcds = input.getVirtualComputeDesc();
        for (VirtualComputeDesc vcd : vcds) {
            String vcdId = vcd.getVirtualComputeDescId();
            if (vcdRepository.findByVirtualComputeDescIdAndVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersion(vcdId, vnfdId, vnfProvider, version).isPresent()) {
                log.error("VCD " + vcdId + " already existing. Impossible to load a new one");
                throw new AlreadyExistingEntityException("VCD " + vcdId + " already existing.");
            }
            VirtualComputeDesc target = new VirtualComputeDesc(output, vcdId, vcd.getLogicalNode(), vcd.getRequestAdditionalCapabilities(),
                    vcd.getVirtualCpu(), vcd.getVirtualMemory());
            vcdRepository.saveAndFlush(target);
            log.debug("Stored VCD " + vcdId);
        }
    }

    private void storeVnfVlds(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing VNF Virtual Link Descriptor");
        String vnfdId = output.getVnfdId();
        String version = output.getVnfdVersion();
        String vnfProvider = output.getVnfProvider();

        List<VnfVirtualLinkDesc> vlds = input.getIntVirtualLinkDesc();
        for (VnfVirtualLinkDesc vld : vlds) {
            String vldId = vld.getVirtualLinkDescId();
            if (vnfVldRepository.findByVirtualLinkDescIdAndVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersion(vldId, vnfdId, vnfProvider, version).isPresent()) {
                log.error("VLD " + vldId + " already existing. Impossible to load a new one");
                throw new AlreadyExistingEntityException("VLD " + vldId + " already existing.");
            }
            VnfVirtualLinkDesc target = new VnfVirtualLinkDesc(output, vldId, vld.getConnectivityType(),
                    vld.getTestAccess(), vld.getDescription(), vld.getMonitoringParameter());
            vnfVldRepository.saveAndFlush(target);
            log.debug("Stored VLD " + vldId);

            List<VirtualLinkDf> vldDfs = vld.getVirtualLinkDescFlavour();
            for (VirtualLinkDf vldDf : vldDfs) {
                String dfId = vldDf.getFlavourId();
                if (vldDfRepository.findByFlavourIdAndVnfVldVirtualLinkDescIdAndVnfVldVnfdVnfdIdAndVnfVldVnfdVnfProviderAndVnfVldVnfdVnfdVersion(dfId, vldId, vnfdId, vnfProvider, version).isPresent()) {
                    log.error("VL DF " + dfId + " for VLD " + vldId + " already existing. Impossible to load a new one");
                    throw new AlreadyExistingEntityException("VL DF " + dfId + " for VLD " + vldId + " already existing");
                }
                VirtualLinkDf targetDf = new VirtualLinkDf(target, dfId, vldDf.getQos(), vldDf.getServiceAvaibilityLevel());
                vldDfRepository.saveAndFlush(targetDf);
                log.debug("Stored VL flavour " + dfId);
            }
        }
    }

    private void storeVnfExtCpds(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing VNF external CPs");
        String vnfdId = output.getVnfdId();
        String version = output.getVnfdVersion();
        String vnfProvider = output.getVnfProvider();

        List<VnfExtCpd> cps = input.getVnfExtCpd();
        for (VnfExtCpd cp : cps) {
            String cpdId = cp.getCpdId();
            if (vnfExtCpdRepository.findByCpdIdAndVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersion(cpdId, vnfdId, vnfProvider, version).isPresent()) {
                log.error("External CP " + cpdId + " already existing. Impossible to load a new one");
                throw new AlreadyExistingEntityException("External CP " + cpdId + " already existing.");
            }
            VnfExtCpd target = new VnfExtCpd(output, cpdId, cp.getLayerProtocol(), cp.getCpRole(), cp.getDescription(),
                    cp.getAddressData(), cp.getIntVirtualLinkDesc(), cp.getIntCpd(), cp.getVirtualNetworkInterfaceRequirements());
            vnfExtCpdRepository.saveAndFlush(target);
            log.debug("Stored external CP " + cpdId);
        }
    }

    private void storeVnfDeploymentFlavours(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing VNF deployment flavour");
        String vnfdId = output.getVnfdId();
        String version = output.getVnfdVersion();
        String vnfProvider = output.getVnfProvider();

        List<VnfDf> dfs = input.getDeploymentFlavour();
        for (VnfDf df : dfs) {
            String dfId = df.getFlavourId();
            if (vnfDfRepository.findByFlavourIdAndVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersion(dfId, vnfdId, vnfProvider, version).isPresent()) {
                log.error("Deployment flavour " + dfId + " already existing. Impossible to load a new one");
                throw new AlreadyExistingEntityException("Deployment flavour " + dfId + " already existing.");
            }
            VnfDf target = new VnfDf(output, dfId, df.getDescription(), df.getDefaultInstantiationLevelId(),
                    df.getSupportedOperation(), df.getAffinityOrAntiAffinityGroup(), df.getMonitoringParameter(),
                    df.getScalingAspect());
            vnfDfRepository.saveAndFlush(target);
            log.debug("Stored VNF deployment flavour " + dfId);


            List<VduProfile> vps = df.getVduProfile();
            for (VduProfile vp : vps) {
                String vduId = vp.getVduId();
                if (vduProfileRepository.findByVduIdAndVnfDfFlavourIdAndVnfDfVnfdVnfdIdAndVnfDfVnfdVnfProviderAndVnfDfVnfdVnfdVersion(vduId, dfId, vnfdId, vnfProvider, version).isPresent()) {
                    log.error("VDU profile for VDU " + vduId + " in deployment flavour " + dfId + " already existing. Impossible to load a new one");
                    throw new AlreadyExistingEntityException("VDU profile for VDU " + vduId + " in deployment flavour " + dfId + " already existing.");
                }
                VduProfile vpTarget = new VduProfile(target, vduId, vp.getMinNumberOfInstances(), vp.getMaxNumberOfInstances(),
                        vp.getLocalAffinityOrAntiAffinityRule(), vp.getAffinityOrAntiAffinityGroupId());
                vduProfileRepository.saveAndFlush(vpTarget);
                log.debug("VDU profile for VDU " + vduId + " saved.");
            }

            List<VirtualLinkProfile> vlPs = df.getVirtualLinkProfile();
            for (VirtualLinkProfile vlp : vlPs) {
                String vlProfileId = vlp.getVirtualLinkProfileId();
                if (vlProfileRepository.findByVirtualLinkProfileIdAndVnfDfFlavourIdAndVnfDfVnfdVnfdIdAndVnfDfVnfdVnfProviderAndVnfDfVnfdVnfdVersion(vlProfileId, dfId, vnfdId, vnfProvider, version).isPresent()) {
                    log.error("VL profile " + vlProfileId + " in deployment flavour " + dfId + " already existing. Impossible to load a new one");
                    throw new AlreadyExistingEntityException("VL profile " + vlProfileId + " in deployment flavour " + dfId + " already existing.");
                }
                VirtualLinkProfile vlpTarget = new VirtualLinkProfile(target, vlProfileId, vlp.getVirtualLinkDescId(),
                        vlp.getFlavourId(), vlp.getLocalAffinityOrAntiAffinityRule(), vlp.getAffinityOrAntiAffinityGroupId(),
                        vlp.getMaxBitrateRequirements(), vlp.getMinBitrateRequirements());
                vlProfileRepository.saveAndFlush(vlpTarget);
                log.debug("VL profile " + vlProfileId + " saved.");
            }

            List<InstantiationLevel> ils = df.getInstantiationLevel();
            for (InstantiationLevel il : ils) {
                String levelId = il.getLevelId();
                if (instantiationLevelRepository.findByLevelIdAndVnfDfFlavourIdAndVnfDfVnfdVnfdIdAndVnfDfVnfdVnfProviderAndVnfDfVnfdVnfdVersion(levelId, dfId, vnfdId, vnfProvider, version).isPresent()) {
                    log.error("Instantiation level " + levelId + " already existing. Impossible to load a new one.");
                    throw new AlreadyExistingEntityException("Instantiation level " + levelId + " already existing.");
                }
                InstantiationLevel ilTarget = new InstantiationLevel(target, levelId, il.getDescription(), il.getVduLevel(), il.getScaleInfo());
                instantiationLevelRepository.saveAndFlush(ilTarget);
                log.debug("Instantiation level " + levelId + " saved.");
            }

            storeLcmOperationsConfiguration(df.getVnfLcmOperationsConfiguration(), target);
        }
    }

    private void storeLcmOperationsConfiguration(VnfLcmOperationsConfiguration input, VnfDf output) {
    	log.debug("Storing VNF LCM operations configuration");
    	
    	if (input == null) {
    		log.debug("No config to be stored");
    		return;
    	}
    	
    	VnfLcmOperationsConfiguration target = new VnfLcmOperationsConfiguration(output, input.getOperateVnfOpConfig(), input.getTerminateVnfOpConfig());
    	vnfLcmOperationsConfigurationRepository.saveAndFlush(target);
    	
    	if (input.getInstantiateVnfOpConfig() != null) {
    		log.debug("Storing instantiate VNF operations config");
    		InstantiateVnfOpConfig iTarget = new InstantiateVnfOpConfig(target, input.getInstantiateVnfOpConfig().getParameter());
    		instantiateVnfOpConfigRepository.saveAndFlush(iTarget);
    		log.debug("Stored instantiate VNF operations config");
    	}
    	
    	if (input.getScaleVnfOpConfig() != null) {
    		log.debug("Storing scale VNF operations config");
    		ScaleVnfOpConfig sTarget = new ScaleVnfOpConfig(target, input.getScaleVnfOpConfig().getParameter(), input.getScaleVnfOpConfig().isScalingByMoreThanOneStepSupported());
    		scaleVnfOpConfigRepository.saveAndFlush(sTarget);
    		log.debug("Stored scale VNF operations config");
    	}
    	
    	if (input.getScaleVnfToLevelOpConfig() != null) {
    		log.debug("Storing scale VNF to level operations config");
    		ScaleVnfToLevelOpConfig slTarget = new ScaleVnfToLevelOpConfig(target, input.getScaleVnfToLevelOpConfig().getParameter(), input.getScaleVnfToLevelOpConfig().isArbitraryTargetLevelsSupported());
    		scaleVnfToLevelOpConfigRepository.saveAndFlush(slTarget);
    		log.debug("Stored scale VNF to level operations config");
    	}
    	
    	if (input.getChangeVnfFlavourOpConfig() != null) {
    		log.debug("Storing change VNF flavour operations config");
    		ChangeVnfFlavourOpConfig cfTarget = new ChangeVnfFlavourOpConfig(target, input.getChangeVnfFlavourOpConfig().getParameter());
    		changeVnfFlavourOpConfigRepository.saveAndFlush(cfTarget);
    		log.debug("Stored change VNF flavour operations config");
    	}
    	
    	if (input.getHealVnfOpConfig() != null) {
    		log.debug("Storing heal VNF operations config");
    		HealVnfOpConfig hTarget = new HealVnfOpConfig(target, input.getHealVnfOpConfig().getParameter(), input.getHealVnfOpConfig().getCause());
    		healVnfOpConfigRepository.saveAndFlush(hTarget);
    		log.debug("Stored heal VNF operations config");
    	}
    	
    	if (input.getChangeExtVnfConnectivityOpConfig() != null) {
    		log.debug("Storing change external VNF connectivity operations config");
    		ChangeExtVnfConnectivityOpConfig cecTarget = new ChangeExtVnfConnectivityOpConfig(target, input.getChangeExtVnfConnectivityOpConfig().getParameter());
    		changeExtVnfConnectivityOpConfigRepository.saveAndFlush(cecTarget);
    		log.debug("Stored change external VNF connectivity operations config");
    	}
    	
    	log.debug("Stored VNF LCM operations configuration");
    }

    private void storeModifiableAttributes(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing VNF info modifiable attributes");

        if (input.getModifiableAttributes() == null) {
            log.debug("No attributes to be stored");
            return;
        }

        String vnfdId = output.getVnfdId();
        String version = output.getVnfdVersion();
        String vnfProvider = output.getVnfProvider();

        if (vnfInfoModifiableAttributesRepository.findByVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersion(vnfdId, vnfProvider, version).isPresent()) {
            log.error("Modifiable attributes already existing. Impossible to load a new one");
            throw new AlreadyExistingEntityException("Modifiable attributes already existing.");
        }

        VnfInfoModifiableAttributes ma = new VnfInfoModifiableAttributes(output, input.getModifiableAttributes().getExtension(),
                input.getModifiableAttributes().getMetadata());
        vnfInfoModifiableAttributesRepository.saveAndFlush(ma);

        log.debug("Stored VNF info modifiable attributes");
    }
    
    private void storeConfigurableProperties(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
    	log.debug("Storing configurable properties");
    	
    	if (input.getConfigurableProperties() == null) {
    		log.debug("No properties to be stored");
    		return;
    	}
    	
    	String vnfdId = output.getVnfdId();
        String version = output.getVnfdVersion();
        String vnfProvider = output.getVnfProvider();

        if (vnfConfigurablePropertiesRepository.findByVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersion(vnfdId, vnfProvider, version).isPresent()) {
        	log.error("Configurable properties already existing. Impossible to load a new one.");
        	throw new AlreadyExistingEntityException("Configurable properties already existing");
        }
        
        VnfConfigurableProperties vcp = new VnfConfigurableProperties(output, input.getConfigurableProperties().isAutoScalable(), 
        		input.getConfigurableProperties().isAutoHealable(), input.getConfigurableProperties().getAdditionalConfigurableProperty());
        vnfConfigurablePropertiesRepository.saveAndFlush(vcp);
        
        log.debug("Stored VNF configurable properties");  
    }

    private void storeLcmScripts(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing LCM Script");

        List<LifeCycleManagementScript> scripts = input.getLifeCycleManagementScript();
        for (LifeCycleManagementScript s : scripts) {
            LifeCycleManagementScript target = new LifeCycleManagementScript(output, s.getEvent(), s.getScript());
            lcmScriptRepository.saveAndFlush(target);
            log.debug("LCM script saved");
        }
    }

    private void storeVnfdElementGroups(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing VNFD element groups");
        String vnfdId = output.getVnfdId();
        String version = output.getVnfdVersion();
        String vnfProvider = output.getVnfProvider();

        List<VnfdElementGroup> egs = input.getElementGroup();
        for (VnfdElementGroup eg : egs) {
            String egId = eg.getVnfdElementGroupId();
            if (vnfdElementGroupRepository.findByVnfdElementGroupIdAndVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersion(egId, vnfdId, vnfProvider, version).isPresent()) {
                log.error("Element group " + egId + " already existing. Impossible to load a new one");
                throw new AlreadyExistingEntityException("Element group " + egId + " already existing.");
            }
            VnfdElementGroup target = new VnfdElementGroup(output, egId, eg.getDescription(), eg.getVdu(), eg.getVirtualLinkDesc());
            vnfdElementGroupRepository.saveAndFlush(target);
            log.debug("Element group " + egId + " saved");
        }
    }

    private void storeRules(Vnfd input, Vnfd output) throws AlreadyExistingEntityException, Exception {
        log.debug("Storing VNFD autoscaling rules");
        List<Rule> rules = input.getAutoScale();
        for (Rule r : rules) {
            Rule targetRule = new Rule(output);
            ruleRepository.saveAndFlush(targetRule);
            log.debug("Stored VNFD autoscaling rule");
        }
    }

}
