package it.nextworks.nfvmano.catalogue.translators.tosca;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

@Service
public class DescriptorsParser {
	
	private static final Logger log = LoggerFactory.getLogger(DescriptorsParser.class);

	public static DescriptorTemplate fileToDescriptorTemplate(String fileName)
			throws JsonParseException, JsonMappingException, IOException {

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		DescriptorTemplate descriptorTemplate = mapper.readValue(new File(fileName), DescriptorTemplate.class);

		return descriptorTemplate;
	}
	
	public static DescriptorTemplate fileToDescriptorTemplate(File file)
			throws JsonParseException, JsonMappingException, IOException {

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		DescriptorTemplate descriptorTemplate = mapper.readValue(file, DescriptorTemplate.class);

		return descriptorTemplate;
	}

	public static DescriptorTemplate stringToDescriptorTemplate(String descriptor)
			throws JsonParseException, JsonMappingException, IOException {

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		DescriptorTemplate descriptorTemplate = mapper.readValue(descriptor, DescriptorTemplate.class);

		return descriptorTemplate;
	}
	
	public static String descriptorTemplateToString(DescriptorTemplate descriptor) 
			throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		String dt = mapper.writeValueAsString(descriptor);
		return dt;
	}
}
