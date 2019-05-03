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
package it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement;

import io.swagger.annotations.*;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-07-23T16:31:35.952+02:00")

@Api(value = "nsd", description = "the nsd API")
public interface NsdApi {

    @ApiOperation(value = "Create NSD Info", nickname = "createNsdInfo", notes = "The POST method is used to create a new NS descriptor resource. This method shall follow the provisions specified in the Tables 5.4.2.3.1-1 and 5.4.2.3.1-2 of GS NFV-SOL 005 for URI query parameters, request and response data structures, and response codes.", response = NsdInfo.class, tags = {})
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Status 201", response = NsdInfo.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/ns_descriptors", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> createNsdInfo(
            @ApiParam(value = "", required = true) @Valid @RequestBody CreateNsdInfoRequest body);

    @ApiOperation(value = "Query NSDs Info", nickname = "getNSDsInfo", notes = "The GET method queries information about multiple NS descriptor resources. This method shall follow the provisions specified in the Tables 5.4.2.3.2-1 and 5.4.2.3.2-2 of GS NFV-SOL 005 for URI query parameters, request and response data structures, and response codes.", response = NsdInfo.class, responseContainer = "List", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Information about zero or more NS descriptors. The response body shall contain a representation of zero or more NS descriptors, as defined in clause 5.5.2.2 of GS NFV-SOL 005.", response = NsdInfo.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "There are two possible scenarios listed below. Error: Invalid attribute-based filtering parameters. The response body shall contain a ProblemDetails structure, in which the \"detail\" attribute should convey more information about the error. Error: Invalid attribute selector. The response body shall contain a ProblemDetails structure, in which the \"detail\" attribute should convey more information about the error.", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/ns_descriptors", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getNSDsInfo();

    @ApiOperation(value = "Query NSD Info", nickname = "getNSDInfo", notes = "The GET method reads information about an individual NS descriptor. This method shall follow the provisions specified in GS NFV-SOL 005 Tables 5.4.3.3.2-1 and 5.4.3.3.2-2 of GS NFV-SOL 005 for URI query parameters, request and response data structures, and response codes.", response = NsdInfo.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Information about the individual NS descriptor. The response body shall contain a representation of the individual NS descriptor, as defined in clause 5.5.2.2 of GS NFV-SOL 005.", response = NsdInfo.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/ns_descriptors/{nsdInfoId}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getNSDInfo(
            @ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId);

    @ApiOperation(value = "Update NSD Info", nickname = "updateNSDInfo", notes = "The PATCH method modifies the operational state and/or user defined data of an individual NS descriptor resource.  This method can be used to: 1) Enable a previously disabled individual NS descriptor resource, allowing again its use for instantiation of new network service with this descriptor. The usage state (i.e. \"IN_USE/NOT_IN_USE\") shall not change as a result. 2) Disable a previously enabled individual NS descriptor resource, preventing any further use for instantiation of new network service(s) with this descriptor. The usage state (i.e. \"IN_USE/NOT_IN_USE\") shall not change as a result. 3) Modify the user defined data of an individual NS descriptor resource. This method shall follow the provisions specified in the Tables 5.4.3.3.4-1 and 5.4.3.3.4-2 for URI query parameters, request and response data structures, and response codes.", response = NsdInfoModifications.class, tags = {})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Status 200", response = NsdInfoModifications.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 412, message = "Status 412", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/ns_descriptors/{nsdInfoId}", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.PATCH)
    ResponseEntity<?> updateNSDInfo(
            @ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId,
            @ApiParam(value = "", required = true) @Valid @RequestBody NsdInfoModifications body);

    @ApiOperation(value = "Delete NSD", nickname = "deleteNSDInfo", notes = "The DELETE method deletes an individual NS descriptor resource. An individual NS descriptor resource can only be deleted when there is no NS instance using it (i.e. usageState = NOT_IN_USE) and has been disabled already (i.e. operationalState = DISABLED). Otherwise, the DELETE method shall fail. This method shall follow the provisions specified in the Tables 5.4.3.3.5-1 and 5.4.3.3.5-2 of GS NFV-SOL 005 for URI query parameters, request and response data structures, and response codes.", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The operation has completed successfully. The response body shall be empty."),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/ns_descriptors/{nsdInfoId}", produces = {"application/json",
            "application/yaml"}, method = RequestMethod.DELETE)
    ResponseEntity<?> deleteNSDInfo(
            @ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId);

