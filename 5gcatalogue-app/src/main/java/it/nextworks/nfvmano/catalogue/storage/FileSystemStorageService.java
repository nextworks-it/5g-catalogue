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
package it.nextworks.nfvmano.catalogue.storage;

import it.nextworks.nfvmano.catalogue.common.ConfigurationParameters;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileSystemStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileSystemStorageService.class);
    private static Path nsdsLocation;
    private static Path vnfPkgsLocation;

    @Value("${catalogue.storageRootDir}")
    private String rootDir;

    public FileSystemStorageService() {
    }

    public static String storePkg(String descriptorId, String version, MultipartFile file, boolean isVnfPkg) throws MalformattedElementException, FailedOperationException {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new MalformattedElementException("Failed to store empty file " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new MalformattedElementException(
                        "Cannot store file with relative path outside current directory "
                                + filename);
            }

            Path locationRoot;
            Path locationVersion;
            if (isVnfPkg) {
                locationRoot = Paths.get(vnfPkgsLocation + "/" + descriptorId);
                locationVersion = Paths.get(vnfPkgsLocation + "/" + descriptorId + "/" + version);
            } else {
                locationRoot = Paths.get(nsdsLocation + "/" + descriptorId);
                locationVersion = Paths.get(nsdsLocation + "/" + descriptorId + "/" + version);
            }
            if (!Files.isDirectory(locationRoot, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isDirectory(locationVersion, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(locationVersion);
                }
            } else {
                if (!Files.isDirectory(locationVersion, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(locationVersion);
                }
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, locationVersion.resolve(filename),
                        StandardCopyOption.REPLACE_EXISTING);
                return filename;
            }
        } catch (IOException e) {
            throw new FailedOperationException("Failed to store file " + filename, e);
        }
    }

    public static String storePkgElement(ZipInputStream zis, ZipEntry element, String descriptorId, String version, boolean isVnfPkg) throws FailedOperationException {
        log.debug("Received request for storing element in Pkg with descriptor Id {} and version {}", descriptorId, version);

        String filename = StringUtils.cleanPath(element.getName());
        log.debug("Storing file with name: " + filename);

        byte[] buffer = new byte[1024];

        Path locationVersion;
        if (isVnfPkg)
            locationVersion = Paths.get(vnfPkgsLocation + "/" + descriptorId + "/" + version);
        else
            locationVersion = Paths.get(nsdsLocation + "/" + descriptorId + "/" + version);

        if (filename.endsWith("/")) {
            log.debug("Zip entry is a directory: " + filename);
            Path newDir;
            if (isVnfPkg)
                newDir = Paths.get(vnfPkgsLocation + "/" + descriptorId + "/" + version + "/" + filename);
            else {
                newDir = Paths.get(nsdsLocation + "/" + descriptorId + "/" + version + "/" + filename);
            }
            if (!Files.isDirectory(newDir, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.createDirectories(newDir);
                } catch (IOException e) {
                    log.error("Not able to create folder: " + filename);
                    throw new FailedOperationException("Not able to create folder: " + filename);
                }
            }
            log.debug("Directory created: " + filename);
        } else {
            File newFile;
            String dirPath;

            if (filename.contains("/")) {
                log.debug("Zip entry is located in a subdirectory: " + filename);
                if (isVnfPkg)
                    dirPath = vnfPkgsLocation + "/" + descriptorId + "/" + version + "/" + filename.substring(0, filename.lastIndexOf("/"));
                else
                    dirPath = nsdsLocation + "/" + descriptorId + "/" + version + "/" + filename.substring(0, filename.lastIndexOf("/"));
                //create subdirectorie if doesn't exist
                if (!Files.isDirectory(Paths.get(dirPath), LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        Files.createDirectories(Paths.get(dirPath));
                        log.debug("Subdirectory: " + dirPath + " created");
                    } catch (IOException e) {
                        log.error("Not able to create folder: " + dirPath);
                        throw new FailedOperationException("Not able to create folder: " + dirPath);
                    }
                }
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                log.debug("Going to create new file: " + filename);
                newFile = newFile(dirPath, filename);
                log.debug("File {} sucessfully created: {}", filename, newFile.getAbsolutePath());
            } else {
                log.debug("Zip entry is located at the root: " + filename);
                newFile = newFile(locationVersion.toString(), filename);
                log.debug("File {} sucessfully created: {}", filename, newFile.getAbsolutePath());
            }

            FileOutputStream fos;
            try {
                fos = new FileOutputStream(newFile);
            } catch (FileNotFoundException e) {
                log.error("New created file not found: " + e.getMessage());
                throw new FailedOperationException("New created file " + filename + "not found while unzipping: " + e.getMessage());
            }

            try {
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            } catch (IOException e) {
                log.error("Unable to write file " + filename + " to filesystem: " + e.getMessage());
                throw new FailedOperationException("Unable to write file " + filename + " to filesystem: " + e.getMessage());
            }
        }

        return filename;
    }

    public static Path loadFile(Path path, String id, String version, String filename) {
        log.debug("Loading file " + filename + " for VNFD|NSD|PNFD " + id);
        Path location = Paths.get(path + "/" + id + "/" + version);
        return location.resolve(filename);
    }

    public static Resource loadFileAsResource(String id, String version, String filename, boolean isVnfPkg) throws NotExistingEntityException {
        log.debug("Searching file " + filename);
        try {
            Path file = null;
            if (isVnfPkg) {
                file = loadFile(vnfPkgsLocation, id, version, filename);
            } else {
                file = loadFile(nsdsLocation, id, version, filename);
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                log.debug("Found file " + filename);
                return resource;
            } else {
                throw new NotExistingEntityException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new NotExistingEntityException("Could not read file: " + filename, e);
        }
    }

    public static Stream<Path> loadAllNsds() throws FailedOperationException {
        log.debug("Loading all files from file system");
        try {
            return Files.walk(nsdsLocation, 1)
                    .filter(path -> !path.equals(nsdsLocation))
                    .map(nsdsLocation::relativize);
        } catch (IOException e) {
            throw new FailedOperationException("Failed to read stored files", e);
        }
    }

    public static Stream<Path> loadAllVnfPkgs() throws FailedOperationException {
        log.debug("Loading all files from file system");
        try {
            return Files.walk(vnfPkgsLocation, 1)
                    .filter(path -> !path.equals(vnfPkgsLocation))
                    .map(vnfPkgsLocation::relativize);
        } catch (IOException e) {
            throw new FailedOperationException("Failed to read stored files", e);
        }
    }

    public static void deleteNsd(String nsdId, String version) {
        log.debug("Removing NSD with nsdId {} and version {}", nsdId, version);
        Path locationVersion = Paths.get(nsdsLocation + "/" + nsdId + "/" + version);

        FileSystemUtils.deleteRecursively(locationVersion.toFile());
        log.debug("NSD with nsdId {} and version {} successfully removed", nsdId, version);
    }

    public static void deleteVnfPkg(String vnfdId, String version) {
        log.debug("Removing VNF Pkg  with vnfdId {} and version {}", vnfdId, version);
        Path locationVersion = Paths.get(vnfPkgsLocation + "/" + vnfdId + "/" + version);

        FileSystemUtils.deleteRecursively(locationVersion.toFile());
        log.debug("VNF Pkg with vnfdId {} and version {} successfully removed", vnfdId, version);
    }

    public static void deleteAll() {
        log.debug("Removing all stored files");
        FileSystemUtils.deleteRecursively(nsdsLocation.toFile());
        FileSystemUtils.deleteRecursively(vnfPkgsLocation.toFile());
        log.debug("Removed all stored files");
    }

    private static File newFile(String destinationDir, String filename) {
        log.debug("Creating new file for zip entry: " + filename);
        File destFile = new File(destinationDir + File.separator + filename);
        log.debug("New file created for zip entry: " + filename);

        return destFile;
    }

    @PostConstruct
    public void initStorageService() {
        log.debug("Initializing file system storage service...");
        nsdsLocation = Paths.get(rootDir + ConfigurationParameters.storageNsdsSubfolder);
        vnfPkgsLocation = Paths.get(rootDir + ConfigurationParameters.storageVnfpkgsSubfolder);

        try {
            init();
        } catch (Exception e) {
            log.error("Could not initialize storage: " + e.getMessage());
        }
    }

    public void init() throws FailedOperationException {
        log.debug("Initializing storage directories...");
        try {
            //create rootLocation + nsds and vnfpkgs folders
            Files.createDirectories(nsdsLocation);
            Files.createDirectories(vnfPkgsLocation);
        } catch (IOException e) {
            throw new FailedOperationException("Unable to create local storage directories: " + e.getMessage());
        }
    }

}
