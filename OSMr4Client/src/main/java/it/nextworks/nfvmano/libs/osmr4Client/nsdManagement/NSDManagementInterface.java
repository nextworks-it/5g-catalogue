package it.nextworks.nfvmano.libs.osmr4Client.nsdManagement;

import it.nextworks.nfvmano.libs.osmr4Client.utilities.HttpResponse;

import java.io.File;

public interface NSDManagementInterface {

    HttpResponse createNsd();
    HttpResponse uploadNsdContent(String nsdInfoId, File content);
    HttpResponse getNsd(String nsdInfoId);
    HttpResponse getNsdInfo(String nsdInfoId);
    HttpResponse deleteNsd(String nsdInfoId);
    HttpResponse getNsdContent(String nsdInfoId);
    HttpResponse getNsdInfoList();
}
