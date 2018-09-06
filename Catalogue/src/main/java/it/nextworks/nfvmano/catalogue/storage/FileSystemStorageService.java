package it.nextworks.nfvmano.catalogue.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import it.nextworks.nfvmano.catalogue.common.ConfigurationParameters;
import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;

@Service
public class FileSystemStorageService implements StorageServiceInterface {

	private static final Logger log = LoggerFactory.getLogger(FileSystemStorageService.class);
	
	@Value("${catalogue.storageRootDir}")
	private String rootDir;
	
	private Path nsdsLocation;
	private Path vnfpckgsLocation;
	
	public FileSystemStorageService() {	}
	
	@PostConstruct
	public void initStorageService() {
		log.debug("Initializing file system storage service.");
		this.nsdsLocation = Paths.get(rootDir + ConfigurationParameters.storageNsdsSubfolder);
		this.vnfpckgsLocation = Paths.get(rootDir + ConfigurationParameters.storageVnfpckgsSubfolder);
		//this.deleteAll();
		try {
			this.init();
		} catch (Exception e) {
			log.error("================================ " + e.getMessage() + " ===========================================");
		}
	}
	
	public void init() throws FailedOperationException {
		log.debug("Initializing storage directories");
		try {
			//create rootLocation + nsds and vnfpckgs folders
            Files.createDirectories(nsdsLocation);
            Files.createDirectories(vnfpckgsLocation);
        }
        catch (IOException e) {
            throw new FailedOperationException("Could not initialize storage", e);
        }
	}

	public String storeNsd(NsdInfoResource nsdInfo, MultipartFile file) throws MalformattedElementException, FailedOperationException {
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
			//create nsd + version dedicated directories, if not there
			Path locationRoot = Paths.get(nsdsLocation + "/" + nsdInfo.getNsdId().toString());
			Path locationVersion = Paths.get(nsdsLocation + "/" + nsdInfo.getNsdId().toString() + "/" + nsdInfo.getNsdVersion());
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
		}
		catch (IOException e) {
			throw new FailedOperationException("Failed to store file " + filename, e);
		}
	}

    public Stream<Path> loadAllNsds() throws FailedOperationException {
    	log.debug("Loading all files from file system");
    	try {
    		return Files.walk(this.nsdsLocation, 1)
    				.filter(path -> !path.equals(this.nsdsLocation))
    				.map(this.nsdsLocation::relativize);
    	}
    	catch (IOException e) {
    		throw new FailedOperationException("Failed to read stored files", e);
    	}
    }

    public Path loadNsd(NsdInfoResource nsdInfo, String filename) {
    	log.debug("Loading file " + filename + " for NSD " + nsdInfo.getNsdId().toString());
    	Path location = Paths.get(nsdsLocation + "/" + nsdInfo.getNsdId().toString() + "/" + nsdInfo.getNsdVersion());
    	return location.resolve(filename);
    }

    public Resource loadNsdAsResource(NsdInfoResource nsdInfo, String filename) throws NotExistingEntityException {
    	log.debug("Searching file " + filename);
    	try {
            Path file = loadNsd(nsdInfo, filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
            	log.debug("Found file " + filename);
                return resource;
            }
            else {
                throw new NotExistingEntityException("Could not read file: " + filename);
            }
        }
        catch (MalformedURLException e) {
            throw new NotExistingEntityException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
    	log.debug("Removing all stored files.");
    	FileSystemUtils.deleteRecursively(nsdsLocation.toFile());
    	FileSystemUtils.deleteRecursively(vnfpckgsLocation.toFile());
    	log.debug("Removed all stored files.");
    }

}
