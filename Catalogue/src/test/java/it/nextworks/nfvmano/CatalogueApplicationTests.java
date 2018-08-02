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
package it.nextworks.nfvmano;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOnboardingFailureNotification;
import it.nextworks.nfvmano.catalogue.plugins.mano.DummyMANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CatalogueApplicationTests {
	
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	
	@Value("${kafka.bootstrap-servers}")
	private String kafkaBootstrapServers;
	
	@Test
	public void contextLoads() {
	}
	
	@Test
	public void testKafkaSendReceive() throws Exception {
		/*
		//create fake MANO
		MANO mano = new MANO("test-dummy", MANOType.DUMMY);
		
		//create dummy mano plugin
		DummyMANOPlugin plugin = new DummyMANOPlugin(MANOType.DUMMY, mano, kafkaBootstrapServers);		
		plugin.init();
		
		System.out.println("\nWait for consumer to setup ... \n");
		Thread.sleep(1500);
		
		NsdOnBoardingNotificationMessage msg = new NsdOnBoardingNotificationMessage("test-nsd");
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		mapper.setSerializationInclusion(Include.NON_EMPTY);

		String json = mapper.writeValueAsString(msg);
		
		System.out.println("\nSending message over kafka bus on topic 'onboard'\n" + json + "\n");
		
		kafkaTemplate.send("onboard", json);
		
		//System.out.println("\nWait for consumer to process ... \n");
		//Thread.sleep(5000);
		*/
  }
}
