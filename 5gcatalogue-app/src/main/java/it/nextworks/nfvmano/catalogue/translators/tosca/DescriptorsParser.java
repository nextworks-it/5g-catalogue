/*
 * Copyright 2018 Nextworks s.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.nextworks.nfvmano.catalogue.translators.tosca;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

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
