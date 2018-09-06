package it.nextworks.nfvmano.catalogue.plugins.mano;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;

@RestController
@CrossOrigin
@RequestMapping("/catalogue/manoManagement")
public class MANOManagementController {
	
	private static final Logger log = LoggerFactory.getLogger(MANOManagementController.class);
	
	@Autowired
	private MANOManagementService manoService;
	
	public MANOManagementController() {

	}
	
	@RequestMapping(value = "/mano", method = RequestMethod.POST)
	public ResponseEntity<?> createMANO(@RequestBody MANO mano) throws MethodNotImplementedException {
		
		log.debug("Received request for new MANO loading");
		if ((mano == null) || (mano.getManoId() == null)) {
			log.error("Malformatted MANO - Not acceptable");
			return new ResponseEntity<String>("MANO or MANO ID null", HttpStatus.BAD_REQUEST);
		} else {
			if (mano.getManoType() == null) {
				log.error("Malformatted MANO - Not acceptable");
				return new ResponseEntity<String>("MANO TYPE null", HttpStatus.BAD_REQUEST);
			}
		}
		
		String createdManoId;
		
		try {
			createdManoId = manoService.createMANOPlugin(mano);
		} catch (AlreadyExistingEntityException e) {
			return new ResponseEntity<String>("MANO already present in DB", HttpStatus.CONFLICT);
		} catch (MethodNotImplementedException e) {
			return new ResponseEntity<String>("Unsupported MANO type", HttpStatus.BAD_REQUEST);
		} catch (MalformattedElementException e) {
			return new ResponseEntity<String>("Malformatted MANO: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<String>(createdManoId, HttpStatus.CREATED);
	}
}
