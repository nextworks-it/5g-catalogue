package it.nextworks.nfvmano.libs.osmr4Client.nsdManagement;

import it.nextworks.nfvmano.libs.osmr4Client.utilities.HttpResponse;

import java.io.File;

public interface NSDManagementInterface {

    HttpResponse createNsd();
    HttpResponse uploadNsdContent(String nsdId, File content);
    HttpResponse getNsd(String vnfdId);
    HttpResponse getNsdInfo(String nsdId);
    HttpResponse deleteNsd(String nsdId);
    HttpResponse getNsdContent(String nsdId);
    HttpResponse getNsdList();
}
