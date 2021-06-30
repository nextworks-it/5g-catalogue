package it.nextworks.nfvmano.libs.osmr10DataModels;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.libs.descriptors.sol006.Vnfd;

public class OsmVnfdSol006 extends Vnfd {

    @JsonProperty("mgmt-cp")
    private String mgmtCp;

    public OsmVnfdSol006 mgmtCp(String mgmtCp) {
        this.mgmtCp = mgmtCp;
        return this;
    }

    public String getMgmtCp() { return mgmtCp;}

    public void setMgmtCp(String mgmtCp) { this.mgmtCp = mgmtCp; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OsmVnfdSol006 {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    ").append(toIndentedString(mgmtCp)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(java.lang.Object o) {
        if (o == null)
            return "null";

        return o.toString().replace("\n", "\n    ");
    }
}
