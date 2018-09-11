package it.nextworks.nfvmano.catalogue.engine;

import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.CreateNsdInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdInfoModifications;
import it.nextworks.nfvmano.catalogue.repos.NsdContentType;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.NotPermittedOperationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Created by Marco Capitani on 07/09/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
public interface NsdManagementInterface {
    NsdInfo createNsdInfo(CreateNsdInfoRequest request) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException;

    void deleteNsdInfo(String nsdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    NsdInfoModifications updateNsdInfo(NsdInfoModifications nsdInfoModification, String nsdInfoId) throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException;
    
    Object getNsd(String nsdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    NsdInfo getNsdInfo(String nsdInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException;

    List<NsdInfo> getAllNsdInfos() throws FailedOperationException, MethodNotImplementedException;

    void uploadNsd(String nsdInfoId, MultipartFile nsd, NsdContentType nsdContentType) throws Exception, FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;
}
