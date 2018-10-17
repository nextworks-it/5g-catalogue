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

import it.nextworks.nfvmano.catalogue.messages.NsdChangeNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdDeletionNotification;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdOnboardingNotification;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;

public interface NsdNotificationsConsumerInterface {

    /**
     * This method is called when a new NSD is on-boarded, after all the steps are done.
     * REF IFA 013 v2.4.1 - 8.2.6
     *
     * @param castMsg notification about the on-boarding of a new NSD
     */
    void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage castMsg);

    /**
     * This method is called when there is a change of state in an on-boarded NSD.
     * Only a change in operational state will be reported. A change in usage state will not be reported.
     * REF IFA 013 v2.4.1 - 8.2.7
     *
     * @param notification notification about the state change of an NSD
     * @throws MethodNotImplementedException 
     */
    void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) throws MethodNotImplementedException;

    /**
     * This method is called when an on-boarded NSD is deleted.
     * REF IFA 013 v2.4.1 - 8.2.8
     *
     * @param notification notification about the deletion of an existing NSD
     * @throws MethodNotImplementedException 
     */
    void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification);

    /**
     * This method is called when a new PNFD is on-boarded, after all the on-boarding steps are done.
     * REF IFA 013 v2.4.1 - 8.2.9
     *
     * @param notification notification about the on-boarding of a PNFD
     * @throws MethodNotImplementedException 
     */
    void acceptPnfdOnBoardingNotification(PnfdOnboardingNotification notification) throws MethodNotImplementedException; // TODO use catalogueMsg

    /**
     * This method is called when an on-boarded PNFD is deleted.
     * REF IFA 013 v2.4.1 - 8.2.10
     *
     * @param notification notification about the deletion of an existing PNFD
     * @throws MethodNotImplementedException 
     */
    void acceptPnfdDeletionNotification(PnfdDeletionNotification notification) throws MethodNotImplementedException; // TODO use catalogueMsg

}
