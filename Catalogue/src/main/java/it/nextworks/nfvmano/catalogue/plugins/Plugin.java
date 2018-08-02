package it.nextworks.nfvmano.catalogue.plugins;

public abstract class Plugin {
	
	protected String pluginId;
	protected PluginType pluginType;
	//KAFKA config attributes
	protected String kafkaBootstrapServers;
	protected String kafkaGroupId;
	
	public Plugin(String pluginId, PluginType pluginType, String kafkaBootstrapServers) {
		this.pluginId = pluginId;
		this.pluginType = pluginType;
		this.kafkaBootstrapServers = kafkaBootstrapServers;
		//IMPORTANT - KAFKA config: we set group id equal to pluginId (assuming pluginId is unique)
		this.kafkaGroupId = pluginId;
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

	public String getKafkaBootstrapServers() {
		return kafkaBootstrapServers;
	}

	public String getKafkaGroupId() {
		return kafkaGroupId;
	}
	
	public abstract void init();
	
}
