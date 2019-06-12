package it.nextworks.nfvmano.catalogue.engine;

import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.CreateVnfPkgInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPkgInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPkgInfoModifications;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VnfPackageManagementInterface {

    VnfPkgInfo createVnfPkgInfo(CreateVnfPkgInfoRequest request, String project) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException;

    void deleteVnfPkgInfo(String vnfPkgInfoId, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    VnfPkgInfoModifications updateVnfPkgInfo(VnfPkgInfoModifications vnfPkgInfoModifications, String vnfPkgInfoId, String project) throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException;

    Object getVnfd(String vnfPkgInfoId, boolean isInternalRequest, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    Object getVnfPkg(String vnfPkgInfoId, boolean isInternalRequest, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    VnfPkgInfo getVnfPkgInfo(String vnfPkgInfoId, String project) throws NotPermittedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException;

    List<VnfPkgInfo> getAllVnfPkgInfos(String project) throws FailedOperationException, MethodNotImplementedException;

    void uploadVnfPkg(String vnfPkgInfoId, MultipartFile vnfPkg, ContentType contentType, boolean isInternalRequest, String project) throws FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;
}
