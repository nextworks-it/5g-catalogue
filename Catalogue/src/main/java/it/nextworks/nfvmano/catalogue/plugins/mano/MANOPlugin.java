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

import it.nextworks.nfvmano.catalogue.common.ConfigurationParameters;
import it.nextworks.nfvmano.catalogue.messages.CatalogueMessage;
import it.nextworks.nfvmano.catalogue.messages.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.messages.NsdChangeNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.plugins.KafkaConnector;
import it.nextworks.nfvmano.catalogue.plugins.Plugin;
import it.nextworks.nfvmano.catalogue.plugins.PluginType;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MANOPlugin
		extends Plugin
		implements NsdNotificationsConsumerInterface, VnfdNotificationsConsumerInterface
{

	private static final Logger log = LoggerFactory.getLogger(MANOPlugin.class);
	
	protected MANOType manoType;
	protected MANO mano;
	protected KafkaConnector connector;
	
	public MANOPlugin(MANOType manoType, MANO mano, String kafkaBootstrapServers) {
		super(mano.getManoId(), PluginType.MANO);
		this.manoType = manoType;
		this.mano = mano;
		String connectorID = "MANO" + mano.getManoId(); // assuming it's unique among MANOs
		Map<CatalogueMessageType, Consumer<CatalogueMessage>> functor = new HashMap<>();
		functor.put(
				CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION,
				msg -> {
					NsdOnBoardingNotificationMessage castMsg = (NsdOnBoardingNotificationMessage) msg;
					acceptNsdOnBoardingNotification(castMsg);
				}
		);
		functor.put(
				CatalogueMessageType.NSD_CHANGE_NOTIFICATION,
				msg -> {
					NsdChangeNotificationMessage castMsg = (NsdChangeNotificationMessage) msg;
					try {
						acceptNsdChangeNotification(castMsg);
					} catch (MethodNotImplementedException e) {
						log.error("Method not yet implemented: " + e.getMessage());
					}
				}
		);
		functor.put(
				CatalogueMessageType.NSD_DELETION_NOTIFICATION,
				msg -> {
                    NsdDeletionNotificationMessage castMsg = (NsdDeletionNotificationMessage) msg;
					try {
						acceptNsdDeletionNotification(castMsg);
					} catch (MethodNotImplementedException e) {
						log.error("Method not yet implemented: " + e.getMessage());
					}
				}
		);
		functor.put(
				CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION,
				msg -> {
					try {
						acceptPnfdOnBoardingNotification(null);
					} catch (MethodNotImplementedException e) {
						log.error("Method not yet implemented: " + e.getMessage());
					}
				}
		);
		functor.put(
				CatalogueMessageType.PNFD_DELETION_NOTIFICATION,
				msg -> {
					try {
						acceptPnfdDeletionNotification(null);
					} catch (MethodNotImplementedException e) {
						log.error("Method not yet implemented: " + e.getMessage());
					}
				}
		);
		connector = KafkaConnector.Builder()
				.setBeanId(connectorID)
				.setKafkaBootstrapServers(kafkaBootstrapServers)
				.setKafkaGroupId(connectorID)
				.addTopic(ConfigurationParameters.kafkaLocalOnboardingNotificationsTopicQueueExchange)
				.setFunctor(functor)
				.build();
	}

	@Override
	public void init() {
		connector.init();
	}

	public MANOType getManoType() {
		return manoType;
	}

	public void setManoType(MANOType manoType) {
		this.manoType = manoType;
	}

	public MANO getMano() {
		return mano;
	}

	public void setMano(MANO mano) {
		this.mano = mano;
	}
}
