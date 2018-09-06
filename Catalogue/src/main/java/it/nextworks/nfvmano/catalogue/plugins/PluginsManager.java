package it.nextworks.nfvmano.catalogue.plugins;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.nextworks.nfvmano.catalogue.plugins.mano.MANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANORepository;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OpenSourceMANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.DummyMANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;

@Service
public class PluginsManager {

	private static final Logger log = LoggerFactory.getLogger(PluginsManager.class);

	public Map<String, MANOPlugin> manoDrivers = new HashMap<>();
	
	private Resource[] resources;

	@Value("${kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${catalogue.defaultMANOType}")
	private String defaultMANOType;
	
	@Value("${catalogue.manoPluginsConfigurations}")
	private String configurationsDir;

	@Autowired
	private MANORepository MANORepository;
	
	public PluginsManager() {

	}

	@PostConstruct
	public void initPlugins() {
		
		log.debug("Loading MANO info from DB.");
		List<MANO> manos = MANORepository.findAll();
		
		if (manos.isEmpty())
			log.debug("No MANO info stored in DB.");

		for (MANO mano : manos) {
			try {
				log.debug("Instantiating MANO with manoId: " + mano.getManoId());
				addMANO(mano);
				log.debug("MANO with manoId " + mano.getManoId() + " successfully instantiated.");
			} catch (MalformattedElementException e) {
				log.error("Malformatted MANO: " + e.getMessage() + ". Skipping.");
			}
		}

		resources = loadConfigurations();

		ObjectMapper mapper = new ObjectMapper();

		if (resources != null) {
			for (int i = 0; i < resources.length; i++) {
				if (resources[i].isFile()) {
					try {
						File tmp = resources[i].getFile();
						log.debug("Loading MANO configuration from config file #" + i + ".");
						MANO newMano = mapper.readValue(tmp, MANO.class);
						log.debug("Successfully loaded configuration for MANO with manoId: " + newMano.getManoId());
						try {
							log.debug("Creating MANO Plugin with manoId " + newMano.getManoId() + " from configuration file.");
							createMANOPlugin(newMano);
						} catch (AlreadyExistingEntityException e) {
							log.error("MANO with manoId " + newMano.getManoId() + " already present in DB.");
						} catch (MethodNotImplementedException e) {
							log.error("Unsupported MANO type for MANO with manoId: " + newMano.getManoId());
						} catch (MalformattedElementException e) {
							log.error("Malformatted MANO with manoId " + newMano.getManoId() + ": " + e.getMessage());
						}
					} catch (IOException e) {
						log.error("Unable to retrieve MANO configuration file: " + e.getMessage());
					}
				}
			}
		}
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
	
	public String createMANOPlugin(MANO mano)
			throws AlreadyExistingEntityException, MethodNotImplementedException, MalformattedElementException {
		String manoId = mano.getManoId();
		if (MANORepository.findByManoId(manoId).isPresent()) {
			log.error("A MANO with the same ID is already available in DB - Not acceptable");
			throw new AlreadyExistingEntityException(
					"A MANO with the same ID is already available in DB - Not acceptable");

		}
		MANOType type = mano.getManoType();
		log.debug("RECEIVED MANO:\nMANO ID: " + manoId + "\nMANO TYPE: " + type);

		switch (type) {
		case OSM:
			log.debug("Processing request for creating " + type + "Plugin.");
			OSMMano osmMano = (OSMMano) mano;
			OSMMano targetOsmMano = new OSMMano(osmMano.getManoId(), osmMano.getIpAddress(), osmMano.getUsername(),
					osmMano.getPassword(), osmMano.getProject());
			targetOsmMano.isValid();
			log.debug("Persisting OSM MANO with manoId: " + manoId);
			OSMMano createdMano = MANORepository.saveAndFlush(targetOsmMano);
			log.debug("OSM MANO with manoId " + manoId + " sucessfully persisted.");
			log.debug("Instantiating OSM MANO with manoId: " + manoId);
			try {
				addMANO(createdMano);
			} catch (MalformattedElementException e) {
				log.error("Unsupported MANO type.");
			}
			log.debug("OSM MANO with manoId " + manoId + "successfully instantiated.");
			return String.valueOf(createdMano.getId());
		default:
			log.error("Unsupported MANO type.");
			throw new MethodNotImplementedException("Unsupported MANO type.");
		}
	}
	
	private Resource[] loadConfigurations() {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Resource[] resources = null;
		try {
			log.debug("Loading MANO configuration files from: " + configurationsDir);
			resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
					.getResources(configurationsDir + "/*.json");
			log.debug("MANO configuration files successfully loaded.");
		} catch (IOException e) {
			log.error("Unable to load MANO configuration files: " + e.getMessage());
		}

		return resources;
	}
}
