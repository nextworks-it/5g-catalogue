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
package it.nextworks.nfvmano.catalogue.engine;

import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NsdManagementInterface {
    NsdInfo createNsdInfo(CreateNsdInfoRequest request, String project) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException;

    void deleteNsdInfo(String nsdInfoId, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    NsdInfoModifications updateNsdInfo(NsdInfoModifications nsdInfoModification, String nsdInfoId, String project) throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException;

    Object getNsdFile(String nsdInfoId, boolean isInternalRequest, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    Object getNsd(String nsdInfoId, boolean isInternalRequest, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    NsdInfo getNsdInfo(String nsdInfoId, String project) throws NotPermittedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException;

    List<NsdInfo> getAllNsdInfos(String project) throws FailedOperationException, MethodNotImplementedException;

    void uploadNsd(String nsdInfoId, MultipartFile nsd, ContentType contentType, boolean isInternalRequest, String project) throws Exception, FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    PnfdInfo createPnfdInfo(CreatePnfdInfoRequest request, String project) throws FailedOperationException, MalformattedElementException, MethodNotImplementedException;

    void deletePnfdInfo(String pnfdInfoId, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    PnfdInfoModifications updatePnfdInfo(PnfdInfoModifications pnfdInfoModifications, String pnfdInfoId, String project) throws NotExistingEntityException, MalformattedElementException, NotPermittedOperationException;

    Object getPnfd(String pnfdInfoId, boolean isInternalRequest, String project) throws FailedOperationException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

    PnfdInfo getPnfdInfo(String pnfdInfoId, String project) throws NotPermittedOperationException, NotExistingEntityException, MalformattedElementException, MethodNotImplementedException;

    List<PnfdInfo> getAllPnfdInfos(String project) throws FailedOperationException, MethodNotImplementedException;

    void uploadPnfd(String pnfdInfoId, MultipartFile pnfd, ContentType contentType, boolean isInternalRequest, String project) throws Exception, FailedOperationException, AlreadyExistingEntityException, NotExistingEntityException, MalformattedElementException, NotPermittedOperationException, MethodNotImplementedException;

}
