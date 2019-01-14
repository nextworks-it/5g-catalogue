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
package it.nextworks.nfvmano.catalogue.storage;


import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.catalogue.engine.resources.VnfPkgInfoResource;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageServiceInterface {

    public void init() throws FailedOperationException;

    public String storeNsd(NsdInfoResource nsdInfo, MultipartFile file) throws MalformattedElementException, FailedOperationException;

    public String storeVnfPkg(VnfPkgInfoResource vnfPkgInfoResource, MultipartFile file) throws MalformattedElementException, FailedOperationException;

    public Stream<Path> loadAllNsds() throws FailedOperationException;

    public Path loadNsd(NsdInfoResource nsdInfo, String filename);

    public Path loadVnfPkg(VnfPkgInfoResource vnfPkgInfo, String filename);

    public Resource loadNsdAsResource(NsdInfoResource nsdInfo, String filename) throws NotExistingEntityException;

    public Resource loadVnfPkgAsResource(VnfPkgInfoResource vnfPkgInfo, String filename) throws NotExistingEntityException;

    public Resource loadVnfdAsResource(VnfPkgInfoResource vnfPkgInfo, String filename) throws NotExistingEntityException, IOException;

    public File loadVnfdAsFile(String vnfdId, String version, String filename) throws NotExistingEntityException, IOException;

    public void deleteAll();

    public void deleteNsd(NsdInfoResource nsdInfo);

    public void deleteVnfPkg(VnfPkgInfoResource vnfPkgInfoResource);

}
