package it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.common;

import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANO;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    public static String getMainServiceTemplateFromMetadata(File metadata) throws IOException, MalformattedElementException {

        BufferedReader reader = new BufferedReader(new FileReader(metadata));

        String mst_name = null;

        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                } else {
                    String regex = "^Entry-Definitions: (Definitions\\/[^\\\\]*\\.yaml)$";
                    if (line.matches(regex)) {
                        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            mst_name = matcher.group(1);
                        }
                    }
                }
            }
        } finally {
            reader.close();
        }

        return mst_name;
    }


    public static String getCloudInitFromManifest(File mf) throws IOException, MalformattedElementException {

        String cloudInitFilename = null;

        BufferedReader br = new BufferedReader(new FileReader(mf));
        try {
            String line;
            String regexRoot = "cloud_init:";
            String regex = "^Source: (Files\\/Scripts\\/[\\s\\S]*)$";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.matches(regexRoot)) {
                    line = br.readLine();

                    if (line != null) {
                        line = line.trim();
                        if (line.matches(regex)) {
                            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                            Matcher matcher = pattern.matcher(line.trim());
                            if (matcher.find()) {
                                cloudInitFilename = matcher.group(1);
                            }
                        }
                    }
                }
            }
        } finally {
            br.close();
        }

        return cloudInitFilename;
    }

    public static String getMonitoringFromManifest(File mf) throws IOException{
        String monitoringParametersFilename = null;
        BufferedReader br = new BufferedReader(new FileReader(mf));
        try {
            String line;
            String regexRoot = "main_monitoring_descriptor:";
            String regex = "^Source: (Files\\/Monitoring\\/[\\s\\S]*)$";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.matches(regexRoot)) {
                    line = br.readLine();

                    if (line != null) {
                        line = line.trim();
                        if (line.matches(regex)) {
                            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                            Matcher matcher = pattern.matcher(line.trim());
                            if (matcher.find()) {
                                monitoringParametersFilename = matcher.group(1);
                            }
                        }
                    }
                }
            }
        } finally {
            br.close();
        }

        return monitoringParametersFilename;
    }

    public static List<String> getScriptsFromManifest(File mf) throws IOException{
        List<String> scriptsFilename = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(mf));
        try {
            String line;
            String regexRoot = "scripts:";
            String regex = "^Source: (Files\\/Scripts\\/[\\s\\S]*)$";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.matches(regexRoot)) {
                    while((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.matches(regex)) {
                            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                            Matcher matcher = pattern.matcher(line.trim());
                            if (matcher.find()) {
                                scriptsFilename.add(matcher.group(1));
                            }
                        }
                    }
                }
            }
        } finally {
            br.close();
        }

        return scriptsFilename;
    }

    public static Set<String> listFiles(String dir) throws IOException {
        Set<String> fileList = new HashSet<>();
        Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (!Files.isDirectory(file)) {
                    fileList.add(file.getFileName().toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return fileList;
    }

    public static boolean isTargetMano (List<String> siteOrManoIds, MANO mano){
        if(siteOrManoIds == null || siteOrManoIds.contains(mano.getManoId()) || siteOrManoIds.contains(mano.getManoSite()))
            return true;
        return false;
    }
}
