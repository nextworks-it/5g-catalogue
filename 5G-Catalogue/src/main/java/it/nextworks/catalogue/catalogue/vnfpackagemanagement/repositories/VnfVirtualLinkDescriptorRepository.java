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
package it.nextworks.catalogue.catalogue.vnfpackagemanagement.repositories;

import it.nextworks.nfvmano.libs.descriptors.vnfd.VnfVirtualLinkDesc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface VnfVirtualLinkDescriptorRepository extends JpaRepository<VnfVirtualLinkDesc, Long> {
	List<VnfVirtualLinkDesc> findByVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersion(String vnfdId, String vnfProvider, String version);
	Optional<VnfVirtualLinkDesc> findByVirtualLinkDescIdAndVnfdVnfdIdAndVnfdVnfProviderAndVnfdVnfdVersion(String vldId, String vnfdId, String vnfProvider, String version);
}
