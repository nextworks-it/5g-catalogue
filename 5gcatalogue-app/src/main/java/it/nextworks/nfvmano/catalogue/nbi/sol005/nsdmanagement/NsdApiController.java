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
package it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiParam;
import it.nextworks.nfvmano.catalogue.common.Utilities;
import it.nextworks.nfvmano.catalogue.engine.NsdManagementInterface;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.NotPermittedOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-07-23T16:31:35.952+02:00")

@CrossOrigin
@Controller
public class NsdApiController implements NsdApi {

    private static final Logger log = LoggerFactory.getLogger(NsdApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    @Autowired
    NsdManagementInterface nsdManagementService;

    @Autowired
    public NsdApiController(ObjectMapper objectMapper, HttpServletRequest request)  {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<?> createNsdInfo(
            @RequestParam(required = false, defaultValue = "Admins") String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @Valid @RequestBody CreateNsdInfoRequest body) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            log.debug("Processing REST request to create an NSD info");
            try {
                NsdInfo nsdInfo = nsdManagementService.createNsdInfo(body, project);
                return new ResponseEntity<NsdInfo>(nsdInfo, HttpStatus.CREATED);
            } catch (MalformattedElementException e) {
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                        "NSD info cannot be created"), HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                log.error("Exception while creating NSD info: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "NSD info cannot be created"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.PRECONDITION_FAILED.value(),
                    "Accept header null or different from application/json"), HttpStatus.PRECONDITION_FAILED);
    }

