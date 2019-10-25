package it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.repos;

import it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.common.TranslationInformation;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TranslationInformationRepository extends JpaRepository<TranslationInformation, UUID> {

    Optional<TranslationInformation> findById(String UUID);

    Optional<TranslationInformation> findByCatInfoIdAndOsmManoId(String catInfoId, String osmManoId);
    List<TranslationInformation> findByOsmManoId(String osmManoId);
    List<TranslationInformation> findByOsmInfoIdAndOsmManoId(String osmInfoId, String osmManoId);
    List<TranslationInformation> findByOsmDescriptorIdAndDescriptorVersionAndOsmManoId(String osmDescriptorId, String descriptorVersion, String osmManoId);
    List<TranslationInformation> findByCatDescriptorIdAndDescriptorVersionAndOsmManoId(String catDescriptorId, String descriptorVersion, String osmManoId);
    List<TranslationInformation> findByCatDescriptorIdAndOsmManoId(String catDescriptorId, String osmManoId);
}
