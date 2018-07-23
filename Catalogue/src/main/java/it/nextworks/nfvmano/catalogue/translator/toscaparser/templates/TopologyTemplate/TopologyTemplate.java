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

package it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate;

import it.nextworks.nfvmano.catalogue.translator.toscaparser.exceptions.NotFoundException;
import it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.CP.CPNodeTemplate;
import it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.NS.NSNodeTemplate;
import it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.NodeTemplate;
import it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.VDU.VDUNodeTemplate;
import it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.VL.VLNodeTemplate;
import it.nextworks.nfvmano.catalogue.translator.toscaparser.templates.TopologyTemplate.Nodes.VNF.VNFNodeTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Created by rvl on 17.08.16. */
public class TopologyTemplate {

	private Object inputs = null;
	private Map<String, NodeTemplate> node_templates;

	public Object getInputs() {
		return inputs;
	}

	public void setInputs(Object inputs) {
		this.inputs = inputs;
	}

	public Map<String, NodeTemplate> getNode_templates() {
		return node_templates;
	}

	public void setNode_templates(Map<String, NodeTemplate> node_templates) {
		this.node_templates = node_templates;
	}

	public List<CPNodeTemplate> getCPNodes() {

		List<CPNodeTemplate> cpNodes = new ArrayList<>();

		for (String nodeName : node_templates.keySet()) {

			NodeTemplate n = node_templates.get(nodeName);
			if (Objects.equals(n.getType().toLowerCase(), "tosca.nodes.nfv.cp")) {

				CPNodeTemplate cpNode = new CPNodeTemplate(n);
				cpNodes.add(cpNode);
			}
		}
		return cpNodes;
	}

	public List<VDUNodeTemplate> getVDUNodes() {

		List<VDUNodeTemplate> vduNodes = new ArrayList<>();

		for (String nodeName : node_templates.keySet()) {

			NodeTemplate n = node_templates.get(nodeName);
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

			NodeTemplate n = node_templates.get(nodeName);
			if (Objects.equals(n.getType().toLowerCase(), "tosca.nodes.nfv.vl")) {
				VLNodeTemplate vduNode = new VLNodeTemplate(n, nodeName);
				vlNodes.add(vduNode);
			}
		}

		return vlNodes;
	}

	public List<VNFNodeTemplate> getVNFNodes() throws NotFoundException {

		List<VNFNodeTemplate> vnfNodes = new ArrayList<>();

		for (String nodeName : node_templates.keySet()) {

			NodeTemplate n = node_templates.get(nodeName);
			if (Objects.equals(n.getType().toLowerCase(), "tosca.nodes.nfv.vnf")) {

				VNFNodeTemplate vnfNode = new VNFNodeTemplate(n, nodeName);
				vnfNodes.add(vnfNode);
			}
		}

		return vnfNodes;
	}

	public List<NSNodeTemplate> getNSNodes() throws NotFoundException {

		List<NSNodeTemplate> nsNodes = new ArrayList<>();

		for (String nodeName : node_templates.keySet()) {

			NodeTemplate n = node_templates.get(nodeName);
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
