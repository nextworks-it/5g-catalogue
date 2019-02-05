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
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.nsDescriptor.OsmNsdPackage;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor.OsmVNFPackage;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class ArchiveBuilder {

    private static final Logger log = LoggerFactory.getLogger(ArchiveBuilder.class);

    private final File tmpDir;
    private final File defaultLogo;

    public ArchiveBuilder(File tmpDir, File defaultLogo) {
        this.tmpDir = tmpDir;
        this.defaultLogo = defaultLogo;
    }

    public File makeNewArchive(OsmNsdPackage ymlFile, String readmeContent, File logoFile) {
        if (!(ymlFile.getNsdCatalog().getNsd().size() == 1)) {
            throw new IllegalArgumentException("Too many Nsds in file.");
        }
        String nsdId = ymlFile.getNsdCatalog().getNsd().get(0).getId();
        File folder = makeFolder(nsdId);
        makeReadme(readmeContent, folder);
        makeYml(ymlFile, nsdId, folder);
        makeSubFolder(folder, "ns_config");
        makeSubFolder(folder, "scripts");
        makeSubFolder(folder, "vnf_config");
        File iconsFolder = makeSubFolder(folder, "icons");
        copyFile(iconsFolder, logoFile);
        // TODO checksum
        return compress(folder);
    }

    public File makeNewArchive(OsmNsdPackage ymlFile, File logoFile) {
        return makeNewArchive(ymlFile, "", logoFile);
    }

    public File makeNewArchive(OsmNsdPackage ymlFile, String readmeContent) {
        return makeNewArchive(ymlFile, readmeContent, defaultLogo);
    }

    public File makeNewArchive(OsmNsdPackage ymlFile) {
        return makeNewArchive(ymlFile, "", defaultLogo);
    }

    public File makeNewArchive(OsmVNFPackage ymlFile, String readmeContent, File logoFile, File cloudInitFile) {
        if (!(ymlFile.getVnfdCatalog().getVnfd().size() == 1)) {
            throw new IllegalArgumentException("Too many Vnfds in file.");
        }
        String vnfdId = ymlFile.getVnfdCatalog().getVnfd().get(0).getId();
        File folder = makeFolder(vnfdId);
        makeReadme(readmeContent, folder);
        makeYml(ymlFile, vnfdId, folder);
        File iconsFolder = makeSubFolder(folder, "icons");
        File cloudInitFolder = makeSubFolder(folder, "cloud_init");
        copyFile(iconsFolder, logoFile);
        if(cloudInitFile != null)
            copyFile(cloudInitFolder, cloudInitFile);
        // TODO checksum
        return compress(folder);
    }

    public File makeNewArchive(OsmVNFPackage ymlFile, File logoFile) {
        return makeNewArchive(ymlFile, "", logoFile, null);
    }

    public File makeNewArchive(OsmVNFPackage ymlFile, String readmeContent) {
        return makeNewArchive(ymlFile, readmeContent, defaultLogo, null);
    }

    public File makeNewArchive(OsmVNFPackage ymlFile) {
        return makeNewArchive(ymlFile, "", defaultLogo, null);
    }

    private File makeFolder(String Id) {
        File folder = new File(tmpDir, Id);
        if (folder.isDirectory()) {
            log.debug("Temporary folder {} already existing. Overwriting.", folder.getAbsolutePath());
            if (!rmRecursively(folder)) {
                throw new IllegalStateException(
                        String.format("Could not delete folder %s", folder.getAbsolutePath())
                );
            }
        }
        if (!folder.mkdir()) {
            throw new IllegalArgumentException(
                    String.format("Cannot create folder %s", folder.getAbsolutePath())
            );
        }
        return folder;
    }

    private boolean rmRecursively(File folder) {
        SimpleFileVisitor<Path> deleter = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw e;
                }
            }
        };
        try {
            Files.walkFileTree(folder.toPath(), deleter);
            return true;
        } catch (IOException e) {
            log.error(
                    "Could not recursively delete folder {}. Error: {}.",
                    folder.getAbsolutePath(),
                    e.getMessage()
            );
            log.debug("Error details: ", e);
            return false;
        }
    }

    private void makeYml(OsmNsdPackage ymlContent, String nsdId, File folder) {
        File nsdFile = new File(folder, nsdId + "_nsd.yaml");
        ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
        ymlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        try {
            List<String> strings = Arrays.asList(ymlMapper.writeValueAsString(ymlContent).split("\n"));
            Files.write(nsdFile.toPath(), strings);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid package provided");
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Could not write nsd file %s. Error: %s", nsdFile.getAbsolutePath(), e.getMessage())
            );
        }
    }

    private void makeYml(OsmVNFPackage ymlContent, String vnfdId, File folder) {
        File vnfdFile = new File(folder, vnfdId + "_vnfd.yaml");
        ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
        ymlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        try {
            List<String> strings = Arrays.asList(ymlMapper.writeValueAsString(ymlContent).split("\n"));
            Files.write(vnfdFile.toPath(), strings);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid package provided");
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Could not write nsd file %s. Error: %s", vnfdFile.getAbsolutePath(), e.getMessage())
            );
        }
    }

    private void makeReadme(String readmeContent, File folder) {
        File readme = new File(folder, "README");
        List<String> strings = Arrays.asList(readmeContent.split("\n"));
        try {
            Files.write(readme.toPath(), strings);
        } catch (IOException e) {
            String msg = String.format(
                    "Could not write readme file %s. Error: %s.",
                    readme.getPath(),
                    e.getMessage()
            );
            throw new IllegalArgumentException(msg);
        }
    }

    private File makeSubFolder(File folder, String subFolder) {
        File newFolder = new File(folder, subFolder);
        if (!newFolder.mkdir()) {
            throw new IllegalArgumentException(
                    String.format("Cannot create folder %s", newFolder.getAbsolutePath())
            );
        }
        return newFolder;
    }

    private void copyFile(File fileFolder, File file) {
        try {
            Files.copy(file.toPath(), new File(fileFolder, file.getName()).toPath());
        } catch (IOException e) {
            String msg = String.format(
                    "Cannot copy icon file %s to folder %s",
                    file.getAbsolutePath(),
                    fileFolder.getAbsolutePath()
            );
            throw new IllegalArgumentException(msg);
        }
    }

    private File compress(File folder) {
        File rootDir = folder.getParentFile();
        File archive = new File(rootDir, folder.getName() + ".tar.gz");
        try (
                FileOutputStream fos = new FileOutputStream(archive);
                GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(fos));
                TarArchiveOutputStream tos = new TarArchiveOutputStream(gos)
        ) {
            SimpleFileVisitor<Path> archiver = new SimpleFileVisitor<Path>() {

                private File ROOT = folder.getParentFile();

                private String relPath(Path target) {
                    return ROOT.toPath().relativize(target).toString();
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    tos.putArchiveEntry(new TarArchiveEntry(file.toFile(), relPath(file)));
                    Files.copy(file, tos);
                    tos.closeArchiveEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes)
                        throws IOException {
                    tos.putArchiveEntry(new TarArchiveEntry(path.toFile(), relPath(path)));
                    tos.closeArchiveEntry();
                    return FileVisitResult.CONTINUE;
                }
            };
            Files.walkFileTree(folder.toPath(), archiver);
            return archive;
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Could not compress package. Error: %s", e.getMessage())
            );
        }
    }
}
