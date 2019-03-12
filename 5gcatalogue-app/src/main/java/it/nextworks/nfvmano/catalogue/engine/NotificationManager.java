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
package it.nextworks.nfvmano.catalogue.engine;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.messages.*;
import it.nextworks.nfvmano.catalogue.plugins.KafkaConnector;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class NotificationManager implements NsdNotificationsConsumerInterface, NsdNotificationsProducerInterface,
        VnfPkgNotificationsConsumerInterface, VnfPkgNotificationsProducerInterface {

    private static final Logger log = LoggerFactory.getLogger(NotificationManager.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaConnector connector;

    @Value("${kafka.skip.send}")
    private boolean skipKafka;

    @Value("${kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${kafkatopic.local}")
    private String localNotificationTopic;

    @Value("${kafkatopic.remote}")
    private String remoteNotificationTopic;

    @Autowired
    private NsdManagementService nsdMgmtService;

    @Autowired
    private VnfPackageManagementService vnfPackageManagementService;

    public NotificationManager() {
        log.debug("Building notification manager.");
    }

    @PostConstruct
    public void init() {
        log.debug("Initializing notification manager.");
        initializeKafkaConnector("ENGINE_NOTIFICATION_MANAGER", kafkaBootstrapServers, remoteNotificationTopic);
    }

    public KafkaConnector getConnector() {
        return connector;
    }

    public void setConnector(KafkaConnector connector) {
        this.connector = connector;
    }

    private void initializeKafkaConnector(String connectorId, String server, String topicQueueExchange) {
        log.debug("Initializing Kafka connector with: \nconnectorId: " + connectorId + "\nkafkaServer: " + server
                + "\ntopic: " + topicQueueExchange);
        Map<CatalogueMessageType, Consumer<CatalogueMessage>> functor = new HashMap<>();
        functor.put(CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION, msg -> {
            NsdOnBoardingNotificationMessage castMsg = (NsdOnBoardingNotificationMessage) msg;
            acceptNsdOnBoardingNotification(castMsg);
        });
        functor.put(CatalogueMessageType.NSD_DELETION_NOTIFICATION, msg -> {
            NsdDeletionNotificationMessage castMsg = (NsdDeletionNotificationMessage) msg;
            acceptNsdDeletionNotification(castMsg);
        });
        functor.put(CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION, msg -> {
            PnfdOnBoardingNotificationMessage castMsg = (PnfdOnBoardingNotificationMessage) msg;
            try {
                acceptPnfdOnBoardingNotification(castMsg);
            } catch (MethodNotImplementedException e) {
                e.printStackTrace();
            }
        });
        functor.put(CatalogueMessageType.PNFD_DELETION_NOTIFICATION, msg -> {
            PnfdDeletionNotificationMessage castMsg = (PnfdDeletionNotificationMessage) msg;
            try {
                acceptPnfdDeletionNotification(castMsg);
            } catch (MethodNotImplementedException e) {
                e.printStackTrace();
            }
        });
        functor.put(CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION, msg -> {
            VnfPkgOnBoardingNotificationMessage castMsg = (VnfPkgOnBoardingNotificationMessage) msg;
            try {
                acceptVnfPkgOnBoardingNotification(castMsg);
            } catch (MethodNotImplementedException e) {
                e.printStackTrace();
            }
        });
        functor.put(CatalogueMessageType.VNFPKG_DELETION_NOTIFICATION, msg -> {
            VnfPkgDeletionNotificationMessage castMsg = (VnfPkgDeletionNotificationMessage) msg;
            try {
                acceptVnfPkgDeletionNotification(castMsg);
            } catch (MethodNotImplementedException e) {
                e.printStackTrace();
            }
        });

        setConnector(KafkaConnector.Builder().setBeanId(connectorId).setKafkaBootstrapServers(server)
                .setKafkaGroupId(connectorId).addTopic(topicQueueExchange).setFunctor(functor).build());
        connector.init();
        log.debug("Kafka connector initialized.");
    }

    @Override
    public void sendNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification)
            throws FailedOperationException, MalformattedElementException, MethodNotImplementedException {
        try {
            log.info("Sending nsdOnBoardingNotification for NSD " + notification.getNsdId());

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);

            String json = mapper.writeValueAsString(notification);

            log.debug("Sending json message over kafka bus on topic " + localNotificationTopic + "\n" + json);

            if (skipKafka) {
                log.debug(" ---- TEST MODE: skipping post to kafka bus ----");
            } else {
                kafkaTemplate.send(localNotificationTopic, json);

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
            log.info("Sending nsdDeletionNotification for NSD with nsdId: " + notification.getNsdId());

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);

            String json = mapper.writeValueAsString(notification);

            log.debug("Sending json message over kafka bus on topic " + localNotificationTopic + "\n" + json);

            if (skipKafka) {
                log.debug(" ---- TEST MODE: skipping post to kafka bus ----");
            } else {
                kafkaTemplate.send(localNotificationTopic, json);

                log.debug("Message sent.");
            }
        } catch (Exception e) {
            log.error("Error while posting NsdDeletionNotificationMessage to Kafka bus");
            throw new FailedOperationException(e.getMessage());
        }
    }

    @Override
    public void sendPnfdOnBoardingNotification(PnfdOnBoardingNotificationMessage notification)
            throws MethodNotImplementedException, FailedOperationException {
        try {
            log.info("Sending pnfdOnBoardingNotification for PNFD " + notification.getPnfdId());

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);

            String json = mapper.writeValueAsString(notification);

            log.debug("Sending json message over kafka bus on topic " + localNotificationTopic + "\n" + json);

            if (skipKafka) {
                log.debug(" ---- TEST MODE: skipping post to kafka bus ----");
            } else {
                kafkaTemplate.send(localNotificationTopic, json);

                log.debug("Message sent.");
            }
        } catch (Exception e) {
            log.error("Error while posting NsdOnBoardingNotificationMessage to Kafka bus");
            throw new FailedOperationException(e.getMessage());
        }
    }

    @Override
    public void sendPnfdDeletionNotification(PnfdDeletionNotificationMessage notification)
            throws MethodNotImplementedException, FailedOperationException {
        try {
            log.info("Sending nsdDeletionNotification for PNFD with pnfdId: " + notification.getPnfdId());

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);

            String json = mapper.writeValueAsString(notification);

            log.debug("Sending json message over kafka bus on topic " + localNotificationTopic + "\n" + json);

            if (skipKafka) {
                log.debug(" ---- TEST MODE: skipping post to kafka bus ----");
            } else {
                kafkaTemplate.send(localNotificationTopic, json);

                log.debug("Message sent.");
            }
        } catch (Exception e) {
            log.error("Error while posting NsdDeletionNotificationMessage to Kafka bus");
            throw new FailedOperationException(e.getMessage());
        }
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
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) {
        log.info("Received NSD deletion notification for NSD {} with info id {}, from plugin {}.",
                notification.getNsdId(), notification.getNsdInfoId(), notification.getPluginId());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        try {
            String json = mapper.writeValueAsString(notification);
            log.debug("RECEIVED MESSAGE: " + json);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse received nsdDeletionNotificationMessage: " + e.getMessage());
        }

        switch (notification.getScope()) {
            case REMOTE:
                log.info("NSD {} with info id {} successfully removed by plugin {}.",
                        notification.getNsdId(), notification.getNsdInfoId(), notification.getPluginId());
                log.debug("Updating consumers internal mapping for operationId {} and plugin {}.",
                        notification.getOperationId(), notification.getPluginId());
                nsdMgmtService.updateOperationInfoInConsumersMap(notification.getOperationId(), notification.getOpStatus(),
                        notification.getPluginId(), notification.getNsdInfoId(), notification.getType());
                log.debug("Consumers internal mapping successfully updated for operationId {} and plugin {}.",
                        notification.getOperationId(), notification.getPluginId());
                break;
            case LOCAL:
                log.error("Nsd LOCAL deletion notification not handled here, REMOTE onboarding message expected.");
                break;
            case GLOBAL:
                log.error("Nsd GLOBAL deletion notification not handled here, REMOTE onboarding message expected.");
                break;
        }
    }

    @Override
    public void acceptPnfdOnBoardingNotification(PnfdOnBoardingNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received PNFD onboarding notification for PNFD {} with info id {}, from plugin {}.",
                notification.getPnfdId(), notification.getPnfdInfoId(), notification.getPluginId());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        try {
            String json = mapper.writeValueAsString(notification);
            log.debug("RECEIVED MESSAGE: " + json);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse received pnfdOnboardingNotificationMessage: " + e.getMessage());
        }

        switch (notification.getScope()) {
            case REMOTE:
                try {
                    log.debug("Updating PnfdInfoResource with plugin {} operation status {} for operation {}.",
                            notification.getPluginId(), notification.getOpStatus(),
                            notification.getOperationId().toString());
                    nsdMgmtService.updatePnfdInfoOperationStatus(notification.getPnfdInfoId(), notification.getPluginId(),
                            notification.getOpStatus(), notification.getType());
                    log.debug("PnfdInfoResource successfully updated with plugin {} operation status {} for operation {}.",
                            notification.getPluginId(), notification.getOpStatus(),
                            notification.getOperationId().toString());
                } catch (NotExistingEntityException e) {
                    log.error(e.getMessage());
                }
                log.debug("Updating consumers internal mapping for operationId {} and plugin {}.",
                        notification.getOperationId(), notification.getPluginId());
                nsdMgmtService.updateOperationInfoInConsumersMap(notification.getOperationId(), notification.getOpStatus(),
                        notification.getPluginId(), notification.getPnfdInfoId(), notification.getType());
                log.debug("Consumers internal mapping successfully updated for operationId {} and plugin {}.",
                        notification.getOperationId(), notification.getPluginId());
                break;
            case LOCAL:
                log.error("Pnfd LOCAL onboarding notification not handled here, REMOTE onboarding message expected.");
                break;
            case GLOBAL:
                log.error("Pnfd GLOBAL onboarding notification not handled here, REMOTE onboarding message expected.");
                break;
        }
    }

    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received PNFD deletion notification for PNFD {} with info id {}, from plugin {}.",
                notification.getPnfdId(), notification.getPnfdInfoId(), notification.getPluginId());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        try {
            String json = mapper.writeValueAsString(notification);
            log.debug("RECEIVED MESSAGE: " + json);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse received nsdDeletionNotificationMessage: " + e.getMessage());
        }

        switch (notification.getScope()) {
            case REMOTE:
                log.info("PNFD {} with info id {} successfully removed by plugin {}.",
                        notification.getPnfdId(), notification.getPnfdInfoId(), notification.getPluginId());
                log.debug("Updating consumers internal mapping for operationId {} and plugin {}.",
                        notification.getOperationId(), notification.getPluginId());
                nsdMgmtService.updateOperationInfoInConsumersMap(notification.getOperationId(), notification.getOpStatus(),
                        notification.getPluginId(), notification.getPnfdInfoId(), notification.getType());
                log.debug("Consumers internal mapping successfully updated for operationId {} and plugin {}.",
                        notification.getOperationId(), notification.getPluginId());
                break;
            case LOCAL:
                log.error("Pnfd LOCAL deletion notification not handled here, REMOTE onboarding message expected.");
                break;
            case GLOBAL:
                log.error("Pnfd GLOBAL deletion notification not handled here, REMOTE onboarding message expected.");
                break;
        }
    }


    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received VNF Pkg onboarding notification for VNF Pkg {} with info id {}, from plugin {}.",
                notification.getVnfdId(), notification.getVnfPkgInfoId(), notification.getPluginId());

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
                    log.debug("Updating VnfPkgInfoResource with plugin {} operation status {} for operation {}.",
                            notification.getPluginId(), notification.getOpStatus(),
                            notification.getOperationId().toString());
                    vnfPackageManagementService.updateVnfPkgInfoOperationStatus(notification.getVnfPkgInfoId(), notification.getPluginId(),
                            notification.getOpStatus(), notification.getType());
                    log.debug("VnfPkgInfoResource successfully updated with plugin {} operation status {} for operation {}.",
                            notification.getPluginId(), notification.getOpStatus(),
                            notification.getOperationId().toString());
                } catch (NotExistingEntityException e) {
                    log.error(e.getMessage());
                }
                log.debug("Updating consumers internal mapping for operationId {} and plugin {}.",
                        notification.getOperationId(), notification.getPluginId());
                vnfPackageManagementService.updateOperationInfoInConsumersMap(notification.getOperationId(), notification.getOpStatus(),
                        notification.getPluginId(), notification.getVnfPkgInfoId(), notification.getType());
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
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) throws MethodNotImplementedException {
        throw new MethodNotImplementedException("acceptVnfPkgChangeNotification method not implemented");
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received VNF Pkg deletion notification for VNF Pkg {} with info id {}, from plugin {}.",
                notification.getVnfdId(), notification.getVnfPkgInfoId(), notification.getPluginId());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        try {
            String json = mapper.writeValueAsString(notification);
            log.debug("RECEIVED MESSAGE: " + json);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse received vnfPkgDeletionNotificationMessage: " + e.getMessage());
        }

        switch (notification.getScope()) {
            case REMOTE:
                log.info("VNF Pkg {} with info id {} successfully removed by plugin {}.",
                        notification.getVnfdId(), notification.getVnfPkgInfoId(), notification.getPluginId());
                log.debug("Updating consumers internal mapping for operationId {} and plugin {}.",
                        notification.getOperationId(), notification.getPluginId());
                vnfPackageManagementService.updateOperationInfoInConsumersMap(notification.getOperationId(), notification.getOpStatus(),
                        notification.getPluginId(), notification.getVnfPkgInfoId(), notification.getType());
                log.debug("Consumers internal mapping successfully updated for operationId {} and plugin {}.",
                        notification.getOperationId(), notification.getPluginId());
                break;
            case LOCAL:
                log.error("VNF Pkg LOCAL deletion notification not handled here, REMOTE onboarding message expected.");
                break;
            case GLOBAL:
                log.error("VNF Pkg GLOBAL deletion notification not handled here, REMOTE onboarding message expected.");
                break;
        }
    }

    @Override
    public void sendVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) throws MethodNotImplementedException, FailedOperationException {
        try {
            log.info("Sending vnfPkgOnBoardingNotification for VNF Pkg with info Id: " + notification.getVnfPkgInfoId());

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);

            String json = mapper.writeValueAsString(notification);

            log.debug("Sending json message over kafka bus on topic " + localNotificationTopic + "\n" + json);

            if (skipKafka) {
                log.debug(" ---- TEST MODE: skipping post to kafka bus ----");
            } else {
                kafkaTemplate.send(localNotificationTopic, json);

                log.debug("Message sent.");
            }
        } catch (Exception e) {
            log.error("Error while posting VnfPkgBoardingNotificationMessage to Kafka bus");
            throw new FailedOperationException(e.getMessage());
        }
    }

    @Override
    public void sendVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) throws MethodNotImplementedException {
        throw new MethodNotImplementedException("sendVnfPkgChangeNotification method not implemented");
    }

    @Override
    public void sendVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) throws MethodNotImplementedException, FailedOperationException {
        try {
            log.info("Sending vnfPkgDeletionNotification for VNF Pkg with info Id: " + notification.getVnfPkgInfoId());

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);

            String json = mapper.writeValueAsString(notification);

            log.debug("Sending json message over kafka bus on topic " + localNotificationTopic + "\n" + json);

            if (skipKafka) {
                log.debug(" ---- TEST MODE: skipping post to kafka bus ----");
            } else {
                kafkaTemplate.send(localNotificationTopic, json);

                log.debug("Message sent.");
            }
        } catch (Exception e) {
            log.error("Error while posting VnfPkgDeletionNotificationMessage to Kafka bus");
            throw new FailedOperationException(e.getMessage());
        }
    }
}
