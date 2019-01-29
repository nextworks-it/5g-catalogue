package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SupplementalBootData {

    @JsonProperty("boot-data-drive")
    private boolean bootDataDrive;

    public boolean isBootDataDrive() {
        return bootDataDrive;
    }

    public void setBootDataDrive(boolean bootDataDrive) {
        this.bootDataDrive = bootDataDrive;
    }

    @Override
    public String toString() {
        return "SupplementalBootData{" +
                "bootDataDrive=" + bootDataDrive +
                '}';
    }
}