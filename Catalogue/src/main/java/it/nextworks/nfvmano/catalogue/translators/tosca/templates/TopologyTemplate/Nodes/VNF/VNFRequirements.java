
package it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VNF;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class VNFRequirements {

	private ArrayList<String> virtualLinks = new ArrayList<>();
	private ArrayList<String> vdus = new ArrayList<>();
	
	public VNFRequirements() {
		
	}

	@SuppressWarnings("unchecked")
	public VNFRequirements(Object reqs) {

		ArrayList<LinkedHashMap<String, String>> resMap = (ArrayList<LinkedHashMap<String, String>>) reqs;

		for (LinkedHashMap<String, String> pair : resMap) {

			if (pair.keySet().toArray()[0].equals("virtualLink")) {
				virtualLinks.add(pair.get("virtualLink"));
			}

			if (pair.keySet().toArray()[0].equals("vdu")) {
				vdus.add(pair.get("vdu"));
			}
		}
	}

	public ArrayList<String> getVirtualLinks() {
		return virtualLinks;
	}

	public void setVirtualLinks(ArrayList<String> virtualLinks) {
		this.virtualLinks = virtualLinks;
	}

	public ArrayList<String> getVDUS() {
		return vdus;
	}

	public void setVDUS(ArrayList<String> forwarders) {
		this.vdus = forwarders;
	}

	@Override
	public String toString() {
		return "links: " + virtualLinks + "\n" + "forwarders" + vdus + "\n";
	}
}
