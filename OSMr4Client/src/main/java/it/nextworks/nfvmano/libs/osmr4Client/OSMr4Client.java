package it.nextworks.nfvmano.libs.osmr4Client;


import com.shc.easyjson.JSON;
import com.shc.easyjson.JSONObject;
import com.shc.easyjson.JSONValue;
import com.shc.easyjson.ParseException;
import it.nextworks.nfvmano.libs.osmr4Client.nsdManagement.NSDManagementInterface;
import it.nextworks.nfvmano.libs.osmr4Client.nsdManagement.elements.CreateNsdInfoRequest;
import it.nextworks.nfvmano.libs.osmr4Client.osmManagement.OSMManagementInterface;
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

    private String      osmIPAddress, user, password, project;
    private JSONObject  validToken;

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

        HttpConnection.disableSslVerification();

        this.osmIPAddress = osmIPAddress;
        this.user = user;
        this.password = password;
        this.project = project;

        HttpResponse response = getAuthenticationToken();
        try {
            validToken = JSON.parse(response.getContent());
        }catch(ParseException e){
            e.printStackTrace();
        }
	    if(response.getCode() == HTTP_UNAUTHORIZED)
            throw new OSMException(validToken.get("detail").getValue());
    }

    /**
     * Obtains OSMr4 server address
     * @return IP Address where OSMr4 is running
     */
    protected String getOSMIPAddress() {
        return this.osmIPAddress;
    }

    /**
     * Obtains OSMr4 user
     * @return User
     */
    protected String getUser() {
        return this.user;
    }

    /**
     * Obtains OSMr4 password
     * @return Password
     */
    protected String getPassword() {
        return this.password;
    }

    /**
     * Obtains OSMr4 project
     * @return Project
     */
    protected String getProject() {
        return this.project;
    }

    /* OSMManagementInterface */

    /**
     * Verifies token validity
     */
    private void verifyToken(){

        Double tokenExpireTime = new Double (validToken.get("expires").getValue().toString());
        if(tokenExpireTime.longValue()*1000 < System.currentTimeMillis()) {
            HttpResponse response = getAuthenticationToken();
            try {
                validToken = JSON.parse(response.getContent());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Obtains Authentication Token
     * @return HttpResponse containing a valid token
     */
    @Override
    public HttpResponse getAuthenticationToken(){

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/json"));
        headers.add(new BasicHeader("Accept", "application/json"));

        JSONObject body = new JSONObject();
        body.put("username", new JSONValue(user));
        body.put("password", new JSONValue(password));
        body.put("project_id", new JSONValue(project));

        String url = "https://" + osmIPAddress + ":9999/osm" + osmManagement + "/tokens";
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.GET, headers, body);
    }

    /* NSDManagementInterface */

    /**
     * Create a new NS descriptor resource
     * @return HttpResponse containing the NS descriptor resource ID
     */
    @Override
    public HttpResponse createNsd(){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/json"));
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        CreateNsdInfoRequest body = new CreateNsdInfoRequest();

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors";
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.POST, headers, body);
    }

    /**
     * Upload the content of a NSD
     * @param nsdInfoId NS descriptor resource ID
     * @param content Zip file with the content
     * @return HttpResponse containing the result of the request
     */
    @Override
    public HttpResponse uploadNsdContent(String nsdInfoId, File content){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/zip"));
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId + "/nsd_content";
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.PUT, headers, content);
    }

    /**
     * Obtains the NS descriptor resource
     * @param nsdInfoId NS descriptor resource ID
     * @return HttpResponse containing the NS descriptor
     */
    @Override
    public HttpResponse getNsd(String nsdInfoId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "text/plain"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId + "/nsd";
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.GET, headers, null);
    }

    /**
     * Delete an individual NS descriptor resource
     * @param nsdInfoId NS descriptor resource ID
     * @return HttpResponse containing the result of the request
     */
    @Override
    public HttpResponse deleteNsd(String nsdInfoId) {

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId;
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.DELETE, headers, null);
    }

    /**
     * Fetch the content of a NSD
     * @param nsdInfoId NS descriptor resource ID
     * @return HttpResponse containing the local path of the downloaded zip file
     */
    @Override
    public HttpResponse getNsdContent(String nsdInfoId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/zip"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId + "/nsd_content";

        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.GET, headers, null);
    }

    /**
     * Read information about an individual NS descriptor resource
     * @param nsdInfoId NS descriptor resource ID
     * @return HttpResponse containing the information
     */
    @Override
    public HttpResponse getNsdInfo(String nsdInfoId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors/" + nsdInfoId;
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.GET, headers, null);
    }

    /**
     * Query information about multiple NS descriptor resources
     * @return HttpResponse containing the information
     */
    @Override
    public HttpResponse getNsdInfoList(){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + nsdManagement + "/ns_descriptors";
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.GET, headers, null);
    }

    /* VNFDManagementInterface */

    /**
     * Create a new VNF package resource
     * @return HttpResponse containing the VNF package resource ID
     */
    @Override
    public HttpResponse createVnfPackage(){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/json"));
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        CreateVnfPkgInfoRequest body = new CreateVnfPkgInfoRequest();

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages";
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.POST, headers, body);
    }

    /**
     * Upload the content of a VNF package
     * @param vnfPkgId VNF package resource ID
     * @param content Zip file with the content
     * @return HttpResponse containing the result of the request
     */
    @Override
    public HttpResponse uploadVnfPackageContent(String vnfPkgId, File content){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/zip"));
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId + "/package_content";
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.PUT, headers, content);
    }

    /**
     * Obtains the VNF descriptor resource
     * @param vnfPkgId NS descriptor resource ID
     * @return HttpResponse containing the VNF descriptor
     */
    @Override
    public HttpResponse getVnfd(String vnfPkgId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "text/plain"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId + "/vnfd";
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.GET, headers, null);
    }

    /**
     * Delete an individual VNF package resource
     * @param vnfPkgId VNF package resource ID
     * @return HttpResponse containing the result of the request
     */
    @Override
    public HttpResponse deleteVnfPackage(String vnfPkgId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId;
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.DELETE, headers, null);
    }

    /**
     * Fetch an on-boarded VNF package
     * @param vnfPkgId VNF package resource ID
     * @return HttpResponse containing the local path of the downloaded zip file
     */
    @Override
    public HttpResponse getVnfPackageContent(String vnfPkgId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/zip"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId + "/package_content";

        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.GET, headers, null);
    }

    /**
     * Read information about an individual VNF package resource
     * @param vnfPkgId VNF package resource ID
     * @return HttpResponse containing the information
     */
    @Override
    public HttpResponse getVnfPackageInfo(String vnfPkgId){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages/" + vnfPkgId;
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.GET, headers, null);
    }

    /**
     * Query information about multiple VNF package resources
     * @return HttpResponse containing the information
     */
    @Override
    public HttpResponse getVnfPackageList(){

        verifyToken();

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + validToken.get("id").getValue().toString()));

        String url = "https://" + osmIPAddress + ":9999/osm" + vnfdManagement + "/vnf_packages";
        return HttpConnection.establishHTTPConnection(url, HttpConnection.HttpMethod.GET, headers, null);
    }
}

