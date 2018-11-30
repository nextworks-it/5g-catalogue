package it.nextworks.nfvmano.catalogue.engine;

import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.CreateVnfPkgInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPkgInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPkgInfoModifications;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VnfPackageManagementInterface {

    VnfPkgInfo createVnfPkgInfo(CreateVnfPkgInfoRequest request) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException;

    void deleteVnfPkgInfo(String vnfPkgInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    VnfPkgInfoModifications updateVnfPkgInfo(VnfPkgInfoModifications vnfPkgInfoModifications, String vnfPkgInfoId) throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException;

    Object getVnfPkg(String vnfPkgInfoId, boolean isInternalRequest) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    VnfPkgInfo getVnfPkgInfo(String vnfPkgInfoId) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException;

    List<VnfPkgInfo> getAllVnfPkgInfos() throws FailedOperationException, MethodNotImplementedException;

    void uploadVnfPkg(String vnfPkgInfoId, MultipartFile vnfPkg, ContentType contentType) throws FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;
}
