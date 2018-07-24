package it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.NS;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class NSRequirements {

	private ArrayList<String> virtualLinks = new ArrayList<>();

	public NSRequirements(Object reqs) {

		ArrayList<LinkedHashMap<String, String>> resMap = (ArrayList<LinkedHashMap<String, String>>) reqs;

		for (LinkedHashMap<String, String> pair : resMap) {

			if (pair.keySet().toArray()[0].equals("virtualLink")) {
				virtualLinks.add(pair.get("virtualLink"));
			}
		}
	}

	public ArrayList<String> getVirtualLinks() {
		return virtualLinks;
	}

	public void setVirtualLinks(ArrayList<String> virtualLinks) {
		this.virtualLinks = virtualLinks;
	}

	@Override
	public String toString() {
		return "links: " + virtualLinks + "\n";
	}
}
