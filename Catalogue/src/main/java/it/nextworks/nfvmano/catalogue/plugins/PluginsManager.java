package it.nextworks.nfvmano.catalogue.plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import it.nextworks.nfvmano.catalogue.plugins.mano.MANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANORepository;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.mano.OpenSourceMANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.DummyMANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;

public class PluginsManager {

	private static final Logger log = LoggerFactory.getLogger(PluginsManager.class);

	public Map<String, MANOPlugin> manoDrivers = new HashMap<>();

	@Value("${kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${catalogue.defaultMANOType}")
	private String defaultMANOType;

	@Autowired
	private MANORepository MANORepository;
	
	public PluginsManager() {

	}

	@PostConstruct
	public void initPlugins() {

		log.debug("Initializing MANO plugins");
		List<MANO> manos = MANORepository.findAll();
		for (MANO mano : manos) {
			try {
				addMANO(mano);
			} catch (MalformattedElementException e) {
				log.warn("Malformed MANO: " + e.getMessage() + ". Skipping.");
				continue;
			}
		}
		log.debug("MANO plugins initialized");
	}

	public void addMANO(MANO mano) throws MalformattedElementException {
		try {
			MANOPlugin manoPlugin = buildMANOPlugin(mano);
			
			//Init kafka consumer
			manoPlugin.init();
			
			manoDrivers.put(mano.getManoId(), manoPlugin);
			
			log.debug("Loaded plugin for MANO " + mano.getManoId());
	
			// TODO: notify MANO plugin creation
		} catch (Exception e) {
			log.error("Failed to add MANO plugin: " + e.getMessage());
		}
	}

	private MANOPlugin buildMANOPlugin(MANO mano) throws MalformattedElementException {
		if (mano.getManoType().equals(MANOType.DUMMY)) {
			return new DummyMANOPlugin(mano.getManoType(), mano, bootstrapServers);
		} else if (mano.getManoType().equals(MANOType.OSM)) {
			return new OpenSourceMANOPlugin(mano.getManoType(), mano, bootstrapServers);
		} else {
			throw new MalformattedElementException("Unsupported MANO type. Skipping.");
		}
	}
}
