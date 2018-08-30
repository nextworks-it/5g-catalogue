package it.nextworks.nfvmano.catalogue.translators;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import it.nextworks.nfvmano.catalogue.repos.DescriptorTemplateRepository;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSProperties;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSRequirements;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkProperties;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.Sap.SapNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.Sap.SapProperties;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.templates.Node;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFCapabilities;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFRequirements;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;

public class ToscaTranslatorTests {

	@Autowired
	DescriptorsParser descriptorParser;
	
	@Autowired
	DescriptorTemplateRepository descriptorTemplateRepository;
	
	private String StoreNSDescriptorTemplate(DescriptorTemplate input) throws AlreadyExistingEntityException {
		
		String descriptorId = input.getMetadata().getDescriptorId();
		
		DescriptorTemplate output = new DescriptorTemplate(
				input.getToscaDefinitionsVersion(),
				input.getToscaDefaultNamespace(),
				input.getDescription(),
				input.getMetadata(),
				input.getTopologyTemplate());
		
		System.out.println(ReflectionToStringBuilder.toString(output, ToStringStyle.MULTI_LINE_STYLE));
		
		try {
			descriptorTemplateRepository.saveAndFlush(output);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		
		Optional<DescriptorTemplate> optionalDT = descriptorTemplateRepository.findByMetadataDescriptorId(descriptorId);
		Long id = null;
		
		if (optionalDT.isPresent()) {
			id = ((DescriptorTemplate) optionalDT.get()).getId();
		}
		
		
		return String.valueOf(id);
	}

	@SuppressWarnings("static-access")
	@Test
	public void parseFileDescriptor() {

		try {

			String currentDirectory;
			File file = new File("");
			currentDirectory = file.getAbsolutePath();
			currentDirectory = currentDirectory.concat("/src/test/java/it/nextworks/nfvmano/catalogue/translators/descriptors/");
			DescriptorTemplate descriptorTemplate = descriptorParser
					.fileToDescriptorTemplate(currentDirectory + "vCDN_tosca_v01.yaml");
			System.out.println(ReflectionToStringBuilder.toString(descriptorTemplate, ToStringStyle.MULTI_LINE_STYLE));
			
			/*try {
				String id = StoreNSDescriptorTemplate(descriptorTemplate);
				System.out.println("NS DESCRIPTOR GENERATED ID: " + id);
			} catch (AlreadyExistingEntityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/

			Map<String, Node> nodes = descriptorTemplate.getTopologyTemplate().getNodeTemplates();

			for (Map.Entry<String, Node> node : nodes.entrySet()) {
				System.out.println("\nNODE -----> " + node.getKey());
				System.out.println(ReflectionToStringBuilder.toString(node.getValue(), ToStringStyle.MULTI_LINE_STYLE));
			}

			try {
				List<NSNode> nsNodes = descriptorTemplate.getTopologyTemplate().getNSNodes();

				for (NSNode nsNode : nsNodes) {

					System.out.println(ReflectionToStringBuilder.toString(nsNode, ToStringStyle.MULTI_LINE_STYLE));

					NSProperties nsProperties = nsNode.getProperties();
					System.out
							.println(ReflectionToStringBuilder.toString(nsProperties, ToStringStyle.MULTI_LINE_STYLE));

					NSRequirements nsRequirements = nsNode.getRequirements();
					System.out.println(
							ReflectionToStringBuilder.toString(nsRequirements, ToStringStyle.MULTI_LINE_STYLE));
				}

				List<NsVirtualLinkNode> nsVirtualLinks = descriptorTemplate.getTopologyTemplate()
						.getNsVirtualLinkNodes();
				System.out.println(ReflectionToStringBuilder.toString(nsVirtualLinks, ToStringStyle.MULTI_LINE_STYLE));

				for (NsVirtualLinkNode nsVLink : nsVirtualLinks) {

					System.out.println(ReflectionToStringBuilder.toString(nsVLink, ToStringStyle.MULTI_LINE_STYLE));

					NsVirtualLinkProperties nsVLinkProperties = nsVLink.getProperties();
					System.out.println(
							ReflectionToStringBuilder.toString(nsVLinkProperties, ToStringStyle.MULTI_LINE_STYLE));
				}

				/*List<SapNode> sapNodes = descriptorTemplate.getTopologyTemplate().getSapNodes();

				for (SapNode sapNode : sapNodes) {

					System.out.println(ReflectionToStringBuilder.toString(sapNode, ToStringStyle.MULTI_LINE_STYLE));

					SapProperties sapProperties = sapNode.getProperties();
					System.out
							.println(ReflectionToStringBuilder.toString(sapProperties, ToStringStyle.MULTI_LINE_STYLE));
				}
				
				List<VNFNode> vnfNodes = descriptorTemplate.getTopologyTemplate().getVNFNodes();

				for (VNFNode vnfNode : vnfNodes) {

					System.out.println(ReflectionToStringBuilder.toString(vnfNode, ToStringStyle.MULTI_LINE_STYLE));

					VNFProperties vnfProperties = vnfNode.getProperties();
					System.out
							.println(ReflectionToStringBuilder.toString(vnfProperties, ToStringStyle.MULTI_LINE_STYLE));

					VNFRequirements vnfRequirements = vnfNode.getRequirements();
					System.out.println(
							ReflectionToStringBuilder.toString(vnfRequirements, ToStringStyle.MULTI_LINE_STYLE));
					
					VNFCapabilities vnfCapabilities = vnfNode.getCapabilities();
					System.out.println(
							ReflectionToStringBuilder.toString(vnfCapabilities, ToStringStyle.MULTI_LINE_STYLE));
				}*/
			} catch (MalformattedElementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
