package it.nextworks.nfvmano.catalogue.repos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

public interface DescriptorTemplateRepository extends JpaRepository<DescriptorTemplate, UUID> {
	Optional<DescriptorTemplate> findById(UUID id);
	
	Optional<DescriptorTemplate> findByMetadataDescriptorIdAndMetadataVendorAndMetadataVersion(String descriptorId, String vendor, String version);
	
	List<DescriptorTemplate> findByMetadataDescriptorId(String descriptorId);
}
