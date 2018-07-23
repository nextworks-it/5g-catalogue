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
package it.nextworks.nfvmano.catalogue.nbi.nsdmanagement;

import it.nextworks.nfvmano.libs.catalogues.interfaces.NsdManagementConsumerInterface;
import it.nextworks.nfvmano.libs.catalogues.interfaces.NsdManagementProviderInterface;
import it.nextworks.nfvmano.libs.catalogues.interfaces.elements.NsdInfo;
import it.nextworks.nfvmano.libs.catalogues.interfaces.elements.PnfdInfo;
import it.nextworks.nfvmano.libs.catalogues.interfaces.enums.NsdChangeType;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.*;
import it.nextworks.nfvmano.libs.common.elements.Filter;
import it.nextworks.nfvmano.libs.common.enums.OperationalState;
import it.nextworks.nfvmano.libs.common.enums.ResponseCode;
import it.nextworks.nfvmano.libs.common.enums.UsageState;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.common.messages.GeneralizedQueryRequest;
import it.nextworks.nfvmano.libs.common.messages.SubscribeRequest;
import it.nextworks.nfvmano.libs.descriptors.common.elements.LifeCycleManagementScript;
import it.nextworks.nfvmano.libs.descriptors.common.elements.Rule;
import it.nextworks.nfvmano.libs.descriptors.common.elements.VirtualLinkDf;
import it.nextworks.nfvmano.libs.descriptors.common.elements.VirtualLinkProfile;
import it.nextworks.nfvmano.libs.descriptors.nsd.*;
import it.nextworks.nfvmano.libs.descriptors.onboardedvnfpackage.OnboardedVnfPkgInfo;
import it.nextworks.nfvmano.libs.descriptors.vnfd.VnfConfigurableProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.Vnfd;
import it.nextworks.nfvmano.catalogue.nbi.nsdmanagement.repositories.*;
import it.nextworks.nfvmano.catalogue.nbi.vnfpackagemanagement.VnfPackageManagementService;
import it.nextworks.nfvmano.catalogue.nbi.vnfpackagemanagement.repositories.LifeCycleManagementScriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


/**
 * This service manages the external requests for NSD management 
 * and interacts with the internal repositories of the NSD catalog.
 * Its methods are invoked by the NsdManagementRestController class.
 * 
 * @author nextworks
 *
 */
@Service
public class NsdManagementService implements NsdManagementProviderInterface {

	private static final Logger log = LoggerFactory.getLogger(NsdManagementService.class);
	
	@Autowired
	NsdRepository nsdRepository;
	
	@Autowired
	SapdRepository sapdRepository;
	
	@Autowired
	NsVirtualLinkDescriptorRepository nsdVldRepository;
	
	@Autowired
	VirtualLinkDfRepository vldDfRepository;
	
	@Autowired
	VnffgdRepository forwardingGraphRepository;
	
	@Autowired
	NfpdRepository forwardingPathRepository;
	
	@Autowired
	RuleRepository ruleRepository;
	
	@Autowired
	MonitoredDataRepository monitoredDataRepository;
	
	@Autowired
	NsdfRepository nsDeploymentFlavourRepository;
	
	@Autowired
	VnfProfileRepository vnfProfileRepository;
	
	@Autowired
	PnfProfileRepository pnfProfileRepository;
	
	@Autowired
	VirtualLinkProfileRepository vlProfileRepository;
	
	@Autowired
	NsScalingAspectRepository nsScalingAspectRepository;
	
	@Autowired
	NsLevelRepository nsLevelRepository;
	
	@Autowired
	NsVirtualLinkConnectivityRepository nsVlConnectivityRepository;
	
	@Autowired
	VirtualLinkToLevelMappingRepository vlToLevelMappingRepository;
	
	@Autowired
	NsdInfoRepository nsdInfoRepository;
	
	@Autowired
	NsProfileRepository nsProfileRepository;
	
	@Autowired
	DependenciesRepository dependenciesRepository;
	
	@Autowired
    LifeCycleManagementScriptRepository lcmScriptRepository;
	
	@Autowired
	NsdManagementSubscriptionRepository nsdManagementSubscriptionRepository;
	
	@Autowired
	PnfdRepository pnfdRepository;
	
	@Autowired
	PnfdInfoRepository pnfdInfoRepository;
	
	@Autowired
	PnfExtCpdRepository pnfExtCpdRepository;
	
	@Autowired
	private VnfPackageManagementService vnfPackageManagement;
	
	public NsdManagementService() {	}

	@Override
	public String onboardNsd(OnboardNsdRequest request) throws MethodNotImplementedException,
			MalformattedElementException, AlreadyExistingEntityException, FailedOperationException {
		log.debug("Received on board NSD request");
		request.isValid();
		Nsd nsd = request.getNsd();
		nsd.isValid();
		
		//check if an NSD with same ID and version already exist
		Optional<Nsd> oldNsdOpt = nsdRepository.findByNsdIdentifierAndVersion(nsd.getNsdIdentifier(), nsd.getVersion());
		if (oldNsdOpt.isPresent()) {
			log.warn("Found an old NSD with same ID " + nsd.getNsdIdentifier() + " and same version " + nsd.getVersion() + ". Impossible to load a new one.");
			throw new AlreadyExistingEntityException("Found an old NSD with same ID " + nsd.getNsdIdentifier() + " and same version " + nsd.getVersion() + ". Impossible to load a new one.");
		}
		
		//check if the PNFD info are present
		List<String> pnfdInfoIds = new ArrayList<>();
		List<String> pnfdIds = nsd.getPnfdId();
		//Here I am assuming that the version of the PNFD is the same of the NSD. 
		//It is not strictly correct, but I do not see any other solution since the PNFD version is not specified in the NSD.
		String version = nsd.getVersion();
		for (String pnfdId : pnfdIds) {
			log.debug("Searching for PNFD info with PNFD ID " + pnfdId + " and version " + version);
			if (pnfdInfoRepository.findByPnfdIdAndVersion(pnfdId, version).isPresent()) {
				String pnfdInfoId = pnfdInfoRepository.findByPnfdIdAndVersion(pnfdId, version).get().getPnfdInfoId();
				log.debug("PNFD Info found. PNFD Info ID: " + pnfdInfoId);
				pnfdInfoIds.add(pnfdInfoId);
			} else {
				log.warn("PNFD Info not found for PNFD " + pnfdId + " and version " + version);
				throw new FailedOperationException("PNFD Info not found for PNFD " + pnfdId + " and version " + version);
			}
		}
		
		//The returned ID is unique for the NSD instance.
		try {
			String internalId = storeNsd(nsd);
			log.debug("NSD correctly stored in DB - NSD instance identifier: " + internalId);
			
			//It also needs to create an instance of internal nsd info
			if ((nsdInfoRepository.findByNsdIdAndVersion(nsd.getNsdIdentifier(), nsd.getVersion()).isPresent()) ||
					(nsdInfoRepository.findByNsdInfoId(internalId).isPresent())) {
				log.error("NSD info already present in DB. Error.");
				nsdRepository.delete(nsdRepository.findByNsdIdentifierAndVersion(nsd.getNsdIdentifier(), nsd.getVersion()).get());
				throw new AlreadyExistingEntityException("NSD info already present in DB. Error.");
			}
			InternalNsdInfo nsdInfo = new InternalNsdInfo(internalId, nsd.getNsdIdentifier(), nsd.getVersion(), 
					null, OperationalState.ENABLED, UsageState.NOT_IN_USE, false, pnfdInfoIds, request.getUserDefinedData());
			nsdInfoRepository.saveAndFlush(nsdInfo);
			
			log.debug("NSD info stored in DB.");
			notify(new NsdOnBoardingNotification(internalId, nsd.getNsdIdentifier()));
			return internalId;
		} catch (Exception e) {
			throw new FailedOperationException("Generic failure when uploading NSD.");
		}
	}

	@Override
	public synchronized void enableNsd(EnableNsdRequest request) throws MethodNotImplementedException, MalformattedElementException,
			NotExistingEntityException, FailedOperationException {
		String nsdInfoId = request.getNsdInfoId();
		log.debug("Received enable NSD request for NSD info " + nsdInfoId);
		request.isValid();
		InternalNsdInfo nsdInfo = findNsdInfo(nsdInfoId);
		if (nsdInfo.getOperationalState() == OperationalState.ENABLED) {
			log.error("NSD info " + nsdInfoId + " already enabled.");
			throw new FailedOperationException("NSD info " + nsdInfoId + " already enabled.");
		}
		if (nsdInfo.isDeletionPending()) {
			log.error("NSD info " + nsdInfoId + " is under deletion. Impossible to enable.");
			throw new FailedOperationException("NSD info " + nsdInfoId + " is under deletion. Impossible to enable.");
		}
		nsdInfo.setOperationalState(OperationalState.ENABLED);
		nsdInfoRepository.saveAndFlush(nsdInfo);
		log.debug("NSD info " + nsdInfoId + " has been enabled.");

		notify(new NsdChangeNotification(nsdInfoId, NsdChangeType.OPERATIONAL_STATE_CHANGE, 
				OperationalState.ENABLED, false));
	}

