package it.nextworks.nfvmano.catalogue.plugins.utils;

import java.util.Map;

import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;

public final class PluginKafkaConsumerUtils {
	
	public static KafkaMessageListenerContainer<Integer, String> createContainer(ContainerProperties containerProps,
																				 Map<String, Object> comsumerProps) {
	    DefaultKafkaConsumerFactory<Integer, String> cf =
	                             new DefaultKafkaConsumerFactory<Integer, String>(comsumerProps);
	    KafkaMessageListenerContainer<Integer, String> container =
	                                      new KafkaMessageListenerContainer<>(cf, containerProps);
	    return container;
	}
}
