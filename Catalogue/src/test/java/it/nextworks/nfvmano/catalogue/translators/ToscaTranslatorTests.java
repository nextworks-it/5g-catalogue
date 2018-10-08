package it.nextworks.nfvmano.catalogue.translators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import it.nextworks.nfvmano.catalogue.repos.DescriptorTemplateRepository;
import it.nextworks.nfvmano.catalogue.translators.tosca.ArchiveParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.Sap.SapNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.templates.Metadata;
import it.nextworks.nfvmano.libs.descriptors.templates.Node;
import it.nextworks.nfvmano.libs.descriptors.templates.SubstitutionMapping;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ToscaTranslatorTests {

	@Autowired
	DescriptorsParser descriptorParser;
	
	@Autowired
	ArchiveParser archiveParser;

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
		File nsd = new File(classLoader.getResource("vCDN_tosca_v03.yaml").getFile());
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
			File nsd = new File(classLoader.getResource("vCDN_UC3_5GMEDIA.yaml").getFile());
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

			DescriptorTemplate descriptorTemplate = descriptorParser.fileToDescriptorTemplate(nsd.getAbsolutePath());		
			Metadata metadata = descriptorTemplate.getMetadata();
			
			System.out.println("===============================================================================================");
			System.out.println("Descriptor Metadata");
			System.out.println(mapper.writeValueAsString(metadata));
			System.out.println("===============================================================================================");
			
			System.out.println("Substitution Mapping");
			SubstitutionMapping subMapping = descriptorTemplate.getTopologyTemplate().getSubstituitionMapping();
			System.out.println(mapper.writeValueAsString(subMapping));
			System.out.println("===============================================================================================");

			System.out.println("Descriptor Nodes");
			Map<String, Node> nodes = descriptorTemplate.getTopologyTemplate().getNodeTemplates();
			for (Map.Entry<String, Node> node : nodes.entrySet()) {
				System.out.println(node.getKey());
			}
			System.out.println("===============================================================================================");

			try {
				Map<String, NSNode> nsNodes = descriptorTemplate.getTopologyTemplate().getNSNodes();

				System.out.println("NS Nodes");
				for (Entry<String, NSNode> nsNode : nsNodes.entrySet()) {
					System.out.println(mapper.writeValueAsString(nsNode.getValue()));
				}
				System.out.println("===============================================================================================");

				Map<String, NsVirtualLinkNode> nsVirtualLinks = descriptorTemplate.getTopologyTemplate()
						.getNsVirtualLinkNodes();

				System.out.println("NS Virtual Link Nodes");
				for (Entry<String, NsVirtualLinkNode> nsVLinkNode : nsVirtualLinks.entrySet()) {
					System.out.println(mapper.writeValueAsString(nsVLinkNode.getValue()));
				}
				System.out.println("===============================================================================================");

				Map<String, SapNode> sapNodes = descriptorTemplate.getTopologyTemplate().getSapNodes();

				System.out.println("SAP Nodes");
				for (Entry<String, SapNode> sapNode : sapNodes.entrySet()) {
					System.out.println(mapper.writeValueAsString(sapNode.getValue()));
				}
				System.out.println("===============================================================================================");
				
				Map<String, VNFNode> vnfNodes = descriptorTemplate.getTopologyTemplate().getVNFNodes();

				System.out.println("VNF Nodes");
				for (Entry<String, VNFNode> vnfNode : vnfNodes.entrySet()) {
					System.out.println(mapper.writeValueAsString(vnfNode.getValue()));
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
	
	@Test
	public void parseArchive() {

		try {

			ClassLoader classLoader = getClass().getClassLoader();
			File archive = new File(classLoader.getResource("vCDN_UC3_5GMEDIA.zip").getFile());
			FileInputStream input = new FileInputStream(archive);
			MultipartFile mp_file = new MockMultipartFile(archive.getName(), input);
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

			DescriptorTemplate descriptorTemplate = null;
			try {
				descriptorTemplate = archiveParser.archiveToMainDescriptor(mp_file);
			} catch (MalformattedElementException e) {
				System.out.println("Unable to parse CSAR: " + e.getMessage());
				e.printStackTrace();
				return;
			}		
			Metadata metadata = descriptorTemplate.getMetadata();
			
			System.out.println("===============================================================================================");
			System.out.println("Descriptor Metadata");
			System.out.println(mapper.writeValueAsString(metadata));
			System.out.println("===============================================================================================");
			
			System.out.println("Substitution Mapping");
			SubstitutionMapping subMapping = descriptorTemplate.getTopologyTemplate().getSubstituitionMapping();
			System.out.println(mapper.writeValueAsString(subMapping));
			System.out.println("===============================================================================================");

			System.out.println("Descriptor Nodes");
			Map<String, Node> nodes = descriptorTemplate.getTopologyTemplate().getNodeTemplates();
			for (Map.Entry<String, Node> node : nodes.entrySet()) {
				System.out.println(node.getKey());
			}
			System.out.println("===============================================================================================");

			try {
				Map<String, NSNode> nsNodes = descriptorTemplate.getTopologyTemplate().getNSNodes();

				System.out.println("NS Nodes");
				for (Entry<String, NSNode> nsNode : nsNodes.entrySet()) {
					System.out.println(mapper.writeValueAsString(nsNode.getValue()));
				}
				System.out.println("===============================================================================================");

				Map<String, NsVirtualLinkNode> nsVirtualLinks = descriptorTemplate.getTopologyTemplate()
						.getNsVirtualLinkNodes();

				System.out.println("NS Virtual Link Nodes");
				for (Entry<String, NsVirtualLinkNode> nsVLinkNode : nsVirtualLinks.entrySet()) {
					System.out.println(mapper.writeValueAsString(nsVLinkNode.getValue()));
				}
				System.out.println("===============================================================================================");

				Map<String, SapNode> sapNodes = descriptorTemplate.getTopologyTemplate().getSapNodes();

				System.out.println("SAP Nodes");
				for (Entry<String, SapNode> sapNode : sapNodes.entrySet()) {
					System.out.println(mapper.writeValueAsString(sapNode.getValue()));
				}
				System.out.println("===============================================================================================");
				
				Map<String, VNFNode> vnfNodes = descriptorTemplate.getTopologyTemplate().getVNFNodes();

				System.out.println("VNF Nodes");
				for (Entry<String, VNFNode> vnfNode : vnfNodes.entrySet()) {
					System.out.println(mapper.writeValueAsString(vnfNode.getValue()));
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
