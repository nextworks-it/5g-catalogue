package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Marco Capitani on 10/09/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
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
            throw new IllegalArgumentException("Too many nsds in file.");
        }
        String nsdId = ymlFile.getNsdCatalog().getNsd().get(0).getId();
        File folder = makeFolder(nsdId);
        makeReadme(readmeContent, folder);
        makeSubFolder(folder,"ns_config");
        makeSubFolder(folder,"scripts");
        makeSubFolder(folder,"vnf_config");
        File iconsFolder = makeSubFolder(folder, "icons");
        copyIcon(iconsFolder, logoFile);
        // TODO checksum
        return folder;
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

    public File makeFolder(String nsdId) {
        File folder = new File(tmpDir, nsdId);
        if (!folder.mkdir()) {
            throw new IllegalArgumentException(
                    String.format("Cannot create folder %s", folder.getAbsolutePath())
            );
        }
        return folder;
    }

    public void makeReadme(String readmeContent, File folder) {
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

    public File makeSubFolder(File folder, String subFolder) {
        File newFolder = new File(folder, subFolder);
        if (!newFolder.mkdir()) {
            throw new IllegalArgumentException(
                    String.format("Cannot create folder %s", newFolder.getAbsolutePath())
            );
        }
        return newFolder;
    }

    public void copyIcon(File iconFolder, File icon) {
        try {
            Files.copy(icon.toPath(), iconFolder.toPath());
        } catch (IOException e) {
            String msg = String.format(
                    "Cannot copy icon file %s to folder %s",
                    icon.getAbsolutePath(),
                    iconFolder.getAbsolutePath()
            );
            throw new IllegalArgumentException(msg);
        }
    }
}
