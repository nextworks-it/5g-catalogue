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
package it.nextworks.nfvmano.catalogue.translators;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.common.enums.DescriptorType;
import it.nextworks.nfvmano.catalogue.translators.tosca.ArchiveParser;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.descriptors.sol006.Vnfd;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ToscaTranslatorTests {

    @Autowired
    DescriptorsParser descriptorParser;

    @Autowired
    ArchiveParser archiveParser;

    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    @Test
    @Ignore
    public void parseStringDescriptor() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File nsd = new File(classLoader.getResource("Descriptors/two_cirros_example_tosca.yaml").getFile());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        String nsd_string = readFile(nsd.getAbsolutePath(), StandardCharsets.UTF_8);

        DescriptorTemplate dt = descriptorParser.stringToDescriptorTemplate(nsd_string);

        assertNotNull(dt);

        System.out.println("\n===============================================================================================");
        System.out.println("Successfull parsing. Parsed NSD: ");
        System.out.println(mapper.writeValueAsString(dt));
        System.out.println("===============================================================================================\n");
    }

    @Test
    @Ignore
    public void parseFileDescriptor() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File nsd = new File(classLoader.getResource("Descriptors/two_cirros_example_tosca.yaml").getFile());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        DescriptorTemplate dt = descriptorParser.fileToDescriptorTemplate(nsd.getAbsolutePath());

        assertNotNull(dt);

        System.out.println("\n===============================================================================================");
        System.out.println("Successfull parsing. Parsed NSD: ");
        System.out.println(mapper.writeValueAsString(dt));
        System.out.println("===============================================================================================\n");
    }

    @Test
    @Ignore
    public void parseArchive() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File archive = new File(classLoader.getResource("Descriptors/cirros_vnf/cirros_vnf.zip").getFile());
        FileInputStream input = new FileInputStream(archive);
        MultipartFile mp_file = new MockMultipartFile(archive.getName(), archive.getName(), null, input);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        DescriptorTemplate dt = archiveParser.archiveToCSARInfo("admin", mp_file, DescriptorType.VNFD, true).getMst();

        assertNotNull(dt);

        System.out.println("\n===============================================================================================");
        System.out.println("Successfull parsing. Parsed VNFD: ");
        System.out.println(mapper.writeValueAsString(dt));
        System.out.println("===============================================================================================\n");
    }

    @Test
    @Ignore
    public void parseVnfArchiveWithSOL006Descriptor() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File archive = new File(classLoader.getResource("Descriptors/csr_vnf/csr_vnf.zip").getFile());
        FileInputStream input = new FileInputStream(archive);
        MultipartFile mp_file = new MockMultipartFile(archive.getName(), archive.getName(), null, input);

        Vnfd vnfd = archiveParser.archiveToCSARInfo("admin", mp_file, DescriptorType.VNFD, true).getVnfd();

        assertNotNull(vnfd);

        System.out.println("\n=======================================================================================");
        System.out.println("Successfull parsing. Parsed VNFD: ");
        System.out.println(vnfd.toString());
        System.out.println("=======================================================================================\n");
    }
}