	@Override
	public void disableNsd(DisableNsdRequest request) throws MethodNotImplementedException,
			MalformattedElementException, NotExistingEntityException, FailedOperationException {
		String nsdInfoId = request.getNsdInfoId();
		log.debug("Received disable NSD request for NSD info " + nsdInfoId);
		request.isValid();
		InternalNsdInfo nsdInfo = findNsdInfo(nsdInfoId);
		if (nsdInfo.getOperationalState() == OperationalState.DISABLED) {
			log.error("NSD info " + nsdInfoId + " already disabled.");
			throw new FailedOperationException("NSD info " + nsdInfoId + " already disabled.");
		}
		if (nsdInfo.isDeletionPending()) {
			log.error("NSD info " + nsdInfoId + " is under deletion. Impossible to disable.");
			throw new FailedOperationException("NSD info " + nsdInfoId + " is under deletion. Impossible to disable.");
		}
		nsdInfo.setOperationalState(OperationalState.DISABLED);
		nsdInfoRepository.saveAndFlush(nsdInfo);
		log.debug("NSD info " + nsdInfoId + " has been disabled.");
		notify(new NsdChangeNotification(nsdInfoId, NsdChangeType.OPERATIONAL_STATE_CHANGE, 
				OperationalState.DISABLED, false));
	}

	@Override
	public String updateNsd(UpdateNsdRequest request) throws MethodNotImplementedException,
			MalformattedElementException, AlreadyExistingEntityException, NotExistingEntityException, FailedOperationException {
		
		log.debug("Received update NSD request");
		request.isValid();
		
		InternalNsdInfo oldNsdInfo = findNsdInfo(request.getNsdInfoId());
		
		if (request.getNsd() != null) {
			log.debug("Request to update an existing NSD");
			Nsd nsd = request.getNsd();
			String nsdId = nsd.getNsdIdentifier();
			String version = nsd.getVersion();
			if (nsdRepository.findByNsdIdentifierAndVersion(nsdId, version).isPresent()) {
				log.error("An NSD with the same NSD ID and version is already existing. Not possible to create a new one.");
				throw new FailedOperationException("An NSD with the same NSD ID and version is already existing. Not possible to create a new one.");
			}
			String oldNsdInfoId = request.getNsdInfoId();
			
			//Store the new NSD and retrieves the new NSD info ID
			String internalId = storeNsd(nsd);
			log.debug("Updated NSD correctly stored in DB - NSD instance identifier: " + internalId);
			
			//Store new NSD Info with previous version
			if ((nsdInfoRepository.findByNsdIdAndVersion(nsd.getNsdIdentifier(), nsd.getVersion()).isPresent()) ||
					(nsdInfoRepository.findByNsdInfoId(internalId).isPresent())) {
				log.error("NSD info already present in DB. Error.");
				nsdRepository.delete(nsdRepository.findByNsdIdentifierAndVersion(nsd.getNsdIdentifier(), nsd.getVersion()).get());
				throw new FailedOperationException("NSD info already present in DB. Error.");
			}
			InternalNsdInfo nsdInfo = new InternalNsdInfo(internalId, nsd.getNsdIdentifier(), nsd.getVersion(), 
					oldNsdInfoId, OperationalState.ENABLED, UsageState.NOT_IN_USE, false, oldNsdInfo.getPnfdInfoId(), request.getUserDefinedData());
			nsdInfoRepository.saveAndFlush(nsdInfo);
			log.debug("New NSD info for updated NSD stored in DB.");
			notify(new NsdOnBoardingNotification(internalId, nsd.getNsdIdentifier()));
			return internalId;
			
		} else {
			log.debug("Request to update user defined data");
			oldNsdInfo.mergeUserDefinedData(request.getUserDefinedData());
			nsdInfoRepository.saveAndFlush(oldNsdInfo);
			log.debug("NSD info " + request.getNsdInfoId() + " updated with new user data.");
			return request.getNsdInfoId();
		}
	}

	@Override
	public synchronized DeleteNsdResponse deleteNsd(DeleteNsdRequest request) throws MethodNotImplementedException,
			MalformattedElementException, NotExistingEntityException, FailedOperationException {
		log.debug("Received delete NSD request");
		request.isValid();
		List<String> nsdInfoIds = request.getNsdInfoId();
		
		//This list is used to store the NSD info IDs which have been actually removed
		List<String> deletedNsdInfoIds = new ArrayList<>();
		
		//This response code is used to keep trace of the whole result
		ResponseCode rc = ResponseCode.OK;
		
		for (String nsdInfoId : nsdInfoIds) {
			log.debug("Trying to remove NSD Info " + nsdInfoId);
			try {
				InternalNsdInfo nsdInfo = findNsdInfo(nsdInfoId);
				if (!(request.isApplyOnAllVersions())) {
					//Just a single NSD info must be removed
					removeNsdInfo(nsdInfo);
					deletedNsdInfoIds.add(nsdInfoId);
				} else {
					// all the NSD must be removed
					String nsdId = nsdInfo.getNsdId();
					Set<Nsd> nsds = nsdRepository.findByNsdIdentifier(nsdId);
					for (Nsd nsd : nsds) {
						String version = nsd.getVersion();
						InternalNsdInfo currentNsdInfo = findNsdInfo(nsdId, version);
						removeNsdInfo(currentNsdInfo);
					}
					deletedNsdInfoIds.add(nsdInfoId);
				}
			} catch (NotExistingEntityException exception) {
				log.debug("Setting overall result code to failed");
				rc = ResponseCode.FAILED_GENERIC;
			}
		}
		if (rc == ResponseCode.FAILED_GENERIC) {
			log.warn("Some of the NSDs have been not removed.");
		} else {
			log.debug("All the NSDs have been correctly removed.");
		}
		return new DeleteNsdResponse(deletedNsdInfoIds);

	}

	@Override
	public QueryNsdResponse queryNsd(GeneralizedQueryRequest request) throws MethodNotImplementedException,
			MalformattedElementException, NotExistingEntityException, FailedOperationException {
		log.debug("Received query NSD request");
		request.isValid();
		
		//At the moment the only filters accepted are:
		//1. NSD ID && version
		//2. NSD Instance ID
		//3. empty filter
		//No attribute selector is supported at the moment
		
		Filter filter = request.getFilter();
		List<String> attributeSelector = request.getAttributeSelector();
		
		List<NsdInfo> queryResult = new ArrayList<>();
		
		if ((attributeSelector == null) || (attributeSelector.isEmpty())) {
			Map<String,String> fp = filter.getParameters();
			if (fp.size()==2 && fp.containsKey("NSD_ID") && fp.containsKey("NSD_VERSION")) {
				String nsdId = fp.get("NSD_ID");
				String version = fp.get("NSD_VERSION");
				NsdInfo nsdInfo = buildNsdInfo(nsdId, version);
				queryResult.add(nsdInfo);
				log.debug("Added NSD info for NSD " + nsdId + " - version " + version);
			} else if (fp.size()==1 && fp.containsKey("NSD_INFO_ID")) {
				String nsdInfoId = fp.get("NSD_INFO_ID");
				NsdInfo nsdInfo = buildNsdInfo(nsdInfoId);
				queryResult.add(nsdInfo);
				log.debug("Added NSD info for NSD Info " + nsdInfoId);
			} else if (fp.isEmpty()) {
				log.debug("Query with empty filter. Returning all the elements");
				List<NsdInfo> nsdInfos = buildNsdInfo();
				queryResult.addAll(nsdInfos);
				log.debug("Added all NSD infos found in DB");
			} else {
				log.error("Received query NSD with not supported filter.");
				throw new MalformattedElementException("Received query NSD with not supported filter.");
			}
			return new QueryNsdResponse(queryResult);
		} else {
			log.error("Received query NSD with attribute selector. Not supported at the moment.");
			throw new MethodNotImplementedException("Received query NSD with attribute selector. Not supported at the moment.");
		}
	}

	@Override
	public synchronized String subscribeNsdInfo(SubscribeRequest request, NsdManagementConsumerInterface consumer)
			throws MethodNotImplementedException, MalformattedElementException, FailedOperationException {
		log.debug("Received subscribe for NSD info request");
		request.isValid();
		NsdManagementConsumer abstractConsumer = (NsdManagementConsumer)consumer;
		switch (abstractConsumer.getType()) {
		case REST: {
			log.debug("Subscription for a REST consumer");
			NsdManagementRestConsumer restConsumer = (NsdManagementRestConsumer)abstractConsumer;
			NsdManagementSubscription subscription = new NsdManagementSubscription(restConsumer);
			this.nsdManagementSubscriptionRepository.saveAndFlush(subscription);
			log.debug("NSD management subscription done: ID " + subscription.getId().toString());
			return subscription.getId().toString();
		}

		default:
			log.error("NSD management consumer type not supported.");
			throw new MethodNotImplementedException("NSD management consumer type not supported.");
		}
	}

