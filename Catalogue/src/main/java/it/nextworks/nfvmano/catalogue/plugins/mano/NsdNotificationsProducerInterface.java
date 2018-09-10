package it.nextworks.nfvmano.catalogue.plugins.mano;

import java.util.UUID;

import it.nextworks.nfvmano.catalogue.messages.NsdChangeNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdDeletionNotification;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdOnboardingNotification;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;

public interface NsdNotificationsProducerInterface {

	/**
     * This method is called when a new NSD is on-boarded, after all the steps are done.
     * REF IFA 013 v2.4.1 - 8.2.6
     *
     * @param notification notification about the on-boarding of a new NSD
	 * @throws MethodNotImplementedException 
	 * @throws MalformattedElementException 
	 * @throws FailedOperationException 
     */
    void sendNsdOnBoardingNotification(String nsdInfoId, String nsdId, UUID operationId) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException;

    /**
     * This method is called when a new NSD is changed, after all the steps are done.
     * REF IFA 013 v2.4.1 - 8.2.6
     *
     * @param notification notification about the change of an NSD
	 * @throws MethodNotImplementedException 
     */
	void sendNsdChangeNotification(NsdChangeNotificationMessage notification) throws MethodNotImplementedException;

	/**
     * This method is called when a new NSD is deleted, after all the steps are done.
     * REF IFA 013 v2.4.1 - 8.2.6
     *
     * @param notification notification about the deletion of an NSD
	 * @throws MethodNotImplementedException 
     */
	void sendNsdDeletionNotification(NsdDeletionNotificationMessage notification) throws MethodNotImplementedException;

	/**
     * This method is called when a new PNF is on-boarded, after all the steps are done.
     * REF IFA 013 v2.4.1 - 8.2.6
     *
     * @param notification notification about the on-boarding of a new PNF
	 * @throws MethodNotImplementedException 
     */
	void sendPnfdOnBoardingNotification(PnfdOnboardingNotification notification) throws MethodNotImplementedException;

	/**
     * This method is called when a new PNF is deleted, after all the steps are done.
     * REF IFA 013 v2.4.1 - 8.2.6
     *
     * @param notification notification about the deletion of a PNF
	 * @throws MethodNotImplementedException 
     */
	void sendPnfdDeletionNotification(PnfdDeletionNotification notification) throws MethodNotImplementedException;
}
