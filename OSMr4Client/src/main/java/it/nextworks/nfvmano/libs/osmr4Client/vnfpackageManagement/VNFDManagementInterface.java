package it.nextworks.nfvmano.libs.osmr4Client.vnfpackageManagement;

import it.nextworks.nfvmano.libs.osmr4Client.utilities.HttpResponse;

import java.io.File;

public interface VNFDManagementInterface {

    HttpResponse createVnfPackage();
    HttpResponse uploadVnfPackageContent(String vnfPkgId, File content);
    HttpResponse getVnfd(String vnfPkgId);
    HttpResponse getVnfPackageInfo(String vnfPkgId);
    HttpResponse deleteVnfPackage(String vnfPkgId);
    HttpResponse getVnfPackageContent(String vnfPkgId);
    HttpResponse getVnfPackageList();

}
