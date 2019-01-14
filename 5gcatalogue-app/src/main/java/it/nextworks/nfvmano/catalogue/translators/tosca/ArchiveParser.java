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

import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ArchiveParser {

    private static final Logger log = LoggerFactory.getLogger(ArchiveParser.class);

    private static ByteArrayOutputStream metadata;
    private static Map<String, ByteArrayOutputStream> templates = new HashMap<>();
    private static ByteArrayOutputStream mainServiceTemplate;
    private static List<String> folderNames = new ArrayList<>();
    private static Set<String> admittedFolders = new HashSet<>();

    public ArchiveParser() {
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

    public static byte[] getMainDescriptorFromArchive(InputStream archive) throws IOException {
        folderNames.clear();
        templates.clear();
        mainServiceTemplate = null;
        metadata = null;

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

                if (fileName.toLowerCase().equalsIgnoreCase("tosca.meta")) {
                    metadata = outStream;
                } else if (fileName.toLowerCase().endsWith(".yaml")) {
                    templates.put(fileName, outStream);
                }
            }
        }

        try {
            setMainServiceTemplateFromMetadata(metadata.toByteArray());
            byte[] mst = mainServiceTemplate.toByteArray();
            return mst;
        } catch (MalformattedElementException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @PostConstruct
    void init() {
        admittedFolders.add("Definitions/");
        admittedFolders.add("Files/");
        admittedFolders.add("Files/Tests/");
        admittedFolders.add("Files/Licences/");
        admittedFolders.add("Scripts/");
    }

    private void parseArchive(InputStream archive) throws IOException, MalformattedElementException {
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

                if (fileName.toLowerCase().equalsIgnoreCase("tosca.meta")) {
                    this.metadata = outStream;
                } else if (fileName.toLowerCase().endsWith(".yaml")) {
                    this.templates.put(fileName, outStream);
                } else {
                    // TODO: process remaining content
                }
            } else {
                log.debug("Parsing Archive: adding folder with name " + entry.getName() + " to folders list");
                folderNames.add(entry.getName());
            }
        }

        if (this.metadata == null) {
            log.error("CSAR without TOSCA.meta");
            throw new MalformattedElementException("CSAR without TOSCA.meta");
        } else {
            try {
                setMainServiceTemplateFromMetadata(this.metadata.toByteArray());
            } catch (MalformattedElementException e) {
                log.error(e.getMessage());
            }
        }
        if (this.mainServiceTemplate == null) {
            log.error("CSAR without main service template");
            throw new MalformattedElementException("CSAR without main service template");
        }

        for (String fName : this.folderNames) {
            if (!this.admittedFolders.contains(fName)) {
                log.error("Folder with name " + fName + " not admitted in CSAR option#1 structure");
                throw new MalformattedElementException("Folder with name " + fName + " not admitted in CSAR option#1 structure");
            }
        }
    }

    public DescriptorTemplate archiveToMainDescriptor(MultipartFile file)
            throws IOException, MalformattedElementException {

        DescriptorTemplate dt = null;

        if (!file.isEmpty()) {
            this.folderNames.clear();
            this.templates.clear();
            this.mainServiceTemplate = null;
            this.metadata = null;

            byte[] bytes = file.getBytes();

            InputStream input = new ByteArrayInputStream(bytes);

            log.debug("Going to parse archive " + file.getName() + "...");
            parseArchive(input);

            if (this.mainServiceTemplate != null) {
                log.debug("Going to parse main service template...");
                String mst_content = this.mainServiceTemplate.toString("UTF-8");
                dt = DescriptorsParser.stringToDescriptorTemplate(mst_content);
            }

            this.mainServiceTemplate.close();
            this.metadata.close();
            input.close();
        }

        return dt;
    }
}
