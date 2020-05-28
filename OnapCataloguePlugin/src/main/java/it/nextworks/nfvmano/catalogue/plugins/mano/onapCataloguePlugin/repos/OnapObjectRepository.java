package it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.repos;

import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.OnapObject;
import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.OnapObjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnapObjectRepository extends JpaRepository<OnapObject, String> {

    Optional<OnapObject> findById(String id);
    Optional<OnapObject> findByDescriptorIdAndTypeAndOnapId(String osmId, OnapObjectType type, String onapId);
    List<OnapObject> findByOnapIdAndType(String onapId, OnapObjectType type);
    List<OnapObject> findByOnapId(String onapId);
    Optional<OnapObject> findByDescriptorIdAndVersionAndOnapId(String osmId, String version, String onapId);
    Optional<OnapObject> findByCatalogueIdAndOnapId(String catalogue, String onapId);

}
