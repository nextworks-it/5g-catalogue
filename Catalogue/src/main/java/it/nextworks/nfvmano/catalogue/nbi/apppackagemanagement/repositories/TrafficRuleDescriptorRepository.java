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
package it.nextworks.nfvmano.catalogue.nbi.apppackagemanagement.repositories;

import it.nextworks.nfvmano.libs.descriptors.appd.TrafficRuleDescriptor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrafficRuleDescriptorRepository extends JpaRepository<TrafficRuleDescriptor, Long> {

	List<TrafficRuleDescriptor> findByAppdAppDIdAndAppdAppDVersion(String appdId, String version);
	Optional<TrafficRuleDescriptor> findByTrafficRuleIdAndAppdAppDIdAndAppdAppDVersion(String trafficRuleId, String appdId, String version);
	
}
