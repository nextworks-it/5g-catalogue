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
import com.google.common.io.CharStreams;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementService;
import it.nextworks.nfvmano.catalogue.engine.VnfPackageManagementInterface;
import it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.Catalogue;
import it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.CataloguePlugin;
import it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.api.nsd.DefaultApi;
import it.nextworks.nfvmano.catalogue.plugins.mano.*;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r3.OpenSourceMANOR3Plugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4plus.OpenSourceMANOR4PlusPlugin;
import it.nextworks.nfvmano.catalogue.plugins.vim.VIM;
import it.nextworks.nfvmano.catalogue.repos.*;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class PluginsManager {

    private static final Logger log = LoggerFactory.getLogger(PluginsManager.class);

    public Map<String, MANOPlugin> manoDrivers = new HashMap<>();

    public Map<String, CataloguePlugin> catalogueDrivers = new HashMap<>();

    private Resource[] resources;

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${catalogue.defaultMANOType}")
    private String defaultMANOType;

    @Value("${catalogue.manoPluginsConfigurations}")
    private String configurationsDir;

    @Value("${catalogue.vimPluginsConfiguration}")
    private String vimConfigurationsDir;

    @Value("${catalogue.skipMANOPluginsConfig}")
    private boolean skipMANOConfig;

    @Value("${catalogue.skipDescriptorsLoad}")
    private boolean skipDescriptorsLoad;

    @Value("${kafkatopic.local}")
    private String localNotificationTopic;

    @Value("${kafkatopic.remote}")
    private String remoteNotificationTopic;

    @Value("${catalogue.defaultMANOType}")
    private String defaultManoType;

    @Value("${catalogue.osm.localDir}")
    private String osmDir;

    /*
    @Value("${catalogue.osmr4.localDir}")
    private Path osmr4Dir;
*/
    @Value("${catalogue.logo}")
    private Path logo;

    @Value("${catalogue.scope}")
    private String catalogueScope;

    @Value("${catalogue.catalogueConfiguration}")
    private String catalogueConfigurationsDir;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NsdManagementService nsdService;

    @Autowired
    private VnfPackageManagementInterface vnfdService;

    @Autowired
    private DescriptorsParser descriptorsParser;

    @Autowired
    private MANORepository MANORepository;

    @Autowired
    private VIMRepository vimRepository;

    @Autowired
    private CatalogueRepository catalogueRepository;

    @Autowired
    private NsdInfoRepository nsdInfoRepo;

    @Autowired
    private PnfdInfoRepository pnfdInfoRepo;

    @Autowired
    private VnfPkgInfoRepository vnfPkgInfoRepository;

    @Autowired
    private  UserRepository userRepository;

    public PluginsManager() {

    }

    @PostConstruct
    public void initPlugins() {

        if (defaultManoType.equalsIgnoreCase("DUMMY")) {
            MANO dummy = new DummyMano("DUMMY", MANOType.DUMMY);
            dummy.setPluginOperationalState(PluginOperationalState.ENABLED);
            MANORepository.saveAndFlush(dummy);
            try {
                addMANO(dummy);
            } catch (MalformattedElementException e) {
                log.error("Unable to instantiate DUMMY MANO Plugin. Malformatted MANO: " + e.getMessage());
            }
        } else {
            if (!skipMANOConfig) {
                resources = loadMANOConfigurations();

                ObjectMapper mapper = new ObjectMapper();

                if (resources != null) {
                    for (int i = 0; i < resources.length; i++) {
                        if (resources[i].isFile() || resources[i] instanceof ClassPathResource) {
                            try {
                                InputStream resource = resources[i].getInputStream();
                                String tmp;
                                try (final Reader reader = new InputStreamReader(resource)) {
                                    tmp = CharStreams.toString(reader);
                                }
                                log.debug("Loading MANO configuration from config file #" + i + "");
                                MANO newMano = mapper.readValue(tmp, MANO.class);
                                log.debug("Successfully loaded configuration for MANO with manoId: "
                                        + newMano.getManoId());
                                try {
                                    log.debug("Creating MANO Plugin with manoId " + newMano.getManoId()
                                            + " from configuration file");
                                    createMANOPlugin(newMano);
                                } catch (AlreadyExistingEntityException e) {
                                    log.error("MANO with manoId " + newMano.getManoId() + " already present in DB");
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

            log.debug("Loading MANO info from DB");
            List<MANO> manos = MANORepository.findAll();

            if (manos.isEmpty())
                log.debug("No MANO info stored in DB");

            for (MANO mano : manos) {
                try {
                    log.debug("Instantiating MANO with manoId: " + mano.getManoId());
                    if (mano.getPluginOperationalState() != PluginOperationalState.DELETING) {
                        log.debug("Instantiating MANO with manoId: " + mano.getManoId());
                        addMANO(mano);
                        log.debug("MANO with manoId " + mano.getManoId() + " successfully instantiated");
                    } else {
                        log.debug("Found MANO with manoId " + mano.getManoId() + " in DELETING state: removing it");
                        MANORepository.deleteById(mano.getId());
                        log.debug("MANO with manoId " + mano.getManoId() + " successfully deleted");
                    }

                } catch (MalformattedElementException e) {
                    log.error("Malformatted MANO: " + e.getMessage() + ". Skipping");
                }
            }

        }

        if (catalogueScope.equalsIgnoreCase("private")) {
            log.debug("Instantiating 5G Catalogue drivers...");

            log.debug("Loading Catalogue info from DB");
            List<Catalogue> catalogues = catalogueRepository.findAll();

            if (catalogues.isEmpty())
                log.debug("No Catalogue info stored in DB");

            for (Catalogue catalogue : catalogues) {
                try {
                    log.debug("Instantiating Catalogue with catalogueId: " + catalogue.getCatalogueId());
                    if (catalogue.getPluginOperationalState() != PluginOperationalState.DELETING) {
                        log.debug("Instantiating Catalogue with catalogueId: " + catalogue.getCatalogueId());
                        create5GCatalogue(catalogue, true);
                        log.debug("Catalogue with catalogueId " + catalogue.getCatalogueId() + " successfully instantiated");
                    } else {
                        log.debug("Found Catalogue with catalogueId " + catalogue.getCatalogueId() + " in DELETING state: removing it");
                        catalogueRepository.deleteById(catalogue.getId());
                        log.debug("Catalogue with catalogueId " + catalogue.getCatalogueId() + " successfully deleted");
                    }

                } catch (AlreadyExistingEntityException e) {
                    log.warn("Catalogue with catalogueId " + catalogue.getCatalogueId() + " already exists");
                } catch (FailedOperationException e) {
                    log.warn(e.getMessage());
                }
            }

            log.debug("Loading Catalogue info from configuration files");

            resources = load5GCatalogueConfigurations();

            ObjectMapper mapper = new ObjectMapper();

            if (resources != null) {
                for (int i = 0; i < resources.length; i++) {
                    if (resources[i].isFile() || resources[i] instanceof ClassPathResource) {
                        try {
                            InputStream resource = resources[i].getInputStream();
                            String tmp;
                            try (final Reader reader = new InputStreamReader(resource)) {
                                tmp = CharStreams.toString(reader);
                            }
                            log.debug("Loading 5G Catalogue configuration from config file #" + i + "");
                            Catalogue newCatalogue = mapper.readValue(tmp, Catalogue.class);
                            log.debug("Successfully loaded configuration for 5G Catalogue with catalogueId: "
                                    + newCatalogue.getCatalogueId());
                            try {
                                log.debug("Creating 5G Catalogue Plugin with catalogueId " + newCatalogue.getCatalogueId()
                                        + " from configuration file");
                                create5GCatalogue(newCatalogue, false);
                            } catch (AlreadyExistingEntityException e) {
                                log.warn("5G Catalogue with catalogueId " + newCatalogue.getCatalogueId() + " already present in DB");
                            } catch (FailedOperationException e) {
                                log.warn(e.getMessage());
                            }
                        } catch (IOException e) {
                            log.error("Unable to parse 5G Catalogue configuration file: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public void addMANO(MANO mano) throws MalformattedElementException {
        try {
            MANOPlugin manoPlugin = buildMANOPlugin(mano);

            // Init kafka consumer and enable plugin
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
            return new DummyMANOPlugin(mano.getManoType(), mano, bootstrapServers, nsdService, vnfdService, descriptorsParser,
                    localNotificationTopic, remoteNotificationTopic, kafkaTemplate);
        } else if (mano.getManoType().equals(MANOType.OSMR3)) {
            Path osmr3Dir = Paths.get(osmDir, "/" + MANOType.OSMR3.toString().toLowerCase());
            return new OpenSourceMANOR3Plugin(mano.getManoType(), mano, bootstrapServers, nsdService, vnfdService, descriptorsParser,
                    localNotificationTopic, remoteNotificationTopic, kafkaTemplate, osmr3Dir, logo);
        } else if (mano.getManoType().equals(MANOType.OSMR4) || mano.getManoType().equals(MANOType.OSMR5) || mano.getManoType().equals(MANOType.OSMR6)) {
            Path osmr4PlusDir = Paths.get(osmDir, "/" + mano.getManoType().toString().toLowerCase());
            return new OpenSourceMANOR4PlusPlugin(mano.getManoType(), mano, bootstrapServers, nsdService, vnfdService, descriptorsParser,
                    MANORepository, localNotificationTopic, remoteNotificationTopic, kafkaTemplate, osmr4PlusDir, logo);
        } else {
            throw new MalformattedElementException("Unsupported MANO type. Skipping");
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

        if (mano.getPluginOperationalState() == null) {
            mano.setPluginOperationalState(PluginOperationalState.ENABLED);
        }
        MANOType type = mano.getManoType();
        log.debug("RECEIVED MANO:\nMANO ID: " + manoId + "\nMANO TYPE: " + type);

        if (type == MANOType.OSMR3 || type == MANOType.OSMR4 || type == MANOType.OSMR5 || type == MANOType.OSMR6) {
            log.debug("Processing request for creating " + type + "Plugin");
            OSMMano osmMano = (OSMMano) mano;
            OSMMano targetOsmMano = new OSMMano(
                    osmMano.getManoId(),
                    osmMano.getIpAddress(),
                    osmMano.getUsername(),
                    osmMano.getPassword(),
                    osmMano.getProject(),
                    type,
                    osmMano.getVimAccounts()
            );
            targetOsmMano.setPluginOperationalState(PluginOperationalState.ENABLED);
            targetOsmMano.isValid();
            log.debug("Persisting OSM MANO with manoId: " + manoId);
            OSMMano createdMano = MANORepository.saveAndFlush(targetOsmMano);
            log.debug("OSM MANO with manoId " + manoId + " successfully persisted");
            log.debug("Instantiating OSM MANO with manoId: " + manoId);
            try {
                addMANO(createdMano);
            } catch (MalformattedElementException e) {
                log.error("Unsupported MANO type");
            }
            log.debug("OSM MANO with manoId " + manoId + " successfully instantiated");
            return String.valueOf(createdMano.getId());
        } else if (type == MANOType.DUMMY) {
            log.debug("Processing request for creating " + type + "Plugin");
            DummyMano dummyMano = (DummyMano) mano;
            log.debug("Persisting DUMMY MANO with manoId: " + manoId);
            DummyMano createdMano = MANORepository.saveAndFlush(dummyMano);
            log.debug("DUMMY MANO with manoId " + manoId + " successfully persisted");
            log.debug("Instantiating DUMMY MANO with manoId: " + manoId);
            try {
                addMANO(createdMano);
            } catch (MalformattedElementException e) {
                log.error("Unsupported MANO type");
            }
            log.debug("DUMMY MANO with manoId " + manoId + " successfully instantiated");
            return String.valueOf(createdMano.getId());
        } else {
            log.error("Unsupported MANO type");
            throw new MethodNotImplementedException("Unsupported MANO type");
        }
    }

    public MANO getMANOPlugin(String manoId) throws NotExistingEntityException {

        log.debug("Processing request for retrieving MANO Plugin with manoId {}", manoId);

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

        log.debug("Processing request for retrieving all MANO Plugins");

        List<MANO> manos = new ArrayList<>();
        manos = MANORepository.findAll();
        return manos;
    }

    public MANO updateMANOPlugin(String manoId, MANO mano) throws NotExistingEntityException, FailedOperationException {

        log.debug("Processing request for updating MANO Plugin with manoId {}", manoId);

        Optional<MANO> optionalMANO = MANORepository.findByManoId(manoId);

        MANO manoInfo = null;

        if (optionalMANO.isPresent()) {
            manoInfo = optionalMANO.get();

            if (manoInfo.getPluginOperationalState() == mano.getPluginOperationalState()) {
                throw new FailedOperationException("MANO Plugin with manoId " + manoId + " already in " + manoInfo.getPluginOperationalState() + " operational state");
            } else {
                manoInfo.setPluginOperationalState(mano.getPluginOperationalState());
                MANORepository.saveAndFlush(manoInfo);

                MANOPlugin manoPlugin = manoDrivers.get(manoInfo.getManoId());
                manoPlugin.setPluginOperationalState(manoInfo.getPluginOperationalState());
                manoDrivers.replace(manoInfo.getManoId(), manoPlugin);
            }
        } else {
            throw new NotExistingEntityException("MANO Plugin with manoId " + manoId + " not present in DB");
        }

        return manoInfo;
    }

    private Resource[] loadMANOConfigurations() {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource[] resources = null;
        try {
            log.debug("Loading MANO configuration files from: " + configurationsDir);
            resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources(configurationsDir + "/*.json");
            log.debug("MANO configuration files successfully loaded");
        } catch (IOException e) {
            log.error("Unable to load MANO configuration files: " + e.getMessage());
        }

        return resources;
    }

    public String createVIMPlugin(VIM vim)
            throws AlreadyExistingEntityException, MethodNotImplementedException, MalformattedElementException {

        return null;
    }

    public VIM getVIMPlugin(String vimId) throws NotExistingEntityException {

        log.debug("Processing request for retrieving VIM Plugin with vimId {}", vimId);

        Optional<VIM> optionalVIM = vimRepository.findByVimId(vimId);

        VIM vim = null;

        if (optionalVIM.isPresent()) {
            vim = optionalVIM.get();
        } else {
            throw new NotExistingEntityException("VIM Plugin with vimId " + vimId + " not present in DB");
        }

        return vim;
    }

    public List<VIM> getAllVIMPlugins() {

        log.debug("Processing request for retrieving all VIM Plugins");

        List<VIM> vims = new ArrayList<>();
        vims = vimRepository.findAll();
        return vims;
    }

    public void addCatalogue(Catalogue catalogue) throws FailedOperationException {
        try {
            CataloguePlugin cataloguePlugin = buildCataloguePlugin(catalogue);

            // Init kafka consumer
            cataloguePlugin.init();

            catalogueDrivers.put(catalogue.getCatalogueId(), cataloguePlugin);

            log.debug("Loaded plugin for 5G Catalogue " + catalogue.getCatalogueId());

            // TODO: notify 5G Catalogue plugin creation
        } catch (Exception e) {
            log.error("Failed to add 5G Catalogue plugin: " + e.getMessage());
            catalogueRepository.deleteById(catalogue.getId());
            throw new FailedOperationException("Failed to add 5G Catalogue plugin: " + e.getMessage());
        }
    }

    private CataloguePlugin buildCataloguePlugin(Catalogue catalogue) {
        return new CataloguePlugin(
                catalogue.getCatalogueId(),
                PluginType.C2C,
                catalogue,
                bootstrapServers,
                nsdService,
                vnfdService,
                new DefaultApi(catalogue),
                new it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue.api.vnf.DefaultApi(catalogue),
                nsdInfoRepo,
                pnfdInfoRepo,
                vnfPkgInfoRepository,
                localNotificationTopic,
                remoteNotificationTopic,
                kafkaTemplate,
                skipDescriptorsLoad
        );
    }

    public String create5GCatalogue(Catalogue catalogue, boolean isInternal)
            throws AlreadyExistingEntityException, FailedOperationException {
        String catalogueId = catalogue.getCatalogueId();

        if (catalogueRepository.findByCatalogueId(catalogueId).isPresent() && !isInternal) {
            log.error("A 5G Catalogue with the same ID is already available in DB - Not acceptable");
            throw new AlreadyExistingEntityException(
                    "A 5G Catalogue with the same ID is already available in DB - Not acceptable");
        }
        ObjectMapper mapper = new ObjectMapper();
        log.debug("Persisting 5G Catalogue with catalogueId: " + catalogueId);
        if (catalogue.getPluginOperationalState() == null) {
            catalogue.setPluginOperationalState(PluginOperationalState.ENABLED);
        }

        Catalogue createdCatalogue = catalogueRepository.saveAndFlush(catalogue);
        log.debug("5G Catalogue with catalogueId " + catalogueId + " successfully persisted");

        log.debug("Instantiating 5G Catalogue with catalogueId: " + catalogueId);
        addCatalogue(catalogue);
        log.debug("5G Catalogue with catalogueId " + catalogueId + " successfully instantiated");

        return String.valueOf(createdCatalogue.getId());
    }

    public Catalogue get5GCataloguePlugin(String catalogueId) throws NotExistingEntityException {

        log.debug("Processing request for retrieving 5G Catalogue Plugin with catalogueId {}", catalogueId);

        Optional<Catalogue> optionalCatalogue = catalogueRepository.findByCatalogueId(catalogueId);

        Catalogue catalogue = null;

        if (optionalCatalogue.isPresent()) {
            catalogue = optionalCatalogue.get();
        } else {
            throw new NotExistingEntityException("Catalogue Plugin with catalogueId " + catalogueId + " not present in DB");
        }

        return catalogue;
    }

    public List<Catalogue> getAll5GCataloguePlugins() {

        log.debug("Processing request for retrieving all 5G Catalogue Plugins");

        List<Catalogue> catalogues = new ArrayList<>();
        catalogues = catalogueRepository.findAll();
        return catalogues;
    }

    public Catalogue update5GCataloguePlugin(String catalogueId, Catalogue catalogue) throws NotExistingEntityException, FailedOperationException {

        log.debug("Processing request for updating 5G Catalogue Plugin with catalogueId {}", catalogueId);

        Optional<Catalogue> optionalCatalogue = catalogueRepository.findByCatalogueId(catalogueId);

        Catalogue catalogueInfo = null;

        if (optionalCatalogue.isPresent()) {
            catalogueInfo = optionalCatalogue.get();

            if (catalogueInfo.getPluginOperationalState() == catalogue.getPluginOperationalState()) {
                throw new FailedOperationException("Catalogue Plugin with catalogueId " + catalogueId + " already in " + catalogueInfo.getPluginOperationalState() + " operational state");
            } else {
                catalogueInfo.setPluginOperationalState(catalogue.getPluginOperationalState());
                catalogueRepository.saveAndFlush(catalogueInfo);

                CataloguePlugin cataloguePlugin = catalogueDrivers.get(catalogueInfo.getCatalogueId());
                cataloguePlugin.setPluginOperationalState(catalogueInfo.getPluginOperationalState());
                catalogueDrivers.replace(catalogueInfo.getCatalogueId(), cataloguePlugin);
            }
        } else {
            throw new NotExistingEntityException("Catalogue Plugin with catalogueId " + catalogueId + " not present in DB");
        }

        return catalogueInfo;
    }

    private Resource[] load5GCatalogueConfigurations() {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource[] resources = null;
        try {
            log.debug("Loading 5G Catalogue configuration files from: " + catalogueConfigurationsDir);
            resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources(catalogueConfigurationsDir + "/*.json");
            log.debug("5G Catalogue configuration files successfully loaded");
        } catch (IOException e) {
            log.error("Unable to load 5G Catalogue configuration files: " + e.getMessage());
        }

        return resources;
    }
}
