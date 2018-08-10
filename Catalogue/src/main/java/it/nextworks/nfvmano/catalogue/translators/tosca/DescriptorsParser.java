package it.nextworks.nfvmano.catalogue.translators.tosca;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

//import it.nextworks.nfvmano.libs.descriptors.common.templates.DescriptorTemplate;

@Service
public class DescriptorsParser {

	/*public static DescriptorTemplate fileToDescriptorTemplate(String fileName)
			throws JsonParseException, JsonMappingException, IOException {

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		DescriptorTemplate descriptorTemplate = mapper.readValue(new File(fileName), DescriptorTemplate.class);

		return descriptorTemplate;
	}

	public static DescriptorTemplate stringToDescriptorTemplate(String descriptor)
			throws JsonParseException, JsonMappingException, IOException {

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		DescriptorTemplate descriptorTemplate = mapper.readValue(descriptor, DescriptorTemplate.class);

		return descriptorTemplate;
	}*/
}
