package it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OnapDriver {

    private String ipAddress, port;

    public OnapDriver(String ipAddress, String port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public List<File> getNsPackages(){
        List<File> nsPackages = new ArrayList<>();
        /* example
        obj = (Resource) vnfApi.getVNFPkg(vnfPkgId, project, range, authorization);
        InputStream inputStream = obj.getInputStream();
        targetFile = new File(String.format("%s%s.zip", storagePath, vnfPkgId));
        java.nio.file.Files.copy(
                inputStream,
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        ArchiveParser.unzip(targetFile, new File(storagePath, vnfPkgId));
         */
        return nsPackages;
    }
}
