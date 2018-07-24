package it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.NS;

import java.util.Map;

public class NSProperties {

	private String ns_id;
	private String designer;
	private String version;
	private String name;
	private String invariant_id;

	public NSProperties() {
	}

	public NSProperties(Object properties) {

		Map<String, Object> propertiesMap = (Map<String, Object>) properties;

		if (propertiesMap.containsKey("ns_id")) {
			this.ns_id = (String) propertiesMap.get("ns_id");
		}

		if (propertiesMap.containsKey("designer")) {
			this.designer = (String) propertiesMap.get("designer");
		}

		if (propertiesMap.containsKey("version")) {
			this.version = (String) propertiesMap.get("version");
		}

		if (propertiesMap.containsKey("name")) {
			this.name = (String) propertiesMap.get("name");
		}

		if (propertiesMap.containsKey("invariant_id")) {
			this.invariant_id = (String) propertiesMap.get("invariant_id");
		}
	}

	public String getNs_id() {
		return ns_id;
	}

	public void setNs_id(String ns_id) {
		this.ns_id = ns_id;
	}

	public String getDesigner() {
		return designer;
	}

	public void setDesigner(String designer) {
		this.designer = designer;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInvariant_id() {
		return invariant_id;
	}

	public void setInvariant_id(String invariant_id) {
		this.invariant_id = invariant_id;
	}
}
