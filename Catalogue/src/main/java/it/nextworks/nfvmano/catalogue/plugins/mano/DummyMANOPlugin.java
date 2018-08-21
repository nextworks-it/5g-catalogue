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
package it.nextworks.nfvmano.catalogue.plugins.mano;

import it.nextworks.nfvmano.catalogue.messages.NsdChangeNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdDeletionNotification;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdOnboardingNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyMANOPlugin extends MANOPlugin {

	private static final Logger log = LoggerFactory.getLogger(DummyMANOPlugin.class);

	public DummyMANOPlugin(MANOType manoType, MANO mano, String kafkaBootstrapServers) {
		super(manoType, mano, kafkaBootstrapServers);
	}

	@Override
	public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
		log.info("Received NSD onboarding notification.");
		log.debug("Body: {}", notification);
	}

	@Override
	public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) {
		log.info("Received NSD change notification.");
		log.debug("Body: {}", notification);
	}

	@Override
	public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) {
		log.info("Received NSD deletion notification.");
		log.debug("Body: {}", notification);
	}

	@Override
	public void acceptPnfdOnBoardingNotification(PnfdOnboardingNotification notification) {
		log.info("Received PNFD onboarding notification.");
		log.debug("Body: {}", notification);
	}

	@Override
	public void acceptPnfdDeletionNotification(PnfdDeletionNotification notification) {
		log.info("Received PNFD deletion notification.");
		log.debug("Body: {}", notification);
	}
}
