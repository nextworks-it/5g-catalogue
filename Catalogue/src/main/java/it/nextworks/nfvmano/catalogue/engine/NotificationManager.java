package it.nextworks.nfvmano.catalogue.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import it.nextworks.nfvmano.catalogue.messages.CatalogueMessage;
import it.nextworks.nfvmano.catalogue.messages.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.messages.NsdChangeNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.ScopeType;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdDeletionNotification;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdOnboardingNotification;
import it.nextworks.nfvmano.catalogue.plugins.KafkaConnector;
import it.nextworks.nfvmano.catalogue.plugins.mano.NsdNotificationsConsumerInterface;
import it.nextworks.nfvmano.catalogue.plugins.mano.NsdNotificationsProducerInterface;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;

@Service
public class NotificationManager implements NsdNotificationsConsumerInterface, NsdNotificationsProducerInterface {

	private static final Logger log = LoggerFactory.getLogger(NotificationManager.class);

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	private KafkaConnector connector;

	@Value("${kafka.skip.send}")
	private boolean skipKafka;

	@Value("${kafka.bootstrap-servers}")
	private String kafkaBootstrapServers;

	@Value("${kafkatopic.local.nsd}")
	private String localNsdNotificationTopic;

	@Value("${kafkatopic.remote.nsd}")
	private String remoteNsdNotificationTopic;

	@Autowired
	private NsdManagementService nsdMgmtService;

	public NotificationManager() {
		log.debug("Building notification manager.");
	}

	@PostConstruct
	public void init() {
		log.debug("Initializing notification manager.");
		initializeKafkaConnector("ENGINE_NOTIFICATION_MANAGER", kafkaBootstrapServers, remoteNsdNotificationTopic);
	}

	private void initializeKafkaConnector(String connectorId, String server, String topicQueueExchange) {
		log.debug("Initializing Kafka connector with: \nconnectorId: " + connectorId + "\nkafkaServer: " + server
				+ "\ntopic: " + topicQueueExchange);
		Map<CatalogueMessageType, Consumer<CatalogueMessage>> functor = new HashMap<>();
		functor.put(CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION, msg -> {
			NsdOnBoardingNotificationMessage castMsg = (NsdOnBoardingNotificationMessage) msg;
			acceptNsdOnBoardingNotification(castMsg);
		});

		setConnector(KafkaConnector.Builder().setBeanId(connectorId).setKafkaBootstrapServers(server)
				.setKafkaGroupId(connectorId).addTopic(topicQueueExchange).setFunctor(functor).build());
		connector.init();
		log.debug("Kafka connector initialized.");
	}

	@Override
	public void sendNsdOnBoardingNotification(String nsdInfoId, String nsdId, UUID operationId)
			throws FailedOperationException, MalformattedElementException, MethodNotImplementedException {
		try {
			log.info("Sending nsdOnBoardingNotification for NSD " + nsdId);

			NsdOnBoardingNotificationMessage msg = new NsdOnBoardingNotificationMessage(nsdInfoId, nsdId, operationId,
					ScopeType.LOCAL, OperationStatus.SENT);

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			mapper.setSerializationInclusion(Include.NON_EMPTY);

			String json = mapper.writeValueAsString(msg);

			log.debug("Sending json message over kafka bus on topic " + localNsdNotificationTopic + "\n" + json);

			if (skipKafka) {
				log.debug(" ---- TEST MODE: skipping post to kafka bus -----");
			} else {
				kafkaTemplate.send(localNsdNotificationTopic, json);

				log.debug("Message sent.");
			}
		} catch (Exception e) {
			log.error("Error while posting NsdOnBoardingNotificationMessage to Kafka bus");
			throw new FailedOperationException(e.getMessage());
		}
	}

	@Override
	public void sendNsdChangeNotification(NsdChangeNotificationMessage notification)
			throws MethodNotImplementedException {
		throw new MethodNotImplementedException("sendNsdChangeNotification method not implemented");
	}

