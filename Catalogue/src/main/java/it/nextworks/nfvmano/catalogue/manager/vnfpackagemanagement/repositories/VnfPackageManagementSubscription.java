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
package it.nextworks.nfvmano.catalogue.manager.vnfpackagemanagement.repositories;


import com.fasterxml.jackson.annotation.JsonIgnore;
import it.nextworks.nfvmano.catalogue.manager.vnfpackagemanagement.VnfPackageManagementRestConsumer;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


/**
 * Internal entity used to store the active subscriptions for
 * VNF package management notifications
 * 
 * @author nextworks
 *
 */
@Entity
public class VnfPackageManagementSubscription {

	@Id
    @GeneratedValue
    @JsonIgnore
    private Long id;
	
	@Embedded
	private VnfPackageManagementRestConsumer consumer;
	
	//TODO: add filters
	
	public VnfPackageManagementSubscription(/*VnfPackageManagementRestConsumer restConsumer*/) {}
	
	public VnfPackageManagementSubscription(VnfPackageManagementRestConsumer consumer) {
		this.consumer = consumer;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the consumer
	 */
	public VnfPackageManagementRestConsumer getConsumer() {
		return consumer;
	}
	
	

}
