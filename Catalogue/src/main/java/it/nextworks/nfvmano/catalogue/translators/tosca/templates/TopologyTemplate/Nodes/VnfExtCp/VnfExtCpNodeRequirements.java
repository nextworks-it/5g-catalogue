
package it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VnfExtCp;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class VnfExtCpNodeRequirements {
	
	private ArrayList<String> external_virtual_links = new ArrayList<>();
	private ArrayList<String> internal_virtual_links = new ArrayList<>();

	public VnfExtCpNodeRequirements(Object requirements) {
		
		ArrayList<LinkedHashMap<String, String>> resMap = (ArrayList<LinkedHashMap<String, String>>) requirements;
		
		for (LinkedHashMap<String, String> pair : resMap) {

			if (pair.keySet().toArray()[0].equals("external_virtual_link")) {
				external_virtual_links.add(pair.get("external_virtual_link"));
			}

			if (pair.keySet().toArray()[0].equals("internal_virtual_link")) {
				internal_virtual_links.add(pair.get("internal_virtual_link"));
			}
		}
	}

	public ArrayList<String> getExternal_virtual_links() {
		return external_virtual_links;
	}

	public void setExternal_virtual_links(ArrayList<String> external_virtual_links) {
		this.external_virtual_links = external_virtual_links;
	}

	public ArrayList<String> getInternal_virtual_links() {
		return internal_virtual_links;
	}

	public void setInternal_virtual_links(ArrayList<String> internal_virtual_links) {
		this.internal_virtual_links = internal_virtual_links;
	}
}
