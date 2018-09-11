package it.nextworks.nfvmano.catalogue.plugins.mano.osm;

import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.ScopeType;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by Marco Capitani on 20/08/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
public class OpenSourceMANOPluginTest {

	private static OpenSourceMANOPlugin plugin;

    @BeforeClass
    public static void connectOsm() {
        OSMMano osmMano = new OSMMano(
                "testOSM",
                "10.0.8.26",
                "admin",
                "admin",
                "default"
        );
        plugin = new OpenSourceMANOPlugin(
                MANOType.OSM,
                osmMano,
                "blabla",
                null,
                null,
                null,
                null
        );
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
		File vnfd = new File(classLoader.getResource("cirros_vnf.tar.gz").getFile());
		File nsd = new File(classLoader.getResource("cirros_2vnf_ns.tar.gz").getFile());
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
		File vnfd = new File(classLoader.getResource("cirros_vnf.tar.gz").getFile());
		File nsd = new File(classLoader.getResource("339b0999-80c3-456c-9ee7-4cf42e4f7be7.tar.gz").getFile());
		plugin.onBoardPackage(vnfd, "test_onb_vnfd");
		plugin.onBoardPackage(nsd, "test_onb_nsd");
		// Sleep for OSM to finish
		Thread.sleep(2000);
		System.out.println(plugin.getNsdIdList());
		assertTrue(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
		assertTrue(plugin.getNsdIdList().contains("NSD cirros_2_test_nsd"));
	}


	@Test
	@Ignore // As above
	public void testacceptOnboardNotf() throws InterruptedException {
		init();
		plugin.acceptNsdOnBoardingNotification(new NsdOnBoardingNotificationMessage(
		        "test_info_id",
                "test_nsd_id",
				UUID.fromString("7a4cea43-e29d-423b-9ac8-9f0110ede94e"),
                ScopeType.LOCAL,
                OperationStatus.SENT
        ));
		Thread.sleep(2000);
		assertTrue(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
		assertTrue(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
	}

	@Test
	@Ignore // As above
	public void testDelete() throws InterruptedException, FailedOperationException {
		// Prerequisite: have the cirros 2vnf NSD and related VNFD loaded into OSM
		init();
		plugin.deleteNsd("7a4cea43-e29d-423b-9ac8-9f0110ede94e", "test_delete_nsd");
		plugin.deleteNsd("cirros_2vnf_ns", "test_delete_nsd");
		plugin.deleteVnfd("cirros_vnf", "test_delete_vnf");
		Thread.sleep(2000);
		assertFalse(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
		assertFalse(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
	}

	@Test
	@Ignore // As above
	public void testacceptDeleteNotf() throws InterruptedException {
		init();
		plugin.acceptNsdDeletionNotification(new NsdDeletionNotificationMessage("test_info_id", "test_nsd_id", ScopeType.LOCAL));
		Thread.sleep(2000);
		assertFalse(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
		assertFalse(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
	}
}