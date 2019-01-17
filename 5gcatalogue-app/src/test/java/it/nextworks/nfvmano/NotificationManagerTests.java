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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gwt.editor.client.Editor.Ignore;
import it.nextworks.nfvmano.catalogue.engine.NotificationManager;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.ScopeType;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
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
public class NotificationManagerTests {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationManager notificationManager;

    @Value("${kafkatopic.remote.nsd}")
    private String remoteNsdNotificationTopic;

    @Test
    @Ignore
    public void acceptNsdRemoteOnboardingNotification() {
        NsdOnBoardingNotificationMessage msg = new NsdOnBoardingNotificationMessage(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                ScopeType.REMOTE,
                OperationStatus.SUCCESSFULLY_DONE,
                "NXWOSMR3.3"
        );

        ObjectMapper mapper = new ObjectMapper();

        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        try {
            String json = mapper.writeValueAsString(msg);
            System.out.println("REMOTE ON-BOARDING MSG: " + json);
            kafkaTemplate.send(remoteNsdNotificationTopic, json);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (notificationManager == null) {
                System.out.println("================= Error : null notification service");
            }
            //notificationManager.acceptNsdOnBoardingNotification(msg);
        } catch (JsonProcessingException e) {
            System.out.println("ERROR while converting message to JSON string");
        }
    }
}
