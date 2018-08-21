package it.nextworks.nfvmano.catalogue.plugins;

public abstract class Plugin {
	
	protected String pluginId;
	protected PluginType pluginType;
	//KAFKA config attributes
	
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

	public abstract void init();
	
}
