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
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;

@Service
public class NotificationManager {

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
	}

	@PostConstruct
	public void init() {
		Map<CatalogueMessageType, Consumer<CatalogueMessage>> functor = new HashMap<>();
		functor.put(CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION, msg -> {
			NsdOnBoardingNotificationMessage castMsg = (NsdOnBoardingNotificationMessage) msg;
			acceptNsdOnBoardingNotification(castMsg);
		});

		String connectorId = "ENGINE_NOTIFICATION_MANAGER";

		connector = KafkaConnector.Builder()
				.setBeanId(connectorId)
				.setKafkaBootstrapServers(kafkaBootstrapServers)
				.setKafkaGroupId(connectorId)
				.addTopic(remoteNsdNotificationTopic)
				.setFunctor(functor)
				.build();
		connector.init();
	}

	void sendNsdOnBoardingNotification(String nsdInfoId, String nsdId, UUID operationId)
			throws FailedOperationException, MalformattedElementException, MethodNotImplementedException {
		try {
			log.info("Sending nsdOnBoardingNotification for NSD " + nsdId);

			NsdOnBoardingNotificationMessage msg = new NsdOnBoardingNotificationMessage(
			        nsdInfoId,
                    nsdId,
					operationId,
                    ScopeType.LOCAL,
                    OperationStatus.SENT
            );

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			mapper.setSerializationInclusion(Include.NON_EMPTY);

			String json = mapper.writeValueAsString(msg);

			log.debug("Sending json message over kafka bus on topic "
					+ localNsdNotificationTopic + "\n" + json);

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

	void sendNsdChangeNotification(NsdChangeNotificationMessage notification) throws MethodNotImplementedException {
		throw new MethodNotImplementedException("nsdChangeNotification method not implemented");
	}

	void sendNsdDeletionNotification(NsdDeletionNotificationMessage notification) throws MethodNotImplementedException {
		throw new MethodNotImplementedException("nsdDeletionNotification method not implemented");
	}

	void sendPnfdOnBoardingNotification(PnfdOnboardingNotification notification) throws MethodNotImplementedException {
		throw new MethodNotImplementedException("pnfdOnBoardingNotification method not implemented");
	}

	void sendPnfdDeletionNotification(PnfdDeletionNotification notification) throws MethodNotImplementedException {
		throw new MethodNotImplementedException("pnfdDeletionNotification method not implemented");
	}

	void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
		log.info("Received NSD onboarding notification for NSD {} with info id {}, from plugin {}.",
				notification.getNsdId(), notification.getNsdInfoId(), notification.getManoId());
		
		switch (notification.getScope()) {
		case REMOTE:
			log.debug("Updating consumers internal mapping for plugin {}.", notification.getManoId());
			
			nsdMgmtService.updateOperationIdToConsumersMap(notification.getOperationId(), notification.getOpStatus(),
					notification.getManoId());
			log.debug("Updating NsdInfoResource with plugin {} operation status {} for operation {}.",
					notification.getManoId(), notification.getOpStatus(), notification.getOperationId().toString());
			nsdMgmtService.updateNsdInfoOnboardingStatus(notification.getNsdInfoId(), notification.getManoId(),
					notification.getOpStatus());
		case LOCAL:
			log.error("Nsd LOCAL onboarding notification not handled here, REMOTE onboarding message expected.");
		case GLOBAL:
			log.error("Nsd GLOBAL onboarding notification not handled here, REMOTE onboarding message expected.");
		}

		log.debug("NsdInfoResource successfully updated with plugin {} operation status {} for operation {}.",
				notification.getManoId(), notification.getOpStatus(), notification.getOperationId().toString());
	}
}
