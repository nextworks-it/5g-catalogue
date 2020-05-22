package it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OnapVnfDescriptor {

    private Map<String, Object> nsDescriptor = new HashMap<String, Object>();

    @JsonAnyGetter
    public Map<String, Object> any() {
        return nsDescriptor;
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        nsDescriptor.put(name, value);
    }

    @JsonIgnore
    public Map <String, Object> getMetadata(){
        return (Map <String, Object>) this.nsDescriptor.get("metadata");
    }

    @JsonIgnore
    public Map <String, Object> getTopologyTemplate(){
        return (Map <String, Object>) this.nsDescriptor.get("topology_template");
    }

    @JsonIgnore
    public Map <String, Object> getNodeTemplates(){
        return (Map <String, Object>) getTopologyTemplate().get("node_templates");
    }

    @JsonIgnore
    public Map<String, Object> getInputs(){
        return (Map <String, Object>) getTopologyTemplate().get("inputs");
    }

    @JsonIgnore
    public String getVnfdId(){
        return (String) getMetadata().get("UUID");
    }

    @JsonIgnore
    public String getImageName() throws IllegalArgumentException{
        Map<String, String> imageDescription = (Map<String, String>) getInputs().get("image_name");
        if(imageDescription == null)
            throw new IllegalArgumentException("Cannot find image name in inputs parameter");
        return imageDescription.get("default");
    }

    @JsonIgnore
    public Integer getRam() throws IllegalArgumentException{
        Map<String, String> ramDescription = (Map<String, String>) getInputs().get("vRam");
        if(ramDescription == null)
            throw new IllegalArgumentException("Cannot find vRam in inputs parameters");
        return Integer.valueOf(ramDescription.get("default"));
    }

    @JsonIgnore
    public Integer getCpu() throws IllegalArgumentException{
        Map<String, String> cpuDescription = (Map<String, String>) getInputs().get("vCpu");
        if(cpuDescription == null)
            throw new IllegalArgumentException("Cannot find vCpu in inputs parameters");
        return Integer.valueOf(cpuDescription.get("default"));
    }

    @JsonIgnore
    public Integer getStorage() throws IllegalArgumentException{
        Map<String, String> storageDescription = (Map<String, String>) getInputs().get("storage");
        if(storageDescription == null)
            throw new IllegalArgumentException("Cannot find storage size in inputs parameters");
        return Integer.valueOf(storageDescription.get("default"));
    }

    @JsonIgnore
    public String getMgmtCp() throws IllegalArgumentException{
        Map<String, String> mgmtCpDescription = (Map<String, String>) getInputs().get("mgmt_port");
        if(mgmtCpDescription == null)
            throw new IllegalArgumentException("Cannot find management port in inputs parameters");
        return mgmtCpDescription.get("default");
    }

    @JsonIgnore
    public Map<String, String> getConnectionPointLinkAssociations() throws IllegalArgumentException{
        Map<String, Object> inputs = getInputs();
        Map<String, String> cpLinkAssociations = new HashMap<>();
        Map<String, Object> nodeTemplates = getNodeTemplates();
        for (Map.Entry<String, Object> nodeTemplate : nodeTemplates.entrySet()){
            Map <String, Object> node = (Map <String, Object>) nodeTemplate.getValue();
            String nodeType = (String) node.get("type");
            if(nodeType.startsWith("org.openecomp.resource.vfc.")){
                Map<String, Object> properties = (Map <String, Object>) node.get("properties");
                List<String> propertiesName = new ArrayList<>(properties.keySet());
                List<String> cpNames = propertiesName.stream().filter(name -> name.endsWith("port_network")).collect(Collectors.toList());
                for(String cpName : cpNames){
                    Object cpProperties = properties.get(cpName);
                    if(cpProperties instanceof List){
                        Object link = ((List) cpProperties).get(0);
                        if(link instanceof Map) {
                            Map<String, String> linkDescription = (Map<String, String>) inputs.get(((Map) link).get("get_input"));
                            cpLinkAssociations.put(cpName, linkDescription.get("default"));
                        }else if(link instanceof String)
                            cpLinkAssociations.put(cpName, findLinkName((String)link));
                        else
                            throw new IllegalArgumentException("Illegal argument provided for port : " + cpName);
                    }else if(cpProperties instanceof Map){
                        Map<String, String> linkDescription = (Map<String, String>) inputs.get(((Map) cpProperties).get("get_input"));
                        cpLinkAssociations.put(cpName, linkDescription.get("default"));
                    } else
                        throw new IllegalArgumentException("Illegal argument provided for port : " + cpName);
                }
            }
        }
        return cpLinkAssociations;
    }

    @JsonIgnore
    private String findLinkName(String networkNodeName) throws IllegalArgumentException{
        Map<String, Object> inputs = getInputs();
        Map<String, Object> nodeTemplates = getNodeTemplates();
        Map<String, Object> networkNode = (Map<String, Object>)nodeTemplates.get(networkNodeName);
        Map<String, Object> properties = (Map<String, Object>)networkNode.get("properties");
        Object networkName =  properties.get("network_name");
        if(networkName instanceof List) {
            Object link = ((List) networkName).get(0);
            Map<String, String> linkDescription = (Map<String, String>) inputs.get(((Map) link).get("get_input"));
            return linkDescription.get("default");
        }else if(networkName instanceof Map){
            Map<String, String> linkDescription = (Map<String, String>) inputs.get(((Map) networkName).get("get_input"));
            return linkDescription.get("default");
        }else
            throw new IllegalArgumentException("Illegal argument for network name in node : " + networkNodeName);
    }
}
