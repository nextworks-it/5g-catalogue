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
package it.nextworks.catalogue.catalogue.apppackagemanagement.repositories;

import it.nextworks.nfvmano.libs.catalogues.interfaces.elements.AppPackageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AppPackageInfoRepository extends JpaRepository<AppPackageInfo, Long>{
	
	@Transactional
	Optional<AppPackageInfo> findByAppPackageInfoId(String appPackageInfoId);
	
	@Transactional
	Optional<AppPackageInfo> findByAppdIdAndVersion(String appdId, String version);
	
	@Transactional
	Optional<AppPackageInfo> findByAppdId(String appdId);

}
