package it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.repos;

import it.nextworks.nfvmano.libs.osmr4PlusDataModel.osmManagement.OsmInfoObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OsmInfoObjectRepository extends JpaRepository<OsmInfoObject, Long> {

    Optional<OsmInfoObject> findById(String id);
    List<OsmInfoObject> findByOsmId(String osmId);
    Optional<OsmInfoObject> findByDescriptorIdAndVersionAndOsmId(String descriptorId, String version, String osmId);
}
