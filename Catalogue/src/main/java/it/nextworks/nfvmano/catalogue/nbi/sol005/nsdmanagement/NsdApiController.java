package it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement;

import it.nextworks.nfvmano.catalogue.common.Utilities;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementService;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.CreateNsdInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdInfoModifications;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdmSubscription;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdmSubscriptionRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdInfoModifications;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.ProblemDetails;
import it.nextworks.nfvmano.catalogue.repos.NsdContentType;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.NotPermittedOperationException;
import io.swagger.annotations.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-07-23T16:31:35.952+02:00")

@Controller
public class NsdApiController implements NsdApi {

	private static final Logger log = LoggerFactory.getLogger(NsdApiController.class);

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;
	
	@Autowired
	NsdManagementService nsdManagementService;

	@org.springframework.beans.factory.annotation.Autowired
	public NsdApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	public ResponseEntity<NsdInfo> createNsdInfo(
			@ApiParam(value = "", required = true) @Valid @RequestBody CreateNsdInfoRequest body) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			log.debug("Processing REST request to create an NSD info");
			try {
				NsdInfo nsdInfo = nsdManagementService.createNsdInfo(body);
				return new ResponseEntity<NsdInfo>(nsdInfo, HttpStatus.CREATED);
			} catch (MalformattedElementException e) {
				return new ResponseEntity<NsdInfo>(HttpStatus.BAD_REQUEST);
			} catch (Exception e) {
				log.error("Exception while creating NSD info: " + e.getMessage());
				return new ResponseEntity<NsdInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
			/*
			try {
				return new ResponseEntity<NsdInfo>(objectMapper.readValue(
						"{  \"nsdOnboardingState\" : { },  \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"_links\" : {    \"self\" : \"http://example.com/aeiou\",    \"nsd_content\" : \"http://example.com/aeiou\"  },  \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"nsdUsageState\" : { },  \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdDesigner\" : \"nsdDesigner\",  \"nsdVersion\" : \"nsdVersion\",  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdName\" : \"nsdName\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdOperationalState\" : { },  \"userDefinedData\" : { }}",
						NsdInfo.class), HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<NsdInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			*/
		} else return new ResponseEntity<NsdInfo>(HttpStatus.BAD_REQUEST);