	@Override
	public synchronized void unsubscribeNsdInfo(String subscriptionId) throws MethodNotImplementedException, MalformattedElementException,
			NotExistingEntityException, FailedOperationException {
		log.debug("Received unsubscribe for NSD info request: ID " + subscriptionId);
		Optional<NsdManagementSubscription> subscriptionOpt = nsdManagementSubscriptionRepository.findById(Long.parseLong(subscriptionId));
		if (subscriptionOpt.isPresent()) {
			NsdManagementSubscription subscription = subscriptionOpt.get();
			nsdManagementSubscriptionRepository.delete(subscription);
			log.debug("Subscription removed");
		} else {
			log.error("NSD management subscription " + subscriptionId + " not found. Impossible to remove");
			throw new NotExistingEntityException("NSD management subscription " + subscriptionId + " not found. Impossible to remove");
		}
	}

	@Override
	public String onboardPnfd(OnboardPnfdRequest request) throws MethodNotImplementedException,
			MalformattedElementException, AlreadyExistingEntityException, FailedOperationException {
		log.debug("Received onboard PNFD request.");
		request.isValid();
		Pnfd pnfd = request.getPnfd(); 
		
		//check if a PNFD with same ID and version already exist
		Optional<Pnfd> oldPnfdOpt = pnfdRepository.findByPnfdIdAndVersion(pnfd.getPnfdId(), pnfd.getVersion());
		if (oldPnfdOpt.isPresent()) {
			log.warn("Found an old PNFD with same ID " + pnfd.getPnfdId() + " and same version " + pnfd.getVersion() + ". Impossible to load a new one.");
			throw new AlreadyExistingEntityException("Found an old PNFD with same ID " + pnfd.getPnfdId() + " and same version " + pnfd.getVersion() + ". Impossible to load a new one.");
		}
		
		String internalId = storePnfd(pnfd);
		log.debug("PNFD correctly stored in DB - PNFD instance identifier: " + internalId);
		
		//It also needs to create an instance of a PNFD info
		if (pnfdInfoRepository.findByPnfdIdAndVersion(pnfd.getPnfdId(), pnfd.getVersion()).isPresent() ||
		pnfdInfoRepository.findByPnfdInfoId(internalId).isPresent()) {
			log.error("PNFD info already present in DB. Error.");
			pnfdRepository.delete(pnfdRepository.findByPnfdIdAndVersion(pnfd.getPnfdId(), pnfd.getVersion()).get());
			throw new AlreadyExistingEntityException("PNFD info already present in DB. Error.");
		}
		PnfdInfo pnfdInfo = new PnfdInfo(internalId, pnfd.getPnfdId(), 
				null, //name 
				pnfd.getVersion(),
				pnfd.getProvider(),
				null, //pnfd
				null, //previousPnfdVersionId
				UsageState.NOT_IN_USE, 
				false, //deletionPending
				request.getUserDefinedData());
		pnfdInfoRepository.saveAndFlush(pnfdInfo);
		log.debug("PNFD info stored in DB.");
		return internalId;
	}

