package it.nextworks.nfvmano.libs.osmr4Client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.shc.easyjson.*;
import it.nextworks.nfvmano.libs.osmr4Client.utilities.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestClass {

    public static void main (String [] args) throws InterruptedException, ParseException, JsonParseException, JsonMappingException, IOException {

        OSMr4Client client = new OSMr4Client("10.0.8.31", "admin","admin", "admin");

        HttpResponse response;
        JSONObject content;
        File file;
        String nsID = "e5500dcc-ee7a-4ac4-a9fe-8e617ab08f9c";
        String vnfID1 = "1f95b486-eaa6-44cf-b93f-57c704d9e712";
        String vnfID2 = "d65172f5-4035-4d4a-ae55-722cbbf54acf";

        /*

        //Onboard NSD e VNFD

        response = client.createVnfPackage();
        content = JSON.parse(response.getContent());
        vnfID1 = content.get("id").getValue().toString();
        System.out.println(response.toString());

        file = new File("/home/leonardo/Scaricati/vPlate-v3_vnfd.tar.gz");
        response = client.uploadVnfPackageContent(vnfID1, file);
        System.out.println(response.toString());

        response = client.createVnfPackage();
        content = JSON.parse(response.getContent());
        vnfID2 = content.get("id").getValue().toString();
        System.out.println(response.toString());

        file = new File("/home/leonardo/Scaricati/vFirewall-v3_vnfd.tar.gz");
        response = client.uploadVnfPackageContent(vnfID2, file);
        System.out.println(response.toString());

        response = client.createNsd();
        content = JSON.parse(response.getContent());
        nsID = content.get("id").getValue().toString();
        System.out.println(response.toString());

        file = new File("/home/leonardo/Scaricati/5GCITY-REVIEW-NS1_nsd.tar.gz");
        response = client.uploadNsdContent(nsID, file);
        System.out.println(response.toString());

        */

        /*

        // Get NSD e VNFD

        response = client.getNsd(nsID);
        System.out.println(response.toString());

        response = client.getVnfd(vnfID1);
        System.out.println(response.toString());

        response = client.getVnfd(vnfID2);
        System.out.println(response.toString());

        */

        /*

        // Get NSD e VNFD Info

        response = client.getNsdInfo(nsID);
        System.out.println(response.toString());
        content = JSON.parse(response.getContent());
        System.out.println(content.toString());


        response = client.getVnfPackageInfo(vnfID1);
        System.out.println(response.toString());

        response = client.getVnfPackageInfo(vnfID2);
        System.out.println(response.toString());

        */

        /*

        //Delete VNFD e NSD

        response = client.deleteNsd(nsID);
        System.out.println(response.toString());

        response = client.deleteVnfPackage(vnfID1);
        System.out.println(response.toString());

        response = client.deletePackage(vnfID2);
        System.out.println(response.toString());

        */

        /*

        //Get NSD e VNFD Content

        response = client.getNsdContent(nsID);
        if(response.getCode() == 200)
            Files.move(response.getFilePath(), Paths.get("/home/leonardo/Scrivania/" + nsID + ".tar.gz"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println(response.toString());

        response = client.getVnfPackageContent(vnfID1);
        if(response.getCode() == 200)
            Files.move(response.getFilePath(), Paths.get("/home/leonardo/Scrivania/" + vnfID1 + ".tar.gz"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println(response.toString());

        response = client.getVnfPackageContent(vnfID2);
        if(response.getCode() == 200)
            Files.move(response.getFilePath(), Paths.get("/home/leonardo/Scrivania/" + vnfID2 + ".tar.gz"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println(response.toString());

        */

        /*

        // Get NSDs List e VNFDs List Info

        response = client.getNsdList();
        System.out.println(response.toString());

        response = client.getVnfPackageList();
        //System.out.println(response.getContent().substring(1, response.getContent().indexOf(",    {")).trim());
        System.out.println(response.toString());

        */
    }
}
