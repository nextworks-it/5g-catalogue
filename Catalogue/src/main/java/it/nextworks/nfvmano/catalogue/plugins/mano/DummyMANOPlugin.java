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

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.nextworks.nfvmano.catalogue.messages.CatalogueMessage;
import it.nextworks.nfvmano.catalogue.messages.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.plugins.utils.PluginKafkaConsumerUtils;

public class DummyMANOPlugin extends MANOPlugin {

	private static final Logger log = LoggerFactory.getLogger(DummyMANOPlugin.class);
	
	public DummyMANOPlugin(MANOType manoType, MANO mano, String kafkaBootstrapServers) {
		super(manoType, mano, kafkaBootstrapServers);
		// TODO Auto-generated constructor stub
	}
	
	public void init() {
		try {
			//Initialize the Kafka listener container service for this consumer/plugin
			
			//TODO: to pass topics (and partitions?) instead of hardcode
			
			//Create container properties listing topics to subscribe
			ContainerProperties containerProps = new ContainerProperties("onboard");
			//Create new listener container
		    KafkaMessageListenerContainer<Integer, String> container =
		    		PluginKafkaConsumerUtils.createContainer(containerProps, consumerProps());
		    container.setBeanName(mano.getManoId());
		    //setup the message listeners overriding MessageListener methods - PUT here the plugin receiver logic
		    container.setupMessageListener(new MessageListener<Integer, String>() {
	
		        @Override
		        public void onMessage(ConsumerRecord<Integer, String> message) {
		        	log.info("Received message from kafka bus \n" + message.value());
		    		
		    		try {
		    			ObjectMapper mapper = new ObjectMapper();
		    			CatalogueMessage catalogueMessage = (CatalogueMessage) mapper.readValue(message.value(), CatalogueMessage.class);
		    			CatalogueMessageType type = catalogueMessage.getType();
		    			
		    			switch (type) {
		    				case NSD_ONBOARDING_NOTIFICATION:
		    					log.info("received NSD_ONBOARDING_NOTIFICATION");
		    					break;
		    				default:
		    					break;
		    			}
		    		} catch (Exception e) {
		    			log.error("Error while receiving message from kafka bus: " + e.getMessage());
		    		}
		        }
	
		    });
		    //Start the Kafka listener container service
		    container.start();
		    
		} catch (Exception e) {
			log.error("Cannot initialize Kafka listener container service for consumer dummy-mano " + mano.getManoId());
		}
	}
	
	private Map<String, Object> consumerProps() {
	    Map<String, Object> props = new HashMap<>();
	    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
	    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
	    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
	    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 500);
	    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
	    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
	    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
	    return props;
	}
}
