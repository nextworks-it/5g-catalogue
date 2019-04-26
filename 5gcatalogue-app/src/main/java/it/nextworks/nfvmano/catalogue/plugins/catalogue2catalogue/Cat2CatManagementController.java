package it.nextworks.nfvmano.catalogue.plugins.catalogue2catalogue;

import io.swagger.annotations.ApiParam;
import it.nextworks.nfvmano.catalogue.plugins.PluginsManager;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/catalogue/cat2catManagement")
public class Cat2CatManagementController {

    private static final Logger log = LoggerFactory.getLogger(Cat2CatManagementController.class);

    @Autowired
    PluginsManager pluginsManager;

    public Cat2CatManagementController() {

    }

    @RequestMapping(value = "/5gcatalogues", method = RequestMethod.POST)
    public ResponseEntity<?> create5GCatalogue(@ApiParam(value = "", required = true) @RequestBody Catalogue catalogue) {

        log.debug("Received request for new 5G Catalogue loading");
        if ((catalogue == null) || (catalogue.getCatalogueId() == null)) {
            log.error("Malformatted 5G Catalogue - Not acceptable");
            return new ResponseEntity<String>("Catalogue or Catalogue ID null", HttpStatus.BAD_REQUEST);
        }

        String createdCatalogueId;

        try {
            createdCatalogueId = pluginsManager.create5GCatalogue(catalogue);
        } catch (AlreadyExistingEntityException e) {
            return new ResponseEntity<String>("5G Catalogue already present in DB", HttpStatus.CONFLICT);
        }

        return new ResponseEntity<String>(createdCatalogueId, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/5gcatalogues", method = RequestMethod.GET)
    public ResponseEntity<List<Catalogue>> get5GCatalogues() {

        log.debug("Received request for getting 5G Catalogue plugins");

        List<Catalogue> catalogues = pluginsManager.getAll5GCataloguePlugins();

        return new ResponseEntity<List<Catalogue>>(catalogues, HttpStatus.OK);
    }

    @RequestMapping(value = "/5gcatalogues/{catalogueId}", method = RequestMethod.GET)
    public ResponseEntity<?> get5Gcatalogue(@ApiParam(value = "", required = true) @PathVariable("catalogueId") String catalogueId) {

        log.debug("Received request for getting 5G Catalogue plugin with catalogueId {}", catalogueId);

        Catalogue catalogue = null;

        try {
            catalogue = pluginsManager.get5GCataloguePlugin(catalogueId);
        } catch (NotExistingEntityException e) {
            return new ResponseEntity<String>("5G Catalogue Plugin with catalogueId " + catalogueId + " not present in DB", HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<Catalogue>(catalogue, HttpStatus.OK);
    }
}
