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

import it.nextworks.nfvmano.catalogue.engine.NsdManagementInterface;
import it.nextworks.nfvmano.catalogue.engine.VnfPackageManagementInterface;
import it.nextworks.nfvmano.catalogue.messages.*;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class DummyMANOPlugin extends MANOPlugin {

    private static final Logger log = LoggerFactory.getLogger(DummyMANOPlugin.class);

    public DummyMANOPlugin(
            MANOType manoType,
            MANO mano,
            String kafkaBootstrapServers,
            NsdManagementInterface nsdService,
            VnfPackageManagementInterface vnfdService,
            String localTopic,
            String remoteTopic,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        super(
                manoType,
                mano,
                kafkaBootstrapServers,
                nsdService,
                vnfdService,
                localTopic,
                remoteTopic,
                kafkaTemplate
        );
    }

    @Override
    public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
        log.info("Received NSD onboarding notification.");
        log.debug("Body: {}", notification);
        NsdOnBoardingNotificationMessage response = new NsdOnBoardingNotificationMessage(
                notification.getNsdInfoId(),
                notification.getNsdId(),
                notification.getOperationId(),
                ScopeType.REMOTE,
                OperationStatus.SUCCESSFULLY_DONE,
                mano.getManoId()
        );
        sendNotification(response);
    }

    @Override
    public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) {
        log.info("Received NSD change notification.");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) {
        log.info("Received NSD deletion notification.");
        log.debug("Body: {}", notification);
        NsdDeletionNotificationMessage response = new NsdDeletionNotificationMessage(
                notification.getNsdInfoId(),
                notification.getNsdId(),
                notification.getOperationId(),
                ScopeType.REMOTE,
                OperationStatus.SUCCESSFULLY_DONE,
                mano.getManoId()
        );
        sendNotification(response);
    }

    @Override
    public void acceptPnfdOnBoardingNotification(PnfdOnBoardingNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received PNFD onboarding notification.");
        log.debug("Body: {}", notification);
        PnfdOnBoardingNotificationMessage response = new PnfdOnBoardingNotificationMessage(
                notification.getPnfdInfoId(),
                notification.getPnfdId(),
                notification.getOperationId(),
                ScopeType.REMOTE,
                OperationStatus.SUCCESSFULLY_DONE,
                mano.getManoId()
        );
        sendNotification(response);
    }

    @Override
    public void acceptPnfdDeletionNotification(PnfdDeletionNotificationMessage notification) throws MethodNotImplementedException {
        log.info("Received PNFD deletion notification.");
        log.debug("Body: {}", notification);
        PnfdDeletionNotificationMessage response = new PnfdDeletionNotificationMessage(
                notification.getPnfdInfoId(),
                notification.getPnfdId(),
                notification.getOperationId(),
                ScopeType.REMOTE,
                OperationStatus.SUCCESSFULLY_DONE,
                mano.getManoId()
        );
        sendNotification(response);
    }

    @Override
    public void acceptVnfPkgOnBoardingNotification(VnfPkgOnBoardingNotificationMessage notification) {
        log.info("Received VNF Pkg onboarding notification.");
        log.debug("Body: {}", notification);
        VnfPkgOnBoardingNotificationMessage response = new VnfPkgOnBoardingNotificationMessage(
                notification.getVnfPkgInfoId(),
                notification.getVnfdId(),
                notification.getOperationId(),
                ScopeType.REMOTE,
                OperationStatus.SUCCESSFULLY_DONE,
                mano.getManoId()
        );
        sendNotification(response);
        log.debug("VnfPkgOnBoardingNotification sent to NotificationManager");
    }

    @Override
    public void acceptVnfPkgChangeNotification(VnfPkgChangeNotificationMessage notification) {
        log.info("Received VNF Pkg change notification.");
        log.debug("Body: {}", notification);
    }

    @Override
    public void acceptVnfPkgDeletionNotification(VnfPkgDeletionNotificationMessage notification) {
        log.info("Received VNF Pkg deletion notification.");
        log.debug("Body: {}", notification);
        VnfPkgDeletionNotificationMessage response = new VnfPkgDeletionNotificationMessage(
                notification.getVnfPkgInfoId(),
                notification.getVnfdId(),
                notification.getOperationId(),
                ScopeType.REMOTE,
                OperationStatus.SUCCESSFULLY_DONE,
                mano.getManoId()
        );
        sendNotification(response);
        log.debug("VnfPkgDeletionNotification sent to NotificationManager");
    }
}
