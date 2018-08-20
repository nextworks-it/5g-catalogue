package it.nextworks.nfvmano.translators;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.common.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.common.templates.Node;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSProperties;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSRequirements;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkProperties;

public class ToscaTranslatorTests {
	
	@Autowired
	DescriptorsParser descriptorParser;
	
	@SuppressWarnings("static-access")
	@Test
	public void parseFileDescriptor() {
		
		try {
			
			String currentDirectory;
			File file = new File("");
			currentDirectory = file.getAbsolutePath();
			currentDirectory = currentDirectory.concat("/src/test/java/it/nextworks/nfvmano/translators/descriptors/");
			DescriptorTemplate descriptorTemplate = descriptorParser.fileToDescriptorTemplate(currentDirectory + "vCDN_tosca_v01.yaml");
			System.out.println(ReflectionToStringBuilder.toString(descriptorTemplate, ToStringStyle.MULTI_LINE_STYLE));
			
			Map<String, Node> nodes = descriptorTemplate.getTopologyTemplate().getNodeTemplates();
			
			for (Map.Entry<String, Node> node : nodes.entrySet()) {
				System.out.println("NODE -----> " + node.getKey() + "\n");
				System.out.println(ReflectionToStringBuilder.toString(node.getValue(), ToStringStyle.MULTI_LINE_STYLE));
			}
			
			try {
				List<NSNode> nsNodes = descriptorTemplate.getTopologyTemplate().getNSNodes();
				
				for (NSNode nsNode : nsNodes) {

					System.out.println(ReflectionToStringBuilder.toString(nsNode, ToStringStyle.MULTI_LINE_STYLE));
					
					NSProperties nsProperties = nsNode.getProperties();
					System.out.println(ReflectionToStringBuilder.toString(nsProperties, ToStringStyle.MULTI_LINE_STYLE));
					
					NSRequirements nsRequirements = nsNode.getRequirements();
					System.out.println(ReflectionToStringBuilder.toString(nsRequirements, ToStringStyle.MULTI_LINE_STYLE));
				}
				
				List<NsVirtualLinkNode> nsVirtualLinks = descriptorTemplate.getTopologyTemplate().getNsVirtualLinkNodes();
				System.out.println(ReflectionToStringBuilder.toString(nsVirtualLinks, ToStringStyle.MULTI_LINE_STYLE));
				
				for (NsVirtualLinkNode nsVLink : nsVirtualLinks) {
					
					System.out.println(ReflectionToStringBuilder.toString(nsVLink, ToStringStyle.MULTI_LINE_STYLE));
					
					NsVirtualLinkProperties nsVLinkProperties = nsVLink.getProperties();
					System.out.println(ReflectionToStringBuilder.toString(nsVLinkProperties, ToStringStyle.MULTI_LINE_STYLE));
				}
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
