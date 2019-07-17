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
/**
 * NOTE: This class is auto generated by the swagger code generator program (2.4.0-SNAPSHOT).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement;

import io.swagger.annotations.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.ProblemDetails;
import it.nextworks.nfvmano.catalogue.nbi.sol005.vnfpackagemanagement.elements.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-10-05T11:50:31.473+02:00")

@Api(value = "vnfpkgm", description = "the vnfpkgm API")
public interface VnfpkgmApi {

    @ApiOperation(value = "Create VNF Package Info", nickname = "createVNFPkgInfo", notes = "", response = VnfPkgInfo.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Status 201", response = VnfPkgInfo.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.POST)
    ResponseEntity<?> createVNFPkgInfo(@RequestParam(required = false) String project,
                                       @ApiParam(value = "", required = true) @Valid @RequestBody CreateVnfPkgInfoRequest body,
                                       @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Query VNF Packages Info", nickname = "getVNFPkgsInfo", notes = "", response = VnfPkgInfo.class, responseContainer = "List", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = VnfPkgInfo.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<?> getVNFPkgsInfo(@RequestParam(required = false) String project,
                                     @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Query VNF Package Info", nickname = "queryVNFPkgInfo", notes = "", response = VnfPkgInfo.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = VnfPkgInfo.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages/{vnfPkgId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<?> queryVNFPkgInfo(@RequestParam(required = false) String project,
                                      @ApiParam(value = "", required = true) @PathVariable("vnfPkgId") String vnfPkgId,
                                      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Update a VNF Package Info", nickname = "updateVNFPkgInfo", notes = "", response = VnfPkgInfoModifications.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = VnfPkgInfoModifications.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 412, message = "Status 412", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages/{vnfPkgId}",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.PATCH)
    ResponseEntity<?> updateVNFPkgInfo(@RequestParam(required = false) String project,
                                       @ApiParam(value = "", required = true) @PathVariable("vnfPkgId") String vnfPkgId,
                                       @ApiParam(value = "", required = true) @Valid @RequestBody VnfPkgInfoModifications body,
                                       @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Delete a VNF Package", nickname = "deleteVNFPkgInfo", notes = "", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Status 204"),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages/{vnfPkgId}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    ResponseEntity<?> deleteVNFPkgInfo(@RequestParam(required = false) String project,
                                       @ApiParam(value = "", required = true) @PathVariable("vnfPkgId") String vnfPkgId,
                                       @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Get VNF Desriptor in a VNF Package.", nickname = "getVNFD", notes = "", response = Object.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = Object.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 406, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages/{vnfPkgId}/vnfd",
            produces = {"application/json", "application/yaml", "text/plain", "application/zip"},
            method = RequestMethod.GET)
    ResponseEntity<?> getVNFD(@RequestParam(required = false) String project,
                              @ApiParam(value = "", required = true) @PathVariable("vnfPkgId") String vnfPkgId,
                              @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Get VNF Package content.", nickname = "getVNFPkg", notes = "", response = Object.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = Object.class),
            @ApiResponse(code = 206, message = "Status 206", response = Object.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 416, message = "Status 416", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages/{vnfPkgId}/package_content",
            produces = {"multipart/form-data", "application/zip", "application/json"},
            method = RequestMethod.GET)
    ResponseEntity<?> getVNFPkg(@RequestParam(required = false) String project,
                                @ApiParam(value = "", required = true) @PathVariable("vnfPkgId") String vnfPkgId,
                                @ApiParam(value = "") @RequestHeader(value = "Range", required = false) String range,
                                @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Upload VNF Package content.", nickname = "uploadVNFPkg", notes = "", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Status 202"),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages/{vnfPkgId}/package_content",
            produces = {"application/json"},
            consumes = {"application/zip", "multipart/form-data"},
            method = RequestMethod.PUT)
    ResponseEntity<?> uploadVNFPkg(@RequestParam(required = false) String project,
                                   @ApiParam(value = "", required = true) @PathVariable("vnfPkgId") String vnfPkgId,
                                   @ApiParam(value = "", required = true) @RequestParam("file") MultipartFile body,
                                   @ApiParam(value = "The payload body contains a VNF Package ZIP file. The request shall set the \"Content-Type\" HTTP header as defined above.") @RequestHeader(value = "Content-Type", required = false) String contentType,
                                   @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Upload VNF Package content from URI.", nickname = "uploadVNFPkgFromURI", notes = "", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200"),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages/{vnfPkgId}/package_content/upload_from_uri",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.POST)
    ResponseEntity<?> uploadVNFPkgFromURI(@RequestParam(required = false) String project,
                                             @ApiParam(value = "", required = true) @PathVariable("vnfPkgId") String vnfPkgId,
                                             @ApiParam(value = "", required = true) @Valid @RequestBody UploadVnfPackageFromUriRequest body,
                                             @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Query VNF Package artifact.", nickname = "queryVNFPkgArtifact", notes = "", response = Object.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = Object.class),
            @ApiResponse(code = 206, message = "Status 206", response = Object.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 416, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/vnf_packages/{vnfPkgId}/artifacts/{artifactPath}",
            produces = {"application/octet-stream"},
            method = RequestMethod.GET)
    ResponseEntity<?> queryVNFPkgArtifact(@RequestParam(required = false) String project,
                                          @ApiParam(value = "", required = true) @PathVariable("vnfPkgId") String vnfPkgId,
                                          @ApiParam(value = "", required = true) @PathVariable("artifactPath") String artifactPath,
                                          @ApiParam(value = "") @RequestHeader(value = "Range", required = false) String range,
                                          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Create Subscription Information", nickname = "createSubscription", notes = "", response = PkgmSubscription.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Status 201", response = PkgmSubscription.class),
            @ApiResponse(code = 303, message = "Status 303"),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/subscriptions",
            produces = {"application/json"},
            method = RequestMethod.POST)
    ResponseEntity<?> createSubscription(@ApiParam(value = "", required = true) @Valid @RequestBody PkgmSubscriptionRequest body,
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Query Subscriptions Information", nickname = "getSubscriptions", notes = "", response = PkgmSubscription.class, responseContainer = "List", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = PkgmSubscription.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/subscriptions",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<?> getSubscriptions(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Query Subscription Information", nickname = "getSubscription", notes = "", response = PkgmSubscription.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = PkgmSubscription.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/subscriptions/{subscriptionId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<?> getSubscription(@ApiParam(value = "", required = true) @PathVariable("subscriptionId") String subscriptionId,
                                      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);

    @ApiOperation(value = "Delete Subscription Information", nickname = "deleteSubscription", notes = "", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Status 204"),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/vnfpkgm/v1/subscriptions/{subscriptionId}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    ResponseEntity<?> deleteSubscription(@ApiParam(value = "", required = true) @PathVariable("subscriptionId") String subscriptionId,
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization);
}
