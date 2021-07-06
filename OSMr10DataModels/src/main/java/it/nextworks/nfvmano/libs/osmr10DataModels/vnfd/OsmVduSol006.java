package it.nextworks.nfvmano.libs.osmr10DataModels.vnfd;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.libs.descriptors.sol006.VnfdVdu;

public class OsmVduSol006 extends VnfdVdu {

    @JsonProperty("cloud-init-file")
    private String cloudInitFile;

    public OsmVduSol006 cloudInitFile(String cloudInitFile) {
        this.cloudInitFile = cloudInitFile;
        return this;
    }

    public String getCloudInitFile() { return cloudInitFile; }

    public void setCloudInitFile(String cloudInitFile) { this.cloudInitFile = cloudInitFile; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OsmVduSol006 {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    ").append(toIndentedString(cloudInitFile)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(java.lang.Object o) {
        if (o == null)
            return "null";

        return o.toString().replace("\n", "\n    ");
    }
}
