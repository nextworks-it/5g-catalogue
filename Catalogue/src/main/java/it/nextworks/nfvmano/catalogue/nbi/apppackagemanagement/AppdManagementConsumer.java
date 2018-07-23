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
package it.nextworks.nfvmano.catalogue.nbi.apppackagemanagement;

import it.nextworks.nfvmano.libs.catalogues.interfaces.MecAppPackageManagementConsumerInterface;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.AppPackageOnBoardingNotification;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.AppPackageStateChangeNotification;
import it.nextworks.nfvmano.catalogue.common.ConsumerType;

/**
 * Abstract class which represents a consumer for MEC app package management notifications.
 * It must be extended by specific types of consumers which implements the actual
 * interaction with the external entities to be notified.
 * 
 * @author nextworks
 *
 */
public abstract class AppdManagementConsumer implements MecAppPackageManagementConsumerInterface {

	protected ConsumerType type;
	
	public AppdManagementConsumer() { }
	
	public AppdManagementConsumer(ConsumerType type) {
		this.type = type;
	}

	@Override
	public void notify(AppPackageOnBoardingNotification notification) {	}

	@Override
	public void notify(AppPackageStateChangeNotification notification) { }

	/**
	 * @return the type
	 */
	public ConsumerType getType() {
		return type;
	}
	
	

}