		//return new ResponseEntity<NsdInfo>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<PnfdInfo> createPNFDInfo(
			@ApiParam(value = "", required = true) @Valid @RequestBody PnfdInfo body) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<PnfdInfo>(objectMapper.readValue(
						"{  \"pnfdName\" : \"pnfdName\",  \"pnfdUsageState\" : { },  \"pnfdOnboardingState\" : { },  \"_links\" : {    \"pnfd_content\" : \"http://example.com/aeiou\",    \"self\" : \"http://example.com/aeiou\"  },  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"pnfdProvider\" : \"pnfdProvider\",  \"pnfdVersion\" : \"pnfdVersion\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"userDefinedData\" : { }}",
						PnfdInfo.class), HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<PnfdInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<PnfdInfo>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<NsdmSubscription> createSubscription(
			@ApiParam(value = "", required = true) @Valid @RequestBody NsdmSubscriptionRequest body) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<NsdmSubscription>(objectMapper.readValue(
						"{  \"filter\" : {    \"nsdOnboardingState\" : { },    \"pnfdName\" : \"pnfdName\",    \"pnfdUsageState\" : { },    \"nsdInfoId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"pnfdOnboardingState\" : { },    \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdUsageState\" : { },    \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdDesigner\" : \"nsdDesigner\",    \"nsdVersion\" : \"nsdVersion\",    \"pnfdProvider\" : \"pnfdProvider\",    \"pnfdVersion\" : \"pnfdVersion\",    \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdName\" : \"nsdName\",    \"notificationTypes\" : [ { }, { } ],    \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdOperationalState\" : { }  },  \"_links\" : {    \"self\" : \"http://example.com/aeiou\"  },  \"callbackUri\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}",
						NsdmSubscription.class), HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<NsdmSubscription>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<NsdmSubscription>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<?> deleteNSDInfo(
			@ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId) {
		String accept = request.getHeader("Accept");
		log.debug("Processing REST request to delete NSD info " + nsdInfoId);
		try {
			nsdManagementService.deleteNsdInfo(nsdInfoId);
			log.debug("NSD info removed");
			return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
		} catch (NotExistingEntityException e) {
			log.error("NSD info " + nsdInfoId + " not found");
			return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(), "NSD info " + nsdInfoId + " not found"), HttpStatus.NOT_FOUND);
		} catch (NotPermittedOperationException e) {
			log.error("NSD info " +  nsdInfoId + " cannot be removed: " + e.getMessage());
			return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(), "NSD info " +  nsdInfoId + " cannot be removed: " + e.getMessage()), HttpStatus.CONFLICT);
		} catch (MalformattedElementException e) {
			log.error("NSD info " + nsdInfoId + " cannot be removed: not acceptable NSD Info ID format");
			return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(), "NSD info " + nsdInfoId + " cannot be removed: not acceptable NSD Info ID format"), 
					HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error("NSD info " + nsdInfoId + " cannot be removed: general internal error.");
			return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(), "NSD info " + nsdInfoId + " cannot be removed: general internal error."), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<Void> deletePNFDInfo(
			@ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId) {
		String accept = request.getHeader("Accept");
		return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<Void> deleteSubscription(
			@ApiParam(value = "", required = true) @PathVariable("subscriptionId") String subscriptionId) {
		String accept = request.getHeader("Accept");
		return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<?> getNSD(
			@ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId,
			@ApiParam(value = "The request may contain a \"Range\" HTTP header to obtain single range of bytes from the NSD file. This can be used to continue an aborted transmission.  If the NFVO does not support range requests, the NFVO shall ignore the 'Range\" header, process the GET request, and return the whole NSD file with a 200 OK response (rather than returning a 4xx error status code).") @RequestHeader(value = "Range", required = false) String range) {
		String accept = request.getHeader("Accept");
		
		//TODO: consistency between accept values and input format when onboarding should be better checked. 
		//TODO: probably we should select the format based on the accept values. At the moment the format is selected based on the original input type, 
		//that is maintained in the DB.
		log.debug("Processing REST request to retrieve NSD for NSD info ID " + nsdInfoId);
		
		try {
			Object nsd = nsdManagementService.getNsd(nsdInfoId);
			//TODO: here it needs to check the type of entity that is returned
			return new ResponseEntity<Resource>((Resource)nsd, HttpStatus.OK);
		} catch (NotExistingEntityException e) {
			log.error("NSD for NSD info ID " + nsdInfoId + " not found");
			return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(), "NSD for NSD info ID " + nsdInfoId + " not found: " + e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (NotPermittedOperationException e) {
			return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(), "NSD for NSD info ID " + nsdInfoId + " not found: " + e.getMessage()), HttpStatus.CONFLICT);
		} catch (Exception e) {
			return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(), "NSD for NSD info ID " + nsdInfoId + " not found: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		/*
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<Object>(objectMapper.readValue("\"{}\"", Object.class),
						HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
		*/
	}

	public ResponseEntity<?> getNSDInfo(
			@ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId) {
		String accept = request.getHeader("Accept");
		/*
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<NsdInfo>(objectMapper.readValue(
						"{  \"nsdOnboardingState\" : { },  \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"_links\" : {    \"self\" : \"http://example.com/aeiou\",    \"nsd_content\" : \"http://example.com/aeiou\"  },  \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"nsdUsageState\" : { },  \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdDesigner\" : \"nsdDesigner\",  \"nsdVersion\" : \"nsdVersion\",  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdName\" : \"nsdName\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdOperationalState\" : { },  \"userDefinedData\" : { }}",
						NsdInfo.class), HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<NsdInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<NsdInfo>(HttpStatus.NOT_IMPLEMENTED);
		*/
		log.debug("Processing REST request to retrieve NSD info " + nsdInfoId);
		if (accept != null && accept.contains("application/json")) {
			try {
				NsdInfo nsdInfo = nsdManagementService.getNsdInfo(nsdInfoId);
				log.debug("NSD info retrieved");
				return new ResponseEntity<NsdInfo>(nsdInfo, HttpStatus.OK);
			} catch (NotExistingEntityException e) {
				log.error("NSD info " + nsdInfoId + " not found");
				return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(), "NSD info " + nsdInfoId + " not found"), HttpStatus.NOT_FOUND);
			} catch (MalformattedElementException e) {
				log.error("NSD info " + nsdInfoId + " cannot be found: not acceptable NSD Info ID format");
				return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(), "NSD info " + nsdInfoId + " cannot be found: not acceptable NSD Info ID format"), 
						HttpStatus.BAD_REQUEST);
			} catch (Exception e) {
				log.error("NSD info " + nsdInfoId + " cannot be retrieved: general internal error.");
				return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(), "NSD info " + nsdInfoId + " cannot be retrieved: general internal error."), 
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else return new ResponseEntity<String>("Unacceptable ACCEPT header type.", HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<?> getNSDsInfo() {
		log.debug("Processing REST request to retrieve all NSD infos.");
		String accept = request.getHeader("Accept");
		/*
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<List<NsdInfo>>(objectMapper.readValue(
						"[ {  \"nsdOnboardingState\" : { },  \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"_links\" : {    \"self\" : \"http://example.com/aeiou\",    \"nsd_content\" : \"http://example.com/aeiou\"  },  \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"nsdUsageState\" : { },  \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdDesigner\" : \"nsdDesigner\",  \"nsdVersion\" : \"nsdVersion\",  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdName\" : \"nsdName\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdOperationalState\" : { },  \"userDefinedData\" : { }}, {  \"nsdOnboardingState\" : { },  \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"_links\" : {    \"self\" : \"http://example.com/aeiou\",    \"nsd_content\" : \"http://example.com/aeiou\"  },  \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"nsdUsageState\" : { },  \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],  \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdDesigner\" : \"nsdDesigner\",  \"nsdVersion\" : \"nsdVersion\",  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdName\" : \"nsdName\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"nsdOperationalState\" : { },  \"userDefinedData\" : { }} ]",
						List.class), HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<List<NsdInfo>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<List<NsdInfo>>(HttpStatus.NOT_IMPLEMENTED);
		*/
		//TODO: process URI parameters for filters and attributes. At the moment it returns all the NSD infos.
		if (accept != null && accept.contains("application/json")) {
			try {
				List<NsdInfo> nsdInfos = nsdManagementService.getAllNsdInfos();
				log.debug("NSD infos retrieved");
				return new ResponseEntity<List<NsdInfo>>(nsdInfos, HttpStatus.OK);
			} catch (Exception e) {
				log.error("General exception while retrieving set of NSD infos: " + e.getMessage());
				return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(), "General exception while retrieving set of NSD infos: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else return new ResponseEntity<String>("Unacceptable ACCEPT header type.", HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<Object> getPNFD(
			@ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<Object>(objectMapper.readValue("\"{}\"", Object.class),
						HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<PnfdInfo> getPNFDInfo(
			@ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<PnfdInfo>(objectMapper.readValue(
						"{  \"pnfdName\" : \"pnfdName\",  \"pnfdUsageState\" : { },  \"pnfdOnboardingState\" : { },  \"_links\" : {    \"pnfd_content\" : \"http://example.com/aeiou\",    \"self\" : \"http://example.com/aeiou\"  },  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"pnfdProvider\" : \"pnfdProvider\",  \"pnfdVersion\" : \"pnfdVersion\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"userDefinedData\" : { }}",
						PnfdInfo.class), HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<PnfdInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<PnfdInfo>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<List<PnfdInfo>> getPNFDsInfo(
			@ApiParam(value = "Indicates to exclude the following complex attributes from the response. See clause 4.3.3 for details. The NFVO shall support this parameter. The following attributes shall be excluded from the PnfdInfo structure in the response body if this parameter is provided, or none of the parameters \"all_fields,\" \"fields\", \"exclude_fields\", \"exclude_default\" are provided: userDefinedData.") @Valid @RequestParam(value = "exclude_default", required = false) String excludeDefault,
			@ApiParam(value = "Include all complex attributes in the response. See clause 4.3.3 for details. The NFVO shall support this parameter.") @Valid @RequestParam(value = "all_fields", required = false) String allFields) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<List<PnfdInfo>>(objectMapper.readValue(
						"[ {  \"pnfdName\" : \"pnfdName\",  \"pnfdUsageState\" : { },  \"pnfdOnboardingState\" : { },  \"_links\" : {    \"pnfd_content\" : \"http://example.com/aeiou\",    \"self\" : \"http://example.com/aeiou\"  },  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"pnfdProvider\" : \"pnfdProvider\",  \"pnfdVersion\" : \"pnfdVersion\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"userDefinedData\" : { }}, {  \"pnfdName\" : \"pnfdName\",  \"pnfdUsageState\" : { },  \"pnfdOnboardingState\" : { },  \"_links\" : {    \"pnfd_content\" : \"http://example.com/aeiou\",    \"self\" : \"http://example.com/aeiou\"  },  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"pnfdProvider\" : \"pnfdProvider\",  \"pnfdVersion\" : \"pnfdVersion\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"userDefinedData\" : { }} ]",
						List.class), HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<List<PnfdInfo>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<List<PnfdInfo>>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<NsdmSubscription> getSubscription(
			@ApiParam(value = "", required = true) @PathVariable("subscriptionId") String subscriptionId) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<NsdmSubscription>(objectMapper.readValue(
						"{  \"filter\" : {    \"nsdOnboardingState\" : { },    \"pnfdName\" : \"pnfdName\",    \"pnfdUsageState\" : { },    \"nsdInfoId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"pnfdOnboardingState\" : { },    \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdUsageState\" : { },    \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdDesigner\" : \"nsdDesigner\",    \"nsdVersion\" : \"nsdVersion\",    \"pnfdProvider\" : \"pnfdProvider\",    \"pnfdVersion\" : \"pnfdVersion\",    \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdName\" : \"nsdName\",    \"notificationTypes\" : [ { }, { } ],    \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdOperationalState\" : { }  },  \"_links\" : {    \"self\" : \"http://example.com/aeiou\"  },  \"callbackUri\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}",
						NsdmSubscription.class), HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<NsdmSubscription>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<NsdmSubscription>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<List<NsdmSubscription>> getSubscriptions() {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<List<NsdmSubscription>>(objectMapper.readValue(
						"[ {  \"filter\" : {    \"nsdOnboardingState\" : { },    \"pnfdName\" : \"pnfdName\",    \"pnfdUsageState\" : { },    \"nsdInfoId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"pnfdOnboardingState\" : { },    \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdUsageState\" : { },    \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdDesigner\" : \"nsdDesigner\",    \"nsdVersion\" : \"nsdVersion\",    \"pnfdProvider\" : \"pnfdProvider\",    \"pnfdVersion\" : \"pnfdVersion\",    \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdName\" : \"nsdName\",    \"notificationTypes\" : [ { }, { } ],    \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdOperationalState\" : { }  },  \"_links\" : {    \"self\" : \"http://example.com/aeiou\"  },  \"callbackUri\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}, {  \"filter\" : {    \"nsdOnboardingState\" : { },    \"pnfdName\" : \"pnfdName\",    \"pnfdUsageState\" : { },    \"nsdInfoId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"pnfdOnboardingState\" : { },    \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdUsageState\" : { },    \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdDesigner\" : \"nsdDesigner\",    \"nsdVersion\" : \"nsdVersion\",    \"pnfdProvider\" : \"pnfdProvider\",    \"pnfdVersion\" : \"pnfdVersion\",    \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdName\" : \"nsdName\",    \"notificationTypes\" : [ { }, { } ],    \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdOperationalState\" : { }  },  \"_links\" : {    \"self\" : \"http://example.com/aeiou\"  },  \"callbackUri\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"} ]",
						List.class), HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<List<NsdmSubscription>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<List<NsdmSubscription>>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<NsdInfoModifications> updateNSDInfo(
			@ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId,
			@ApiParam(value = "", required = true) @Valid @RequestBody NsdInfoModifications body) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<NsdInfoModifications>(
						objectMapper.readValue("{  \"nsdOperationalState\" : { },  \"userDefinedData\" : { }}",
								NsdInfoModifications.class),
						HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<NsdInfoModifications>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<NsdInfoModifications>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<PnfdInfoModifications> updatePNFDInfo(
			@ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId,
			@ApiParam(value = "", required = true) @Valid @RequestBody PnfdInfoModifications body) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<PnfdInfoModifications>(
						objectMapper.readValue("{  \"userDefinedData\" : { }}", PnfdInfoModifications.class),
						HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<PnfdInfoModifications>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<PnfdInfoModifications>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<?> uploadNSD(
			@ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId,
			@ApiParam(value = "", required = true) @RequestParam("file") MultipartFile body,
			//@ApiParam(value = "", required = true) @Valid @RequestBody String body,
			//@ApiParam(value = "", required = true) @Valid @RequestBody Object body,
			@ApiParam(value = "The payload body contains a copy of the file representing the NSD or a ZIP file that contains the file or multiple files representing the NSD, as specified above. The request shall set the \"Content-Type\" HTTP header as defined above.") @RequestHeader(value = "Content-Type", required = false) String contentType) {
	
		log.debug("Processing REST request for Uploading NSD content in NSD info " + nsdInfoId);
		if (body.isEmpty()) {
			return new ResponseEntity<String>("Error message: File is empty!", HttpStatus.BAD_REQUEST);
		}

		if (!contentType.startsWith("multipart/form-data")) {
			//TODO: to be implemented later on
			return new ResponseEntity<String>("Unable to parse content " + contentType, HttpStatus.NOT_IMPLEMENTED);
		} else {
			try {
				NsdContentType type = null;
				log.debug("NSD content file name is: " + body.getOriginalFilename());
				if (body.getOriginalFilename().endsWith("zip")) {
					type = NsdContentType.ZIP;
				} else if (body.getOriginalFilename().endsWith("yaml")) {
					type = NsdContentType.YAML;
				} else {
					//TODO: to be implemented later on
					return new ResponseEntity<String>("Unable to parse file type that is not .zip or .yaml", HttpStatus.NOT_IMPLEMENTED);
				}
				//nsdManagementService.uploadNsd(nsdInfoId, (String)body, NsdContentType.YAML);
				nsdManagementService.uploadNsd(nsdInfoId, body, type);
				log.debug("Upload processing done");
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
				//TODO: check if we need to introduce the asynchronous mode
			} catch (NotPermittedOperationException | AlreadyExistingEntityException e) {
				log.error("Impossible to upload NSD: " + e.getMessage());
				return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(), "Impossible to upload NSD: " + e.getMessage()), HttpStatus.CONFLICT);
			} catch (MalformattedElementException e) {
				log.error("Impossible to upload NSD: " + e.getMessage());
				return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(), "Impossible to upload NSD: " + e.getMessage()), HttpStatus.BAD_REQUEST);
			} catch (NotExistingEntityException e) {
				log.error("Impossible to upload NSD: " + e.getMessage());
				return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(), "Impossible to upload NSD: " + e.getMessage()), HttpStatus.NOT_FOUND);
			} catch (Exception e) {
				log.error("General exception while uploading NSD content: " + e.getMessage());
				return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(), "General exception while uploading NSD content: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		/*
		if (accept != null && accept.contains("application/json")) {
			try {
				return new ResponseEntity<Object>(objectMapper.readValue("\"{}\"", Object.class),
						HttpStatus.NOT_IMPLEMENTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
		*/
	}

	public ResponseEntity<Void> uploadPNFD(
			@ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId,
			@ApiParam(value = "", required = true) @Valid @RequestBody Object body,
			@ApiParam(value = "The request shall set the \"Content-Type\" HTTP header to \"text/plain\".") @RequestHeader(value = "Content-Type", required = false) String contentType) {
		String accept = request.getHeader("Accept");
		return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
	}

}
