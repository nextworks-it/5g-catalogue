/*
 * Copyright 2018 Nextworks s.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.nextworks.nfvmano.catalogue.translators.tosca;

import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.translators.tosca.elements.CSARInfo;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ArchiveParser {

    private static final Logger log = LoggerFactory.getLogger(ArchiveParser.class);

    private static ByteArrayOutputStream metadata;
    private static ByteArrayOutputStream manifest;
    private static Map<String, ByteArrayOutputStream> templates = new HashMap<>();
    private static ByteArrayOutputStream mainServiceTemplate;
    private static Set<String> admittedFolders = new HashSet<>();
    private static CSARInfo csarInfo;

    @Autowired
    FileSystemStorageService storageService;

    public ArchiveParser() {
    }

    private static void parseArchive(InputStream archive) throws IOException, MalformattedElementException {
        ZipEntry entry;

        ZipInputStream zipStream = new ZipInputStream(archive);
        while ((entry = zipStream.getNextEntry()) != null) {

            if (!entry.isDirectory()) {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();

                int count;
                byte[] buffer = new byte[1024];
                while ((count = zipStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, count);
                }

                String fileName = entry.getName();
                log.debug("Parsing Archive: found file with name " + fileName);

                if (fileName.toLowerCase().endsWith(".mf")) {
                    manifest = outStream;
                    csarInfo.setMfFilename(fileName);
                } else if (fileName.toLowerCase().endsWith(".meta")) {
                    metadata = outStream;
                    csarInfo.setMetaFilename(fileName);
                } else if (fileName.toLowerCase().endsWith(".yaml")) {
                    templates.put(fileName, outStream);
                } else {
                    // TODO: process remaining content
                }
            } else {
                log.debug("Parsing Archive: checking folder with name " + entry.getName());
                if (!admittedFolders.contains(entry.getName())) {
                    log.error("Folder with name " + entry.getName() + " not admitted in CSAR option#1 structure");
                    throw new MalformattedElementException("Folder with name " + entry.getName() + " not admitted in CSAR option#1 structure");
                }
            }
        }

        if (metadata == null) {
            log.error("CSAR without TOSCA.meta");
            throw new MalformattedElementException("CSAR without TOSCA.meta");
        }
        if (manifest == null) {
            log.error("CSAR without manifest");
            throw new MalformattedElementException("CSAR without manifest");
        } else {
            try {
                setMainServiceTemplateFromMetadata(metadata.toByteArray());
            } catch (MalformattedElementException e) {
                log.error(e.getMessage());
            }
        }

        if (mainServiceTemplate == null) {
            log.error("CSAR without main service template");
            throw new MalformattedElementException("CSAR without main service template");
        }
    }

    public static CSARInfo archiveToMainDescriptor(MultipartFile file)
            throws IOException, MalformattedElementException, FailedOperationException {

        DescriptorTemplate dt = null;

        if (!file.isEmpty()) {
            templates.clear();
            mainServiceTemplate = null;
            metadata = null;
            manifest = null;
            csarInfo = new CSARInfo();

            byte[] bytes = file.getBytes();

            InputStream input = new ByteArrayInputStream(bytes);

            log.debug("Going to parse archive " + file.getName() + "...");
            parseArchive(input);

            if (mainServiceTemplate != null) {
                log.debug("Going to parse main service template...");
                String mst_content = mainServiceTemplate.toString("UTF-8");
                dt = DescriptorsParser.stringToDescriptorTemplate(mst_content);
                csarInfo.setMst(dt);
                log.debug("Main service template with vnfdId {} and verions {} successfully parsed", dt.getMetadata().getDescriptorId(), dt.getMetadata().getVersion());
            }

            try {
                String vnfPkg_filename = FileSystemStorageService.storeVnfPkg(dt.getMetadata().getDescriptorId(), dt.getMetadata().getVersion(), file);
                csarInfo.setVnfPkgFilename(vnfPkg_filename);
                log.debug("Stored VNF Pkg: " + vnfPkg_filename);
            } catch (FailedOperationException e) {
                log.error("Failure while storing VNF Pkg with vnfdId " + dt.getMetadata().getDescriptorId() + ": " + e.getMessage());
                throw new FailedOperationException("Failure while storing VNF Pkg with vnfdId " + dt.getMetadata().getDescriptorId() + ": " + e.getMessage());
            }

            try {
                unzip(new ByteArrayInputStream(bytes), dt);
            } catch (FailedOperationException e) {
                log.error("Failure while unzipping VNF Pkg with vnfdId " + dt.getMetadata().getDescriptorId() + ": " + e.getMessage());
                throw new FailedOperationException("Failure while unzipping VNF Pkg with vnfdId " + dt.getMetadata().getDescriptorId() + ": " + e.getMessage());
            }

            mainServiceTemplate.close();
            metadata.close();
            manifest.close();
            for (Map.Entry<String, ByteArrayOutputStream> entry : templates.entrySet()) {
                entry.getValue().close();
            }
            input.close();
        }

        return csarInfo;
    }

    private static void setMainServiceTemplateFromMetadata(byte[] metadata) throws IOException, MalformattedElementException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(metadata), "UTF-8"));

        log.debug("Going to parse TOSCA.meta...");

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
                            String mst_name = matcher.group(1);
                            csarInfo.setVnfdFilename(mst_name);
                            log.debug("Parsing metadata: found Main Service Template " + mst_name);
                            if (templates.containsKey(mst_name)) {
                                mainServiceTemplate = templates.get(mst_name);
                            } else {
                                log.error("Main Service Template specified in TOSCA.meta not present in CSAR Definitions directory: " + mst_name);
                                throw new MalformattedElementException(
                                        "Main Service Template specified in TOSCA.meta not present in CSAR Definitions directory: " + mst_name);
                            }
                        }
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    public static void unzip(InputStream archive, DescriptorTemplate dt) throws IOException, FailedOperationException, MalformattedElementException {
        ZipInputStream zis = new ZipInputStream(archive);
        ZipEntry zipEntry = zis.getNextEntry();
        String element_filename;
        while (zipEntry != null) {
            log.debug("Storing CSAR element: " + zipEntry.getName());
            element_filename = FileSystemStorageService.storeVnfPkgElement(zis, zipEntry, dt.getMetadata().getDescriptorId(), dt.getMetadata().getVersion());
            log.debug("Stored VNF Pkg element: " + element_filename);
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    @PostConstruct
    void init() {
        admittedFolders.add("TOSCA-Metadata/");
        admittedFolders.add("Definitions/");
        admittedFolders.add("Files/");
        admittedFolders.add("Files/Tests/");
        admittedFolders.add("Files/Licenses/");
        admittedFolders.add("Files/Scripts/");
        admittedFolders.add("Files/Monitoring/");
    }
}
