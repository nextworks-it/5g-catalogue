package it.nextworks.nfvmano.catalogue.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
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

import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;

@Service
public class FileSystemStorageService implements StorageServiceInterface {

	private static final Logger log = LoggerFactory.getLogger(FileSystemStorageService.class);
	
	@Value("${catalogue.storageRootDir}")
	private String rootDir;
	
	private Path rootLocation;
	
	public FileSystemStorageService() {	}
	
	@PostConstruct
	public void initStorageService() {
		log.debug("Initializing file system storage service.");
		this.rootLocation = Paths.get(rootDir);
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
            Files.createDirectories(rootLocation);
        }
        catch (IOException e) {
            throw new FailedOperationException("Could not initialize storage", e);
        }
	}

	public String store(MultipartFile file) throws MalformattedElementException, FailedOperationException {
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
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, this.rootLocation.resolve(filename),
						StandardCopyOption.REPLACE_EXISTING);
				return filename;
			}
		}
		catch (IOException e) {
			throw new FailedOperationException("Failed to store file " + filename, e);
		}
	}

    public Stream<Path> loadAll() throws FailedOperationException {
    	log.debug("Loading all files from file system");
    	try {
    		return Files.walk(this.rootLocation, 1)
    				.filter(path -> !path.equals(this.rootLocation))
    				.map(this.rootLocation::relativize);
    	}
    	catch (IOException e) {
    		throw new FailedOperationException("Failed to read stored files", e);
    	}
    }

    @Override
    public Path load(String filename) {
    	log.debug("Loading file " + filename);
    	return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) throws NotExistingEntityException {
    	log.debug("Searching file " + filename);
    	try {
            Path file = load(filename);
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
    	FileSystemUtils.deleteRecursively(rootLocation.toFile());
    	log.debug("Removed all stored files.");
    }

}
