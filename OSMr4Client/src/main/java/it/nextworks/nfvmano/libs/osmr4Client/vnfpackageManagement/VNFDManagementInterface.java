package it.nextworks.nfvmano.libs.osmr4Client.vnfpackageManagement;

import it.nextworks.nfvmano.libs.osmr4Client.utilities.HttpResponse;

import java.io.File;

public interface VNFDManagementInterface {

    HttpResponse createVnfPackage();
    HttpResponse uploadVnfPackageContent(String vnfdId, File content);
    HttpResponse getVnfd(String vnfdId);
    HttpResponse getVnfPackageInfo(String vnfdId);
    HttpResponse deleteVnfPackage(String vnfdId);
    HttpResponse getVnfPackageContent(String vnfdId);
    HttpResponse getVnfPackageList();

}
