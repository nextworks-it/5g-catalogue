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
package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement;

import it.nextworks.nfvmano.catalogue.engine.VnfPackageManagementInterface;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.CreateVnfPkgInfoRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.PkgmSubscription;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.PkgmSubscriptionRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.ProblemDetails;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.UploadVnfPackageFromUriRequest;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPkgInfo;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.VnfPkgInfoModifications;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-10-05T11:50:31.473+02:00")

@Controller
@CrossOrigin
public class VnfpkgmApiController implements VnfpkgmApi {

    private static final Logger log = LoggerFactory.getLogger(VnfpkgmApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @Autowired
    VnfPackageManagementInterface vnfPackageManagementInterface;

    @org.springframework.beans.factory.annotation.Autowired
    public VnfpkgmApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<PkgmSubscription> createSubscription(@ApiParam(value = "" ,required=true )  @Valid @RequestBody PkgmSubscriptionRequest body) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<PkgmSubscription>(objectMapper.readValue("{  \"filter\" : {    \"vnfProducts\" : [ \"vnfProducts\", \"vnfProducts\" ],    \"usageState\" : { },    \"versions\" : [ \"versions\", \"versions\" ],    \"vnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfProductsFromProvider\" : [ \"vnfProductsFromProvider\", \"vnfProductsFromProvider\" ],    \"notificationTypes\" : { },    \"vnfpkgId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"operationalState\" : { },    \"vnfprovider\" : \"vnfprovider\",    \"vfnProductName\" : \"vfnProductName\",    \"vnfSoftwareVersion\" : \"vnfSoftwareVersion\",    \"vnfdVersions\" : [ \"vnfdVersions\", \"vnfdVersions\" ]  },  \"_links\" : \"http://example.com/aeiou\",  \"callbackUri\" : \"http://example.com/aeiou\",  \"self\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}", PkgmSubscription.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<PkgmSubscription>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<PkgmSubscription>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<VnfPkgInfo> createVNFPkgInfo(@ApiParam(value = "" ,required=true )  @Valid @RequestBody CreateVnfPkgInfoRequest body) {

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            log.debug("Processing REST request to create a VNF Pkg info");
            try {
                VnfPkgInfo vnfPkgInfo = vnfPackageManagementInterface.createVnfPkgInfo(body);
                return new ResponseEntity<VnfPkgInfo>(vnfPkgInfo, HttpStatus.CREATED);
            } catch (MalformattedElementException e) {
                return new ResponseEntity<VnfPkgInfo>(HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                log.error("Exception while creating VNF Pkg info: " + e.getMessage());
                return new ResponseEntity<VnfPkgInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else
            return new ResponseEntity<VnfPkgInfo>(HttpStatus.BAD_REQUEST);

        /*String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<VnfPkgInfo>(objectMapper.readValue("{  \"vnfProductName\" : \"vnfProductName\",  \"vnfd\" : \"http://example.com/aeiou\",  \"vnfdVersion\" : \"vnfdVersion\",  \"vnfProvider\" : \"vnfProvider\",  \"_links\" : \"http://example.com/aeiou\",  \"vnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"packageContent\" : \"http://example.com/aeiou\",  \"additionalArtifacts\" : [ {    \"checksum\" : \"checksum\",    \"artifactPath\" : \"artifactPath\"  }, {    \"checksum\" : \"checksum\",    \"artifactPath\" : \"artifactPath\"  } ],  \"usageState\" : { },  \"checksum\" : \"checksum\",  \"softwareImages\" : [ {    \"imagePath\" : \"imagePath\",    \"version\" : \"version\",    \"minDisk\" : 0,    \"createdAt\" : \"createdAt\",    \"size\" : 1,    \"provider\" : \"provider\",    \"minRam\" : 6,    \"name\" : \"name\",    \"checksum\" : \"checksum\",    \"containerFormat\" : { },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"diskFormat\" : { },    \"userMetadata\" : { }  }, {    \"imagePath\" : \"imagePath\",    \"version\" : \"version\",    \"minDisk\" : 0,    \"createdAt\" : \"createdAt\",    \"size\" : 1,    \"provider\" : \"provider\",    \"minRam\" : 6,    \"name\" : \"name\",    \"checksum\" : \"checksum\",    \"containerFormat\" : { },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"diskFormat\" : { },    \"userMetadata\" : { }  } ],  \"self\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"operationalState\" : { },  \"onboardingState\" : { },  \"vnfSoftwareVersion\" : \"vnfSoftwareVersion\"}", VnfPkgInfo.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<VnfPkgInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<VnfPkgInfo>(HttpStatus.NOT_IMPLEMENTED);*/
    }

    public ResponseEntity<Void> deleteSubscription(@ApiParam(value = "",required=true) @PathVariable("subscriptionId") String subscriptionId) {
        String accept = request.getHeader("Accept");
        return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Void> deleteVNFPkgInfo(@ApiParam(value = "",required=true) @PathVariable("vnfPkgId") String vnfPkgId) {
        String accept = request.getHeader("Accept");
        return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<PkgmSubscription> getSubscription(@ApiParam(value = "",required=true) @PathVariable("subscriptionId") String subscriptionId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<PkgmSubscription>(objectMapper.readValue("{  \"filter\" : {    \"vnfProducts\" : [ \"vnfProducts\", \"vnfProducts\" ],    \"usageState\" : { },    \"versions\" : [ \"versions\", \"versions\" ],    \"vnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfProductsFromProvider\" : [ \"vnfProductsFromProvider\", \"vnfProductsFromProvider\" ],    \"notificationTypes\" : { },    \"vnfpkgId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"operationalState\" : { },    \"vnfprovider\" : \"vnfprovider\",    \"vfnProductName\" : \"vfnProductName\",    \"vnfSoftwareVersion\" : \"vnfSoftwareVersion\",    \"vnfdVersions\" : [ \"vnfdVersions\", \"vnfdVersions\" ]  },  \"_links\" : \"http://example.com/aeiou\",  \"callbackUri\" : \"http://example.com/aeiou\",  \"self\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}", PkgmSubscription.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<PkgmSubscription>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<PkgmSubscription>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<List<PkgmSubscription>> getSubscriptions() {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<PkgmSubscription>>(objectMapper.readValue("[ {  \"filter\" : {    \"vnfProducts\" : [ \"vnfProducts\", \"vnfProducts\" ],    \"usageState\" : { },    \"versions\" : [ \"versions\", \"versions\" ],    \"vnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfProductsFromProvider\" : [ \"vnfProductsFromProvider\", \"vnfProductsFromProvider\" ],    \"notificationTypes\" : { },    \"vnfpkgId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"operationalState\" : { },    \"vnfprovider\" : \"vnfprovider\",    \"vfnProductName\" : \"vfnProductName\",    \"vnfSoftwareVersion\" : \"vnfSoftwareVersion\",    \"vnfdVersions\" : [ \"vnfdVersions\", \"vnfdVersions\" ]  },  \"_links\" : \"http://example.com/aeiou\",  \"callbackUri\" : \"http://example.com/aeiou\",  \"self\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}, {  \"filter\" : {    \"vnfProducts\" : [ \"vnfProducts\", \"vnfProducts\" ],    \"usageState\" : { },    \"versions\" : [ \"versions\", \"versions\" ],    \"vnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"vnfProductsFromProvider\" : [ \"vnfProductsFromProvider\", \"vnfProductsFromProvider\" ],    \"notificationTypes\" : { },    \"vnfpkgId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"operationalState\" : { },    \"vnfprovider\" : \"vnfprovider\",    \"vfnProductName\" : \"vfnProductName\",    \"vnfSoftwareVersion\" : \"vnfSoftwareVersion\",    \"vnfdVersions\" : [ \"vnfdVersions\", \"vnfdVersions\" ]  },  \"_links\" : \"http://example.com/aeiou\",  \"callbackUri\" : \"http://example.com/aeiou\",  \"self\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"} ]", List.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<PkgmSubscription>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<PkgmSubscription>>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Object> getVNFD(@ApiParam(value = "",required=true) @PathVariable("vnfPkgId") String vnfPkgId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<Object>(objectMapper.readValue("\"{}\"", Object.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Object> getVNFPkg(@ApiParam(value = "",required=true) @PathVariable("vnfPkgId") String vnfPkgId,@ApiParam(value = "" ) @RequestHeader(value="Range", required=false) String range) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<Object>(objectMapper.readValue("\"{}\"", Object.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<List<VnfPkgInfo>> getVNFPkgsInfo() {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<VnfPkgInfo>>(objectMapper.readValue("[ {  \"vnfProductName\" : \"vnfProductName\",  \"vnfd\" : \"http://example.com/aeiou\",  \"vnfdVersion\" : \"vnfdVersion\",  \"vnfProvider\" : \"vnfProvider\",  \"_links\" : \"http://example.com/aeiou\",  \"vnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"packageContent\" : \"http://example.com/aeiou\",  \"additionalArtifacts\" : [ {    \"checksum\" : \"checksum\",    \"artifactPath\" : \"artifactPath\"  }, {    \"checksum\" : \"checksum\",    \"artifactPath\" : \"artifactPath\"  } ],  \"usageState\" : { },  \"checksum\" : \"checksum\",  \"softwareImages\" : [ {    \"imagePath\" : \"imagePath\",    \"version\" : \"version\",    \"minDisk\" : 0,    \"createdAt\" : \"createdAt\",    \"size\" : 1,    \"provider\" : \"provider\",    \"minRam\" : 6,    \"name\" : \"name\",    \"checksum\" : \"checksum\",    \"containerFormat\" : { },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"diskFormat\" : { },    \"userMetadata\" : { }  }, {    \"imagePath\" : \"imagePath\",    \"version\" : \"version\",    \"minDisk\" : 0,    \"createdAt\" : \"createdAt\",    \"size\" : 1,    \"provider\" : \"provider\",    \"minRam\" : 6,    \"name\" : \"name\",    \"checksum\" : \"checksum\",    \"containerFormat\" : { },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"diskFormat\" : { },    \"userMetadata\" : { }  } ],  \"self\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"operationalState\" : { },  \"onboardingState\" : { },  \"vnfSoftwareVersion\" : \"vnfSoftwareVersion\"}, {  \"vnfProductName\" : \"vnfProductName\",  \"vnfd\" : \"http://example.com/aeiou\",  \"vnfdVersion\" : \"vnfdVersion\",  \"vnfProvider\" : \"vnfProvider\",  \"_links\" : \"http://example.com/aeiou\",  \"vnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"packageContent\" : \"http://example.com/aeiou\",  \"additionalArtifacts\" : [ {    \"checksum\" : \"checksum\",    \"artifactPath\" : \"artifactPath\"  }, {    \"checksum\" : \"checksum\",    \"artifactPath\" : \"artifactPath\"  } ],  \"usageState\" : { },  \"checksum\" : \"checksum\",  \"softwareImages\" : [ {    \"imagePath\" : \"imagePath\",    \"version\" : \"version\",    \"minDisk\" : 0,    \"createdAt\" : \"createdAt\",    \"size\" : 1,    \"provider\" : \"provider\",    \"minRam\" : 6,    \"name\" : \"name\",    \"checksum\" : \"checksum\",    \"containerFormat\" : { },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"diskFormat\" : { },    \"userMetadata\" : { }  }, {    \"imagePath\" : \"imagePath\",    \"version\" : \"version\",    \"minDisk\" : 0,    \"createdAt\" : \"createdAt\",    \"size\" : 1,    \"provider\" : \"provider\",    \"minRam\" : 6,    \"name\" : \"name\",    \"checksum\" : \"checksum\",    \"containerFormat\" : { },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"diskFormat\" : { },    \"userMetadata\" : { }  } ],  \"self\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"operationalState\" : { },  \"onboardingState\" : { },  \"vnfSoftwareVersion\" : \"vnfSoftwareVersion\"} ]", List.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<VnfPkgInfo>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<VnfPkgInfo>>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Object> queryVNFPkgArtifact(@ApiParam(value = "",required=true) @PathVariable("vnfPkgId") String vnfPkgId,@ApiParam(value = "",required=true) @PathVariable("artifactPath") String artifactPath,@ApiParam(value = "" ) @RequestHeader(value="Range", required=false) String range) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("")) {
            try {
                return new ResponseEntity<Object>(objectMapper.readValue("", Object.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type ", e);
                return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<VnfPkgInfo> queryVNFPkgInfo(@ApiParam(value = "",required=true) @PathVariable("vnfPkgId") String vnfPkgId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<VnfPkgInfo>(objectMapper.readValue("{  \"vnfProductName\" : \"vnfProductName\",  \"vnfd\" : \"http://example.com/aeiou\",  \"vnfdVersion\" : \"vnfdVersion\",  \"vnfProvider\" : \"vnfProvider\",  \"_links\" : \"http://example.com/aeiou\",  \"vnfdId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"packageContent\" : \"http://example.com/aeiou\",  \"additionalArtifacts\" : [ {    \"checksum\" : \"checksum\",    \"artifactPath\" : \"artifactPath\"  }, {    \"checksum\" : \"checksum\",    \"artifactPath\" : \"artifactPath\"  } ],  \"usageState\" : { },  \"checksum\" : \"checksum\",  \"softwareImages\" : [ {    \"imagePath\" : \"imagePath\",    \"version\" : \"version\",    \"minDisk\" : 0,    \"createdAt\" : \"createdAt\",    \"size\" : 1,    \"provider\" : \"provider\",    \"minRam\" : 6,    \"name\" : \"name\",    \"checksum\" : \"checksum\",    \"containerFormat\" : { },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"diskFormat\" : { },    \"userMetadata\" : { }  }, {    \"imagePath\" : \"imagePath\",    \"version\" : \"version\",    \"minDisk\" : 0,    \"createdAt\" : \"createdAt\",    \"size\" : 1,    \"provider\" : \"provider\",    \"minRam\" : 6,    \"name\" : \"name\",    \"checksum\" : \"checksum\",    \"containerFormat\" : { },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"diskFormat\" : { },    \"userMetadata\" : { }  } ],  \"self\" : \"http://example.com/aeiou\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"operationalState\" : { },  \"onboardingState\" : { },  \"vnfSoftwareVersion\" : \"vnfSoftwareVersion\"}", VnfPkgInfo.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<VnfPkgInfo>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<VnfPkgInfo>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<VnfPkgInfoModifications> updateVNFPkgInfo(@ApiParam(value = "",required=true) @PathVariable("vnfPkgId") String vnfPkgId,@ApiParam(value = "" ,required=true )  @Valid @RequestBody VnfPkgInfoModifications body) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<VnfPkgInfoModifications>(objectMapper.readValue("{  \"operationalState\" : { },  \"userDefinedData\" : { }}", VnfPkgInfoModifications.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<VnfPkgInfoModifications>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<VnfPkgInfoModifications>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Void> uploadVNFPkg(@ApiParam(value = "",required=true) @PathVariable("vnfPkgId") String vnfPkgId,@ApiParam(value = "" ,required=true )  @Valid @RequestBody Object body,@ApiParam(value = "The payload body contains a VNF Package ZIP file. The request shall set the \"Content-Type\" HTTP header as defined above." ) @RequestHeader(value="Content-Type", required=false) String contentType) {
        String accept = request.getHeader("Accept");
        return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Void> uploadVNFPkgFromURI(@ApiParam(value = "",required=true) @PathVariable("vnfPkgId") String vnfPkgId,@ApiParam(value = "" ,required=true )  @Valid @RequestBody UploadVnfPackageFromUriRequest body) {
        String accept = request.getHeader("Accept");
        return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
    }

}
