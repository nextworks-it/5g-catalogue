package it.nextworks.nfvmano.catalogue.plugins.mano;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

import it.nextworks.nfvmano.catalogue.plugins.PluginsManager;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;

@Service
public class MANOManagementService {

	private static final Logger log = LoggerFactory.getLogger(MANOManagementController.class);

	@Value("${catalogue.manoPluginsConfigurations}")
	private String configurationsDir;

	private static Resource[] resources;

	@Autowired
	private PluginsManager pluginsManager;

	@Autowired
	private MANORepository manoRepository;

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

	@PostConstruct
	public void init() {
		
		log.debug("Loading MANO info from DB.");
		List<MANO> manos = manoRepository.findAll();

		for (MANO mano : manos) {
			try {
				log.debug("Instantiating MANO with manoId: " + mano.getManoId());
				pluginsManager.addMANO(mano);
				log.debug("MANO with manoId " + mano.getManoId() + " successfully instantiated.");
			} catch (MalformattedElementException e) {
				log.error("Malformatted MANO: " + e.getMessage());
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

	public String createMANOPlugin(MANO mano)
			throws AlreadyExistingEntityException, MethodNotImplementedException, MalformattedElementException {
		String manoId = mano.getManoId();
		if (manoRepository.findByManoId(manoId).isPresent()) {
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
			OSMMano createdMano = manoRepository.saveAndFlush(targetOsmMano);
			log.debug("OSM MANO with manoId " + manoId + " sucessfully persisted.");
			log.debug("Instantiating OSM MANO with manoId: " + manoId);
			try {
				pluginsManager.addMANO(createdMano);
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
}
