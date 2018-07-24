
package it.nextworks.nfvmano.catalogue.translators.tosca.templates.TopologyTemplate.Nodes.VnfExtCp;

import java.util.ArrayList;
import java.util.Map;

import it.nextworks.nfvmano.catalogue.common.exceptions.NotFoundException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;

public class VnfExtCpNodeProperties {

	ArrayList<LayerProtocolType> layer_protocols;
	RoleType role;
	String description;
	ArrayList<CpProtocolData> protocol_data;
	boolean trunk_mode;
	
	public VnfExtCpNodeProperties() {
		
	}

	@SuppressWarnings("unchecked")
	public VnfExtCpNodeProperties(Object properties) throws NotFoundException, MalformattedElementException {

		Map<String, Object> propertiesMap = (Map<String, Object>) properties;

		if (propertiesMap.containsKey("layer_protocols")) {
			this.layer_protocols = (ArrayList<LayerProtocolType>) propertiesMap.get("layer_protocol");
		} else {
			throw new NotFoundException("layer_protocols fild is required in CP's properties");
		}

		if (propertiesMap.containsKey("role") && ((RoleType) propertiesMap.get("role") == RoleType.LEAF
				|| (RoleType) propertiesMap.get("role") == RoleType.ROOT)) {
			this.role = (RoleType) propertiesMap.get("role");
		} else throw new MalformattedElementException("Specified role is not valid for CP, valid roles are: LEAF, ROOT");

		if (propertiesMap.containsKey("description")) {
			this.description = (String) propertiesMap.get("descripetion");
		}
		
		if (propertiesMap.containsKey("protocol_data")) {
			this.protocol_data = (ArrayList<CpProtocolData>) propertiesMap.get("protocol_data");
		} else {
			throw new NotFoundException("protocol_data fild is required in CP's properties");
		}
		
		if (propertiesMap.containsKey("trunk_mode")) {
			this.trunk_mode = (boolean) propertiesMap.get("trunk_mode");
		} else {
			throw new NotFoundException("trunk_mode fild is required in CP's properties");
		}
	}

	public ArrayList<LayerProtocolType> getLayer_protocols() {
		return layer_protocols;
	}

	public void setLayer_protocols(ArrayList<LayerProtocolType> layer_protocols) {
		this.layer_protocols = layer_protocols;
	}

	public RoleType getRole() {
		return role;
	}

	public void setRole(RoleType role) {
		this.role = role;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArrayList<CpProtocolData> getProtocol_data() {
		return protocol_data;
	}

	public void setProtocol_data(ArrayList<CpProtocolData> protocol_data) {
		this.protocol_data = protocol_data;
	}

	public boolean isTrunk_mode() {
		return trunk_mode;
	}

	public void setTrunk_mode(boolean trunk_mode) {
		this.trunk_mode = trunk_mode;
	}
	
	
}
