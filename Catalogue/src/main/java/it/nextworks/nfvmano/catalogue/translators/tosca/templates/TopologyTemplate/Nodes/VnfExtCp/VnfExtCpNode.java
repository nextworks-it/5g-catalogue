
package it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VnfExtCp;

import it.nextworks.nfvmano.catalogue.common.exceptions.NotFoundException;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.Node;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.CP.CPNodeTemplate;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VNF.VNFProperties;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VNF.VNFRequirements;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;

public class VnfExtCpNode {
	
	private String type;
	private String name;
	
	private VnfExtCpNodeProperties properties;
	private VnfExtCpNodeRequirements requirements;
	
	public VnfExtCpNode() {
		
	}

	public VnfExtCpNode(Node nodeTemplate, String nodeName) throws NotFoundException, MalformattedElementException {
		
		this.name = nodeName;
		this.type = nodeTemplate.getType();
		
		if (nodeTemplate.getProperties() == null)
			throw new NotFoundException(
					"You should specify at least the following properties for CP: " + "\n- layer_protocols"
							+ "\n- protocol_data" + "\n- trunk_mode");
		
		this.properties = new VnfExtCpNodeProperties(nodeTemplate.getProperties());
		this.requirements = new VnfExtCpNodeRequirements(nodeTemplate.getRequirements());
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

	public VnfExtCpNodeProperties getProperties() {
		return properties;
	}

	public void setProperties(VnfExtCpNodeProperties properties) {
		this.properties = properties;
	}

	public VnfExtCpNodeRequirements getRequirements() {
		return requirements;
	}

	public void setRequirements(VnfExtCpNodeRequirements requirements) {
		this.requirements = requirements;
	}
	
	@Override
	public String toString() {

		return "VnfExtCp Node; " + "\n" + "type " + type + "\n" + "prop: " + properties + "\n" + "reqs: " + requirements;
	}
}
