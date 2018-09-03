package it.nextworks.nfvmano.catalogue.storage;


import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageServiceInterface {

	public void init() throws FailedOperationException;

    public String store(MultipartFile file) throws MalformattedElementException, FailedOperationException;

    public Stream<Path> loadAll() throws FailedOperationException;

    public Path load(String filename);

    public Resource loadAsResource(String filename) throws NotExistingEntityException;

    public void deleteAll();
	
}
