package it.nextworks.nfvmano.catalogue.translators;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import it.nextworks.nfvmano.catalogue.repos.DescriptorTemplateRepository;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.Sap.SapNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.templates.Metadata;
import it.nextworks.nfvmano.libs.descriptors.templates.Node;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;

public class ToscaTranslatorTests {

	@Autowired
	DescriptorsParser descriptorParser;

	@Autowired
	DescriptorTemplateRepository descriptorTemplateRepository;

	@SuppressWarnings("unused")
	private String StoreNSDescriptorTemplate(DescriptorTemplate input) throws AlreadyExistingEntityException {

		String descriptorId = input.getMetadata().getDescriptorId();
		String vendor = input.getMetadata().getVendor();
		String version = input.getMetadata().getVersion();

		DescriptorTemplate output = new DescriptorTemplate(input.getToscaDefinitionsVersion(),
				input.getToscaDefaultNamespace(), input.getDescription(), input.getMetadata(),
				input.getTopologyTemplate());

		System.out.println(ReflectionToStringBuilder.toString(output, ToStringStyle.MULTI_LINE_STYLE));

		try {
			descriptorTemplateRepository.saveAndFlush(output);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		Optional<DescriptorTemplate> optionalDT = descriptorTemplateRepository
				.findByMetadataDescriptorIdAndMetadataVendorAndMetadataVersion(descriptorId, vendor, version);
		UUID id = null;

		if (optionalDT.isPresent()) {
			id = ((DescriptorTemplate) optionalDT.get()).getId();
		}

		return String.valueOf(id);
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	@Ignore
	@SuppressWarnings("static-access")
	@Test
	public void parseStringDescriptor() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		File nsd = new File(classLoader.getResource("vCDN_tosca_v02.yaml").getFile());
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		String nsd_string = readFile(nsd.getAbsolutePath(), StandardCharsets.UTF_8);

		System.out.println("===============================================================================================");
		System.out.println("NSD from FILE: ");
		System.out.println(nsd_string);
		System.out.println("===============================================================================================");
		
		DescriptorTemplate dt = descriptorParser.stringToDescriptorTemplate(nsd_string);
		System.out.println("Successfull parsing. Parsed NSD: ");
		System.out.println(mapper.writeValueAsString(dt));
	}

	@Ignore
	@SuppressWarnings("static-access")
	@Test
	public void parseFileDescriptor() {

		try {

			ClassLoader classLoader = getClass().getClassLoader();
			File nsd = new File(classLoader.getResource("vCDN_tosca_v02.yaml").getFile());
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

			DescriptorTemplate descriptorTemplate = descriptorParser.fileToDescriptorTemplate(nsd.getAbsolutePath());		
			Metadata metadata = descriptorTemplate.getMetadata();
			
			System.out.println("===============================================================================================");
			System.out.println("Descriptor Metadata");
			System.out.println(mapper.writeValueAsString(metadata));
			System.out.println("===============================================================================================");

			System.out.println("Descriptor Nodes");
			Map<String, Node> nodes = descriptorTemplate.getTopologyTemplate().getNodeTemplates();
			for (Map.Entry<String, Node> node : nodes.entrySet()) {
				System.out.println(node.getKey());
			}
			System.out.println("===============================================================================================");

			try {
				List<NSNode> nsNodes = descriptorTemplate.getTopologyTemplate().getNSNodes();

				System.out.println("NS Nodes");
				for (NSNode nsNode : nsNodes) {
					System.out.println(mapper.writeValueAsString(nsNode));
				}
				System.out.println("===============================================================================================");

				List<NsVirtualLinkNode> nsVirtualLinks = descriptorTemplate.getTopologyTemplate()
						.getNsVirtualLinkNodes();

				System.out.println("NS Virtual Link Nodes");
				for (NsVirtualLinkNode nsVLinkNode : nsVirtualLinks) {
					System.out.println(mapper.writeValueAsString(nsVLinkNode));
				}
				System.out.println("===============================================================================================");

				List<SapNode> sapNodes = descriptorTemplate.getTopologyTemplate().getSapNodes();

				System.out.println("SAP Nodes");
				for (SapNode sapNode : sapNodes) {
					System.out.println(mapper.writeValueAsString(sapNode));
				}
				System.out.println("===============================================================================================");
				
				List<VNFNode> vnfNodes = descriptorTemplate.getTopologyTemplate().getVNFNodes();

				System.out.println("VNF Nodes");
				for (VNFNode vnfNode : vnfNodes) {
					System.out.println(mapper.writeValueAsString(vnfNode));
				}
				System.out.println("===============================================================================================");

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
