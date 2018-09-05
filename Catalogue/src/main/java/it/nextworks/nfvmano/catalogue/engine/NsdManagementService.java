package it.nextworks.nfvmano.catalogue.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;


import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.CreateNsdInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.KeyValuePairs;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdLinksType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOnboardingStateType;
import it.nextworks.nfvmano.catalogue.repos.NsdContentType;
import it.nextworks.nfvmano.catalogue.repos.NsdInfoRepository;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
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
	
	@Autowired
	private FileSystemStorageService storageService;
	
	@Autowired
	private NotificationManager notificationManager;
	
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
		
		/*
		DescriptorTemplate nsd = dbWrapper.getNsd(nsdId);
		log.debug("Got NSD content.");
		*/
		
		NsdContentType ct = nsdInfo.getNsdContentType();
		switch (ct) {
		case YAML: {
			//try {
				List<String> nsdFilenames = nsdInfo.getNsdFilename();
				if (nsdFilenames.size() != 1) {
					log.error("Found zero or more than one file for NSD in YAML format. Error.");
					throw new FailedOperationException("Found more than one file for NSD in YAML format. Error.");
				}
				String nsdFilename = nsdFilenames.get(0);
				return storageService.loadNsdAsResource(nsdInfo, nsdFilename);
			
				/*
				//String nsdString = DescriptorsParser.descriptorTemplateToString(nsd);
				//log.debug("NSD content translated into YAML format");
				//return nsdString;
			} catch (JsonProcessingException e) {
				log.error("Error while translating descriptor");
				throw new FailedOperationException("Error while translating descriptor: " + e.getMessage());
			}
			*/
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
	
	public synchronized void uploadNsd(String nsdInfoId, MultipartFile nsd, NsdContentType nsdContentType) throws Exception, FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException {
		log.debug("Processing request to upload NSD content for NSD info " + nsdInfoId);
		NsdInfoResource nsdInfo = getNsdInfoResource(nsdInfoId);
		if (nsdInfo.getNsdOnboardingState() != NsdOnboardingStateType.CREATED) {
			log.error("NSD info " + nsdInfoId + " not in CREATED onboarding state.");
			throw new NotPermittedOperationException("NSD info " + nsdInfoId + " not in CREATED onboarding state.");
		}		
		
		//convert to File
		File inputFile = convertToFile(nsd);
		
		String nsdFilename = null;
		DescriptorTemplate dt = null;
		
		//pre-set nsdinfo attributes to properly store NSDs
		UUID nsdId = UUID.randomUUID();
		nsdInfo.setNsdId(nsdId);
		
		switch (nsdContentType) {
		case ZIP: {
			try {
				log.info("NSD file is in format: zip");
				
				//TODO: assuming for now one single file into the zip
				MultipartFile nsdMpFile = extractNsdFile(inputFile);
				//convert to File
				File nsdFile = convertToFile(nsdMpFile);

				dt = DescriptorsParser.fileToDescriptorTemplate(nsdFile);

				log.debug("NSD succssfully parsed - its content is: \n" + DescriptorsParser.descriptorTemplateToString(dt));				
				//pre-set nsdinfo attributes to properly store NSDs
				nsdInfo.setNsdVersion(dt.getMetadata().getVersion());

				nsdFilename = storageService.storeNsd(nsdInfo, nsdMpFile);
				
				// change nsdContentType to YAML as nsd file is no more zip from now on
				nsdContentType = NsdContentType.YAML;
				
				log.debug("NSD file successfully stored");
				//clean tmp files
				if (!nsdFile.delete()) {
					log.warn("Could not delete temporary NSD zip content file");
				}
				
			} catch (IOException e) {
				log.error("Error while parsing NSD in zip format: " + e.getMessage());
				throw new MalformattedElementException("Error while parsing NSD.");
			} catch (Exception e) {
				log.error("Error while parsing NSD in zip format: " + e.getMessage());
				throw new MalformattedElementException(e.getMessage());
			}
			break;
		}
		case YAML: {
			try {
				log.info("NSD file is in format: yaml");
				
				dt = DescriptorsParser.fileToDescriptorTemplate(inputFile);
				log.debug("NSD succssfully parsed - its content is: \n" + DescriptorsParser.descriptorTemplateToString(dt));
				//pre-set nsdinfo attributes to properly store NSDs
				nsdInfo.setNsdVersion(dt.getMetadata().getVersion());
				
				nsdFilename = storageService.storeNsd(nsdInfo, nsd);
				
				log.debug("NSD file successfully stored");

			} catch (IOException e) {
				log.error("Error while parsing NSD in yaml format: " + e.getMessage());
				throw new MalformattedElementException("Error while parsing NSD.");
			}
			break;
		}

		default: {
			log.error("Unsupported content type: " + nsdContentType.toString());
			throw new MethodNotImplementedException("Unsupported content type: " + nsdContentType.toString());
		}
		}
		
		if (nsdFilename == null ||
			dt == null) {
			throw new FailedOperationException("Invalid internal structures");
		}
				
		log.debug("Updating NSD info");
		//nsdInfo.setNsdId(nsdId);
		//TODO: here it is actually onboarded only locally and just in the DB. To be updated when we will implement also the package uploading
		nsdInfo.setNsdOnboardingState(NsdOnboardingStateType.ONBOARDED);
		nsdInfo.setNsdDesigner(dt.getMetadata().getVendor());
		nsdInfo.setNsdInvariantId(UUID.fromString(dt.getMetadata().getDescriptorId()));
		String nsdName = dt.getTopologyTemplate().getNSNodes().get(0).getProperties().getName();
		log.debug("NSD name: " + nsdName);
		nsdInfo.setNsdName(nsdName);
		//nsdInfo.setNsdVersion(dt.getMetadata().getVersion());
		nsdInfo.setNsdContentType(nsdContentType);
		nsdInfo.addNsdFilename(nsdFilename);
		nsdInfoRepo.saveAndFlush(nsdInfo);
		log.debug("NSD info updated");
		
		//clean tmp files
		if (!inputFile.delete()) {
			log.warn("Could not delete temporary NSD content file");
		}
		
		//send notification over kafka bus
		notificationManager.nsdOnBoardingNotification(nsdInfo.getId().toString(), nsdId.toString());
		
		log.debug("NSD content uploaded and nsdOnBoardingNotification delivered");
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
	
	private File convertToFile(MultipartFile multipart) throws Exception {
	    File convFile = new File(multipart.getOriginalFilename());
	    convFile.createNewFile();
	    FileOutputStream fos = new FileOutputStream(convFile);
	    fos.write(multipart.getBytes());
	    fos.close();
	    return convFile;
	}
	
	private MultipartFile extractNsdFile(File file) throws IOException{
		
		MultipartFile archivedNsd = null;
				
		ZipFile zipFile = new ZipFile(file);
		if (zipFile.size() == 0) {
			throw new IOException("The zip archive does not contain any entries.");
		}
		if (zipFile.size() > 1) {
			throw new IOException("The zip archive contains more than one entry.");
		}
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		
		while (entries.hasMoreElements()) {						
			ZipEntry entry = entries.nextElement();
			if (!entry.isDirectory() && entry.getName().endsWith(".yaml")) {
				// it is the NSD
				log.debug("File inside zip: " + entry.getName());
				
				if (archivedNsd != null) {
					log.error("Archive validation failed: multiple NSDs in the zip");
					throw new IOException("Multiple NSDs in the zip");
				}
				InputStream zipIn = zipFile.getInputStream(entry);
				archivedNsd = new MockMultipartFile(entry.getName(), entry.getName(), null, zipIn);
			    //found (ASSUME one single .yaml in the zip)
			    break;
			}												
		}
		if (archivedNsd == null) {
			throw new IOException("The zip archive does not contain nsd file.");
		}
		zipFile.close();
		return archivedNsd;
	}
}
