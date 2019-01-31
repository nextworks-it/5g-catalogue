/*
 * Copyright 2018 Nextworks s.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.nextworks.nfvmano.catalogue.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementService;
import it.nextworks.nfvmano.catalogue.engine.VnfPackageManagementInterface;
import it.nextworks.nfvmano.catalogue.plugins.mano.*;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r3.OpenSourceMANOR3Plugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4.OpenSourceMANOR4Plugin;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

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

    @Value("${catalogue.skipMANOPluginsConfig}")
    private boolean skipMANOConfig;

    @Value("${kafkatopic.local.nsd}")
    private String localNsdNotificationTopic;

    @Value("${kafkatopic.remote.nsd}")
    private String remoteNsdNotificationTopic;

    @Value("${catalogue.defaultMANOType}")
    private String defaultManoType;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NsdManagementService nsdService;

    @Autowired
    private VnfPackageManagementInterface vnfdService;

    @Autowired
    private MANORepository MANORepository;

    public PluginsManager() {

    }

    @PostConstruct
    public void initPlugins() {

        if (defaultManoType.equalsIgnoreCase("DUMMY")) {
            MANO dummy = new DummyMano("DUMMY", MANOType.DUMMY);
            try {
                addMANO(dummy);
            } catch (MalformattedElementException e) {
                log.error("Unable to instantiate DUMMY MANO Plugin. Malformatted MANO: " + e.getMessage());
            }
        } else {
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

            if (!skipMANOConfig) {
                resources = loadConfigurations();

                ObjectMapper mapper = new ObjectMapper();

                if (resources != null) {
                    for (int i = 0; i < resources.length; i++) {
                        if (resources[i].isFile()) {
                            try {
                                File tmp = resources[i].getFile();
                                log.debug("Loading MANO configuration from config file #" + i + ".");
                                MANO newMano = mapper.readValue(tmp, MANO.class);
                                log.debug("Successfully loaded configuration for MANO with manoId: "
                                        + newMano.getManoId());
                                try {
                                    log.debug("Creating MANO Plugin with manoId " + newMano.getManoId()
                                            + " from configuration file.");
                                    createMANOPlugin(newMano);
                                } catch (AlreadyExistingEntityException e) {
                                    log.error("MANO with manoId " + newMano.getManoId() + " already present in DB.");
                                } catch (MethodNotImplementedException e) {
                                    log.error("Unsupported MANO type for MANO with manoId: " + newMano.getManoId());
                                } catch (MalformattedElementException e) {
                                    log.error("Malformatted MANO with manoId " + newMano.getManoId() + ": "
                                            + e.getMessage());
                                }
                            } catch (IOException e) {
                                log.error("Unable to retrieve MANO configuration file: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    public void addMANO(MANO mano) throws MalformattedElementException {
        try {
            MANOPlugin manoPlugin = buildMANOPlugin(mano);

            // Init kafka consumer
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
            return new DummyMANOPlugin(mano.getManoType(), mano, bootstrapServers, nsdService, localNsdNotificationTopic,
                    remoteNsdNotificationTopic, kafkaTemplate);
        } else if (mano.getManoType().equals(MANOType.OSMR3)) {
            return new OpenSourceMANOR3Plugin(mano.getManoType(), mano, bootstrapServers, nsdService, vnfdService,
                    localNsdNotificationTopic, remoteNsdNotificationTopic, kafkaTemplate);
        } else if (mano.getManoType().equals(MANOType.OSMR4)) {
                return new OpenSourceMANOR4Plugin(mano.getManoType(), mano, bootstrapServers, nsdService, vnfdService,
                        localNsdNotificationTopic, remoteNsdNotificationTopic, kafkaTemplate);
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
            case OSMR3:
            case OSMR4:
                log.debug("Processing request for creating " + type + "Plugin.");
                OSMMano osmMano = (OSMMano) mano;
                OSMMano targetOsmMano = new OSMMano(osmMano.getManoId(), osmMano.getIpAddress(), osmMano.getUsername(),
                            osmMano.getPassword(), osmMano.getProject(), type);
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
                log.debug("OSM MANO with manoId " + manoId + " successfully instantiated.");
                return String.valueOf(createdMano.getId());
            default:
                log.error("Unsupported MANO type.");
                throw new MethodNotImplementedException("Unsupported MANO type.");
        }
    }

    public MANO getMANOPlugin(String manoId) throws NotExistingEntityException {

        log.debug("Processing request for retrieving MANO Plugin with manoId {}.", manoId);

        Optional<MANO> optionalMANO = MANORepository.findByManoId(manoId);

        MANO mano = null;

        if (optionalMANO.isPresent()) {
            mano = optionalMANO.get();
        } else {
            throw new NotExistingEntityException("MANO Plugin with manoId " + manoId + " not present in DB");
        }

        return mano;
    }

    public List<MANO> getAllMANOPlugins() {

        log.debug("Processing request for retrieving all MANO Plugins.");

        List<MANO> manos = new ArrayList<>();
        manos = MANORepository.findAll();
        return manos;
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
