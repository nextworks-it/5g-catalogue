package it.nextworks.nfvmano.catalogue.plugins.mano;

import it.nextworks.nfvmano.catalogue.messages.NsdChangeNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdDeletionNotification;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdOnboardingNotification;

public interface NsdNotificationsConsumerInterface {

    /**
     * This method is called when a new NSD is on-boarded, after all the steps are done.
     * REF IFA 013 v2.4.1 - 8.2.6
     *
     * @param notification notification about the on-boarding of a new NSD
     */
    void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification);

    /**
     * This method is called when there is a change of state in an on-boarded NSD.
     * Only a change in operational state will be reported. A change in usage state will not be reported.
     * REF IFA 013 v2.4.1 - 8.2.7
     *
     * @param notification notification about the state change of an NSD
     */
    void acceptNsdChangeNotification(NsdChangeNotificationMessage notification);

    /**
     * This method is called when an on-boarded NSD is deleted.
     * REF IFA 013 v2.4.1 - 8.2.8
     *
     * @param notification notification about the deletion of an existing NSD
     */
    void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification);

    /**
     * This method is called when a new PNFD is on-boarded, after all the on-boarding steps are done.
     * REF IFA 013 v2.4.1 - 8.2.9
     *
     * @param notification notification about the on-boarding of a PNFD
     */
    void acceptPnfdOnBoardingNotification(PnfdOnboardingNotification notification); // TODO use catalogueMsg

    /**
     * This method is called when an on-boarded PNFD is deleted.
     * REF IFA 013 v2.4.1 - 8.2.10
     *
     * @param notification notification about the deletion of an existing PNFD
     */
    void acceptPnfdDeletionNotification(PnfdDeletionNotification notification); // TODO use catalogueMsg

}
