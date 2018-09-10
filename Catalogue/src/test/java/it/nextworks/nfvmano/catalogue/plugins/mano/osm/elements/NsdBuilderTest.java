package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by Marco Capitani on 10/09/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
public class NsdBuilderTest {

    static DescriptorTemplate readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String result = new String(encoded, encoding);
        return DescriptorsParser.stringToDescriptorTemplate(result);
    }

    @Test
    public void parseDescriptorTemplate() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        NsdBuilder nsdBuilder = new NsdBuilder();

        DescriptorTemplate dt = readFile(
                new File(classLoader.getResource("vCDN_tosca_v03.yaml").getFile()).getPath(),
                StandardCharsets.UTF_8
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        nsdBuilder.parseDescriptorTemplate(dt);


        ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
        ymlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        System.out.println(ymlMapper.writeValueAsString(nsdBuilder.getPackage()));
    }
}