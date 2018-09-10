package it.nextworks.nfvmano.catalogue.repos;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nextworks.nfvmano.catalogue.engine.resources.NsdInfoResource;

public interface NsdInfoRepository extends JpaRepository<NsdInfoResource, UUID> {

	Optional<NsdInfoResource> findById(UUID id);
	Optional<NsdInfoResource> findByNsdId(UUID nsdId);
	
}
