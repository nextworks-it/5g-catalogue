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
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.mano.*;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r3.OpenSourceMANOR3Plugin;
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

    @Value("${kafkatopic.local.nsd}")
    private String localNsdNotificationTopic;

    @Value("${kafkatopic.remote.nsd}")
    private String remoteNsdNotificationTopic;

    @Test
    public void contextLoads() {
    }

    // Infrastructure required, should not be run by default (hence @Ignore)
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
                localNsdNotificationTopic,
                remoteNsdNotificationTopic,
                kafkaTemplate
        );
        plugin.init();

        System.out.println("\nWait for consumer to setup ... \n");
        Thread.sleep(1500);

        NsdOnBoardingNotificationMessage msg =
                new NsdOnBoardingNotificationMessage(
                        "test-nsd-info",
                        "test-nsd",
                        UUID.fromString("7a4cea43-e29d-423b-9ac8-9f0110ede94e"),
                        ScopeType.LOCAL,
                        OperationStatus.SENT
                );

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        String json = mapper.writeValueAsString(msg);

        System.out.println("\nSending message over kafka bus on topic 'onboard'\n" + json + "\n");

        kafkaTemplate.send("onboard", json);

        //System.out.println("\nWait for consumer to process ... \n");
        //Thread.sleep(5000);

    }

    // Infrastructure required, should not be run by default (hence @Ignore)
    // It's here for practicality (to be run via IDE)
    // NOTE: remove ignore to run (the spring runner will refuse to run it otherwise)
    @Test
    @Ignore
    public void testIntegrationOSMR3() throws Exception {

        //create r3 MANO
        MANO mano = new OSMMano(
                "test-r3",
                "10.0.8.26",
                "admin",
                "admin",
                "default",
                MANOType.OSMR3
        );

        //create r3 mano plugin
        MANOPlugin plugin = new OpenSourceMANOR3Plugin(
                MANOType.OSMR3,
                mano,
                kafkaBootstrapServers,
                null,
                null,
                localNsdNotificationTopic,
                remoteNsdNotificationTopic,
                kafkaTemplate
        );
        plugin.init();

        System.out.println("\nWait for consumer to setup ... \n");
        Thread.sleep(1500);

        NsdOnBoardingNotificationMessage msg =
                new NsdOnBoardingNotificationMessage(
                        "test-nsd-info",
                        "test-nsd",
                        UUID.fromString("7a4cea43-e29d-423b-9ac8-9f0110ede94e"),
                        ScopeType.LOCAL,
                        OperationStatus.SENT
                );


        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        String json = mapper.writeValueAsString(msg);

        System.out.println("\nSending message over kafka bus on topic 'onboard'\n" + json + "\n");

        kafkaTemplate.send("onboard", json);

        System.out.println("\nWait for consumer to process ... \n");
        Thread.sleep(4000);

    }

    // Infrastructure required, should not be run by default (hence @Ignore)
    // It's here for practicality (to be run via IDE)
    // NOTE: remove ignore to run (the spring runner will refuse to run it otherwise)
    @Test
    @Ignore
    public void testMultiReceiver() throws Exception {

        //create fake MANO
        MANO mano = new DummyMano("test-dummy", MANOType.DUMMY);

        //create dummy mano plugin
        DummyMANOPlugin plugin = new DummyMANOPlugin(
                MANOType.DUMMY,
                mano,
                kafkaBootstrapServers,
                null,
                localNsdNotificationTopic,
                remoteNsdNotificationTopic,
                kafkaTemplate
        );
        plugin.init();

        //create r3 MANO
        MANO osmMano = new OSMMano(
                "test-r3",
                "10.0.8.26",
                "admin",
                "admin",
                "default",
                MANOType.OSMR3
        );

        //create r3 mano plugin
        MANOPlugin osmPlugin = new OpenSourceMANOR3Plugin(
                MANOType.OSMR3,
                osmMano,
                kafkaBootstrapServers,
                null,
                null,
                localNsdNotificationTopic,
                remoteNsdNotificationTopic,
                kafkaTemplate
        );
        osmPlugin.init();

        System.out.println("\nWait for consumer to setup ... \n");
        Thread.sleep(1500);

        NsdOnBoardingNotificationMessage msg =
                new NsdOnBoardingNotificationMessage(
                        "test-nsd-info",
                        "test-nsd",
                        UUID.fromString("7a4cea43-e29d-423b-9ac8-9f0110ede94e"),
                        ScopeType.LOCAL,
                        OperationStatus.SENT
                );


        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        String json = mapper.writeValueAsString(msg);

        System.out.println("\nSending message over kafka bus on topic 'onboard'\n" + json + "\n");

        kafkaTemplate.send("onboard", json);

        //System.out.println("\nWait for consumer to process ... \n");
        Thread.sleep(4000);

    }
}