	@Override
	public void sendNsdDeletionNotification(NsdDeletionNotificationMessage notification)
			throws MethodNotImplementedException, FailedOperationException {
		try {
			log.info("Sending nsdOnBoardingNotification for NSD with nsdId: " + notification.getNsdId());

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			mapper.setSerializationInclusion(Include.NON_EMPTY);

			String json = mapper.writeValueAsString(notification);

			log.debug("Sending json message over kafka bus on topic " + localNsdNotificationTopic + "\n" + json);

			if (skipKafka) {
				log.debug(" ---- TEST MODE: skipping post to kafka bus -----");
			} else {
				kafkaTemplate.send(localNsdNotificationTopic, json);

				log.debug("Message sent.");
			}
		} catch (Exception e) {
			log.error("Error while posting NsdDeletionNotificationMessage to Kafka bus");
			throw new FailedOperationException(e.getMessage());
		}
	}

	@Override
	public void sendPnfdOnBoardingNotification(PnfdOnboardingNotification notification)
			throws MethodNotImplementedException {
		throw new MethodNotImplementedException("sendPnfdOnBoardingNotification method not implemented");
	}

	@Override
	public void sendPnfdDeletionNotification(PnfdDeletionNotification notification)
			throws MethodNotImplementedException {
		throw new MethodNotImplementedException("sendPnfdDeletionNotification method not implemented");
	}

	@Override
	public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
		log.info("Received NSD onboarding notification for NSD {} with info id {}, from plugin {}.",
				notification.getNsdId(), notification.getNsdInfoId(), notification.getPluginId());
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		mapper.setSerializationInclusion(Include.NON_EMPTY);

		try {
			String json = mapper.writeValueAsString(notification);
			log.debug("RECEIVED MESSAGE: " + json);
		} catch (JsonProcessingException e) {
			log.error("Unable to parse received nsdOnboardingNotificationMessage: " + e.getMessage());
		}
		
		switch (notification.getScope()) {
		case REMOTE:
			try {
				log.debug("Updating NsdInfoResource with plugin {} operation status {} for operation {}.",
						notification.getPluginId(), notification.getOpStatus(),
						notification.getOperationId().toString());
				nsdMgmtService.updateNsdInfoOperationStatus(notification.getNsdInfoId(), notification.getPluginId(),
						notification.getOpStatus(), notification.getType());
				log.debug("NsdInfoResource successfully updated with plugin {} operation status {} for operation {}.",
						notification.getPluginId(), notification.getOpStatus(),
						notification.getOperationId().toString());
			} catch (NotExistingEntityException e) {
				log.error(e.getMessage());
			}
			log.debug("Updating consumers internal mapping for operationId {} and plugin {}.",
					notification.getOperationId(), notification.getPluginId());
			nsdMgmtService.updateOperationInfoInConsumersMap(notification.getOperationId(), notification.getOpStatus(),
					notification.getPluginId(), notification.getNsdInfoId(), notification.getType());
			log.debug("Consumers internal mapping successfully updated for operationId {} and plugin {}.",
					notification.getOperationId(), notification.getPluginId());
			break;
		case LOCAL:
			log.error("Nsd LOCAL onboarding notification not handled here, REMOTE onboarding message expected.");
			break;
		case GLOBAL:
			log.error("Nsd GLOBAL onboarding notification not handled here, REMOTE onboarding message expected.");
			break;
		}
	}

	@Override
	public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification)
			throws MethodNotImplementedException {
		throw new MethodNotImplementedException("acceptNsdChangeNotification method not implemented");
	}

	@Override
	public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification)
			throws MethodNotImplementedException {
		throw new MethodNotImplementedException("acceptNsdDeletionNotification method not implemented");
	}

	@Override
	public void acceptPnfdOnBoardingNotification(PnfdOnboardingNotification notification)
			throws MethodNotImplementedException {
		throw new MethodNotImplementedException("acceptPnfdOnBoardingNotification method not implemented");
	}

	@Override
	public void acceptPnfdDeletionNotification(PnfdDeletionNotification notification)
			throws MethodNotImplementedException {
		throw new MethodNotImplementedException("acceptPnfdDeletionNotification method not implemented");
	}

	public KafkaConnector getConnector() {
		return connector;
	}

	public void setConnector(KafkaConnector connector) {
		this.connector = connector;
	}
}
