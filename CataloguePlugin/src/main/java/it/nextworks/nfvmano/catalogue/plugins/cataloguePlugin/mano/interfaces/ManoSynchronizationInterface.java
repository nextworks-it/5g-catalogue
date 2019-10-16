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
     * This method is called at startup to synchronize Catalogue and Mano Plugin.
     *
     * @param project Project
     *
     * @return Map of all Ns descriptor Id and version handled by the Mano Plugin
     */
    Map<String, String> getAllNs(String project);

    /**
     * This method is called when a new Descriptor is found.
     *
     * @param descriptorId Vnf descriptor Id
     * @param descriptorVersion Vnf descriptor version
     * @param project Project
     *
     * @return KeyValuePair with Vnf Pkg Path and PathType
     */
    KeyValuePair getTranslatedPkgPath(String descriptorId, String descriptorVersion, String project);

    /**
     * This method is called to notify the Descriptor on-boarding status.
     *
     * @param infoId Catalogue info Id
     * @param descriptorId Descriptor Id
     * @param descriptorVersion Descriptor version
     * @param project Project
     *
     */
    void notifyOnboarding(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus);

    /**
     * This method is called to notify the Descriptor delete status.
     *
     * @param infoId Catalogue info Id
     * @param descriptorId Descriptor Id
     * @param descriptorVersion Descriptor version
     * @param project Project
     *
     */
    void notifyDelete(String infoId, String descriptorId, String descriptorVersion, String project, OperationStatus opStatus);

}
