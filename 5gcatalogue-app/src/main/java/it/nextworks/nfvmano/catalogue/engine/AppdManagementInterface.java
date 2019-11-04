package it.nextworks.nfvmano.catalogue.engine;

import it.nextworks.nfvmano.libs.common.exceptions.NotAuthorizedOperationException;
import it.nextworks.nfvmano.libs.ifa.catalogues.interfaces.MecAppPackageManagementConsumerInterface;
import it.nextworks.nfvmano.libs.ifa.catalogues.interfaces.messages.OnboardAppPackageRequest;
import it.nextworks.nfvmano.libs.ifa.catalogues.interfaces.messages.OnboardAppPackageResponse;
import it.nextworks.nfvmano.libs.ifa.catalogues.interfaces.messages.QueryOnBoadedAppPkgInfoResponse;
import it.nextworks.nfvmano.libs.ifa.common.exceptions.*;
import it.nextworks.nfvmano.libs.ifa.common.messages.GeneralizedQueryRequest;
import it.nextworks.nfvmano.libs.ifa.common.messages.SubscribeRequest;

import java.io.File;

public interface AppdManagementInterface {

    File fetchOnboardedApplicationPackage(String onboardedAppPkgId)
            throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException, NotAuthorizedOperationException;
    QueryOnBoadedAppPkgInfoResponse queryApplicationPackage(GeneralizedQueryRequest request, String project)
            throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException, NotAuthorizedOperationException;
    String subscribeMecAppPackageInfo(SubscribeRequest request, MecAppPackageManagementConsumerInterface consumer)
            throws MethodNotImplementedException, MalformattedElementException, FailedOperationException, NotAuthorizedOperationException;
    void unsubscribeMecAppPackageInfo(String subscriptionId)
            throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException, NotAuthorizedOperationException;
    OnboardAppPackageResponse onboardAppPackage(OnboardAppPackageRequest request, String project)
            throws MethodNotImplementedException, AlreadyExistingEntityException, FailedOperationException,
            MalformattedElementException, NotAuthorizedOperationException;
    void enableAppPackage(String onboardedAppPkgId, String project) throws MethodNotImplementedException,
            NotExistingEntityException, FailedOperationException, MalformattedElementException, NotAuthorizedOperationException;
    void disableAppPackage(String onboardedAppPkgId, String project) throws MethodNotImplementedException,
            NotExistingEntityException, FailedOperationException, MalformattedElementException, NotAuthorizedOperationException;
    void deleteAppPackage(String onboardedAppPkgId, String project) throws MethodNotImplementedException,
            NotExistingEntityException, FailedOperationException, MalformattedElementException, NotAuthorizedOperationException;
    void abortAppPackageDeletion(String onboardedAppPkgId) throws MethodNotImplementedException,
            NotExistingEntityException, FailedOperationException, MalformattedElementException, NotAuthorizedOperationException;

}
