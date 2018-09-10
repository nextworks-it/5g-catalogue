package it.nextworks.nfvmano.catalogue.plugins.mano;

public interface NsdNotificationsProducerInterface {

	/**
     * This method is called when a new NSD is on-boarded, after all the steps are done.
     * REF IFA 013 v2.4.1 - 8.2.6
     *
     * @param notification notification about the on-boarding of a new NSD
     */
    void sendNsdOnBoardingNotification(String notification);
}
