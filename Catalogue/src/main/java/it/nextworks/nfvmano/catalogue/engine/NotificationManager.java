package it.nextworks.nfvmano.catalogue.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import it.nextworks.nfvmano.catalogue.common.ConfigurationParameters;
import it.nextworks.nfvmano.catalogue.messages.NsdChangeNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdDeletionNotification;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdOnboardingNotification;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;

@Service
public class NotificationManager {

	private static final Logger log = LoggerFactory.getLogger(NotificationManager.class);
	
	@Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
	
	@Value("${kafka.skip.send}")
	private boolean skipKafka;
	
	public NotificationManager() { }

	void nsdOnBoardingNotification(String nsdInfoId, String nsdId) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException {
		try {
			log.info("Sending nsdOnBoardingNotification for NSD " + nsdId);
			
			NsdOnBoardingNotificationMessage msg =
	                new NsdOnBoardingNotificationMessage(nsdInfoId, nsdId);
	
	        ObjectMapper mapper = new ObjectMapper();
	        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	        mapper.setSerializationInclusion(Include.NON_EMPTY);
	
	        String json = mapper.writeValueAsString(msg);
	
	        log.debug("Sending json message over kafka bus on topic " + 
	        ConfigurationParameters.kafkaOnboardingNotificationsTopicQueueExchange + "\n" + json);
	
	        if (skipKafka) {
	        	log.debug(" ---- TEST MODE: skipping post to kafka bus -----");
	        } else {
		        kafkaTemplate.send(ConfigurationParameters.kafkaOnboardingNotificationsTopicQueueExchange, json);
		        
		        log.debug("Message sent.");
	        }
	        
		} catch (Exception e) {
			log.error("Error while posting NsdOnBoardingNotificationMessage to Kafka bus");
			throw new FailedOperationException(e.getMessage());
		}
	}


    void nsdChangeNotification(NsdChangeNotificationMessage notification) throws MethodNotImplementedException {
    	throw new MethodNotImplementedException("nsdChangeNotification method not implemented");
    }



    void nsdDeletionNotification(NsdDeletionNotificationMessage notification) throws MethodNotImplementedException {
    	throw new MethodNotImplementedException("nsdDeletionNotification method not implemented");
    }

    void pnfdOnBoardingNotification(PnfdOnboardingNotification notification) throws MethodNotImplementedException {
    	throw new MethodNotImplementedException("pnfdOnBoardingNotification method not implemented");
    }


    void pnfdDeletionNotification(PnfdDeletionNotification notification) throws MethodNotImplementedException {
    	throw new MethodNotImplementedException("pnfdDeletionNotification method not implemented");
    }	
}
