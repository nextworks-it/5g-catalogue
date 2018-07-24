
package it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VNF;

import it.nextworks.nfvmano.catalogue.common.exceptions.NotFoundException;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.Node;

import java.util.Map;

public class VNFNode {

	private String type = "";
	private String name = "";

	private VNFProperties properties = null;
	private VNFRequirements requirements = null;
	private VNFCapabilities capabilities = null;
	private VNFInterfaces interfaces = null;

	public VNFNode(Node nodeTemplate, String nodeName) throws NotFoundException {

		this.name = nodeName;
		this.type = nodeTemplate.getType();

		if (nodeTemplate.getProperties() == null)
			throw new NotFoundException(
					"You should specify at least the following properties for VNF: " + "\n- descriptor_id"
							+ "\n- descriptor_version" + "\n- provider" + "\n- product_name" + "\n- software_version"
							+ "\n- product_info_name" + "\n- vnfm_info" + "\n- flavour_id" + "\n- flavour_description");
		
		this.properties = new VNFProperties(nodeTemplate.getProperties());
		this.requirements = new VNFRequirements(nodeTemplate.getRequirements());
		this.capabilities = new VNFCapabilities(nodeTemplate.getCapabilities());
		this.interfaces = new VNFInterfaces(nodeTemplate.getInterfaces());
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	@Override
	public String toString() {

		return "VNFD Node; " + "\n" + "type " + type + "\n" + "prop: " + properties + "\n" + "reqs: " + requirements
				+ "\n" + "cap: " + capabilities + "\n" + "int: " + interfaces + "\n";
	}
}
