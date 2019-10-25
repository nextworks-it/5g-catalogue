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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.catalogueNotificaton.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.dummy.DummyMano;
import it.nextworks.nfvmano.catalogue.plugins.mano.DummyMANOPlugin;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class CatalogueApplicationTests {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${kafkatopic.local}")
    private String localNotificationTopic;

    @Value("${kafkatopic.remote}")
    private String remoteNotificationTopic;

    @Test
    @Ignore
    public void contextLoads() {
    }

    // Infrastructure required (Kafka), should not be run by default (hence @Ignore)
    // It's here for practicality (to be run via IDE)
    // NOTE: remove ignore to run (the spring runner will refuse to run it otherwise)
    @Test
    @Ignore
    public void testKafkaSendReceive() throws Exception {

        //create fake MANO
        MANO mano = new DummyMano("test-dummy", MANOType.DUMMY);

        //create dummy mano plugin
        DummyMANOPlugin plugin = new DummyMANOPlugin(
                MANOType.DUMMY,
                mano,
                kafkaBootstrapServers,
                null,
                null,
                null,
                localNotificationTopic,
                remoteNotificationTopic,
                kafkaTemplate,
                false
        );
        plugin.init();

        System.out.println("Wait for consumer to setup ...");
        Thread.sleep(1500);

        NsdOnBoardingNotificationMessage msg =
                new NsdOnBoardingNotificationMessage(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        "1.0",
                        null,
                        UUID.randomUUID(),
                        ScopeType.LOCAL,
                        OperationStatus.SENT,
                        null,
                        null,
                        null
                );

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        String json = mapper.writeValueAsString(msg);

        System.out.println("\n===============================================================================================");
        System.out.println("Sending message over kafka bus on topic " + localNotificationTopic + ":\n" + json);
        System.out.println("===============================================================================================\n");

        kafkaTemplate.send(localNotificationTopic, json);
    }

    // Infrastructure required, should not be run by default (hence @Ignore)
    // It's here for practicality (to be run via IDE)
    // NOTE: remove ignore to run (the spring runner will refuse to run it otherwise)
    @Test
    @Ignore
    public void testMultiReceiver() throws Exception {

        //create fake MANO
        MANO mano1 = new DummyMano("test-dummy1", MANOType.DUMMY);

        //create dummy mano plugin
        DummyMANOPlugin plugin1 = new DummyMANOPlugin(
                MANOType.DUMMY,
                mano1,
                kafkaBootstrapServers,
                null,
                null,
                null,
                localNotificationTopic,
                remoteNotificationTopic,
                kafkaTemplate,
                false
        );
        plugin1.init();

        //create fake MANO
        MANO mano2 = new DummyMano("test-dummy2", MANOType.DUMMY);

        //create dummy mano plugin
        DummyMANOPlugin plugin2 = new DummyMANOPlugin(
                MANOType.DUMMY,
                mano2,
                kafkaBootstrapServers,
                null,
                null,
                null,
                localNotificationTopic,
                remoteNotificationTopic,
                kafkaTemplate,
                false
        );
        plugin2.init();

        System.out.println("Wait for consumer to setup ...");
        Thread.sleep(1500);

        NsdOnBoardingNotificationMessage msg =
                new NsdOnBoardingNotificationMessage(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        "1.0",
                        null,
                        UUID.randomUUID(),
                        ScopeType.LOCAL,
                        OperationStatus.SENT,
                         null,
                        null,
                        null
                );


        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        String json = mapper.writeValueAsString(msg);

        System.out.println("\n===============================================================================================");
        System.out.println("Sending message over kafka bus on topic " + localNotificationTopic + ":\n" + json);
        System.out.println("===============================================================================================\n");

        kafkaTemplate.send(localNotificationTopic, json);
    }
}
