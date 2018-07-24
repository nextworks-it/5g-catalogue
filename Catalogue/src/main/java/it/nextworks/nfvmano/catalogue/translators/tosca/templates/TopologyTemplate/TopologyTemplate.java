
package it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate;

import it.nextworks.nfvmano.catalogue.common.exceptions.NotFoundException;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.Node;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.CP.CPNodeTemplate;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.NS.NSNodeTemplate;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VDU.VDUNodeTemplate;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VL.VLNodeTemplate;
import it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VNF.VNFNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class TopologyTemplate {

	private Object inputs = null;
	private Map<String, Node> node_templates;

	public Object getInputs() {
		return inputs;
	}

	public void setInputs(Object inputs) {
		this.inputs = inputs;
	}

	public Map<String, Node> getNode_templates() {
		return node_templates;
	}

	public void setNode_templates(Map<String, Node> node_templates) {
		this.node_templates = node_templates;
	}

	public List<CPNodeTemplate> getCPNodes() {

		List<CPNodeTemplate> cpNodes = new ArrayList<>();

		for (String nodeName : node_templates.keySet()) {

			Node n = node_templates.get(nodeName);
			if (Objects.equals(n.getType().toLowerCase(), "tosca.nodes.nfv.cp")) {

				CPNodeTemplate cpNode = new CPNodeTemplate(n, nodeName);
				cpNodes.add(cpNode);
			}
		}
		return cpNodes;
	}

	public List<VDUNodeTemplate> getVDUNodes() {

		List<VDUNodeTemplate> vduNodes = new ArrayList<>();

		for (String nodeName : node_templates.keySet()) {

			Node n = node_templates.get(nodeName);
			if (Objects.equals(n.getType().toLowerCase(), "tosca.nodes.nfv.vdu")) {

				VDUNodeTemplate vduNode = new VDUNodeTemplate(n, nodeName);
				vduNodes.add(vduNode);
			}
		}

		return vduNodes;
	}

	public List<VLNodeTemplate> getVLNodes() {

		List<VLNodeTemplate> vlNodes = new ArrayList<>();

		for (String nodeName : node_templates.keySet()) {

			Node n = node_templates.get(nodeName);
			if (Objects.equals(n.getType().toLowerCase(), "tosca.nodes.nfv.vl")) {
				VLNodeTemplate vduNode = new VLNodeTemplate(n, nodeName);
				vlNodes.add(vduNode);
			}
		}

		return vlNodes;
	}

	public List<VNFNode> getVNFNodes() throws NotFoundException {

		List<VNFNode> vnfNodes = new ArrayList<>();

		for (String nodeName : node_templates.keySet()) {

			Node n = node_templates.get(nodeName);
			if (Objects.equals(n.getType().toLowerCase(), "tosca.nodes.nfv.vnf")) {

				VNFNode vnfNode = new VNFNode(n, nodeName);
				vnfNodes.add(vnfNode);
			}
		}

		return vnfNodes;
	}

	public List<NSNodeTemplate> getNSNodes() throws NotFoundException {

		List<NSNodeTemplate> nsNodes = new ArrayList<>();

		for (String nodeName : node_templates.keySet()) {

			Node n = node_templates.get(nodeName);
			if (Objects.equals(n.getType().toLowerCase(), "tosca.nodes.nfv.ns")) {

				NSNodeTemplate nsNode = new NSNodeTemplate(n, nodeName);
				nsNodes.add(nsNode);
			}
		}

		return nsNodes;
	}

	@Override
	public String toString() {
		return "Topology: \n" + "inuts: " + inputs + "\n" + "Nodes: \n" + node_templates;
	}
}
