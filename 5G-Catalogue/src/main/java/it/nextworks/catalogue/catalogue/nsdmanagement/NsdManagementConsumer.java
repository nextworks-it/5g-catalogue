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
package it.nextworks.catalogue.catalogue.nsdmanagement;

import it.nextworks.nfvmano.libs.catalogues.interfaces.NsdManagementConsumerInterface;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.NsdChangeNotification;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.NsdOnBoardingNotification;
import it.nextworks.catalogue.common.ConsumerType;

/**
 * Abstract class which represents a consumer for NSD management notifications.
 * It must be extended by specific types of consumers which implements the actual
 * interaction with the external entities to be notified.
 * 
 * @author nextworks
 *
 */
public abstract class NsdManagementConsumer implements NsdManagementConsumerInterface {

	protected ConsumerType type;
	
	public NsdManagementConsumer() { }
	
	/**
	 * Constructor
	 * 
	 * @param type type of the consumer; it specifies the kind of interaction with the external entities
	 */
	public NsdManagementConsumer(ConsumerType type) {
		this.type = type;
	}

	@Override
	public void notify(NsdOnBoardingNotification notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notify(NsdChangeNotification notification) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the type
	 */
	public ConsumerType getType() {
		return type;
	}
	
	

}
