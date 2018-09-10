package it.nextworks.nfvmano.catalogue.storage;


import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageServiceInterface {

	public void init() throws FailedOperationException;

    public String storeNsd(NsdInfoResource nsdInfo, MultipartFile file) throws MalformattedElementException, FailedOperationException;

    public Stream<Path> loadAllNsds() throws FailedOperationException;

    public Path loadNsd(NsdInfoResource nsdInfo, String filename);

    public Resource loadNsdAsResource(NsdInfoResource nsdInfo, String filename) throws NotExistingEntityException;

    public void deleteAll();
	
}
