package it.nextworks.nfvmano.libs.osmr4Client;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextworks.nfvmano.libs.osmr4Client.nsdManagement.NSDManagementInterface;
import it.nextworks.nfvmano.libs.osmr4Client.nsdManagement.elements.CreateNsdInfoRequest;
import it.nextworks.nfvmano.libs.osmr4Client.osmManagement.OSMManagementInterface;
import it.nextworks.nfvmano.libs.osmr4Client.osmManagement.elements.Token;
import it.nextworks.nfvmano.libs.osmr4Client.osmManagement.elements.TokenRequest;
import it.nextworks.nfvmano.libs.osmr4Client.utilities.*;
import it.nextworks.nfvmano.libs.osmr4Client.vnfpackageManagement.VNFDManagementInterface;
import it.nextworks.nfvmano.libs.osmr4Client.vnfpackageManagement.elements.CreateVnfPkgInfoRequest;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class OSMr4Client implements OSMManagementInterface, NSDManagementInterface, VNFDManagementInterface {

    private String  osmIPAddress, user, password, project;
    private Token   validToken;

    final private String osmManagement = "/admin/v1";
    final private String nsdManagement = "/nsd/v1";
    final private String vnfdManagement = "/vnfpkgm/v1";

    /**
     * OSMr4Client constructor
     * @param osmIPAddress IP Address where OSMr4 is running
     * @param user OSM user
     * @param password OSM password
     * @param project OSM project (optional). If it is not specified, project admin will be used
     */
    public OSMr4Client(String osmIPAddress, String user, String password, String project) {

        OSMHttpConnection.disableSslVerification();

        this.osmIPAddress = osmIPAddress;
        this.user = user;
        this.password = password;
        this.project = project;

        OSMHttpResponse response = getAuthenticationToken();
        if (response.getCode() == HTTP_UNAUTHORIZED)
                throw new RuntimeException(response.getContent());
        ObjectMapper mapper = new ObjectMapper();
        try {
            validToken = mapper.readValue(response.getContent(), Token.class);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Obtains OSMr4 server address
     * @return IP Address where OSMr4 is running
     */
    protected String getOSMIPAddress() { return this.osmIPAddress; }

    /**
     * Obtains OSMr4 user
     * @return User
     */
    protected String getUser() { return this.user; }

    /**
     * Obtains OSMr4 password
     * @return Password
     */
    protected String getPassword() { return this.password; }

    /**
     * Obtains OSMr4 project
     * @return Project
     */
    protected String getProject() { return this.project; }

    /* OSMManagementInterface */

    /**
     * Verifies token validity
     */
    private void verifyToken(){

        Double tokenExpireTime = new Double (validToken.getExpires());
        if(tokenExpireTime.longValue()*1000 < System.currentTimeMillis()) {
            OSMHttpResponse response = getAuthenticationToken();
            ObjectMapper mapper = new ObjectMapper();
            try {
                validToken = mapper.readValue(response.getContent(), Token.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Obtains Authentication Token
     * @return OSMHttpResponse containing a valid token
     */
    @Override
    public OSMHttpResponse getAuthenticationToken(){

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/json"));
        headers.add(new BasicHeader("Accept", "application/json"));

        TokenRequest tokenReq = new TokenRequest(user, password, project);

        String url = "https://" + osmIPAddress + ":9999/osm" + osmManagement + "/tokens";
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.GET, headers, tokenReq);
    }

    /* NSDManagementInterface */

    /**
     * Create a new NS descriptor resource
     * @return OSMHttpResponse containing the NS descriptor resource ID
     */
    @Override
    public OSMHttpResponse createNsd(){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/json"));
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        CreateNsdInfoRequest body = new CreateNsdInfoRequest();

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors";
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.POST, headers, body);
    }

    /**
     * Upload the content of a NSD
     * @param nsdInfoId NS descriptor resource ID
     * @param content Zip file with the content
     * @return OSMHttpResponse containing the result of the request
     */
    @Override
    public OSMHttpResponse uploadNsdContent(String nsdInfoId, File content){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/zip"));
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId + "/nsd_content";
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.PUT, headers, content);
    }

    /**
     * Obtains the NS descriptor resource
     * @param nsdInfoId NS descriptor resource ID
     * @return OSMHttpResponse containing the NS descriptor
     */
    @Override
    public OSMHttpResponse getNsd(String nsdInfoId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "text/plain"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId + "/nsd";
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.GET, headers, null);
    }

    /**
     * Delete an individual NS descriptor resource
     * @param nsdInfoId NS descriptor resource ID
     * @return OSMHttpResponse containing the result of the request
     */
    @Override
    public OSMHttpResponse deleteNsd(String nsdInfoId) {

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId;
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.DELETE, headers, null);
    }

    /**
     * Fetch the content of a NSD
     * @param nsdInfoId NS descriptor resource ID
     * @return OSMHttpResponse containing the local path of the downloaded zip file
     */
    @Override
    public OSMHttpResponse getNsdContent(String nsdInfoId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/zip"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId + "/nsd_content";

        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.GET, headers, null);
    }

    /**
     * Read information about an individual NS descriptor resource
     * @param nsdInfoId NS descriptor resource ID
     * @return OSMHttpResponse containing the information
     */
    @Override
    public OSMHttpResponse getNsdInfo(String nsdInfoId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId;
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.GET, headers, null);
    }

    /**
     * Query information about multiple NS descriptor resources
     * @return OSMHttpResponse containing the information
     */
    @Override
    public OSMHttpResponse getNsdInfoList(){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors";
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.GET, headers, null);
    }

    /* VNFDManagementInterface */

    /**
     * Create a new VNF package resource
     * @return OSMHttpResponse containing the VNF package resource ID
     */
    @Override
    public OSMHttpResponse createVnfPackage(){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/json"));
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        CreateVnfPkgInfoRequest body = new CreateVnfPkgInfoRequest();

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages";
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.POST, headers, body);
    }

    /**
     * Upload the content of a VNF package
     * @param vnfPkgId VNF package resource ID
     * @param content Zip file with the content
     * @return OSMHttpResponse containing the result of the request
     */
    @Override
    public OSMHttpResponse uploadVnfPackageContent(String vnfPkgId, File content){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/zip"));
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId + "/package_content";
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.PUT, headers, content);
    }

    /**
     * Obtains the VNF descriptor resource
     * @param vnfPkgId NS descriptor resource ID
     * @return OSMHttpResponse containing the VNF descriptor
     */
    @Override
    public OSMHttpResponse getVnfd(String vnfPkgId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "text/plain"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId + "/vnfd";
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.GET, headers, null);
    }

    /**
     * Delete an individual VNF package resource
     * @param vnfPkgId VNF package resource ID
     * @return OSMHttpResponse containing the result of the request
     */
    @Override
    public OSMHttpResponse deleteVnfPackage(String vnfPkgId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId;
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.DELETE, headers, null);
    }

    /**
     * Fetch an on-boarded VNF package
     * @param vnfPkgId VNF package resource ID
     * @return OSMHttpResponse containing the local path of the downloaded zip file
     */
    @Override
    public OSMHttpResponse getVnfPackageContent(String vnfPkgId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/zip"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId + "/package_content";

        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.GET, headers, null);
    }

    /**
     * Read information about an individual VNF package resource
     * @param vnfPkgId VNF package resource ID
     * @return OSMHttpResponse containing the information
     */
    @Override
    public OSMHttpResponse getVnfPackageInfo(String vnfPkgId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId;
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.GET, headers, null);
    }

    /**
     * Query information about multiple VNF package resources
     * @return OSMHttpResponse containing the information
     */
    @Override
    public OSMHttpResponse getVnfPackageList(){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.getId()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages";
        return OSMHttpConnection.establishOSMHttpConnection(url, OSMHttpConnection.OSMHttpMethod.GET, headers, null);
    }
}

