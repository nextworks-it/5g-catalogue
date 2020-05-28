package it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin;

import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.OnapNsDescriptor;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utilities {

    public static File getNsDescriptorFile(File nsPackage) throws FailedOperationException {
        File definitionsFolder = getDefinitionsFolder(nsPackage);
        FilenameFilter fileTemplateFilter = (f, name) -> name.startsWith("service-") && name.endsWith("-template.yml");
        String[] files = definitionsFolder.list(fileTemplateFilter);
        if(files == null || files.length != 1)
            throw new FailedOperationException("Cannot obtain the NS Descriptor file");
        return new File(definitionsFolder, files[0]);
    }

    public static List<File> getVnfDescriptorFiles(File nsPackage, OnapNsDescriptor nsDescriptor) throws FailedOperationException{
        List<File> vnfDescriptorFiles = new ArrayList<>();
        File definitionsFolder = getDefinitionsFolder(nsPackage);
        //Map <nodeName, vfIdentifier>
        Map<String, String> nodeVfIdentifierMapping = nsDescriptor.getVFIdentifiers();
        Set<String> vfIdentifiersSet = new HashSet<>(nodeVfIdentifierMapping.values());
        for(String vfIdentifier : vfIdentifiersSet)
            vnfDescriptorFiles.add(new File(definitionsFolder, "resource-" + vfIdentifier + "-template.yml"));
        return vnfDescriptorFiles;
    }

    public static File getDefinitionsFolder(File nsPackage) throws FailedOperationException{
        File serviceFolder = new File(nsPackage, "service");
        FilenameFilter csarDirectoryFilter = (f, name) -> f.isDirectory() && name.endsWith("-csar");
        String[] files = serviceFolder.list(csarDirectoryFilter);//get only the directory name
        if(files == null || files.length != 1)
            throw new FailedOperationException("Cannot obtain the Definitions folder");
        return new File(serviceFolder, files[0] + "/Definitions");
    }

    public static boolean isTargetMano (List<String> siteOrManoIds, MANO mano){
        if(siteOrManoIds.contains(mano.getManoId()) || siteOrManoIds.contains(mano.getManoSite()))
            return true;
        return false;
    }

    public static String unzip(File zipfile, File directory) throws IOException {
        String rootDir = null;
        ZipFile zfile = new ZipFile(zipfile);
        Enumeration<? extends ZipEntry> entries = zfile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            rootDir = entry.getName().split("/")[0];//all the files are within a root directory
            File file = new File(directory, entry.getName());
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                InputStream in = zfile.getInputStream(entry);
                try {
                    copy(in, file);
                } finally {
                    in.close();
                }
            }
        }
        return rootDir;
    }

    private static void copy(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
        } finally {
            out.close();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }
}
