package it.nextworks.nfvmano.catalogue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;

final class Http2YamlStringMessageConverter extends StringHttpMessageConverter {
	Http2YamlStringMessageConverter() {
    	//super();
    	super(StandardCharsets.UTF_8);
    	setSupportedMediaTypes(Arrays.asList(MediaType.parseMediaType("application/x-yaml")));
    }
}