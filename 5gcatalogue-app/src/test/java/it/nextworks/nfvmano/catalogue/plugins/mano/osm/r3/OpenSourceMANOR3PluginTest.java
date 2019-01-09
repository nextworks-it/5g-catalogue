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
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.r3;

import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.OSMMano;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class OpenSourceMANOR3PluginTest {

	private static OpenSourceMANOR3Plugin plugin;

	@BeforeClass
	public static void connectOsm() {
		OSMMano osmMano = new OSMMano("testOSM", "10.0.8.26", "admin", "admin", "default", MANOType.OSMR3);
		plugin = new OpenSourceMANOR3Plugin(MANOType.OSMR3, osmMano, "blabla", null, null, null, null);
		// TODO: mock the service
	}

	// It's an integration test and depends on OSM.
	// it's here for practicality, but should not be run by default (hence @Ignore)
	@Test
	@Ignore
	public void init() {
		plugin.initOsmConnection();
	}

	@Test
	@Ignore // As above
	public void testOnBoard() throws InterruptedException, FailedOperationException {
		init();
		ClassLoader classLoader = getClass().getClassLoader();
		File vnfd = new File(classLoader.getResource("Descriptors/OSM/cirros_vnf.tar.gz").getFile());
		File nsd = new File(classLoader.getResource("Descriptors/OSM/cirros_2vnf_ns.tar.gz").getFile());
		plugin.onBoardPackage(vnfd, "test_onb_vnfd");
		plugin.onBoardPackage(nsd, "test_onb_nsd");
		// Sleep for OSM to finish
		Thread.sleep(2000);
		assertTrue(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
		assertTrue(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
	}

	@Test
	@Ignore // As above
	public void testOnBoardGenerated() throws InterruptedException, FailedOperationException {
		init();
		ClassLoader classLoader = getClass().getClassLoader();
		File vnfd = new File(classLoader.getResource("Descriptors/OSM/cirros_vnf.tar.gz").getFile());
		File nsd = new File(classLoader.getResource("339b0999-80c3-456c-9ee7-4cf42e4f7be7.tar.gz").getFile());
		plugin.onBoardPackage(vnfd, "test_onb_vnfd");
		plugin.onBoardPackage(nsd, "test_onb_nsd");
		// Sleep for OSM to finish
		Thread.sleep(1000);
		assertTrue(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
		assertTrue(plugin.getNsdIdList().contains("NSD 339b0999-80c3-456c-9ee7-4cf42e4f7be7"));
		System.out.println(plugin.getNsdIdList());
	}


	@Test
	@Ignore // As above
	public void testacceptOnboardNotf() throws InterruptedException {
		init();
		plugin.acceptNsdOnBoardingNotification(new NsdOnBoardingNotificationMessage("test_info_id", "test_nsd_id",
				UUID.fromString("7a4cea43-e29d-423b-9ac8-9f0110ede94e"), ScopeType.LOCAL, OperationStatus.SENT));
		Thread.sleep(2000);
		assertTrue(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
		assertTrue(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
	}

	@Test
	@Ignore // As above
	public void testDelete() throws InterruptedException, FailedOperationException {
		// Prerequisite: have the cirros 2vnf NSD and related VNFD loaded into OSM
		init();
		plugin.deleteNsd("339b0999-80c3-456c-9ee7-4cf42e4f7be7", "test_delete_nsd_gen");
		plugin.deleteNsd("cirros_2vnf_ns", "test_delete_nsd");
		plugin.deleteVnfd("cirros_vnf", "test_delete_vnf");
		Thread.sleep(1000);
		assertFalse(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
		assertFalse(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
	}

	@Test
	@Ignore // As above
	public void testacceptDeleteNotf() throws InterruptedException {
		init();
		plugin.acceptNsdDeletionNotification(new NsdDeletionNotificationMessage("test_info_id", "test_nsd_id",
				UUID.randomUUID(), ScopeType.LOCAL, OperationStatus.SUCCESSFULLY_DONE, "DUMMY"));
		Thread.sleep(2000);
		assertFalse(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
		assertFalse(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
	}
}