    public ResponseEntity<?> getNSDsInfo(@RequestParam(required = false) String project, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        log.debug("Processing REST request to retrieve all NSD infos");

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        // TODO: process URI parameters for filters and attributes. At the moment it returns all the NSDs info
        if (accept != null && accept.contains("application/json")) {
            try {
                List<NsdInfo> nsdInfos = nsdManagementService.getAllNsdInfos(project);
                log.debug("NSD infos retrieved");
                return new ResponseEntity<List<NsdInfo>>(nsdInfos, HttpStatus.OK);
            } catch (Exception e) {
                log.error("General exception while retrieving set of NSD infos: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "General exception while retrieving set of NSD infos: " + e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.PRECONDITION_FAILED.value(),
                    "Accept header null or different from application/json"), HttpStatus.PRECONDITION_FAILED);
    }

    public ResponseEntity<?> getNSDInfo(
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        log.debug("Processing REST request to retrieve NSD info " + nsdInfoId);
        if (accept != null && accept.contains("application/json")) {
            try {
                NsdInfo nsdInfo = nsdManagementService.getNsdInfo(nsdInfoId, project);
                log.debug("NSD info retrieved");
                return new ResponseEntity<NsdInfo>(nsdInfo, HttpStatus.OK);
            } catch (NotExistingEntityException e) {
                log.error("NSD info " + nsdInfoId + " not found");
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(),
                        "NSD info " + nsdInfoId + " not found"), HttpStatus.NOT_FOUND);
            } catch (MalformattedElementException e) {
                log.error("NSD info " + nsdInfoId + " cannot be found: not acceptable NSD Info ID format");
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                                "NSD info " + nsdInfoId + " cannot be found: not acceptable NSD Info ID format"),
                        HttpStatus.BAD_REQUEST);
            } catch (NotPermittedOperationException e) {
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                                e.getMessage()),
                        HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                log.error("NSD info " + nsdInfoId + " cannot be retrieved: general internal error");
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "NSD info " + nsdInfoId + " cannot be retrieved: general internal error"),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.PRECONDITION_FAILED.value(),
                    "Accept header null or different from application/json"), HttpStatus.PRECONDITION_FAILED);
    }

    public ResponseEntity<?> updateNSDInfo(
            @RequestParam(required = false, defaultValue = "Admins") String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId,
            @ApiParam(value = "", required = true) @Valid @RequestBody NsdInfoModifications body) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        log.debug("Processing REST request for Updating NSD info " + nsdInfoId);
        if (body == null) {
            return new ResponseEntity<String>("Error message: Body is empty!", HttpStatus.BAD_REQUEST);
        }

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                NsdInfoModifications nsdInfoMods = nsdManagementService.updateNsdInfo(body, nsdInfoId, project);
                return new ResponseEntity<NsdInfoModifications>(nsdInfoMods, HttpStatus.OK);
            } catch (NotExistingEntityException e) {
                log.error("Impossible to update NSD info: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(),
                        "Impossible to update NSD info: " + e.getMessage()), HttpStatus.NOT_FOUND);
            } catch (MalformattedElementException e) {
                log.error("Impossible to update NSD info: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                        "Impossible to update NSD info: " + e.getMessage()), HttpStatus.BAD_REQUEST);
            } catch (NotPermittedOperationException e) {
                log.error("Impossible to update NSD info: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(),
                        "Impossible to update NSD info: " + e.getMessage()), HttpStatus.CONFLICT);
            }
        } else {
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.PRECONDITION_FAILED.value(),
                    "Accept header null or different from application/json"), HttpStatus.PRECONDITION_FAILED);
        }
    }

    public ResponseEntity<?> deleteNSDInfo(
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        log.debug("Processing REST request to delete NSD info " + nsdInfoId);
        try {
            nsdManagementService.deleteNsdInfo(nsdInfoId, project);
            log.debug("NSD info removed");
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        } catch (NotExistingEntityException e) {
            log.error("NSD info " + nsdInfoId + " not found");
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(), "NSD info " + nsdInfoId + " not found"),
                    HttpStatus.NOT_FOUND);
        } catch (NotPermittedOperationException e) {
            log.error("NSD info " + nsdInfoId + " cannot be removed: " + e.getMessage());
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(),
                    "NSD info " + nsdInfoId + " cannot be removed: " + e.getMessage()), HttpStatus.CONFLICT);
        } catch (MalformattedElementException e) {
            log.error("NSD info " + nsdInfoId + " cannot be removed: not acceptable NSD Info ID format");
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                            "NSD info " + nsdInfoId + " cannot be removed: not acceptable NSD Info ID format"),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("NSD info " + nsdInfoId + " cannot be removed: general internal error");
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "NSD info " + nsdInfoId + " cannot be removed: general internal error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> getNSD(
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId,
            @ApiParam(value = "The request may contain a \"Range\" HTTP header to obtain single range of bytes from the NSD file. This can be used to continue an aborted transmission.  If the NFVO does not support range requests, the NFVO shall ignore the 'Range\" header, process the GET request, and return the whole NSD file with a 200 OK response (rather than returning a 4xx error status code).") @RequestHeader(value = "Range", required = false) String range) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");

        // TODO: consistency between accept values and input format when onboarding
        // should be better checked.
        // TODO: probably we should select the format based on the accept values. At the
        // at the moment the format is selected based on the original input type,
        // that is maintained in the DB.
        log.debug("Processing REST request to retrieve NSD for NSD info ID " + nsdInfoId);

        try {
            Object nsd = nsdManagementService.getNsd(nsdInfoId, false, project);
            // TODO: here it needs to check the type of entity that is returned
            return new ResponseEntity<Resource>((Resource) nsd, HttpStatus.OK);
        } catch (NotExistingEntityException e) {
            log.error("NSD for NSD info ID " + nsdInfoId + " not found");
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(),
                            "NSD for NSD info ID " + nsdInfoId + " not found: " + e.getMessage()),
                    HttpStatus.NOT_FOUND);
        } catch (NotPermittedOperationException e) {
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(),
                    "NSD for NSD info ID " + nsdInfoId + " not found: " + e.getMessage()), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "NSD for NSD info ID " + nsdInfoId + " not found: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> uploadNSD(
            @RequestParam(required = false, defaultValue = "Admins") String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId,
            @ApiParam(value = "", required = true) @RequestParam("file") MultipartFile body,
            @ApiParam(value = "The payload body contains a copy of the file representing the NSD or a ZIP file that contains the file or multiple files representing the NSD, as specified above. The request shall set the \"Content-Type\" HTTP header as defined above.") @RequestHeader(value = "Content-Type", required = false) String contentType) {

        log.debug("Processing REST request for Uploading NSD content in NSD info " + nsdInfoId);

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        //if (accept != null && accept.contains("application/json")) {
        if (body.isEmpty()) {
            return new ResponseEntity<String>("Error message: File is empty!", HttpStatus.BAD_REQUEST);
        }

        // TODO: the content-type as per SOL005 v2.4.1 should be text/plain or application/zip

        if (!contentType.startsWith("multipart/form-data")) {
            // TODO: to be implemented later on
            return new ResponseEntity<String>("Unable to parse content " + contentType, HttpStatus.NOT_IMPLEMENTED);
        } else {
            try {
                ContentType type = null;
                log.debug("NSD content file name is: " + body.getOriginalFilename());
                if (body.getOriginalFilename().endsWith("zip")) {
                    type = ContentType.ZIP;
                } else if (body.getOriginalFilename().endsWith("yaml")) {
                    type = ContentType.YAML;
                } else {
                    // TODO: to be implemented later on
                    return new ResponseEntity<String>("Unable to parse file type that is not .zip or .yaml",
                            HttpStatus.NOT_IMPLEMENTED);
                }
                nsdManagementService.uploadNsd(nsdInfoId, body, type, false, project);
                log.debug("Upload processing done");
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                // TODO: check if we need to introduce the asynchronous mode
            } catch (NotPermittedOperationException | AlreadyExistingEntityException e) {
                log.error("Impossible to upload NSD: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(),
                        "Impossible to upload NSD: " + e.getMessage()), HttpStatus.CONFLICT);
            } catch (MalformattedElementException e) {
                log.error("Impossible to upload NSD: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                        "Impossible to upload NSD: " + e.getMessage()), HttpStatus.BAD_REQUEST);
            } catch (NotExistingEntityException e) {
                log.error("Impossible to upload NSD: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(),
                        "Impossible to upload NSD: " + e.getMessage()), HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                log.error("General exception while uploading NSD content: " + e.getMessage());
                log.error("Details: ", e);
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "General exception while uploading NSD content: " + e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        /*} else {
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.PRECONDITION_FAILED.value(),
                    "Accept header null or different from application/json"), HttpStatus.PRECONDITION_FAILED);
        }*/
    }

    public ResponseEntity<?> createPNFDInfo(
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @Valid @RequestBody CreatePnfdInfoRequest body) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            log.debug("Processing REST request to create a PNFD info");
            try {
                PnfdInfo pnfdInfo = nsdManagementService.createPnfdInfo(body, project);
                return new ResponseEntity<PnfdInfo>(pnfdInfo, HttpStatus.CREATED);
            } catch (MalformattedElementException e) {
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                        "PNFD info cannot be created"), HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                log.error("Exception while creating PNFD info: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "PNFD info cannot be created"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.PRECONDITION_FAILED.value(),
                    "Accept header null or different from application/json"), HttpStatus.PRECONDITION_FAILED);

        /*String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<PnfdInfo>(objectMapper.readValue(
                        "{  \"pnfdName\" : \"pnfdName\",  \"pnfdUsageState\" : { },  \"pnfdOnboardingState\" : { },  \"_links\" : {    \"pnfd_content\" : \"http://example.com/aeiou\",    \"self\" : \"http://example.com/aeiou\"  },  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"pnfdProvider\" : \"pnfdProvider\",  \"pnfdVersion\" : \"pnfdVersion\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"userDefinedData\" : { }}",
                        PnfdInfo.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<PnfdInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<PnfdInfo>(HttpStatus.NOT_IMPLEMENTED);*/
    }

    public ResponseEntity<?> getPNFDsInfo(
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "Indicates to exclude the following complex attributes from the response. See clause 4.3.3 for details. The NFVO shall support this parameter. The following attributes shall be excluded from the PnfdInfo structure in the response body if this parameter is provided, or none of the parameters \"all_fields,\" \"fields\", \"exclude_fields\", \"exclude_default\" are provided: userDefinedData.")
            @Valid @RequestParam(value = "exclude_default", required = false) String excludeDefault,
            @ApiParam(value = "Include all complex attributes in the response. See clause 4.3.3 for details. The NFVO shall support this parameter.") @Valid @RequestParam(value = "all_fields", required = false) String allFields) {
        log.debug("Processing REST request to retrieve all PNFD infos");

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");

        // TODO: process URI parameters for filters and attributes. At the moment it returns all the PNFDs info
        if (accept != null && accept.contains("application/json")) {
            try {
                List<PnfdInfo> pnfdInfos = nsdManagementService.getAllPnfdInfos(project);
                log.debug("PNFD infos retrieved");
                return new ResponseEntity<List<PnfdInfo>>(pnfdInfos, HttpStatus.OK);
            } catch (Exception e) {
                log.error("General exception while retrieving set of PNFD infos: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "General exception while retrieving set of PNFD infos: " + e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.PRECONDITION_FAILED.value(),
                    "Accept header null or different from application/json"), HttpStatus.PRECONDITION_FAILED);

        /*String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<PnfdInfo>>(objectMapper.readValue(
                        "[ {  \"pnfdName\" : \"pnfdName\",  \"pnfdUsageState\" : { },  \"pnfdOnboardingState\" : { },  \"_links\" : {    \"pnfd_content\" : \"http://example.com/aeiou\",    \"self\" : \"http://example.com/aeiou\"  },  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"pnfdProvider\" : \"pnfdProvider\",  \"pnfdVersion\" : \"pnfdVersion\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"userDefinedData\" : { }}, {  \"pnfdName\" : \"pnfdName\",  \"pnfdUsageState\" : { },  \"pnfdOnboardingState\" : { },  \"_links\" : {    \"pnfd_content\" : \"http://example.com/aeiou\",    \"self\" : \"http://example.com/aeiou\"  },  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"pnfdProvider\" : \"pnfdProvider\",  \"pnfdVersion\" : \"pnfdVersion\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"userDefinedData\" : { }} ]",
                        List.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<PnfdInfo>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<PnfdInfo>>(HttpStatus.NOT_IMPLEMENTED);*/
    }

    public ResponseEntity<?> getPNFDInfo(
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");

        log.debug("Processing REST request to retrieve PNFD info " + pnfdInfoId);
        if (accept != null && accept.contains("application/json")) {
            try {
                PnfdInfo pnfdInfo = nsdManagementService.getPnfdInfo(pnfdInfoId, project);
                log.debug("PNFD info retrieved");
                return new ResponseEntity<PnfdInfo>(pnfdInfo, HttpStatus.OK);
            } catch (NotExistingEntityException e) {
                log.error("PNFD info " + pnfdInfoId + " not found");
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(),
                        "PNFD info " + pnfdInfoId + " not found"), HttpStatus.NOT_FOUND);
            } catch (MalformattedElementException e) {
                log.error("PNFD info " + pnfdInfoId + " cannot be found: not acceptable PNFD Info ID format");
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                                "PNFD info " + pnfdInfoId + " cannot be found: not acceptable PNFD Info ID format"),
                        HttpStatus.BAD_REQUEST);
            } catch (NotPermittedOperationException e) {
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                                e.getMessage()),
                        HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                log.error("PNFD info " + pnfdInfoId + " cannot be retrieved: general internal error");
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "PNFD info " + pnfdInfoId + " cannot be retrieved: general internal error"),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.PRECONDITION_FAILED.value(),
                    "Accept header null or different from application/json"), HttpStatus.PRECONDITION_FAILED);

        /*String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<PnfdInfo>(objectMapper.readValue(
                        "{  \"pnfdName\" : \"pnfdName\",  \"pnfdUsageState\" : { },  \"pnfdOnboardingState\" : { },  \"_links\" : {    \"pnfd_content\" : \"http://example.com/aeiou\",    \"self\" : \"http://example.com/aeiou\"  },  \"onboardingFailureDetails\" : {    \"instance\" : \"http://example.com/aeiou\",    \"detail\" : \"detail\",    \"type\" : \"http://example.com/aeiou\",    \"title\" : \"title\",    \"status\" : 0  },  \"pnfdProvider\" : \"pnfdProvider\",  \"pnfdVersion\" : \"pnfdVersion\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"userDefinedData\" : { }}",
                        PnfdInfo.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<PnfdInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<PnfdInfo>(HttpStatus.NOT_IMPLEMENTED);*/
    }

    public ResponseEntity<?> updatePNFDInfo(
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId,
            @ApiParam(value = "", required = true) @Valid @RequestBody PnfdInfoModifications body) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<PnfdInfoModifications>(
                        objectMapper.readValue("{  \"userDefinedData\" : { }}", PnfdInfoModifications.class),
                        HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<PnfdInfoModifications>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<PnfdInfoModifications>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<?> deletePNFDInfo(
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        log.debug("Processing REST request to delete PNFD info " + pnfdInfoId);
        try {
            nsdManagementService.deletePnfdInfo(pnfdInfoId, project);
            log.debug("PNFD info removed");
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        } catch (NotExistingEntityException e) {
            log.error("PNFD info " + pnfdInfoId + " not found");
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(), "PNFD info " + pnfdInfoId + " not found"),
                    HttpStatus.NOT_FOUND);
        } catch (NotPermittedOperationException e) {
            log.error("PNFD info " + pnfdInfoId + " cannot be removed: " + e.getMessage());
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(),
                    "PNFD info " + pnfdInfoId + " cannot be removed: " + e.getMessage()), HttpStatus.CONFLICT);
        } catch (MalformattedElementException e) {
            log.error("PNFD info " + pnfdInfoId + " cannot be removed: not acceptable PNFD Info ID format");
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                            "PNFD info " + pnfdInfoId + " cannot be removed: not acceptable PNFD Info ID format"),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("PNFD info " + pnfdInfoId + " cannot be removed: general internal error");
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "PNFD info " + pnfdInfoId + " cannot be removed: general internal error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        /*String accept = request.getHeader("Accept");
        return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);*/
    }

    public ResponseEntity<?> getPNFD(
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");

        // TODO: consistency between accept values and input format when onboarding
        // should be better checked.
        // TODO: probably we should select the format based on the accept values. At the
        // at the moment the format is selected based on the original input type,
        // that is maintained in the DB.
        log.debug("Processing REST request to retrieve PNFD for PNFD info ID " + pnfdInfoId);

        try {
            Object pnfd = nsdManagementService.getPnfd(pnfdInfoId, false, project);
            // TODO: here it needs to check the type of entity that is returned
            return new ResponseEntity<Resource>((Resource) pnfd, HttpStatus.OK);
        } catch (NotExistingEntityException e) {
            log.error("PNFD for PNFD info ID " + pnfdInfoId + " not found");
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(),
                            "PNFD for PNFD info ID " + pnfdInfoId + " not found: " + e.getMessage()),
                    HttpStatus.NOT_FOUND);
        } catch (NotPermittedOperationException e) {
            return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(),
                    "PNFD for PNFD info ID " + pnfdInfoId + " not found: " + e.getMessage()), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "PNFD for PNFD info ID " + pnfdInfoId + " not found: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        /*String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<Object>(objectMapper.readValue("\"{}\"", Object.class),
                        HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);*/
    }

    public ResponseEntity<?> uploadPNFD(
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId,
            @ApiParam(value = "", required = true) @RequestParam("file") MultipartFile body,
            @ApiParam(value = "The request shall set the \"Content-Type\" HTTP header to \"text/plain\".")
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestParam(required = false) String project,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        log.debug("Processing REST request for Uploading PNFD content in PNFD info " + pnfdInfoId);

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        //if (accept != null && accept.contains("application/json")) {
        if (body.isEmpty()) {
            return new ResponseEntity<String>("Error message: File is empty!", HttpStatus.BAD_REQUEST);
        }

        // TODO: the content-type as per SOL005 v2.4.1 should be text/plain or application/zip

        if (!contentType.startsWith("multipart/form-data")) {
            // TODO: to be implemented later on
            return new ResponseEntity<String>("Unable to parse content " + contentType, HttpStatus.NOT_IMPLEMENTED);
        } else {
            try {
                ContentType type = null;
                log.debug("PNFD content file name is: " + body.getOriginalFilename());
                if (body.getOriginalFilename().endsWith("zip")) {
                    type = ContentType.ZIP;
                } else if (body.getOriginalFilename().endsWith("yaml")) {
                    type = ContentType.YAML;
                } else {
                    // TODO: to be implemented later on
                    return new ResponseEntity<String>("Unable to parse file type that is not .zip or .yaml",
                            HttpStatus.NOT_IMPLEMENTED);
                }
                nsdManagementService.uploadPnfd(pnfdInfoId, body, type, false, project);
                log.debug("Upload processing done");
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                // TODO: check if we need to introduce the asynchronous mode
            } catch (NotPermittedOperationException | AlreadyExistingEntityException e) {
                log.error("Impossible to upload PNFD: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.CONFLICT.value(),
                        "Impossible to upload PNFD: " + e.getMessage()), HttpStatus.CONFLICT);
            } catch (MalformattedElementException e) {
                log.error("Impossible to upload PNFD: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.BAD_REQUEST.value(),
                        "Impossible to upload PNFD: " + e.getMessage()), HttpStatus.BAD_REQUEST);
            } catch (NotExistingEntityException e) {
                log.error("Impossible to upload PNFD: " + e.getMessage());
                return new ResponseEntity<ProblemDetails>(Utilities.buildProblemDetails(HttpStatus.NOT_FOUND.value(),
                        "Impossible to upload PNFD: " + e.getMessage()), HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                log.error("General exception while uploading PNFD content: " + e.getMessage());
                log.error("Details: ", e);
                return new ResponseEntity<ProblemDetails>(
                        Utilities.buildProblemDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "General exception while uploading PNFD content: " + e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        /*String accept = request.getHeader("Accept");

        // TODO: the content-type as per SOL005 v2.4.1 should be text/plain
        return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);*/
    }


    public ResponseEntity<?> createSubscription(
            @ApiParam(value = "", required = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody NsdmSubscriptionRequest body) {

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<NsdmSubscription>(objectMapper.readValue(
                        "{  \"filter\" : {    \"nsdOnboardingState\" : { },    \"pnfdName\" : \"pnfdName\",    \"pnfdUsageState\" : { },    \"nsdInfoId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"pnfdOnboardingState\" : { },    \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdUsageState\" : { },    \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdDesigner\" : \"nsdDesigner\",    \"nsdVersion\" : \"nsdVersion\",    \"pnfdProvider\" : \"pnfdProvider\",    \"pnfdVersion\" : \"pnfdVersion\",    \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdName\" : \"nsdName\",    \"notificationTypes\" : [ { }, { } ],    \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdOperationalState\" : { }  },  \"_links\" : {    \"self\" : \"http://example.com/aeiou\"  },  \"callbackUri\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}",
                        NsdmSubscription.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<NsdmSubscription>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NsdmSubscription>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<?> getSubscriptions(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<NsdmSubscription>>(objectMapper.readValue(
                        "[ {  \"filter\" : {    \"nsdOnboardingState\" : { },    \"pnfdName\" : \"pnfdName\",    \"pnfdUsageState\" : { },    \"nsdInfoId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"pnfdOnboardingState\" : { },    \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdUsageState\" : { },    \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdDesigner\" : \"nsdDesigner\",    \"nsdVersion\" : \"nsdVersion\",    \"pnfdProvider\" : \"pnfdProvider\",    \"pnfdVersion\" : \"pnfdVersion\",    \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdName\" : \"nsdName\",    \"notificationTypes\" : [ { }, { } ],    \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdOperationalState\" : { }  },  \"_links\" : {    \"self\" : \"http://example.com/aeiou\"  },  \"callbackUri\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}, {  \"filter\" : {    \"nsdOnboardingState\" : { },    \"pnfdName\" : \"pnfdName\",    \"pnfdUsageState\" : { },    \"nsdInfoId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"pnfdOnboardingState\" : { },    \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdUsageState\" : { },    \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdDesigner\" : \"nsdDesigner\",    \"nsdVersion\" : \"nsdVersion\",    \"pnfdProvider\" : \"pnfdProvider\",    \"pnfdVersion\" : \"pnfdVersion\",    \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdName\" : \"nsdName\",    \"notificationTypes\" : [ { }, { } ],    \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdOperationalState\" : { }  },  \"_links\" : {    \"self\" : \"http://example.com/aeiou\"  },  \"callbackUri\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"} ]",
                        List.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<NsdmSubscription>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<NsdmSubscription>>(HttpStatus.NOT_IMPLEMENTED);
    }


    public ResponseEntity<?> getSubscription(
            @ApiParam(value = "", required = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("subscriptionId") String subscriptionId) {
        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<NsdmSubscription>(objectMapper.readValue(
                        "{  \"filter\" : {    \"nsdOnboardingState\" : { },    \"pnfdName\" : \"pnfdName\",    \"pnfdUsageState\" : { },    \"nsdInfoId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfPkgIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"pnfdOnboardingState\" : { },    \"nestedNsdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdUsageState\" : { },    \"pnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"pnfdInfoIds\" : [ \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\" ],    \"nsdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdDesigner\" : \"nsdDesigner\",    \"nsdVersion\" : \"nsdVersion\",    \"pnfdProvider\" : \"pnfdProvider\",    \"pnfdVersion\" : \"pnfdVersion\",    \"nsdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdName\" : \"nsdName\",    \"notificationTypes\" : [ { }, { } ],    \"pnfdInvariantId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"nsdOperationalState\" : { }  },  \"_links\" : {    \"self\" : \"http://example.com/aeiou\"  },  \"callbackUri\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}",
                        NsdmSubscription.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<NsdmSubscription>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NsdmSubscription>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<?> deleteSubscription(
            @ApiParam(value = "", required = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("subscriptionId") String subscriptionId) {
        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        String accept = request.getHeader("Accept");
        return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
    }
}