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
package it.nextworks.catalogue.catalogue.vnfpackagemanagement;

import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.VnfPackageChangeNotification;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.VnfPackageOnboardingNotification;
import it.nextworks.catalogue.common.ConsumerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * REST-based consumer of VNF package management notifications
 * 
 * @author nextworks
 *
 */
@Embeddable
public class VnfPackageManagementRestConsumer extends VnfPackageManagementConsumer {

	private static final Logger log = LoggerFactory.getLogger(VnfPackageManagementRestConsumer.class);
	
	private String callbackUrl;
	private String authentication;	//this is for further definition
	
	@Transient
	private RestTemplate restTemplate;
	
	public VnfPackageManagementRestConsumer() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Constructor
	 * 
	 * @param callbackUrl URL to be invoked to send a notification to the external entity
	 * @param authentication credentials to be used - not yet supported
	 */
	public VnfPackageManagementRestConsumer(String callbackUrl, String authentication) {
		super(ConsumerType.REST);
		this.callbackUrl = callbackUrl;
		this.authentication = authentication;
		this.restTemplate = new RestTemplate();
	}

	/**
	 * @return the callbackUrl
	 */
	public String getCallbackUrl() {
		return callbackUrl;
	}

	/**
	 * @return the authentication
	 */
	public String getAuthentication() {
		return authentication;
	}
	
	@Override
	public void notify(VnfPackageOnboardingNotification notification) {
		log.debug("Notifying VNF Package on boarding");
		try {
			restTemplate.postForLocation(new URI(this.callbackUrl), notification);
			log.debug("VNF Package on boarding notification sent to " + this.callbackUrl);
		} catch (URISyntaxException exception) {
			log.error("Impossible to send VNF Package on boarding notification: malformed URI - " + this.callbackUrl);
		} catch (RestClientException exception) {
			log.error("Impossible to send VNF Package on boarding notification: rest client error");
		}
	}

	@Override
	public void notify(VnfPackageChangeNotification notification) {
		log.debug("Notifying VNF Package change");
		try {
			restTemplate.postForLocation(new URI(this.callbackUrl), notification);
			log.debug("VNF Package change notification sent to " + this.callbackUrl);
		} catch (URISyntaxException exception) {
			log.error("Impossible to send VNF Package change notification: malformed URI - " + this.callbackUrl);
		} catch (RestClientException exception) {
			log.error("Impossible to send VNF Package change notification: REST client error");
		}
	}

}
