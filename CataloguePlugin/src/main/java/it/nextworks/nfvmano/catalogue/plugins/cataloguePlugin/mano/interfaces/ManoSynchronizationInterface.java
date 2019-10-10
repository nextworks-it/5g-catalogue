package it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.interfaces;

import it.nextworks.nfvmano.libs.common.elements.KeyValuePair;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

import java.util.Map;

public interface ManoSynchronizationInterface {

    /**
     * This method is called at startup to synchronize Catalogue and Mano Plugin.
     *
     * @param project Project
     *
     * @return Map of all Vnf descriptor Id and version handled by the Mano Plugin
     */
    Map<String, String> getAllVnf(String project);

    /**
     * This method is called when a new Vnf descriptor is found.
     *
     * @param vnfdId Vnf descriptor Id
     * @param vnfdVersion Vnf descriptor version
     * @param project Project
     *
     * @return KeyValuePair with Vnf Pkg Path and PathType
     */
    KeyValuePair getTranslatedVnfPkgPath(String vnfdId, String vnfdVersion, String project);

    /**
     * This method is called to notify the Vnf descriptor on-boarding status.
     *
     * @param vnfInfoId Catalogue Vnf Info Id
     * @param vnfdId Vnf descriptor Id
     * @param vnfdVersion Vnf descriptor version
     * @param project Project
     *
     */
    void notifyVnfOnboarding(String vnfInfoId, String vnfdId, String vnfdVersion, String project, OperationStatus opStatus);

}
