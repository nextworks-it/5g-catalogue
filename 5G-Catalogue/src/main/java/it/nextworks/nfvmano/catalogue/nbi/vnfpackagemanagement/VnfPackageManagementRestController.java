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
package it.nextworks.nfvmano.catalogue.nbi.vnfpackagemanagement;

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

/**
 * This class implements the REST APIs for the VNF Package management
 * service of the NFVO. 
 * The internal implementation of the service is implemented through
 * the VnfPackageManagementService.
 * 
 * @author nextworks
 *
 */
@RestController
@CrossOrigin
@RequestMapping("/nfvo/vnfdManagement")
public class VnfPackageManagementRestController {
	
	private static final Logger log = LoggerFactory.getLogger(VnfPackageManagementRestController.class);

	@Autowired
	VnfPackageManagementService vnfPackageManagement;
	
	public VnfPackageManagementRestController() { }
	
	@RequestMapping(value = "/vnfPackage", method = RequestMethod.POST)
	public ResponseEntity<?> onboardVnfPackage(@RequestBody OnBoardVnfPackageRequest request) {
		log.debug("Received on board VNF package request");
		try {
			request.isValid();
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		try {
			OnBoardVnfPackageResponse response = vnfPackageManagement.onBoardVnfPackage(request);
			return new ResponseEntity<OnBoardVnfPackageResponse>(response, HttpStatus.CREATED);
		} catch (MethodNotImplementedException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (AlreadyExistingEntityException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.CONFLICT);
		} catch (FailedOperationException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/vnfPackage/{vnfPackageId}/enable", method = RequestMethod.PUT)
	public ResponseEntity<?> enableVnfPackage(@PathVariable String vnfPackageId) {
		log.debug("Received request to enable VNF package " + vnfPackageId);
		EnableVnfPackageRequest request = new EnableVnfPackageRequest(vnfPackageId);
		try {
			vnfPackageManagement.enableVnfPackage(request);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MethodNotImplementedException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (NotExistingEntityException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (FailedOperationException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} 
	}
	
	@RequestMapping(value = "/vnfPackage/{vnfPackageId}/disable", method = RequestMethod.PUT)
	public ResponseEntity<?> disableVnfPackage(@PathVariable String vnfPackageId) {
		log.debug("Received request to disable VNF package " + vnfPackageId);
		DisableVnfPackageRequest request = new DisableVnfPackageRequest(vnfPackageId);
		try {
			vnfPackageManagement.disableVnfPackage(request);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MethodNotImplementedException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (NotExistingEntityException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (FailedOperationException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/vnfPackage/{vnfPackageId}/delete", method = RequestMethod.PUT)
	public ResponseEntity<?> deleteVnfPackage(@PathVariable String vnfPackageId) {
		log.debug("Received request to delete VNF package " + vnfPackageId);
		DeleteVnfPackageRequest request = new DeleteVnfPackageRequest(vnfPackageId);
		try {
			vnfPackageManagement.deleteVnfPackage(request);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MethodNotImplementedException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (NotExistingEntityException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (FailedOperationException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/vnfPackage/query", method = RequestMethod.POST)
	public ResponseEntity<?> queryVnfPackage(@RequestBody GeneralizedQueryRequest request) {
		log.debug("Received query VNF package request");
		try {
			request.isValid();
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		try {
			QueryOnBoardedVnfPkgInfoResponse response = vnfPackageManagement.queryVnfPackageInfo(request);
			return new ResponseEntity<QueryOnBoardedVnfPkgInfoResponse>(response, HttpStatus.OK);
		} catch (MethodNotImplementedException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (NotExistingEntityException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "/vnfPackage/subscription", method = RequestMethod.POST)
	public ResponseEntity<?> subscribe(@RequestBody SubscribeRequest request) {
		log.debug("Received VNF package subscribe request");
		try {
			request.isValid();
		} catch (MalformattedElementException e) {
			log.error("Malformatted request: " + e.getMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		try {
			String response = vnfPackageManagement.subscribeVnfPackageInfo(request, new VnfPackageManagementRestConsumer(request.getCallbackUri(), null));
			return new ResponseEntity<String>(response, HttpStatus.CREATED);
		} catch (MethodNotImplementedException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (FailedOperationException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (MalformattedElementException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "/vnfPackage/subscription/{subscriptionId}", method = RequestMethod.DELETE)
	public ResponseEntity<?> unsubscribe(@PathVariable String subscriptionId) {
		log.debug("Received VNF pacakge unsubscribe request for subscription " + subscriptionId);
		try {
			vnfPackageManagement.unsubscribeVnfPackageInfo(subscriptionId);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MethodNotImplementedException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (NotExistingEntityException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (MalformattedElementException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	//TODO: add methods to fetch VNF package, artifacts etc.
}
