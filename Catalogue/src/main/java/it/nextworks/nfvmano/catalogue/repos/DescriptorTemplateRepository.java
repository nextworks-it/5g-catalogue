package it.nextworks.nfvmano.catalogue.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

public interface DescriptorTemplateRepository extends JpaRepository<DescriptorTemplate, Long> {
	Optional<DescriptorTemplate> findById(Long id);
}
