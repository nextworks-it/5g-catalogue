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
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.r4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.elements.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.ArchiveBuilder;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.NsdBuilder;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.nsDescriptor.OsmNsdPackage;
import it.nextworks.nfvmano.catalogue.translators.tosca.DescriptorsParser;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import org.junit.BeforeClass;
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
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class OpenSourceMANOR4PluginTest {

    @Value("${catalogue.osmr4.localDir}")
    private static String osmr4LocalDir;

    @Value("${catalogue.logo}")
    private static String logoPath;

    private static OpenSourceMANOR4Plugin plugin;

    private static File TMP_DIR;

    private static File DEF_IMG;

    static DescriptorTemplate readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String result = new String(encoded, encoding);
        return DescriptorsParser.stringToDescriptorTemplate(result);
    }

    @BeforeClass
    public static void connectOsm() {
        TMP_DIR = new File(osmr4LocalDir);
        DEF_IMG = new File(OpenSourceMANOR4Plugin.class.getClassLoader().getResource(logoPath).getFile());
        OSMMano osmMano = new OSMMano("testOSM", "10.0.2.97", "admin", "admin", "admin", MANOType.OSMR4, new ArrayList<>());
        plugin = new OpenSourceMANOR4Plugin(MANOType.OSMR4, osmMano, "blabla", null, null, null, null, null, null, Paths.get("/etc/osmr4"), Paths.get("nxw_logo.png"));
        // TODO: mock the nsdService
    }

    // It's an integration test and depends on OSM.
    // it's here for practicality, but should not be run by default (hence @Ignore)
    @Test
    @Ignore
    public void init() {
        plugin.initOsmConnection();
    }

    @Test
    @Ignore// As above
    public void testOnBoard() throws InterruptedException, FailedOperationException, IOException {
        init();
        ClassLoader classLoader = getClass().getClassLoader();
        File vnfd = new File(classLoader.getResource("Descriptors/OSM/cirros_vnf.tar.gz").getFile());
        File nsd = new File(classLoader.getResource("Descriptors/OSM/cirros_2vnf_ns.tar.gz").getFile());
        plugin.onBoardVnfPackage(vnfd, "cirros_vnf_info", "test_onb_vnfd");
        plugin.onBoardNsPackage(nsd, "cirros_2vnf_ns_info", "test_onb_nsd");
        // Sleep for OSM to finish
        Thread.sleep(2000);
        assertTrue(plugin.getVnfdIdList().contains("cirros_vnfd"));
        assertTrue(plugin.getNsdIdList().contains("cirros_2vnf_nsd"));
    }

    @Test
    @Ignore // As above
    public void testOnBoardGenerated() throws InterruptedException, FailedOperationException, IOException, MalformattedElementException {
        // Prerequisite: have the related VNFD (cirros_vnf) with id cb0bcae1-00e7-41fa-b407-ba384e76a3e3 loaded into OSM
        init();
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

        OsmNsdPackage packageData = nsdBuilder.getPackage();

        System.out.println("===============================================================================================");
        System.out.println("OSM NSD: ");
        System.out.println(mapper.writeValueAsString(packageData));
        System.out.println("===============================================================================================");

        ArchiveBuilder archiver = new ArchiveBuilder(TMP_DIR, DEF_IMG);
        File nsd = archiver.makeNewArchive(packageData, "Generated by NXW Catalogue");

        System.out.println("===============================================================================================");
        plugin.onBoardNsPackage(nsd, "cirros_2vnf_ns_info", "test_onb_nsd");
        System.out.println("Onboarding nsd finished");
        System.out.println("===============================================================================================");
        // Sleep for OSM to finish
        Thread.sleep(1000);
        System.out.println("===============================================================================================");
        System.out.println("NSDs list:");
        System.out.println(plugin.getNsdIdList());
        System.out.println("===============================================================================================");
        assertTrue(plugin.getNsdIdList().contains("339b0999-80c3-456c-9ee7-4cf42e4f7be7"));
    }


    @Test
    @Ignore// As above
    public void testacceptOnboardNotf() throws InterruptedException, FailedOperationException {
        init();
        plugin.acceptNsdOnBoardingNotification(new NsdOnBoardingNotificationMessage("test_info_id", "test_nsd_id",
                UUID.fromString("7a4cea43-e29d-423b-9ac8-9f0110ede94e"), ScopeType.LOCAL, OperationStatus.SENT));
        Thread.sleep(2000);
        assertTrue(plugin.getVnfdIdList().contains("cirros_vnfd"));
        assertTrue(plugin.getNsdIdList().contains("cirros_2vnf_nsd"));
    }

    @Test
    @Ignore// As above
    public void testDelete() throws InterruptedException, FailedOperationException {
        // Prerequisite: have the cirros 2vnf NSD and related VNFD loaded into OSM
        init();
        //plugin.deleteNsd("339b0999-80c3-456c-9ee7-4cf42e4f7be7", "test_delete_nsd_gen");
        plugin.deleteNsd("cirros_2vnf_ns_info", "test_delete_nsd");
        plugin.deleteVnfd("cirros_vnf_info", "test_delete_vnf");
        Thread.sleep(1000);
        assertFalse(plugin.getVnfdIdList().contains("cirros_vnfd"));
        assertFalse(plugin.getNsdIdList().contains("cirros_2vnf_nsd"));
    }

    @Test
    @Ignore// As above
    public void testacceptDeleteNotf() throws InterruptedException, FailedOperationException {
        init();
        plugin.acceptNsdDeletionNotification(new NsdDeletionNotificationMessage("test_info_id", "test_nsd_id",
                UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SUCCESSFULLY_DONE, "DUMMY"));
        Thread.sleep(2000);
        assertFalse(plugin.getVnfdIdList().contains("cirros_vnfd"));
        assertFalse(plugin.getNsdIdList().contains("cirros_2vnf_nsd"));
    }
}