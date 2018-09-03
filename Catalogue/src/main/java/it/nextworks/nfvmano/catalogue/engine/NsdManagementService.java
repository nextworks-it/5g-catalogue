package it.nextworks.nfvmano.catalogue.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.CreateNsdInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.KeyValuePairs;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdLinksType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOnboardingStateType;
import it.nextworks.nfvmano.catalogue.repos.NsdContentType;
import it.nextworks.nfvmano.catalogue.repos.NsdInfoRepository;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.NotPermittedOperationException;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

@Service
public class NsdManagementService {
	
	private static final Logger log = LoggerFactory.getLogger(NsdManagementService.class);
	
	@Autowired
	private NsdInfoRepository nsdInfoRepo;
	
	@Autowired
	private DbPersistencyHandler dbWrapper;
	
	public NsdManagementService() {	}
	
	public NsdInfo createNsdInfo(CreateNsdInfoRequest request) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException {
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
	
	public synchronized void deleteNsdInfo(String nsdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
		log.debug("Processing request to delete an NSD info.");
		
		if (nsdInfoId == null) throw new MalformattedElementException("Invalid NSD info ID.");
		
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
					dbWrapper.deleteNsd(nsdId);
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

	public Object getNsd(String nsdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
		log.debug("Processing request to retrieve an NSD content for NSD info " + nsdInfoId);
		NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);
		if (nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.ONBOARDED) {
			log.error("NSD info " + nsdInfoId + " does not have an onboarded NSD yet");
			throw new NotPermittedOperationException("NSD info " + nsdInfoId + " does not have an onboarded NSD yet");
		}
		UUID nsdId = nsdInfo.getNsdId();
		log.debug("Internal NSD ID: " + nsdId);
		
		DescriptorTemplate nsd = dbWrapper.getNsd(nsdId);
		log.debug("Got NSD content.");
		
		NsdContentType ct = nsdInfo.getNsdContentType();
		switch (ct) {
		case YAML: {
			try {
				String nsdString = DescriptorsParser.descriptorTemplateToString(nsd);
				log.debug("NSD content translated into YAML format");
				return nsdString;
			} catch (JsonProcessingException e) {
				log.error("Error while translating descriptor");
				throw new FailedOperationException("Error while translating descriptor: " + e.getMessage());
			}
		}

		default: {
			log.error("Content type not yet supported.");
			throw new MethodNotImplementedException("Content type not yet supported.");
		}
		}
		
	}
	
	public NsdInfo getNsdInfo(String nsdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException {
		log.debug("Processing request to get an NSD info.");
		
		NsdInfoResource nsdInfoResource = getNsdInfoResource(nsdInfoId);
		log.debug("Found NSD info resource with id: " + nsdInfoId);
		NsdInfo nsdInfo = buildNsdInfo(nsdInfoResource);
		log.debug("Built NSD info with id: " + nsdInfoId);
		return nsdInfo;
		
	}
	
	private NsdInfoResource getNsdInfoResource(String nsdInfoId) throws NotExistingEntityException, MalformattedElementException {
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
	
	public synchronized void uploadNsd(String nsdInfoId, String nsd, NsdContentType nsdContentType) throws FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
		log.debug("Processing request to upload NSD content for NSD info " + nsdInfoId);
		NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);
		if (nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.CREATED) {
			log.error("NSD info " + nsdInfoId + " not in CREATED onboarding state.");
			throw new NotPermittedOperationException("NSD info " + nsdInfoId + " not in CREATED onboarding state.");
		}
		switch (nsdContentType) {
		case YAML: {
			log.debug("Input NSD: \n" + nsd);
			try {
				DescriptorTemplate dt = DescriptorsParser.stringToDescriptorTemplate(nsd);
				log.debug("NSD parsed");
				UUID nsdId = dbWrapper.createNsd(dt);
				log.debug("Updating NSD info");
				nsdInfo.setNsdId(nsdId);
				//TODO: here it is actually onboarded only locally and just in the DB. To be updated when we will implement also the package uploading
				nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.ONBOARDED);
				nsdInfo.setNsdDesigner(dt.getMetadata().getVendor());
				nsdInfo.setNsdInvariantId(UUID.fromString(dt.getMetadata().getDescriptorId()));
				String nsdName = dt.getTopologyTemplate().getNSNodes().get(0).getProperties().getName();
				log.debug("NSD name: " + nsdName);
				nsdInfo.setNsdName(nsdName);
				nsdInfo.setNsdVersion(dt.getMetadata().getVersion());
				nsdInfo.setNsdContentType(NsdContentType.YAML);
				nsdInfoRepo.saveAndFlush(nsdInfo);
				log.debug("NSD info updated");
				//TODO: send notification
			} catch (IOException e) {
				log.error("Error while parsing NSD: " + e.getMessage());
				throw new MalformattedElementException("Error while parsing NSD.");
			}
			break;
		}

		default: {
			log.error("Unsupported content type: " + nsdContentType.toString());
			throw new MethodNotImplementedException("Unsupported content type: " + nsdContentType.toString());
		}
		}
	}
	
	private NsdInfo buildNsdInfo (NsdInfoResource nsdInfoResource) {
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
	
}
