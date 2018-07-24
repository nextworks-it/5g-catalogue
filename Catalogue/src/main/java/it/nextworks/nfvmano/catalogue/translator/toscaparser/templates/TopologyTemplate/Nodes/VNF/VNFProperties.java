/*
 * Copyright (c) 2016 Open Baton (http://www.openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.VNF;

import it.nextworks.nfvmano.catalogue.common.exceptions.NotFoundException;

import java.util.*;

/** Created by rvl on 19.08.16. */
public class VNFProperties {

	private String descriptor_id;
	private String descriptor_version;
	private String provider;
	private String product_name;
	private String software_version;
	private String product_info_name;
	private String product_info_description;
	private ArrayList<String> vnfm_info;
	private ArrayList<String> localization_languages;
	private String default_localization_language;
	private VNFConfigurableProperties configurable_properties;
	private VNFInfoModifiableAttributes modifiable_attributes;
	private String flavour_id;
	private String flavour_description;

	public VNFProperties() {
	}

	@SuppressWarnings({ "unsafe", "unchecked" })
	public VNFProperties(Object properties) {

		Map<String, Object> propertiesMap = (Map<String, Object>) properties;

		if (propertiesMap.containsKey("descriptor_id")) {
			this.descriptor_id = (String) propertiesMap.get("descriptor_id");
		}

		if (propertiesMap.containsKey("descriptor_version")) {
			this.descriptor_version = (String) propertiesMap.get("descriptor_version");
		}

		if (propertiesMap.containsKey("provider")) {
			this.provider = (String) propertiesMap.get("provider");
		}

		if (propertiesMap.containsKey("product_name")) {
			this.product_name = (String) propertiesMap.get("product_name");
		}

		if (propertiesMap.containsKey("software_version")) {
			this.software_version = (String) propertiesMap.get("software_version");
		}

		if (propertiesMap.containsKey("product_info_name")) {
			this.product_info_name = (String) propertiesMap.get("product_info_name");
		}

		if (propertiesMap.containsKey("product_info_description")) {
			this.product_info_description = (String) propertiesMap.get("product_info_description");
		}

		if (propertiesMap.containsKey("vnfm_info")) {
			this.vnfm_info = (ArrayList<String>) propertiesMap.get("vnfm_info");
		}

		if (propertiesMap.containsKey("localization_languages")) {
			this.localization_languages = (ArrayList<String>) propertiesMap.get("localization_languages");
		}

		if (propertiesMap.containsKey("default_localization_language")) {
			this.default_localization_language = (String) propertiesMap.get("default_localization_language");
		}

		if (propertiesMap.containsKey("configurable_properties")) {
			this.configurable_properties = (VNFConfigurableProperties) propertiesMap.get("configurable_properties");
		}

		if (propertiesMap.containsKey("modifiable_attributes")) {
			this.modifiable_attributes = (VNFInfoModifiableAttributes) propertiesMap.get("modifiable_attributes");
		}

		if (propertiesMap.containsKey("flavour_id")) {
			this.flavour_id = (String) propertiesMap.get("flavour_id");
		}

		if (propertiesMap.containsKey("flavour_description")) {
			this.flavour_description = (String) propertiesMap.get("flavour_description");
		}

	}

	public String getDescriptor_id() {
		return descriptor_id;
	}

	public void setDescriptor_id(String descriptor_id) {
		this.descriptor_id = descriptor_id;
	}

	public String getDescriptor_version() {
		return descriptor_version;
	}

	public void setDescriptor_version(String descriptor_version) {
		this.descriptor_version = descriptor_version;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getProduct_name() {
		return product_name;
	}

	public void setProduct_name(String product_name) {
		this.product_name = product_name;
	}

	public String getSoftware_version() {
		return software_version;
	}

	public void setSoftware_version(String software_version) {
		this.software_version = software_version;
	}

	public String getProduct_info_name() {
		return product_info_name;
	}

	public void setProduct_info_name(String product_info_name) {
		this.product_info_name = product_info_name;
	}

	public String getProduct_info_description() {
		return product_info_description;
	}

	public void setProduct_info_description(String product_info_description) {
		this.product_info_description = product_info_description;
	}

	public ArrayList<String> getVnfm_info() {
		return vnfm_info;
	}

	public void setVnfm_info(ArrayList<String> vnfm_info) {
		this.vnfm_info = vnfm_info;
	}

	public ArrayList<String> getLocalization_languages() {
		return localization_languages;
	}

	public void setLocalization_languages(ArrayList<String> localization_languages) {
		this.localization_languages = localization_languages;
	}

	public String getDefault_localization_language() {
		return default_localization_language;
	}

	public void setDefault_localization_language(String default_localization_language) {
		this.default_localization_language = default_localization_language;
	}

	public VNFConfigurableProperties getConfigurable_properties() {
		return configurable_properties;
	}

	public void setConfigurable_properties(VNFConfigurableProperties configurable_properties) {
		this.configurable_properties = configurable_properties;
	}

	public VNFInfoModifiableAttributes getModifiable_attributes() {
		return modifiable_attributes;
	}

	public void setModifiable_attributes(VNFInfoModifiableAttributes modifiable_attributes) {
		this.modifiable_attributes = modifiable_attributes;
	}

	public String getFlavour_id() {
		return flavour_id;
	}

	public void setFlavour_id(String flavour_id) {
		this.flavour_id = flavour_id;
	}

	public String getFlavour_description() {
		return flavour_description;
	}

	public void setFlavour_description(String flavour_description) {
		this.flavour_description = flavour_description;
	}

	@Override
	public String toString() {

		/*
		 * return "VNFP : " + "\n" + "id : " + getID() + "\n" + "vendor : " + vendor +
		 * "\n" + "version : " + version + "\n" + "package : " + vnfPackageLocation +
		 * "\n" + "depl flavour : " + deploymentFlavour + "\n";
		 */
		return "";
	}
}