	@Override
	public String updatePnfd(UpdatePnfdRequest request) throws MethodNotImplementedException,
			MalformattedElementException, NotExistingEntityException, AlreadyExistingEntityException, FailedOperationException {
		log.debug("Received update PNFD request.");
		request.isValid();
		
		PnfdInfo oldPnfdInfo = findPnfdInfo(request.getPnfdInfoId());
		
		if (request.getPnfd() != null) {
			log.debug("Request to update an existing PNFD");
			Pnfd pnfd = request.getPnfd();
			String pnfdId = pnfd.getPnfdId();
			String version = pnfd.getVersion();
			if (pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).isPresent()) {
				log.error("A PNFD with the same PNFD ID and version is already existing. Not possible to create a new one.");
				throw new FailedOperationException("A PNFD with the same PNFD ID and version is already existing. Not possible to create a new one.");
			}
			
			//Store the new PNFD and retrieves the new PNFD info ID
			String internalId = storePnfd(pnfd);
			log.debug("Updated PNFD correctly stored in DB - PNFD instance identifier: " + internalId);
			
			//Store new PNFD Info
			if ((pnfdInfoRepository.findByPnfdIdAndVersion(pnfd.getPnfdId(), pnfd.getVersion()).isPresent()) ||
					(pnfdInfoRepository.findByPnfdInfoId(internalId).isPresent())) {
				log.error("PNFD info already present in DB. Error.");
				pnfdRepository.delete(pnfdRepository.findByPnfdIdAndVersion(pnfd.getPnfdId(), pnfd.getVersion()).get());
				throw new AlreadyExistingEntityException("PNFD info already present in DB. Error.");
			}
			PnfdInfo pnfdInfo = new PnfdInfo(internalId, 
					pnfdId, 
					null, //name
					version, 
					pnfd.getVersion(), 
					null, //pnfd 
					oldPnfdInfo.getVersion(), 
					UsageState.NOT_IN_USE,
					false, //deletionPending,
					oldPnfdInfo.getUserDefinedData());
			pnfdInfoRepository.saveAndFlush(pnfdInfo);
			log.debug("New PNFD info for updated PNFD stored in DB.");
			return internalId;
			
		} else {
			log.debug("Request to update user defined data");
			oldPnfdInfo.mergeUserDefinedData(request.getUserDefinedData());
			pnfdInfoRepository.saveAndFlush(oldPnfdInfo);
			log.debug("PNFD info " + request.getPnfdInfoId() + " updated with new user data.");
			return request.getPnfdInfoId();
		}	
	}

	@Override
	public DeletePnfdResponse deletePnfd(DeletePnfdRequest request) throws MethodNotImplementedException,
			MalformattedElementException, NotExistingEntityException, FailedOperationException {
		log.debug("Received delete PNFD request.");
		request.isValid();
		
		List<String> pnfdInfoIds = request.getPnfdInfoId();
		
		//This list is used to store the PNFD info IDs which have been actually removed
		List<String> deletedPnfdInfoIds = new ArrayList<>();
		
		//This response code is used to keep trace of the whole result
		ResponseCode rc = ResponseCode.OK;
		
		for (String pnfdInfoId : pnfdInfoIds) {
			log.debug("Trying to remove PNFD Info " + pnfdInfoId);
			try {
				if (pnfdInfoRepository.findByPnfdInfoId(pnfdInfoId).isPresent()) {
					PnfdInfo pnfdInfo = pnfdInfoRepository.findByPnfdInfoId(pnfdInfoId).get();
					if (!(request.isApplyOnAllVersions())) {
						//Just a single PNFD info must be removed
						removePnfdInfo(pnfdInfo);
						deletedPnfdInfoIds.add(pnfdInfoId);
					} else {
						// all the PNFD must be removed
						String pnfdId = pnfdInfo.getPnfdId();
						Set<Pnfd> pnfds = pnfdRepository.findByPnfdId(pnfdId);
						for (Pnfd pnfd : pnfds) {
							String version = pnfd.getVersion();
							PnfdInfo currentPnfdInfo = pnfdInfoRepository.findByPnfdIdAndVersion(pnfdId, version).get();
							removePnfdInfo(currentPnfdInfo);
						}
						deletedPnfdInfoIds.add(pnfdInfoId);
					}
				} else {
					log.warn("PNFD info " + pnfdInfoId + " not found in internal DB.");
				}
			} catch (NotExistingEntityException exception) {
				log.debug("Setting overall result code to failed");
				rc = ResponseCode.FAILED_GENERIC;
			}
		}
		if (rc == ResponseCode.FAILED_GENERIC) {
			log.warn("Some of the PNFDs have been not removed.");
		} else {
			log.debug("All the PNFDs have been correctly removed.");
		}
		return new DeletePnfdResponse(deletedPnfdInfoIds);

	}

	@Override
	public QueryPnfdResponse queryPnfd(GeneralizedQueryRequest request) throws MethodNotImplementedException,
			MalformattedElementException, NotExistingEntityException, FailedOperationException {
		log.debug("Received query PNFD request.");
		request.isValid();
		
		//At the moment the only filters accepted are:
		//1. PNFD ID && version
		//2. PNFD Info ID
		//3. empty filter
		//No attribute selector is supported at the moment
		
		Filter filter = request.getFilter();
		List<String> attributeSelector = request.getAttributeSelector();

		List<PnfdInfo> queryResult = new ArrayList<>();
		if ((attributeSelector == null) || (attributeSelector.isEmpty())) {
			Map<String,String> fp = filter.getParameters();
			if (fp.size()==2 && fp.containsKey("PNFD_ID") && fp.containsKey("PNFD_VERSION")) {
				String pnfdId = fp.get("PNFD_ID");
				String version = fp.get("PNFD_VERSION");
				PnfdInfo pnfdInfo = buildPnfdInfo(pnfdId, version);
				queryResult.add(pnfdInfo);
				log.debug("Added PNFD info for PNFD " + pnfdId + " - version " + version);
			} else if (fp.size()==1 && fp.containsKey("PNFD_INFO_ID")) {
				String pnfdInfoId = fp.get("PNFD_INFO_ID");
				PnfdInfo pnfdInfo = buildPnfdInfo(pnfdInfoId);
				queryResult.add(pnfdInfo);
				log.debug("Added PNFD info for PNFD Info " + pnfdInfoId);
			} else if (fp.isEmpty()) {
				log.debug("Query with empty filter. Returning all the elements");
				List<PnfdInfo> pnfdInfos = buildPnfdInfo();
				queryResult.addAll(pnfdInfos);
				log.debug("Added all PNFD infos found in DB");
			} else {
				log.error("Received query NSD with not supported filter.");
				throw new MalformattedElementException("Received query NSD with not supported filter.");
			}
			return new QueryPnfdResponse(queryResult);
		} else {
			log.error("Received query PNFD with attribute selector. Not supported at the moment.");
			throw new MethodNotImplementedException("Received query PNFD with attribute selector. Not supported at the moment.");
		}
	}
	
	//Methods to handle notifications from external entities about NS instances lifecycle
	/**
	 * Updates the NSD info repository following the creation of a new NS instance
	 * 
	 * @param nsdInfoId NSD INFO ID associated to the new NS instance
	 * @throws NotExistingEntityException in case the NSD INFO ID is not found
	 */
	public synchronized void notifyNsInstanceCreation (String nsdInfoId, String nsId) throws NotExistingEntityException {
		InternalNsdInfo nsdInfo = findNsdInfo(nsdInfoId);
		if (nsdInfo.getUsageState() == UsageState.NOT_IN_USE)
			nsdInfo.setUsageState(UsageState.IN_USE);
		nsdInfo.addNsInstanceId(nsId);
		nsdInfoRepository.saveAndFlush(nsdInfo);
		log.debug("Updated NSD info " + nsdInfoId + " for the instantiation of the new NS instance " + nsId);
		
		List<String> pnfdInfoIds = nsdInfo.getPnfdInfoId();
		for (String pnfdInfoId : pnfdInfoIds) {
			try {
				PnfdInfo pnfdInfo = findPnfdInfo(pnfdInfoId);
				if (pnfdInfo.getUsageState() == UsageState.NOT_IN_USE)
					pnfdInfo.setUsageState(UsageState.IN_USE);
				pnfdInfo.addNsInstanceId(nsId);
				pnfdInfoRepository.saveAndFlush(pnfdInfo);
				log.debug("Updated PNF info " + pnfdInfoId + " for the instantiation of the new NS instance " + nsId);
			} catch (NotExistingEntityException e) {
				log.warn("PNFD info " + pnfdInfoId + " not found in DB. Unable to update its status. This is a non-blocking issue.");
				continue;
			}
		}
	}
	
	/**
	 * 
	 * @param nsId nsInstanceId
	 * @throws NotExistingEntityException
	 */
	public synchronized void notifyNsInstanceDeletion (String nsdInfoId, String nsId) throws NotExistingEntityException {
		InternalNsdInfo nsdInfo = findNsdInfo(nsdInfoId);
		nsdInfo.removeNsInstanceId(nsId);
		if (nsdInfo.getNsInstanceId().isEmpty()) 
			nsdInfo.setUsageState(UsageState.NOT_IN_USE);
		nsdInfoRepository.saveAndFlush(nsdInfo);
		log.debug("Updated NSD info " + nsdInfoId + " for the deletion of the NS instance " + nsId);
		
		if ((nsdInfo.getUsageState()==(UsageState.NOT_IN_USE)) && (nsdInfo.isDeletionPending())) {
			log.debug("The NSD info can be now removed");
			removeNsdFromDb(nsdInfo);
		}
		
		List<String> pnfdInfoIds = nsdInfo.getPnfdInfoId();
		for (String pnfdInfoId : pnfdInfoIds) {
			try {
				PnfdInfo pnfdInfo = findPnfdInfo(pnfdInfoId);
				pnfdInfo.removeNsInstanceId(nsId);
				if (pnfdInfo.getNsInstanceId().isEmpty())
					pnfdInfo.setUsageState(UsageState.NOT_IN_USE);
				pnfdInfoRepository.saveAndFlush(pnfdInfo);
				log.debug("Updated PNF info " + pnfdInfoId + " for the deletion of the NS instance " + nsId);
				if ((pnfdInfo.getUsageState()==(UsageState.NOT_IN_USE)) && (pnfdInfo.isDeletionPending())) {
					log.debug("The PNFD info can be now removed");
					removePnfdFromDb(pnfdInfo);
				}
			} catch (NotExistingEntityException e) {
				log.warn("PNFD info " + pnfdInfoId + " not found in DB. Unable to update its status. This is a non-blocking issue.");
				continue;
			}
		}
		
	}
	//End of methods to handle notifications from external entities about NS instances lifecycle
	
	
	//Methods to manage and store PNFD entities
	private synchronized String storePnfd(Pnfd input) throws AlreadyExistingEntityException {
		String pnfdId = input.getPnfdId();
		String version = input.getVersion();
		
		Pnfd output = new Pnfd(pnfdId, input.getProvider(), version, input.getSecurity());
		pnfdRepository.saveAndFlush(output);
		
		Pnfd createdPnfd = pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).get();
		Long id = createdPnfd.getId();
		log.debug("Added PNFD with ID " + pnfdId + " and version " + version + ". Internal ID assigned to the PNFD instance: " + id);
		
		try {
			storePnfdExtCp(input, output);
		} catch (AlreadyExistingEntityException e) {
			log.error("Error while storing the elements of the PNFD since one of the element already existed: " + e.getMessage());
			pnfdRepository.delete(id);
			log.debug("PNFD with ID " + pnfdId + " and version " + version + " removed from the DB.");
			throw new AlreadyExistingEntityException(e.getMessage());
		}
		
		return String.valueOf(id);
	}
	
	private void storePnfdExtCp(Pnfd input, Pnfd output) throws AlreadyExistingEntityException {
		List<PnfExtCpd> cpds = input.getPnfExtCp();
		for (PnfExtCpd cpd : cpds) {
			log.debug("Storing PNF External CPD with ID " + cpd.getCpdId());
			Optional<PnfExtCpd> optCp = pnfExtCpdRepository.findByCpdId(cpd.getCpdId());
			if (optCp.isPresent()) {
				log.warn("PNF external CPD with ID " + cpd.getCpdId() + " already present in DB.");
				throw new AlreadyExistingEntityException("PNF external CPD with ID " + cpd.getCpdId() + " already present in DB.");
			}
			PnfExtCpd target = new PnfExtCpd(output, cpd.getCpdId(), cpd.getLayerProtocol(), cpd.getCpRole(),
					cpd.getDescription(), cpd.getAddressData());
			pnfExtCpdRepository.saveAndFlush(target);
		}
		log.debug("Stored all PNFD external CP for PNFD " + output.getPnfdId());
	}
	
	private PnfdInfo buildPnfdInfo(String pnfdInfoId) throws NotExistingEntityException {
		if (pnfdInfoRepository.findByPnfdInfoId(pnfdInfoId).isPresent()) {
			PnfdInfo pnfdInfo = pnfdInfoRepository.findByPnfdInfoId(pnfdInfoId).get();
			String pnfdId = pnfdInfo.getPnfdId();
			String version = pnfdInfo.getVersion();
			if (pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).isPresent()) {
				Pnfd pnfd = pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).get();
				pnfdInfo.setPnfd(pnfd);
				return pnfdInfo;
			} else {
				log.warn("PNFD with ID " + pnfdId + " not found in DB.");
				throw new NotExistingEntityException("PNFD with ID " + pnfdId + " not found in DB.");
			}
		} else {
			log.warn("PNFD info with ID " + pnfdInfoId + " not found in DB.");
			throw new NotExistingEntityException("PNFD info with ID " + pnfdInfoId + " not found in DB.");
		}
	}
	
	private PnfdInfo buildPnfdInfo(String pnfdId, String version) throws NotExistingEntityException {
		if (pnfdInfoRepository.findByPnfdIdAndVersion(pnfdId, version).isPresent()) {
			PnfdInfo pnfdInfo = pnfdInfoRepository.findByPnfdIdAndVersion(pnfdId, version).get();
			if (pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).isPresent()) {
				Pnfd pnfd = pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).get();
				pnfdInfo.setPnfd(pnfd);
				return pnfdInfo;
			} else {
				log.warn("PNFD with ID " + pnfdId + " not found in DB.");
				throw new NotExistingEntityException("PNFD with ID " + pnfdId + " not found in DB.");
			}
		} else {
			log.warn("PNFD info with PNFD ID " + pnfdId + " and version " + version + " not found in DB.");
			throw new NotExistingEntityException("PNFD info with PNFD ID " + pnfdId + " and version " + version + " not found in DB.");
		}
	}
	
	private List<PnfdInfo> buildPnfdInfo() throws NotExistingEntityException {
		List<PnfdInfo> queryResult = new ArrayList<>();
		List<PnfdInfo> pnfdInfos = pnfdInfoRepository.findAll();
		for (PnfdInfo pnfdInfo : pnfdInfos) {
			String pnfdId = pnfdInfo.getPnfdId();
			String version = pnfdInfo.getVersion();
			if (pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).isPresent()) {
				Pnfd pnfd = pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).get();
				pnfdInfo.setPnfd(pnfd);
				queryResult.add(pnfdInfo);
			} else {
				log.warn("PNFD info with PNFD ID " + pnfdId + " and version " + version + " not found in DB.");
			}
		}
		return queryResult;
	}
	
	private void removePnfdInfo(PnfdInfo pnfdInfo) throws NotExistingEntityException {
		if (pnfdInfo.getUsageState()==UsageState.IN_USE) {
			log.warn("Request to delete an PNFD still in use. Setting it to pending delete.");
			pnfdInfo.setDeletionPending(true);
			pnfdInfoRepository.save(pnfdInfo);
		} else {
			//the PNFD info is not in use and it can be deleted
			//it removes also the associated PNFD
			removePnfdFromDb(pnfdInfo);
		}
	}
	
	private void removePnfdFromDb(PnfdInfo pnfdInfo) throws NotExistingEntityException {
		String pnfdId = pnfdInfo.getPnfdId();
		String version = pnfdInfo.getVersion();
		if (pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).isPresent()) {
			Pnfd pnfd = pnfdRepository.findByPnfdIdAndVersion(pnfdId, version).get();
			pnfdRepository.delete(pnfd);
			log.debug("Removed PNFD " + pnfdId + " - version " + version);
		} else {
			log.error("PNFD " + pnfdId + " - version " + version + " not found");
			throw new NotExistingEntityException("PNFD " + pnfdId + " - version " + version + " not found");
		}
		pnfdInfoRepository.delete(pnfdInfo);
		log.debug("Removed PNFD info");
	}
	
	public PnfdInfo findPnfdInfo(String pnfdInfoId) throws NotExistingEntityException {
		if (!(pnfdInfoRepository.findByPnfdInfoId(pnfdInfoId).isPresent())) {
			log.error("PNFD info " + pnfdInfoId + " not found in internal DB.");
			throw new NotExistingEntityException("PNFD info " + pnfdInfoId + " not found.");
		}
		return pnfdInfoRepository.findByPnfdInfoId(pnfdInfoId).get();
	}
	
	//End of methods to manage and store PNFD entities
	
	//Methods to store and manage NSD entities
	
	private synchronized String storeNsd(Nsd input) throws AlreadyExistingEntityException {
		String nsdId = input.getNsdIdentifier();
		String version = input.getVersion();
		
		Nsd output = new Nsd(nsdId, 
				input.getDesigner(), 
				version, 
				input.getNsdName(),
				input.getNsdInvariantId(), 
				input.getNestedNsdId(), 
				input.getVnfdId(), 
				input.getPnfdId(), 
				input.getSecurity());
		
		nsdRepository.saveAndFlush(output);
		Nsd createdNsd = nsdRepository.findByNsdIdentifierAndVersion(nsdId, version).get();
		Long id = createdNsd.getId();
		log.debug("Added NSD with ID " + nsdId + " and version " + version + ". Internal ID assigned to the NSD instance: " + id);
		
		try {
			storeSapd(input, output);
			storeNsVirtualLinkDescriptor(input, output);
			storeVnfFg(input, output);
			storeMonitoredData(input, output);
			storeRule(input, output);
			storeNsdDeploymentFlavour(input, output);
			storeLifecycleManagementScript(input, output);
		} catch (AlreadyExistingEntityException e) {
			log.error("Error while storing the elements of the NSD since one of the element already existed: " + e.getMessage());
			nsdRepository.delete(id);
			log.debug("NSD with ID " + nsdId + " and version " + version + " removed from the DB.");
			throw new AlreadyExistingEntityException(e.getMessage());
		} 
		return String.valueOf(id);
	}
	
	private void storeLifecycleManagementScript(Nsd input, Nsd output) throws AlreadyExistingEntityException {
		List<LifeCycleManagementScript> lifeCycleManagementScript = input.getLifeCycleManagementScript();
		if ((lifeCycleManagementScript != null) && (!(lifeCycleManagementScript.isEmpty()))) {
			log.debug("Storing NSD lifecycle management script");
			for (LifeCycleManagementScript lcms : lifeCycleManagementScript) {
				LifeCycleManagementScript target = new LifeCycleManagementScript(output, lcms.getEvent(), lcms.getScript());
				lcmScriptRepository.saveAndFlush(target);
				log.debug("Stored LCM script for NSD");
			}
			log.debug("All NSD LCM scripts have been stored");
		}
	}
	
	private void storeSapd(Nsd input, Nsd output) throws AlreadyExistingEntityException {
		String nsdId = output.getNsdIdentifier();
		String nsdVersion = output.getVersion();
		List<Sapd> sapds = input.getSapd();
		if (sapds != null) {
			log.debug("Storing NSD SAPDS");
			for (Sapd sapd : sapds) {
				String sapdId = sapd.getCpdId();
				if (sapdRepository.findByCpdIdAndNsdNsdIdentifierAndNsdVersion(sapdId, nsdId, nsdVersion).isPresent()) {
					log.error("SAPD " + sapdId + " already existing. Impossible to load a new one.");
					throw new AlreadyExistingEntityException("SAPD " + sapdId + " already existing.");
				}
				Sapd target = new Sapd(output, sapdId, sapd.getLayerProtocol(), sapd.getCpRole(), sapd.getDescription(), sapd.getAddressData(),
						sapd.isSapAddressAssignment(), sapd.getNsVirtualLinkDescId(), sapd.getAssociatedCpdId());
				sapdRepository.saveAndFlush(target);
				log.debug("Stored NSD SAPD " + sapdId);
			}
			log.debug("All NSD SAPDs have been stored");
		}
	}
	
	private void storeNsVirtualLinkDescriptor(Nsd input, Nsd output) throws AlreadyExistingEntityException {
		String nsdId = output.getNsdIdentifier();
		String nsdVersion = output.getVersion();
		List<NsVirtualLinkDesc> vlds = input.getVirtualLinkDesc();
		if (vlds != null) {
			log.debug("Storing NSD VLDs");
			for (NsVirtualLinkDesc vld : vlds) {
				String vldId = vld.getVirtualLinkDescId();
				String vldVersion = vld.getVirtuaLinkDescVersion();
				if (nsdVldRepository.findByVirtualLinkDescIdAndVirtuaLinkDescVersionAndNsdNsdIdentifierAndNsdVersion(vldId, vldVersion, nsdId, nsdVersion).isPresent()) {
					log.error("NS VLD " + vldId + " - version " + vldVersion +" already existing. Impossible to load a new one.");
					throw new AlreadyExistingEntityException("NS VLD " + vldId + " - version " + vldVersion +" already existing.");
				}
				NsVirtualLinkDesc target = new NsVirtualLinkDesc(output, vldId, vld.getVirtualLinkDescProvider(), vldVersion, 
						vld.getConnectivityType(), vld.getTestAccess(), vld.getDescription(), vld.getSecurity());
				nsdVldRepository.saveAndFlush(target);
				log.debug("Stored NS VLD " + vldId + " - version " + vldVersion);
				
				List<VirtualLinkDf> dfs = vld.getVirtualLinkDf();
				if (dfs != null) {
					log.debug("Storing NSD VLD Deployment Flavours");
					for (VirtualLinkDf df : dfs) {
						String dfId = df.getFlavourId();
						if (vldDfRepository.findByFlavourIdAndNsVldVirtualLinkDescIdAndNsVldVirtuaLinkDescVersionAndNsVldNsdNsdIdentifierAndNsVldNsdVersion(dfId, vldId, vldVersion, nsdId, nsdVersion).isPresent()) {
							log.error("NS VLD DF " + dfId + " already existing. Impossible to load a new one.");
							throw new AlreadyExistingEntityException("NS VLD DF " + dfId + " already existing.");
						}
						VirtualLinkDf targetDf = new VirtualLinkDf(target, dfId, df.getQos(), df.getServiceAvaibilityLevel());
						vldDfRepository.saveAndFlush(targetDf);
						log.debug("Stored NS VLD Deployment Flavour " + dfId);
					}
					log.debug("Stored all NSD VLD Deployment Flavours");
				}
				
			}
			log.debug("All NSD VLDs have been stored");
		}
	}
	
	private void storeVnfFg(Nsd input, Nsd output) throws AlreadyExistingEntityException {
		String nsdId = output.getNsdIdentifier();
		String nsdVersion = output.getVersion();
		List<Vnffgd> fgds = input.getVnffgd();
		if (fgds != null) {
			log.debug("Storing Forwarding Graphs");
			for (Vnffgd fgd : fgds) {
				String fgdId = fgd.getVnffgdId();
				if (forwardingGraphRepository.findByVnffgdIdAndNsdNsdIdentifierAndNsdVersion(fgdId, nsdId, nsdVersion).isPresent()) {
					log.error("NS Forwarding Graph " + fgdId + " already existing. Impossible to load a new one.");
					throw new AlreadyExistingEntityException("NS Forwarding Graph " + fgdId + " already existing.");
				}
				//TODO: here it should check if the VNFD, PFND and VLD are all available
				Vnffgd target = new Vnffgd(output,
						fgdId,
						fgd.getVnfdId(),
						fgd.getPnfdId(),
						fgd.getVirtualLinkDescId(),
						fgd.getCpdPoolId());
				forwardingGraphRepository.saveAndFlush(target);
				log.debug("Stored Forwarding Graph " + fgdId);
				
				List<Nfpd> nfpds = fgd.getNfpd();
				if (nfpds != null) {
					log.debug("Storing forwarding paths");
					for (Nfpd nfpd : nfpds) {
						String nfpdId = nfpd.getNfpId();
						if (forwardingPathRepository.findByNfpIdAndFgVnffgdIdAndFgNsdNsdIdentifierAndFgNsdVersion(nfpdId, fgdId, nsdId, nsdVersion).isPresent()) {
							log.error("NS Forwarding Path " + nfpdId + " already existing. Impossible to load a new one.");
							throw new AlreadyExistingEntityException("NS Forwarding Path " + nsdId + " already existing.");
						}
						Nfpd nfpdTarget = new Nfpd(target, nfpdId, nfpd.getCpd());
						forwardingPathRepository.saveAndFlush(nfpdTarget);
						log.debug("Stored forwarding path " + nfpdId);
						if (nfpd.getNfpRule() != null) {
							log.debug("Storing rule for forwarding path " + nfpdId);
							if (ruleRepository.findByNfpdNfpIdAndNfpdFgVnffgdIdAndNfpdFgNsdNsdIdentifierAndNfpdFgNsdVersion(nfpdId, fgdId, nsdId, nsdVersion).isPresent()) {
								log.error("Rule for NS Forwarding Path " + nfpdId + " already existing. Impossible to load a new one.");
								throw new AlreadyExistingEntityException("Rule for NS Forwarding Path " + nsdId + " already existing.");
							}
							Rule targetRule = new Rule(nfpdTarget);
							ruleRepository.saveAndFlush(targetRule);
							log.debug("Stored rule for forwarding path " + nfpdId);
						}
					}
					log.debug("All forwarding paths have been stored");
				}
			}
			log.debug("All VNF Forwarding Graphs have been stored");
		}
	}
	
	private void storeMonitoredData(Nsd input, Nsd output) throws AlreadyExistingEntityException {
		if (input.getMonitoredInfo() != null) {
			log.debug("Storing NSD monitored data");
			List<MonitoredData> mds = input.getMonitoredInfo();
			for (MonitoredData md : mds) {
				MonitoredData mdTarget = new MonitoredData(output, md.getVnfIndicatorInfo(), md.getMonitoringParameter());
				monitoredDataRepository.saveAndFlush(mdTarget);
				log.debug("Stored monitored data");
			}
			log.debug("All monitored data have been stored");
		}
	}
	
	private void storeRule(Nsd input, Nsd output) throws AlreadyExistingEntityException {
		if (input.getAutoScalingRule() != null) {
			log.debug("Storing NSD autoscaling rules");
			List<Rule> rules = input.getAutoScalingRule();
			for (Rule r : rules) {
				Rule targetRule = new Rule(output);
				ruleRepository.saveAndFlush(targetRule);
				log.debug("Stored NSD autoscaling rule");
			}
			log.debug("All autoscaling rules have been stored");
		}
	}
	
	private void storeNsdDeploymentFlavour(Nsd input, Nsd output) throws AlreadyExistingEntityException {
		String nsdId = output.getNsdIdentifier();
		String nsdVersion = output.getVersion();
		if (input.getNsDf() != null) {
			log.debug("Storing NS Deployment Flavours");
			List<NsDf> dfs = input.getNsDf();
			for(NsDf df : dfs) {
				String dfId = df.getNsDfId();
				if (nsDeploymentFlavourRepository.findByNsDfIdAndNsdNsdIdentifierAndNsdVersion(dfId, nsdId, nsdVersion).isPresent()) {
					log.error("NS Deployment Flavour " + dfId + " already existing. Impossible to load a new one.");
					throw new AlreadyExistingEntityException("NS Deployment Flavour " + dfId + " already existing.");
				}
				NsDf target = new NsDf(output, dfId, df.getFlavourKey(), df.getAffinityOrAntiAffinityGroup(), 
						df.getDefaultNsInstantiationLevelId());
				nsDeploymentFlavourRepository.saveAndFlush(target);
				log.debug("Stored NS Deployment Flavour " + dfId);
				
				
				//NS profiles
				List<NsProfile> nsProfile = df.getNsProfile();
				if (nsProfile != null) {
					log.debug("Storing NS profiles");
					for (NsProfile nsP : nsProfile) {
						if (nsProfileRepository.findByNsProfileIdAndNsDfNsDfIdAndNsDfNsdNsdIdentifierAndNsDfNsdVersion(nsP.getNsProfileId(), dfId, nsdId, nsdVersion).isPresent()) {
							log.error("NS profile " + nsP.getNsProfileId() + " already existing. Impossible to load a new one.");
							throw new AlreadyExistingEntityException("NS profile " + nsP.getNsProfileId() + " already existing.");
						}
						NsProfile nsProfileTarget = new NsProfile(target, nsP.getNsProfileId(), nsP.getNsdId(), nsP.getNsDeploymentFlavourId(),
								nsP.getNsInstantiationLevelId(), nsP.getMinNumberOfInstances(), nsP.getMaxNumberOfInstances(), nsP.getAffinityOrAntiaffinityGroupId());
						nsProfileRepository.saveAndFlush(nsProfileTarget);
						log.debug("Stored NS profile " + nsP.getNsProfileId());
					}
					log.debug("Stored all NS profiles");
				}
				
				//Dependencies
				List<Dependencies> dependencies = df.getDependencies();
				if (dependencies != null) {
					log.debug("Storing VNF dependencies withing NS DF");
					for (Dependencies dep : dependencies) {
						Dependencies dTarget = new Dependencies(target, dep.getPrimaryId(), dep.getSecondaryId());
						dependenciesRepository.saveAndFlush(dTarget);
						log.debug("Stored dependency");
					}
					log.debug("Stored all dependencies");
				}
				
				//VNF profiles
				List<VnfProfile> vnfProfile = df.getVnfProfile();
				if (vnfProfile != null) {
					log.debug("Storing VNF profiles");
					for (VnfProfile vnfP : vnfProfile) {
						if (vnfProfileRepository.findByVnfProfileIdAndNsDfNsDfIdAndNsDfNsdNsdIdentifierAndNsDfNsdVersion(vnfP.getVnfProfileId(), dfId, nsdId, nsdVersion).isPresent()) {
							log.error("VNF profile " + vnfP.getVnfProfileId() + " already existing. Impossible to load a new one.");
							throw new AlreadyExistingEntityException("VNF profile " + vnfP.getVnfProfileId() + " already existing.");
						}
						VnfProfile vnfProfileTarget = new VnfProfile(target, vnfP.getVnfProfileId(), vnfP.getVnfdId(), 
								vnfP.getFlavourId(), vnfP.getInstantiationLevel(), vnfP.getMinNumberOfInstances(), vnfP.getMaxNumberOfInstances(), 
								vnfP.getLocalAffinityOrAntiAffinityRule(), vnfP.getAffinityOrAntiAffinityGroupId());
						vnfProfileRepository.saveAndFlush(vnfProfileTarget);
						log.debug("Stored VNF profile " + vnfP.getVnfProfileId());
						
						List<NsVirtualLinkConnectivity> vlConns = vnfP.getNsVirtualLinkConnectivity();
						if (vlConns != null) {
							log.debug("Storing NS VL connectivity for VNF");
							for (NsVirtualLinkConnectivity vlConn : vlConns) {
								NsVirtualLinkConnectivity vlConnTarget = new NsVirtualLinkConnectivity(vnfProfileTarget, vlConn.getVirtualLinkProfileId(),
										vlConn.getCpdId());
								nsVlConnectivityRepository.saveAndFlush(vlConnTarget);
							}
							log.debug("Stored NS VL connectivity for VNF");
						}
					}
					log.debug("Stored VNF profiles");
				}
				
				//PNF profiles
				List<PnfProfile> pnfProfile = df.getPnfProfile();
				if (pnfProfile != null) {
					log.debug("Storing PNF profiles");
					for (PnfProfile pnfP : pnfProfile) {
						if (pnfProfileRepository.findByPnfProfileIdAndNsDfNsDfIdAndNsDfNsdNsdIdentifierAndNsDfNsdVersion(pnfP.getPnfProfileId(), dfId, nsdId, nsdVersion).isPresent()) {
							log.error("PNF profile " + pnfP.getPnfProfileId() + " already existing. Impossible to load a new one.");
							throw new AlreadyExistingEntityException("PNF profile " + pnfP.getPnfProfileId() + " already existing.");
						}
						PnfProfile pnfProfileTarget = new PnfProfile(target, pnfP.getPnfProfileId(), pnfP.getPnfdId());
						pnfProfileRepository.saveAndFlush(pnfProfileTarget);
						log.debug("Stored PNF profile " + pnfP.getPnfProfileId());
						
						List<NsVirtualLinkConnectivity> vlConns = pnfP.getNsVirtualLinkConnectivity();
						if (vlConns != null) {
							log.debug("Storing NS VL connectivity for PNF");
							for (NsVirtualLinkConnectivity vlConn : vlConns) {
								NsVirtualLinkConnectivity vlConnTarget = new NsVirtualLinkConnectivity(pnfProfileTarget, vlConn.getVirtualLinkProfileId(),
										vlConn.getCpdId());
								nsVlConnectivityRepository.saveAndFlush(vlConnTarget);
							}
							log.debug("Stored NS VL connectivity for PNF");
						}
					}
					log.debug("Stored PNF profiles");
				}
				
				//VL profiles
				List<VirtualLinkProfile> virtualLinkProfile = df.getVirtualLinkProfile();
				if (virtualLinkProfile != null) {
					log.debug("Storing VL profiles");
					for (VirtualLinkProfile vlP : virtualLinkProfile) {
						if (vlProfileRepository.findByVirtualLinkProfileIdAndNsDfNsDfIdAndNsDfNsdNsdIdentifierAndNsDfNsdVersion(vlP.getVirtualLinkProfileId(), dfId, nsdId, nsdVersion).isPresent()) {
							log.error("VL profile " + vlP.getVirtualLinkProfileId() + " already existing. Impossible to load a new one.");
							throw new AlreadyExistingEntityException("VL profile " + vlP.getVirtualLinkProfileId() + " already existing.");
						}
						VirtualLinkProfile vlProfileTarget = new VirtualLinkProfile(target, vlP.getVirtualLinkProfileId(), vlP.getVirtualLinkDescId(),
								vlP.getFlavourId(), vlP.getLocalAffinityOrAntiAffinityRule(), vlP.getAffinityOrAntiAffinityGroupId(), 
								vlP.getMaxBitrateRequirements(), vlP.getMinBitrateRequirements());
						vlProfileRepository.saveAndFlush(vlProfileTarget);
						log.debug("Stored VL profile " + vlP.getVirtualLinkProfileId());
					}
					log.debug("Stored VL profiles");
				}
				
				//Scaling aspects
				List<NsScalingAspect> scalingAspect = df.getScalingAspect();
				if (scalingAspect != null) {
					log.debug("Storing scaling aspects");
					for (NsScalingAspect sa : scalingAspect) {
						String saId = sa.getNsScalingAspectId();
						if (nsScalingAspectRepository.findByNsScalingAspectIdAndNsDfNsDfIdAndNsDfNsdNsdIdentifierAndNsDfNsdVersion(saId, dfId, nsdId, nsdVersion).isPresent()) {
							log.error("Scaling aspect " + saId + " already existing. Impossible to load a new one.");
							throw new AlreadyExistingEntityException("Scaling aspect " + saId + " already existing.");
						}
						NsScalingAspect saTarget = new NsScalingAspect(target, saId, sa.getName(), sa.getDescription());
						nsScalingAspectRepository.saveAndFlush(saTarget);
						log.debug("Stored scaling aspect profile " + saId);
						
						List<NsLevel> nsLevels = sa.getNsScaleLevel();
						if(nsLevels != null) {
							log.debug("Storing NS levels associated to scaling aspects");
							for (NsLevel nsl : nsLevels) {
								if (nsLevelRepository.findByNsLevelIdAndNsScaleNsScalingAspectIdAndNsScaleNsDfNsDfIdAndNsScaleNsDfNsdNsdIdentifierAndNsScaleNsDfNsdVersion(nsl.getNsLevelId(),
										saId, dfId, nsdId, nsdVersion).isPresent()) {
									log.error("NS level " + nsl.getNsLevelId() + " associated to scaling aspect " + saId + " already existing. Impossible to load a new one.");
									throw new AlreadyExistingEntityException("NS level " + nsl.getNsLevelId() + " associated to scaling aspect " + saId + " already existing.");
								}
								NsLevel nsLevelTarget = new NsLevel(saTarget, nsl.getNsLevelId(), nsl.getDescription(), nsl.getVnfToLevelMapping(), nsl.getNsToLevelMapping());
								nsLevelRepository.saveAndFlush(nsLevelTarget);
								log.debug("Stored NS level " + nsl.getNsLevelId());
								
								List<VirtualLinkToLevelMapping> virtualLinkToLevelMapping = nsl.getVirtualLinkToLevelMapping();
								if (virtualLinkToLevelMapping != null) {
									for (VirtualLinkToLevelMapping vllm : virtualLinkToLevelMapping) {
										VirtualLinkToLevelMapping vllmTarget = new VirtualLinkToLevelMapping(nsLevelTarget, 
												vllm.getVirtualLinkProfileId(), vllm.getBitRateRequirements());
										vlToLevelMappingRepository.saveAndFlush(vllmTarget);
										log.debug("Stored VL to level mapping");
									}
								}
							}
							log.debug("Stored all NS levels associated to scaling aspects");
						}
					}
					log.debug("Stored all scaling aspects");
				}
				
				List<NsLevel> nsInstantiationLevel = df.getNsInstantiationLevel();
				if (nsInstantiationLevel != null) {
					log.debug("Storing NS levels associated to deployment flavour");
					for (NsLevel nsl : nsInstantiationLevel) {
						if (nsLevelRepository.findByNsLevelIdAndNsDfNsDfIdAndNsDfNsdNsdIdentifierAndNsDfNsdVersion(nsl.getNsLevelId(),
								dfId, nsdId, nsdVersion).isPresent()) {
							log.error("NS level " + nsl.getNsLevelId() + " already existing. Impossible to load a new one.");
							throw new AlreadyExistingEntityException("NS level " + nsl.getNsLevelId() + " already existing.");
						}
						NsLevel nsLevelTarget = new NsLevel(target, nsl.getNsLevelId(), nsl.getDescription(), nsl.getVnfToLevelMapping(), nsl.getNsToLevelMapping());
						nsLevelRepository.saveAndFlush(nsLevelTarget);
						log.debug("Stored NS level " + nsl.getNsLevelId());
						
						List<VirtualLinkToLevelMapping> virtualLinkToLevelMapping = nsl.getVirtualLinkToLevelMapping();
						if (virtualLinkToLevelMapping != null) {
							for (VirtualLinkToLevelMapping vllm : virtualLinkToLevelMapping) {
								VirtualLinkToLevelMapping vllmTarget = new VirtualLinkToLevelMapping(nsLevelTarget, 
										vllm.getVirtualLinkProfileId(), vllm.getBitRateRequirements());
								vlToLevelMappingRepository.saveAndFlush(vllmTarget);
								log.debug("Stored VL to level mapping");
							}
						}
					}
					log.debug("Stored all NS levels associated to deployment flavour");
				}
			}
			log.debug("All NS Deployment Flavours have been stored");
		}
	}
	//End of methods to store NSD entities
	
	
	
	//Methods to handle notifications towards external entities
	private void notify(NsdOnBoardingNotification notification) {
		//TODO: manage filters
		log.debug("Going to notify NSD onboarding to all the subscribers");
		List<NsdManagementSubscription> subscriptions = nsdManagementSubscriptionRepository.findAll();
		for (NsdManagementSubscription s : subscriptions) {
			NsdManagementRestConsumer consumer = s.getConsumer();
			consumer.notify(notification);
		}
		log.debug("NSD onboarding notified to all the subscribers");
	}

	private void notify(NsdChangeNotification notification) {
		//TODO: manage filters
		log.debug("Going to notify NSD change to all the subscribers");
		List<NsdManagementSubscription> subscriptions = nsdManagementSubscriptionRepository.findAll();
		for (NsdManagementSubscription s : subscriptions) {
			NsdManagementRestConsumer consumer = s.getConsumer();
			consumer.notify(notification);
		}
		log.debug("NSD change notified to all the subscribers");
	}
	//End of methods to handle notifications
	
	
	//Methods to handle internal NSD infos
	public InternalNsdInfo findNsdInfo(String nsdInfoId) throws NotExistingEntityException {
		if (!(nsdInfoRepository.findByNsdInfoId(nsdInfoId).isPresent())) {
			log.error("NSD info " + nsdInfoId + " not found in internal DB.");
			throw new NotExistingEntityException("NSD info " + nsdInfoId + " not found.");
		}
		return nsdInfoRepository.findByNsdInfoId(nsdInfoId).get();
	}
	
	private InternalNsdInfo findNsdInfo(String nsdId, String version) throws NotExistingEntityException {
		if (!(nsdInfoRepository.findByNsdIdAndVersion(nsdId, version).isPresent())) {
			log.error("NSD info for NSD " + nsdId + " and version " + version + " not found in internal DB.");
			throw new NotExistingEntityException("NSD info for NSD " + nsdId + " and version " + version + " not found.");
		}
		return nsdInfoRepository.findByNsdIdAndVersion(nsdId, version).get();
	}
	
	public Nsd findNsd(InternalNsdInfo iNsdInfo) throws NotExistingEntityException {
		String nsdId = iNsdInfo.getNsdId();
		String version = iNsdInfo.getVersion();
		if (nsdRepository.findByNsdIdentifierAndVersion(nsdId, version).isPresent()) {
			return nsdRepository.findByNsdIdentifierAndVersion(nsdId, version).get();
		} else {
			log.error("NSD " + nsdId + " - version " + version + " not found");
			throw new NotExistingEntityException("NSD " + nsdId + " - version " + version + " not found");
		}
	}
	
	private void removeNsdInfo(InternalNsdInfo nsdInfo) throws NotExistingEntityException {
		if (nsdInfo.getUsageState()==UsageState.IN_USE) {
			log.warn("Request to delete an NSD still in use. Setting it to pending delete.");
			nsdInfo.setDeletionPending(true);
			nsdInfoRepository.save(nsdInfo);
			notify(new NsdChangeNotification(nsdInfo.getNsdInfoId(), NsdChangeType.NSD_IN_DELETION_PENDING, 
					null, true));
		} else {
			//the NSD info is not in use and it can be deleted
			//it removes also the associated NS
			removeNsdFromDb(nsdInfo);
			notify(new NsdChangeNotification(nsdInfo.getNsdInfoId(), NsdChangeType.NSD_DELETION, 
					null, false));
		}
	}
	
	private void removeNsdFromDb(InternalNsdInfo nsdInfo) throws NotExistingEntityException {
		String nsdId = nsdInfo.getNsdId();
		String version = nsdInfo.getVersion();
		if (nsdRepository.findByNsdIdentifierAndVersion(nsdId, version).isPresent()) {
			Nsd nsd = nsdRepository.findByNsdIdentifierAndVersion(nsdId, version).get();
			nsdRepository.delete(nsd);
			log.debug("Removed NSD " + nsdId + " - version " + version);
		} else {
			log.error("NSD " + nsdId + " - version " + version + " not found");
			throw new NotExistingEntityException("NSD " + nsdId + " - version " + version + " not found");
		}
		nsdInfoRepository.delete(nsdInfo);
		log.debug("Removed NSD info");
	}
	
	
	private NsdInfo buildNsdInfo(String nsdInfoId) throws NotExistingEntityException {
		InternalNsdInfo iNsdInfo = findNsdInfo(nsdInfoId);
		Nsd nsd = findNsd(iNsdInfo);
		
		List<String> onboardedVnfPkgInfoId = new ArrayList<>();
		List<String> vnfdIds = nsd.getVnfdId();
		for (String vnfdId : vnfdIds) {
			onboardedVnfPkgInfoId.add(vnfPackageManagement.getOnboardedVnfPkgInfoFromVnfd(vnfdId).getOnboardedVnfPkgInfoId());
		}
		
		NsdInfo nsdInfo = new NsdInfo(nsdInfoId, nsd.getNsdIdentifier(), nsd.getNsdName(), nsd.getVersion(), nsd.getDesigner(), nsd, 
				onboardedVnfPkgInfoId, iNsdInfo.getPnfdInfoId(), iNsdInfo.getPreviousNsdVersionId(), iNsdInfo.getOperationalState(), iNsdInfo.getUsageState(), 
				iNsdInfo.isDeletionPending(), iNsdInfo.getUserDefinedData());
		
		return nsdInfo;
	}

	private List<NsdInfo> buildNsdInfo() throws NotExistingEntityException{
		List<NsdInfo> nsdInfos = new ArrayList<>();
		List<InternalNsdInfo> internalNsdInfos = nsdInfoRepository.findAll();
		for (InternalNsdInfo iNsdInfo : internalNsdInfos) {
			Nsd nsd = findNsd(iNsdInfo);
			NsdInfo nsdInfo = new NsdInfo(iNsdInfo.getNsdInfoId(), nsd.getNsdIdentifier(), nsd.getNsdName(), nsd.getVersion(), nsd.getDesigner(), nsd,
					null, nsd.getPnfdId(), iNsdInfo.getPreviousNsdVersionId(), iNsdInfo.getOperationalState(), iNsdInfo.getUsageState(),
					iNsdInfo.isDeletionPending(), iNsdInfo.getUserDefinedData());
			nsdInfos.add(nsdInfo);
		}
		return nsdInfos;
	}
	
	private NsdInfo buildNsdInfo(String nsdId, String version) throws NotExistingEntityException {
		InternalNsdInfo iNsdInfo = findNsdInfo(nsdId, version);
		
		log.debug("Found NSD info");
		
		Nsd nsd = findNsd(iNsdInfo);

		log.debug("Found NSD");
		
		List<String> vnfPackageInfoIds = new ArrayList<>();
		List<String> vnfdIds = nsd.getVnfdId();
		
		log.debug("Got VNFD IDs");
		
		for (String vnfdId : vnfdIds) {
			log.debug("Searching VNFD ID " + vnfdId);
			OnboardedVnfPkgInfo pkgInfo = vnfPackageManagement.getOnboardedVnfPkgInfoFromVnfd(vnfdId);
			String pkgInfoId = pkgInfo.getOnboardedVnfPkgInfoId();
			vnfPackageInfoIds.add(pkgInfoId);
		}
		
		log.debug("Read VNFD IDs");
		
		NsdInfo nsdInfo = new NsdInfo(iNsdInfo.getNsdInfoId(), nsd.getNsdIdentifier(), nsd.getNsdName(), nsd.getVersion(), nsd.getDesigner(), nsd, 
				vnfPackageInfoIds, iNsdInfo.getPnfdInfoId(), iNsdInfo.getPreviousNsdVersionId(), iNsdInfo.getOperationalState(), iNsdInfo.getUsageState(), 
				iNsdInfo.isDeletionPending(), iNsdInfo.getUserDefinedData());
		
		log.debug("Built NSD info");
		
		return nsdInfo;
	}

	//End of methods to handle internal NSD infos
	
	public Map<String, String> findVnfsUserParameters(String nsdInfoId) throws NotExistingEntityException {
		List<String> vnfsParameters = new ArrayList<>();
		Map<String, String> vnfsParametersMap = new HashMap<>();

		InternalNsdInfo iNsdInfo = null;
		iNsdInfo = findNsdInfo(nsdInfoId);
		log.debug("Found NSD info");

		Nsd nsd = findNsd(iNsdInfo);
		log.debug("Found NSD");

		NsDf nsDf = nsd.getNsDf().get(0);
		List<VnfProfile> profiles = nsDf.getVnfProfile();

		List<String> vnfdIds = nsd.getVnfdId();

		OnboardedVnfPkgInfo pkg;
		for (String vnfdId : vnfdIds) {
			pkg = vnfPackageManagement.getOnboardedVnfPkgInfoFromVnfd(vnfdId);
			Vnfd vnfd = pkg.getVnfd();
			VnfConfigurableProperties configurableProperties = vnfd.getConfigurableProperties();
			String vnfdProfileId = null;
			for (VnfProfile profile : profiles) {
				if (profile.getVnfdId().equalsIgnoreCase(vnfdId)) {
					vnfdProfileId = profile.getVnfProfileId();
				}
			}
			if (configurableProperties != null) {
				List<String> configData = configurableProperties.getAdditionalConfigurableProperty();
				if (configData != null) {
					for (String s: configData) {
						log.debug("Found user config parameter " + s + " in VNFD " + vnfdId);
						vnfsParametersMap.put(s, vnfdProfileId);
					}
				}
			}
		}

		vnfsParameters.addAll(vnfsParametersMap.keySet());
		String logString = "User configuration parameters in NSD " + nsdInfoId + ": ";
		for (String s : vnfsParameters) {
			logString += s + " \n";
		}
		log.debug(logString);

		return vnfsParametersMap;
	}


}
