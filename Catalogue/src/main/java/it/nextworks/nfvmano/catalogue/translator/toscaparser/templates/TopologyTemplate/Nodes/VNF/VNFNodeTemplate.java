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

import it.nextworks.nfvmano.catalogue.translator.toscaparser.exceptions.NotFoundException;
import it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.NodeTemplate;

import java.util.Map;

/** Created by rvl on 19.08.16. */
public class VNFNodeTemplate {

	private String type = "";
	private String name = "";

	private VNFProperties properties = null;
	private VNFRequirements requirements = null;
	private VNFCapabilities capabilities = null;
	private VNFInterfaces interfaces = null;

	@SuppressWarnings({ "unsafe", "unchecked" })
	public VNFNodeTemplate(NodeTemplate nodeTemplate, String nodeName) throws NotFoundException {

		this.name = nodeName;
		this.type = nodeTemplate.getType();

		if (nodeTemplate.getProperties() == null)
			throw new NotFoundException(
					"You should specify at least endpoint, deployment_flavour and type in properties for VNF: ");
		properties = new VNFProperties(nodeTemplate.getProperties());

		requirements = new VNFRequirements(nodeTemplate.getRequirements());
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public VNFProperties getProperties() {
		return properties;
	}

	public void setProperties(VNFProperties properties) {
		this.properties = properties;
	}

	public VNFRequirements getRequirements() {
		return requirements;
	}

	public void setRequirements(VNFRequirements requirements) {
		this.requirements = requirements;
	}

	public String getName() {
		return name;
	}

	public VNFCapabilities getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(VNFCapabilities capabilities) {
		this.capabilities = capabilities;
	}

	public VNFInterfaces getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(VNFInterfaces interfaces) {
		this.interfaces = interfaces;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {

		return "VNFD Node; " + "\n" + "type " + type + "\n" + "prop: " + properties + "\n" + "reqs: " + requirements
				+ "\n" + "cap: " + capabilities + "\n" + "int: " + interfaces + "\n";
	}
}