    @ApiOperation(value = "Get NSD Content", nickname = "getNSD", notes = "The GET method fetches the content of the NSD. The NSD can be implemented as a single file or as a collection of multiple files. If the NSD is implemented in the form of multiple files, a ZIP file embedding these files shall be returned. If the NSD is implemented as a single file, either that file or a ZIP file embedding that file shall be returned. The selection of the format is controlled by the \"Accept\" HTTP header passed in the GET request: • If the \"Accept\" header contains only \"text/plain\" and the NSD is implemented as a single file, the file shall be returned; otherwise, an error message shall be returned. • If the \"Accept\" header contains only \"application/zip\", the single file or the multiple files that make up the NSD shall be returned embedded in a ZIP file. • If the \"Accept\" header contains both \"text/plain\" and \"application/zip\", it is up to the NFVO to choose the format to return for a single-file NSD; for a multi-file NSD, a ZIP file shall be returned. NOTE: The structure of the NSD zip file is outside the scope of the present document.", response = Object.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "On success, the content of the NSD is returned. The payload body shall contain a copy of the file representing the NSD or a ZIP file that contains the file or multiple files representing the NSD, as specified above. The \"Content-Type\" HTTP header shall be set according to the format of the returned file, i.e. to \"text/plain\" for a YAML file or to \"application/zip\" for a ZIP file.", response = Object.class),
            @ApiResponse(code = 206, message = "On success, if the NFVO supports range requests, a single consecutive byte range from the content of the NSD file is returned.  The response body shall contain the requested part of the NSD file.  The \"Content-Range\" HTTP header shall be provided according to IETF RFC 7233 [23].  The \"Content-Type\" HTTP header shall be set as defined above for the \"200 OK\" response."),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 406, message = "Status 406", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 416, message = "Status 416", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/ns_descriptors/{nsdInfoId}/nsd_content", produces = {"application/json",
            "application/yaml", "application/zip"}, method = RequestMethod.GET)
    ResponseEntity<?> getNSD(@ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId,
                             @ApiParam(value = "The request may contain a \"Range\" HTTP header to obtain single range of bytes from the NSD file. This can be used to continue an aborted transmission.  If the NFVO does not support range requests, the NFVO shall ignore the 'Range\" header, process the GET request, and return the whole NSD file with a 200 OK response (rather than returning a 4xx error status code).") @RequestHeader(value = "Range", required = false) String range);

    @ApiOperation(value = "Upload NSD", nickname = "uploadNSD", notes = "The PUT method is used to upload the content of a NSD. The NSD to be uploaded can be implemented as a single file or as a collection of multiple files, as defined in clause 5.4.4.3.2 of GS NFV-SOL 005. If the NSD is implemented in the form of multiple files, a ZIP file embedding these files shall be uploaded. If the NSD is implemented as a single file, either that file or a ZIP file embedding that file shall be uploaded. The \"Content-Type\" HTTP header in the PUT request shall be set accordingly based on the format selection of the NSD. If the NSD to be uploaded is a text file, the \"Content-Type\" header is set to \"text/plain\". If the NSD to be uploaded is a zip file, the \"Content-Type\" header is set to \"application/zip\". This method shall follow the provisions specified in the Tables 5.4.4.3.3-1 and 5.4.4.3.3-2 of GS-NFV-SOL 005 for URI query parameters, request and response data structures, and response codes.", response = Object.class, tags = {})
    @ApiResponses(value = {@ApiResponse(code = 202, message = "Status 202", response = Object.class),
            @ApiResponse(code = 204, message = "The NSD content was successfully uploaded and validated (synchronous mode). The response body shall be empty."),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Error: The operation cannot be executed currently, due to a conflict with the state of the resource. Typically, this is due to the fact that the NsdOnboardingState has a value other than CREATED. The response body shall contain a ProblemDetails structure, in which the \"detail\" attribute shall convey more information about the error.", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/ns_descriptors/{nsdInfoId}/nsd_content", produces = {"application/json"}, consumes = {"application/json", "application/x-yaml",
            "application/zip", "multipart/form-data"}, method = RequestMethod.PUT)
    ResponseEntity<?> uploadNSD(@ApiParam(value = "", required = true) @PathVariable("nsdInfoId") String nsdInfoId,
                                @ApiParam(value = "", required = true) @RequestParam("file") MultipartFile body,
                                @ApiParam(value = "The payload body contains a copy of the file representing the NSD or a ZIP file that contains the file or multiple files representing the NSD, as specified above. The request shall set the \"Content-Type\" HTTP header as defined above.") @RequestHeader(value = "Content-Type", required = false) String contentType);

    @ApiOperation(value = "Create PNFD Info", nickname = "createPNFDInfo", notes = "The POST method is used to create a new PNF descriptor resource.", response = PnfdInfo.class, tags = {})
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Status 201", response = PnfdInfo.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/pnf_descriptors", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> createPNFDInfo(@ApiParam(value = "", required = true) @Valid @RequestBody CreatePnfdInfoRequest body);

    @ApiOperation(value = "Query PFNDs Info", nickname = "getPNFDsInfo", notes = "The GET method queries information about multiple PNF descriptor resources.", response = PnfdInfo.class, responseContainer = "List", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = PnfdInfo.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/pnf_descriptors", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getPNFDsInfo(
            @ApiParam(value = "Indicates to exclude the following complex attributes from the response. See clause 4.3.3 for details. The NFVO shall support this parameter. The following attributes shall be excluded from the PnfdInfo structure in the response body if this parameter is provided, or none of the parameters \"all_fields,\" \"fields\", \"exclude_fields\", \"exclude_default\" are provided: userDefinedData.") @Valid @RequestParam(value = "exclude_default", required = false) String excludeDefault,
            @ApiParam(value = "Include all complex attributes in the response. See clause 4.3.3 for details. The NFVO shall support this parameter.") @Valid @RequestParam(value = "all_fields", required = false) String allFields);

    @ApiOperation(value = "Query PNFD Info", nickname = "getPNFDInfo", notes = "The GET method reads information about an individual PNF descriptor. This method shall follow the provisions specified in the Tables 5.4.6.3.2-1 and 5.4.6.3.2-2 of GS NFV-SOL 005 for URI query parameters, request and response data structures, and response codes.", response = PnfdInfo.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Information about the individual PNFD descriptor. The response body shall contain a representation of the individual PNF descriptor, as defined in clause 5.5.2.5 of GS NFV-SOL 005.", response = PnfdInfo.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/pnf_descriptors/{pnfdInfoId}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getPNFDInfo(
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId);

    @ApiOperation(value = "Update PNFD Info", nickname = "updatePNFDInfo", notes = "The PATCH method modifies the user defined data of an individual PNF descriptor resource. This method shall follow the provisions specified in the Tables 5.4.6.3.4-1 and 5.4.6.3.4-2 for URI query parameters, request and response data structures, and response codes.", response = PnfdInfoModifications.class, tags = {})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Status 200", response = PnfdInfoModifications.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 412, message = "Status 412", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/pnf_descriptors/{pnfdInfoId}", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.PATCH)
    ResponseEntity<?> updatePNFDInfo(
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId,
            @ApiParam(value = "", required = true) @Valid @RequestBody PnfdInfoModifications body);

    @ApiOperation(value = "Delete PNFD", nickname = "deletePNFDInfo", notes = "The DELETE method deletes an individual PNF descriptor resource. An individual PNF descriptor resource can only be deleted when there is no NS instance using it or there is NSD referencing it. To delete all PNFD versions identified by a particular value of the \"pnfdInvariantId\" attribute, the procedure is to first use the GET method with filter \"pnfdInvariantId\" towards the PNF descriptors resource to find all versions of the PNFD. Then, the client uses the DELETE method described in this clause to delete each PNFD version individually. This method shall follow the provisions specified in the Tables 5.4.6.3.5-1 and 5.4.6.3.5-2 of GS NFV-SOL 005 for URI query parameters, request and response data structures, and response codes.", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The operation has completed successfully. The response body shall be empty."),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/pnf_descriptors/{pnfdInfoId}", produces = {"application/json"}, method = RequestMethod.DELETE)
    ResponseEntity<?> deletePNFDInfo(
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId);

    //produces = {"application/json","application/yaml", "application/zip"}
    @ApiOperation(value = "Get PNFD Content", nickname = "getPNFD", notes = "The GET method fetches the content of the PNFD.", response = Object.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "On success, the content of the PNFD is returned. The payload body shall contain a copy of the file representing the PNFD. The \"Content-Type\" HTTP header shall be set to \"text/plain\".", response = Object.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/pnf_descriptors/{pnfdInfoId}/pnfd_content", produces = {"text/plain", "application/yaml", "application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getPNFD(
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId);

    @ApiOperation(value = "Upload PNFD", nickname = "uploadPNFD", notes = "The PUT method is used to upload the content of a PNFD. This method shall follow the provisions specified in the Tables 5.4.7.3.3-1 and 5.4.7.3.3-2 of GS NFV-SOL 005for URI query parameters, request and response data structures, and response codes.", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The PNFD content was successfully uploaded and validated. The response body shall be empty."),
            @ApiResponse(code = 400, message = "", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 409, message = "Status 409", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/pnf_descriptors/{pnfdInfoId}/pnfd_content", produces = {"application/json"}, consumes = {"application/json", "application/yaml", "application/zip", "multipart/form-data"}, method = RequestMethod.PUT)
    ResponseEntity<?> uploadPNFD(
            @ApiParam(value = "", required = true) @PathVariable("pnfdInfoId") String pnfdInfoId,
            @ApiParam(value = "", required = true) @RequestParam("file") MultipartFile body,
            @ApiParam(value = "The request shall set the \"Content-Type\" HTTP header to \"text/plain\".") @RequestHeader(value = "Content-Type", required = false) String contentType);


    @ApiOperation(value = "Subscribe", nickname = "createSubscription", notes = "The POST method creates a new subscription. This method shall support the URI query parameters, request and response data structures, and response codes, as specified in the Tables 5.4.8.3.1-1 and 5.4.8.3.1-2 of GS-NFV SOL 005. Creation of two subscription resources with the same callbackURI and the same filter can result in performance degradation and will provide duplicates of notifications to the OSS, and might make sense only in very rare use cases. Consequently, the NFVO may either allow creating a subscription resource if another subscription resource with the same filter and callbackUri already exists (in which case it shall return the \"201 Created\" response code), or may decide to not create a duplicate subscription resource (in which case it shall return a \"303 See Other\" response code referencing the existing subscription resource with the same filter and callbackUri).", response = NsdmSubscription.class, tags = {})
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Status 201", response = NsdmSubscription.class),
            @ApiResponse(code = 303, message = "A subscription with the same callbackURI and the same filter already exits and the policy of the NFVO is to not create redundant subscriptions. The response body shall be empty.", response = ProblemDetails.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/subscriptions", produces = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> createSubscription(
            @ApiParam(value = "", required = true) @Valid @RequestBody NsdmSubscriptionRequest body);

    @ApiOperation(value = "Query Subscriptions Information", nickname = "getSubscriptions", notes = "The GET method queries the list of active subscriptions of the functional block that invokes the method. It can be used e.g. for resynchronization after error situations. This method shall support the URI query parameters, request and response data structures, and response codes, as specified in the Tables 5.4.8.3.2-1 and 5.4.8.3.2-2 of GS NFV-SOL 005.", response = NsdmSubscription.class, responseContainer = "List", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status 200", response = NsdmSubscription.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/subscriptions", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getSubscriptions();

    @ApiOperation(value = "Query Subscription Information", nickname = "getSubscription", notes = "The GET method retrieves information about a subscription by reading an individual subscription resource. This method shall support the URI query parameters, request and response data structures, and response codes, as specified in the Tables 5.4.9.3.2-1 and 5.4.9.3.2-2.", response = NsdmSubscription.class, tags = {})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Status 200", response = NsdmSubscription.class),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "Status 500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/subscriptions/{subscriptionId}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getSubscription(
            @ApiParam(value = "", required = true) @PathVariable("subscriptionId") String subscriptionId);

    @ApiOperation(value = "Terminate Subscription", nickname = "deleteSubscription", notes = "The DELETE method terminates an individual subscription. This method shall support the URI query parameters, request and response data structures, and response codes, as specified in the Tables 5.4.9.3.5-1 and 5.4.9.3.3-2 of GS NFV-SOL 005.", tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The subscription resource was deleted successfully. The response body shall be empty."),
            @ApiResponse(code = 400, message = "Status 400", response = ProblemDetails.class),
            @ApiResponse(code = 404, message = "Status 404", response = ProblemDetails.class),
            @ApiResponse(code = 500, message = "500", response = ProblemDetails.class)})
    @RequestMapping(value = "/nsd/v1/subscriptions/{subscriptionId}", produces = {"application/json"}, method = RequestMethod.DELETE)
    ResponseEntity<?> deleteSubscription(
            @ApiParam(value = "", required = true) @PathVariable("subscriptionId") String subscriptionId);
}
