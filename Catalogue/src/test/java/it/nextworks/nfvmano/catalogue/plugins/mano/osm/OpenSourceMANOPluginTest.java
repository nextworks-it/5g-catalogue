package it.nextworks.nfvmano.catalogue.plugins.mano.osm;

import it.nextworks.nfvmano.catalogue.OperationFailedException;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

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
        plugin = new OpenSourceMANOPlugin(MANOType.OSM, osmMano, "blabla");
    }

    // It's an integration test and depends on OSM.
    // it's here for practicality, but should not be run by default (hence @Ignore)
    @Test
    @Ignore
    public void init() {
        plugin.initOsmConnection();
    }

    @Test
    @Ignore  // As above
    public void testOnBoard() throws InterruptedException, OperationFailedException {
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
    @Ignore  // As above
    public void testacceptOnboardNotf() throws InterruptedException {
        init();
        plugin.acceptNsdOnBoardingNotification(new NsdOnBoardingNotificationMessage(
                "test_info_id",
                "test_nsd_id"
        ));
        Thread.sleep(2000);
        assertTrue(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
        assertTrue(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
    }

    @Test
    @Ignore  // As above
    public void testDelete() throws InterruptedException, OperationFailedException {
        // Prerequisite: have the cirros 2vnf NSD and related VNFD loaded into OSM
        init();
        plugin.deleteNsd("cirros_2vnf_ns", "test_delete_nsd");
        plugin.deleteVnfd("cirros_vnf", "test_delete_vnf");
        Thread.sleep(2000);
        assertFalse(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
        assertFalse(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
    }


    @Test
    @Ignore  // As above
    public void testacceptDeleteNotf() throws InterruptedException {
        init();
        plugin.acceptNsdDeletionNotification(new NsdDeletionNotificationMessage(
                "test_info_id",
                "test_nsd_id"
        ));
        Thread.sleep(2000);
        assertFalse(plugin.getVnfdIdList().contains("VNFD cirros_vnf"));
        assertFalse(plugin.getNsdIdList().contains("NSD cirros_2vnf_ns"));
    }
}