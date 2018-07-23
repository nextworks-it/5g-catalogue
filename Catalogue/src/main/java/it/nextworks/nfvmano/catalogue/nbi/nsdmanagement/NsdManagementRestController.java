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

import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.*;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import it.nextworks.nfvmano.libs.common.messages.GeneralizedQueryRequest;
import it.nextworks.nfvmano.libs.common.messages.SubscribeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * This class implements the REST APIs for the NSD management
 * service of the NFVO. 
 * The internal implementation of the service is implemented through
 * the NsdManagementService.
 * 
 * @author nextworks
 *
 */
@RestController
@CrossOrigin
@RequestMapping("/nfvo/nsdManagement")
public class NsdManagementRestController {

	private static final Logger log = LoggerFactory.getLogger(NsdManagementRestController.class);
	
	@Autowired
	NsdManagementService nsdManagement;
	
	public NsdManagementRestController() { }
	
	@RequestMapping(value = "/nsd", method = RequestMethod.POST)
	public ResponseEntity<?> onBoardNsd(@RequestBody OnboardNsdRequest request) {
		log.debug("Received on board NSD request");
		try {
			String nsdInfoId = nsdManagement.onboardNsd(request);
			return new ResponseEntity<String>(nsdInfoId, HttpStatus.CREATED);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (AlreadyExistingEntityException e) {
			log.error("Already existing entity: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.CONFLICT);
		} catch (Exception e) {
			log.error("General exception: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/nsd/delete", method = RequestMethod.POST)
	public ResponseEntity<?> deleteNsd(@RequestBody DeleteNsdRequest request) {
		log.debug("Received delete NSD request");
		try {
			DeleteNsdResponse response = nsdManagement.deleteNsd(request);
			return new ResponseEntity<DeleteNsdResponse>(response, HttpStatus.NO_CONTENT);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (NotExistingEntityException e) {
			log.error("Not existing entities: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}
	
	@RequestMapping(value = "/nsd/query", method = RequestMethod.POST)
	public ResponseEntity<?> queryNsd(@RequestBody GeneralizedQueryRequest request) {
		log.debug("Received query NSD request");
		try {
			QueryNsdResponse response = nsdManagement.queryNsd(request);
			return new ResponseEntity<QueryNsdResponse>(response, HttpStatus.OK);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (NotExistingEntityException e) {
			log.error("Not existing entities: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}
	
	@RequestMapping(value = "/nsdInfo/{nsdInfoId}/enable", method = RequestMethod.PUT)
	public ResponseEntity<?> enableNsd(@PathVariable String nsdInfoId) {
		log.debug("Received enable NSD request");
		EnableNsdRequest request = new EnableNsdRequest(nsdInfoId);
		try {
			request.isValid();
			nsdManagement.enableNsd(request);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (NotExistingEntityException e) {
			log.error("Not existing entities: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/nsdInfo/{nsdInfoId}/disable", method = RequestMethod.PUT)
	public ResponseEntity<?> disableNsd(@PathVariable String nsdInfoId) {
		log.debug("Received disable NSD request");
		DisableNsdRequest request = new DisableNsdRequest(nsdInfoId);
		try {
			request.isValid();
			nsdManagement.disableNsd(request);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (NotExistingEntityException e) {
			log.error("Not existing entities: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/nsdInfo/{nsdInfoId}/update", method = RequestMethod.PUT)
	public ResponseEntity<?> updateNsd(@RequestBody UpdateNsdRequest request) {
		log.debug("Received update NSD request");
		try {
			request.isValid();
			String nsdInfoId = nsdManagement.updateNsd(request);
			return new ResponseEntity<String>(nsdInfoId, HttpStatus.OK);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (NotExistingEntityException e) {
			log.error("Not existing entities: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (AlreadyExistingEntityException e) {
			log.error("Already existing entity: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.CONFLICT);
		}
	}
	
	@RequestMapping(value = "/nsd/subscription", method = RequestMethod.POST)
	public ResponseEntity<?> subscribe(@RequestBody SubscribeRequest request) {
		log.debug("Received NSD subscribe request");
		try {
			String subscriptionId = nsdManagement.subscribeNsdInfo(request, new NsdManagementRestConsumer(request.getCallbackUri(), null));
			return new ResponseEntity<String>(subscriptionId, HttpStatus.CREATED);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/nsd/subscription/{subscriptionId}", method = RequestMethod.DELETE)
	public ResponseEntity<?> unsubscribe(@PathVariable String subscriptionId) {
		log.debug("Received NSD unsubscribe request for subscription " + subscriptionId);
		try {
			nsdManagement.unsubscribeNsdInfo(subscriptionId);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (NotExistingEntityException e) {
			log.error("Not existing entities: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = "/nsd/{nsdInfoId}/internal/vnfsuserparameters", method = RequestMethod.GET)
	public ResponseEntity<?> queryVnfsUserParameters (@PathVariable String nsdInfoId) {
		log.debug("Received VNFs user parameters request for NSD" + nsdInfoId);

		Map<String, String> response = null;
		try {
			response = nsdManagement.findVnfsUserParameters(nsdInfoId);
		} catch (NotExistingEntityException e) {
			log.error("Not able to find NSD " + nsdInfoId);
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Map<String, String>>(response, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/pnfd", method = RequestMethod.POST)
	public ResponseEntity<?> onBoardPnfd(@RequestBody OnboardPnfdRequest request) {
		log.debug("Received on board PNFD request");
		try {
			String pnfdInfoId = nsdManagement.onboardPnfd(request);
			return new ResponseEntity<String>(pnfdInfoId, HttpStatus.CREATED);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (AlreadyExistingEntityException e) {
			log.error("Already existing entity: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.CONFLICT);
		} catch (Exception e) {
			log.error("General exception: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/pnfd/delete", method = RequestMethod.POST)
	public ResponseEntity<?> deletePnfd(@RequestBody DeletePnfdRequest request) {
		log.debug("Received delete PNFD request");
		try {
			DeletePnfdResponse response = nsdManagement.deletePnfd(request);
			return new ResponseEntity<DeletePnfdResponse>(response, HttpStatus.NO_CONTENT);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (NotExistingEntityException e) {
			log.error("Not existing entities: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}
	
	@RequestMapping(value = "/pnfd/query", method = RequestMethod.POST)
	public ResponseEntity<?> queryPnfd(@RequestBody GeneralizedQueryRequest request) {
		log.debug("Received query PNFD request");
		try {
			QueryPnfdResponse response = nsdManagement.queryPnfd(request);
			return new ResponseEntity<QueryPnfdResponse>(response, HttpStatus.OK);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (NotExistingEntityException e) {
			log.error("Not existing entities: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}
	
	@RequestMapping(value = "/pnfdInfo/{pnfdInfoId}/update", method = RequestMethod.PUT)
	public ResponseEntity<?> updatePnfd(@RequestBody UpdatePnfdRequest request) {
		log.debug("Received update PNFD request");
		try {
			request.isValid();
			String pnfdInfoId = nsdManagement.updatePnfd(request);
			return new ResponseEntity<String>(pnfdInfoId, HttpStatus.OK);
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (NotExistingEntityException e) {
			log.error("Not existing entities: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (MethodNotImplementedException e) {
			log.error("Method not implemented. " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (FailedOperationException e) {
			log.error("Operation failed: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (AlreadyExistingEntityException e) {
			log.error("Already existing entity: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.CONFLICT);
		}
	}
}
