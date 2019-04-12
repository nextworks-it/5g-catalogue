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
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.r3.OpenSourceMANOR3Plugin;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class NsdBuilderTest {

    @Value("${catalogue.logo}")
    private String logoPath;

    private File DEF_IMG;

    static DescriptorTemplate readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String result = new String(encoded, encoding);
        return DescriptorsParser.stringToDescriptorTemplate(result);
    }

    @Before
    public void setUp() {
        DEF_IMG = new File(
                OpenSourceMANOR3Plugin.class.getClassLoader().getResource(logoPath).getFile());
    }

    @Test
    @Ignore
    public void parseDescriptorTemplate() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        NsdBuilder nsdBuilder = new NsdBuilder(DEF_IMG);

        DescriptorTemplate dt = readFile(
                new File(classLoader.getResource("Descriptors/two_cirros_example_tosca.yaml").getFile()).getPath(),
                StandardCharsets.UTF_8
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        DescriptorTemplate vnfd_dt = readFile(
                new File(classLoader.getResource("Descriptors/Definitions/cirros_vnfd_sol001.yaml").getFile()).getPath(),
                StandardCharsets.UTF_8
        );

        List<DescriptorTemplate> vnfds = new ArrayList<>();
        vnfds.add(vnfd_dt);

        nsdBuilder.parseDescriptorTemplate(dt, vnfds, MANOType.OSMR4);

        ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
        ymlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        System.out.println(ymlMapper.writeValueAsString(nsdBuilder.getPackage()));
    }

    @Test
    @Ignore
    public void archiveTest() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        DescriptorTemplate dt = readFile(
                new File(classLoader.getResource("Descriptors/two_cirros_example_tosca.yaml").getFile()).getPath(),
                StandardCharsets.UTF_8
        );

        ArchiveBuilder archiveBuilder = new ArchiveBuilder(
                new File("/tmp"),
                new File(classLoader.getResource("osm_2x.png").getFile())
        );
        NsdBuilder nsdBuilder = new NsdBuilder(DEF_IMG);

        DescriptorTemplate vnfd_dt = readFile(
                new File(classLoader.getResource("Descriptors/Definitions/cirros_vnfd_sol001.yaml").getFile()).getPath(),
                StandardCharsets.UTF_8
        );

        List<DescriptorTemplate> vnfds = new ArrayList<>();
        vnfds.add(vnfd_dt);

        nsdBuilder.parseDescriptorTemplate(dt, vnfds, MANOType.OSMR4);
        File archive = archiveBuilder.makeNewArchive(nsdBuilder.getPackage(), "README CONTENT");
        System.out.println("NSD archive created in: " + archive.getAbsolutePath());
    }
}