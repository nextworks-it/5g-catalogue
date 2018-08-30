package it.nextworks.nfvmano.catalogue.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.CreateNsdInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.KeyValuePairs;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdLinksType;
import it.nextworks.nfvmano.catalogue.repos.NsdInfoRepository;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;

@Service
public class NsdManagementService {
	
	private static final Logger log = LoggerFactory.getLogger(NsdManagementService.class);
	
	@Autowired
	private NsdInfoRepository nsdInfoRepo;
	
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
	
	public void deleteNsdInfo(String nsdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException {
		//TODO:
		throw new MethodNotImplementedException();
	}

	public Object getNsd(String nsdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException {
		//TODO:
		throw new MethodNotImplementedException();
	}
	
	public NsdInfo getNsdInfo(String nsdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException {
		//TODO:
		throw new MethodNotImplementedException();
	}
	
	public List<NsdInfo> getAllNsdInfos() throws FailedOperationException, MethodNotImplementedException {
		//TODO:
		throw new MethodNotImplementedException();
	}
	
	public void uploadNsd(String nsdInfoId, Object nsd) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException {
		//TODO:
		throw new MethodNotImplementedException();
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
