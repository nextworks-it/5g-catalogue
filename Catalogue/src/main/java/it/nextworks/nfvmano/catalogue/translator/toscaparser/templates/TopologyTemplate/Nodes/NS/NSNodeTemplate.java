package it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.NS;

import it.nextworks.nfvmano.catalogue.common.exceptions.NotFoundException;
import it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.NodeTemplate;

public class NSNodeTemplate {

	private String type;
	private String name;

	private NSProperties properties = null;
	private NSRequirements requirements = null;

	public NSNodeTemplate() {
	}

	public NSNodeTemplate(NodeTemplate nodeTemplate, String nodeName) throws NotFoundException {

		this.name = nodeName;
		this.type = nodeTemplate.getType();

		if (nodeTemplate.getProperties() == null)
			throw new NotFoundException(
					"You should specify at least endpoint, deployment_flavour and type in properties for VNF: ");

		this.properties = new NSProperties(nodeTemplate.getProperties());

		requirements = new NSRequirements(nodeTemplate.getRequirements());
	}

	@Override
	public String toString() {

		return "NSD Node; " + "\n" + "type " + type + "\n" + "prop: " + properties + "\n" + "reqs: " + requirements
				+ "\n";
	}
}
