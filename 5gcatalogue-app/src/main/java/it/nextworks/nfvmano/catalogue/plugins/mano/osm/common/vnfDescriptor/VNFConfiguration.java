package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VNFConfiguration {

    private Script script;
    @JsonProperty("juju")
    private JujuCharm jujuCharm;
    @JsonProperty("config-primitive")
    private List<ServicePrimitive> servicePrimitives;
    @JsonProperty("initial-config-primitive")
    private List<InitialConfigurationPrimitive> initialConfigurationPrimitives;

    private Map<String, Object> otherProperties = new HashMap<String, Object>();

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    public JujuCharm getJujuCharm() {
        return jujuCharm;
    }

    public void setJujuCharm(JujuCharm jujuCharm) {
        this.jujuCharm = jujuCharm;
    }

    public List<ServicePrimitive> getServicePrimitives() {
        return servicePrimitives;
    }

    public void setServicePrimitives(List<ServicePrimitive> servicePrimitives) {
        this.servicePrimitives = servicePrimitives;
    }

    public List<InitialConfigurationPrimitive> getInitialConfigurationPrimitives() {
        return initialConfigurationPrimitives;
    }

    public void setInitialConfigurationPrimitives(List<InitialConfigurationPrimitive> initialConfigurationPrimitives) {
        this.initialConfigurationPrimitives = initialConfigurationPrimitives;
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return otherProperties;
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        otherProperties.put(name, value);
    }

    @Override
    public String toString() {
        return "VNFConfiguration{" +
                "  script=" + script +
                ", jujuCharm=" + jujuCharm +
                ", servicePrimitives=" + servicePrimitives +
                ", initialConfigurationPrimitives=" + initialConfigurationPrimitives +
                ", otherProperties=" + otherProperties +
                '}';
    }
}
