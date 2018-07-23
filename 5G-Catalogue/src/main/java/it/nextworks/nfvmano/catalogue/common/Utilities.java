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
package it.nextworks.nfvmano.catalogue.common;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.libs.common.elements.Filter;
import it.nextworks.nfvmano.libs.common.enums.VimResourceStatus;
import it.nextworks.nfvmano.libs.common.enums.VimResourceType;
import it.nextworks.nfvmano.libs.descriptors.nsd.Dependencies;
import it.nextworks.nfvmano.libs.descriptors.nsd.VnfToLevelMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Utilities {

	public Utilities() { }
	
	public static ObjectMapper buildObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		return mapper;
	}
	
	public static Filter buildVnfInfoFilter(String vnfInstanceId) {
		Map<String, String> filterParams = new HashMap<>();
		filterParams.put("VNF_INSTANCE_ID", vnfInstanceId);
		return new Filter(filterParams);
	}
	
	public static Filter buildVimResourceFilter(VimResourceType type, String resourceId) {
		Map<String,String> parameters = new HashMap<>();
		switch (type) {
		case NETWORK: {
			parameters.put("RESOURCE_TYPE", "NETWORK");
			break;
		}
		
		case SUBNET: {
			parameters.put("RESOURCE_TYPE", "SUBNET");
			break;
		}
		
		case PORT: {
			parameters.put("RESOURCE_TYPE", "PORT");
			break;
		}
		
		case VM: {
			parameters.put("RESOURCE_TYPE", "VM");
			break;
		}

		default:
			break;
		}
		parameters.put("RESOURCE_ID", resourceId);
		Filter filter = new Filter(parameters);
		return filter;
	}
	
	public static VimResourceStatus readResourceStatusFromMetadata(Map<String, String> metadata) {
		if (metadata == null) return null;
		if (metadata.containsKey("VIM_RESOURCE_STATUS")) {
			String statusString = metadata.get("VIM_RESOURCE_STATUS");
			//TODO
			System.out.println("Getting status: " + statusString);
			return VimResourceStatus.fromString(statusString);
		} else return null;
	}
	
	public static String readMacAddressFromMetadata(Map<String, String> metadata) {
		if (metadata == null) return null;
		return metadata.get("MAC_ADDRESS");
	}
	
	public static String readIpAddressFromMetadata(Map<String, String> metadata) {
		if (metadata == null) return null;
		return metadata.get("IP_ADDRESS");
	}
	
	public static String readFloatingIpAddressFromMetadata(Map<String, String> metadata) {
		if (metadata == null) return null;
		return metadata.get("FLOATING_IP_ADDRESS");
	}
	
	public static List<VnfToLevelMapping> orderVnfsBasedOnDependencies(List<VnfToLevelMapping> input, List<Dependencies> dependencies) {
		if ((dependencies == null) || (dependencies.isEmpty())) return input;
		List<String> vduProfileIds = new ArrayList<>();
		int size = dependencies.size();
		String lastItem = null;
		for (int i = size; i>0; i--) {
			Dependencies dep = findDependencyWithSecondaryNumber(i, dependencies);
			String vduId = dep.getPrimaryId().get(0);
			vduProfileIds.add(vduId);
			if (i == 1) lastItem = dep.getSecondaryId().get(0);
		}
		List<VnfToLevelMapping> orderedList = new ArrayList<>();
		for (String vp : vduProfileIds) {
			VnfToLevelMapping level = findVnfToLevelMappingForVdu(vp, input);
			if (input.contains(level)) orderedList.add(level);
		}
		orderedList.add(findVnfToLevelMappingForVdu(lastItem, input));
		return orderedList;
	}
	
	private static Dependencies findDependencyWithSecondaryNumber(int x, List<Dependencies> dependencies) {
		for (Dependencies d : dependencies) {
			if (d.getSecondaryId().size() == x) return d;
		}
		return null;
	}
	
	private static VnfToLevelMapping findVnfToLevelMappingForVdu(String vduProfileId, List<VnfToLevelMapping> input) {
		for (VnfToLevelMapping l : input) {
			if (l.getVnfProfileId().equals(vduProfileId)) return l;
		}
		return null;
	}


}
