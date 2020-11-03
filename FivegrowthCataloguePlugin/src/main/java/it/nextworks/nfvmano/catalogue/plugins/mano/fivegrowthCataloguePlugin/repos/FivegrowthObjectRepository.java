package it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin.repos;

import it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin.model.SoObject;
import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.SoObjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FivegrowthObjectRepository extends JpaRepository<SoObject, String> {

    Optional<SoObject> findById(String id);
    Optional<SoObject> findByDescriptorIdAndTypeAndSoId(String descriptorId, SoObjectType type, String soId);
    List<SoObject> findBySoIdAndType(String soId, SoObjectType type);
    List<SoObject> findBySoId(String onapId);
    Optional<SoObject> findByDescriptorIdAndVersionAndSoId(String descriptorId, String version, String soId);
    Optional<SoObject> findByCatalogueIdAndSoId(String catalogue, String soId);
}
