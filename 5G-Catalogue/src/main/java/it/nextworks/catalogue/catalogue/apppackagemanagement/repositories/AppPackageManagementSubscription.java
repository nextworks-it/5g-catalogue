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
package it.nextworks.catalogue.catalogue.apppackagemanagement.repositories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.nextworks.catalogue.catalogue.apppackagemanagement.AppdManagementRestConsumer;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Internal entity used to store the active subscriptions for
 * MEC app package management notifications
 * 
 * @author nextworks
 *
 */
@Entity
public class AppPackageManagementSubscription {
	
	@Id
    @GeneratedValue
    @JsonIgnore
    private Long id;
	
	@Embedded
	private AppdManagementRestConsumer consumer;
	
	//TODO: add filters

	public AppPackageManagementSubscription() {	}
	
	/**
	 * Constructor
	 * 
	 * @param consumer consumer of the notification
	 */
	public AppPackageManagementSubscription(AppdManagementRestConsumer consumer) {
		this.consumer = consumer;
	}

	/**
	 * @return the consumer
	 */
	public AppdManagementRestConsumer getConsumer() {
		return consumer;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	
	

}
