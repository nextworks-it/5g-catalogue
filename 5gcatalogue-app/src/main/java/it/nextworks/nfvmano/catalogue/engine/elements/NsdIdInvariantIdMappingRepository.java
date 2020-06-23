package it.nextworks.nfvmano.catalogue.engine.elements;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NsdIdInvariantIdMappingRepository extends JpaRepository<NsdIdInvariantIdMapping, Long> {

    Optional<NsdIdInvariantIdMapping> findByNsdId(String nsdId);
    Optional<NsdIdInvariantIdMapping> findByInvariantId(String invariantId);
}
