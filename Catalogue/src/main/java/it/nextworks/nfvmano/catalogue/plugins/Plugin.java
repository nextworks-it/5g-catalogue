package it.nextworks.nfvmano.catalogue.plugins;

public abstract class Plugin {

	private String pluginId;
	private PluginType pluginType;

	public Plugin(String pluginId, PluginType pluginType) {
		this.pluginId = pluginId;
		this.pluginType = pluginType;
	}

	public String getPluginId() {
		return pluginId;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	public PluginType getPluginType() {
		return pluginType;
	}

	public void setPluginType(PluginType pluginType) {
		this.pluginType = pluginType;
	}
}
