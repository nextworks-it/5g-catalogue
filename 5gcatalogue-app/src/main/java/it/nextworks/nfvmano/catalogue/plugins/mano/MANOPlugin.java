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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.common.KafkaConnector;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementInterface;
import it.nextworks.nfvmano.catalogue.engine.VnfPackageManagementInterface;
import it.nextworks.nfvmano.catalogue.messages.*;
import it.nextworks.nfvmano.catalogue.messages.elements.CatalogueMessageType;
import it.nextworks.nfvmano.catalogue.messages.interfaces.NsdNotificationsConsumerInterface;
import it.nextworks.nfvmano.catalogue.messages.interfaces.VnfPkgNotificationsConsumerInterface;
import it.nextworks.nfvmano.catalogue.plugins.Plugin;
import it.nextworks.nfvmano.catalogue.plugins.PluginOperationalState;
import it.nextworks.nfvmano.catalogue.plugins.PluginType;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


public abstract class MANOPlugin
        extends Plugin
        implements NsdNotificationsConsumerInterface, VnfPkgNotificationsConsumerInterface {

    private static final Logger log = LoggerFactory.getLogger(MANOPlugin.class);

    protected MANOType manoType;
    protected MANO mano;
    protected KafkaConnector connector;
    protected NsdManagementInterface nsdService;
    protected VnfPackageManagementInterface vnfdService;
    protected DescriptorsParser descriptorsParser;
    protected KafkaTemplate<String, String> kafkaTemplate;
    private String remoteTopic;

    public MANOPlugin(
            MANOType manoType,
            MANO mano,
            String kafkaBootstrapServers,
            NsdManagementInterface nsdService,
            VnfPackageManagementInterface vnfdService,
            DescriptorsParser descriptorsParser,
            String localTopic,
            String remoteTopic,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        super(mano.getManoId(), PluginType.MANO);
        this.manoType = manoType;
        this.mano = mano;
        this.nsdService = nsdService;
        this.vnfdService = vnfdService;
        this.descriptorsParser = descriptorsParser;
        this.remoteTopic = remoteTopic;
        if (this.mano.getManoType() != this.manoType) {
            throw new IllegalArgumentException("Mano type and Mano do not agree");
        }
        String connectorID = "MANO_" + mano.getManoId(); // assuming it's unique among MANOs
        Map<CatalogueMessageType, Consumer<CatalogueMessage>> functor = new HashMap<>();
        functor.put(
                CatalogueMessageType.NSD_ONBOARDING_NOTIFICATION,
                msg -> {
                    NsdOnBoardingNotificationMessage castMsg = (NsdOnBoardingNotificationMessage) msg;
                    try {
                        acceptNsdOnBoardingNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.NSD_CHANGE_NOTIFICATION,
                msg -> {
                    NsdChangeNotificationMessage castMsg = (NsdChangeNotificationMessage) msg;
                    try {
                        acceptNsdChangeNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.NSD_DELETION_NOTIFICATION,
                msg -> {
                    NsdDeletionNotificationMessage castMsg = (NsdDeletionNotificationMessage) msg;
                    try {
                        acceptNsdDeletionNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.PNFD_ONBOARDING_NOTIFICATION,
                msg -> {
                    PnfdOnBoardingNotificationMessage castMsg = (PnfdOnBoardingNotificationMessage) msg;
                    try {
                        acceptPnfdOnBoardingNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.PNFD_DELETION_NOTIFICATION,
                msg -> {
                    PnfdDeletionNotificationMessage castMsg = (PnfdDeletionNotificationMessage) msg;
                    try {
                        acceptPnfdDeletionNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.VNFPKG_ONBOARDING_NOTIFICATION,
                msg -> {
                    VnfPkgOnBoardingNotificationMessage castMsg = (VnfPkgOnBoardingNotificationMessage) msg;
                    try {
                        acceptVnfPkgOnBoardingNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.VNFPKG_CHANGE_NOTIFICATION,
                msg -> {
                    VnfPkgChangeNotificationMessage castMsg = (VnfPkgChangeNotificationMessage) msg;
                    try {
                        acceptVnfPkgChangeNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        functor.put(
                CatalogueMessageType.VNFPKG_DELETION_NOTIFICATION,
                msg -> {
                    VnfPkgDeletionNotificationMessage castMsg = (VnfPkgDeletionNotificationMessage) msg;
                    try {
                        acceptVnfPkgDeletionNotification(castMsg);
                    } catch (MethodNotImplementedException e) {
                        log.error("Method not yet implemented: " + e.getMessage());
                    }
                }
        );
        connector = KafkaConnector.Builder()
                .setBeanId(connectorID)
                .setKafkaBootstrapServers(kafkaBootstrapServers)
                .setKafkaGroupId(connectorID)
                .addTopic(localTopic)
                .setFunctor(functor)
                .build();
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(CatalogueMessage msg) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        try {
            String json = mapper.writeValueAsString(msg);
            kafkaTemplate.send(remoteTopic, json);
        } catch (JsonProcessingException e) {
            log.error("Could not notify main engine: {}", e.getMessage());
            log.debug("Error details: ", e);
        }
    }

    @Override
    public void init() {
        this.setPluginOperationalState(PluginOperationalState.ENABLED);
        connector.init();
    }

    public MANOType getManoType() {
        return manoType;
    }

    public void setManoType(MANOType manoType) {
        this.manoType = manoType;
    }

    public MANO getMano() {
        return mano;
    }

    public void setMano(MANO mano) {
        this.mano = mano;
    }
}
