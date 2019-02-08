package it.nextworks.nfvmano.libs.osmr4Client.vnfpackageManagement;

import it.nextworks.nfvmano.libs.osmr4Client.utilities.OSMHttpResponse;

import java.io.File;

public interface VNFDManagementInterface {

    OSMHttpResponse createVnfPackage();
    OSMHttpResponse uploadVnfPackageContent(String vnfPkgId, File content);
    OSMHttpResponse getVnfd(String vnfPkgId);
    OSMHttpResponse getVnfPackageInfo(String vnfPkgId);
    OSMHttpResponse deleteVnfPackage(String vnfPkgId);
    OSMHttpResponse getVnfPackageContent(String vnfPkgId);
    OSMHttpResponse getVnfPackageList();
}
