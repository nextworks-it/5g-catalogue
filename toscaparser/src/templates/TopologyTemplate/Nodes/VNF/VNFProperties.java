/*
 * Copyright (c) 2016 Open Baton (http://www.openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package templates.TopologyTemplate.Nodes.VNF;

import exceptions.NotFoundException;

import java.util.*;

/** Created by rvl on 19.08.16. */
public class VNFProperties {

  private String vendor = null;
  private String ID = null;
  private String version = "";
  private String vnfPackageLocation;
  private ArrayList<HashMap<String, String>> deploymentFlavour;
  private VNFConfigurations configurations = null;
  private String endpoint = null;
  private String type = null;
  private Map<String, String> metadata;

  public VNFProperties() {}

  @SuppressWarnings({"unsafe", "unchecked"})
  public VNFProperties(Object properties) {

    Map<String, Object> propertiesMap = (Map<String, Object>) properties;

    if (propertiesMap.containsKey("vendor")) {
      vendor = (String) propertiesMap.get("vendor");
    }
    if (propertiesMap.containsKey("version")) {
      version = String.valueOf(propertiesMap.get("version"));
    }
    if (propertiesMap.containsKey("vnfPackageLocation")) {
      vnfPackageLocation = (String) propertiesMap.get("vnfPackageLocation");
    }
    if (propertiesMap.containsKey("deploymentFlavour")) {
      deploymentFlavour =
          (ArrayList<HashMap<String, String>>) propertiesMap.get("deploymentFlavour");
    }
    if (propertiesMap.containsKey("ID")) {
      ID = (String) propertiesMap.get("ID");
    }
    if (propertiesMap.containsKey("configurations")) {
      configurations = new VNFConfigurations(propertiesMap.get("configurations"));
    }
    if (propertiesMap.containsKey("endpoint")) {
      endpoint = (String) propertiesMap.get("endpoint");
    }
    if (propertiesMap.containsKey("type")) {
      type = (String) propertiesMap.get("type");
    }
    /*if (propertiesMap.containsKey("auto_scale_policy")) {
      auto_scale_policy = new VNFAutoscaling(propertiesMap.get("auto_scale_policy"));
    }*/
    if (propertiesMap.containsKey("metadata")) {
      metadata = (Map<String, String>) propertiesMap.get("metadata");
    }
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public String getID() {
    return ID;
  }

  public void setID(String ID) {
    this.ID = ID;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getVnfPackageLocation() {
    return vnfPackageLocation;
  }

  public void setVnfPackageLocation(String vnfPackageLocation) {
    this.vnfPackageLocation = vnfPackageLocation;
  }

  public ArrayList<HashMap<String, String>> getDeploymentFlavour() {
    return deploymentFlavour;
  }

  public void setDeploymentFlavour(ArrayList<HashMap<String, String>> deploymentFlavour) {
    this.deploymentFlavour = deploymentFlavour;
  }

  public VNFConfigurations getConfigurations() {
    return configurations;
  }

  public void setConfigurations(VNFConfigurations configurations) {
    this.configurations = configurations;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {

    return "VNFP : "
        + "\n"
        + "id : "
        + getID()
        + "\n"
        + "vendor : "
        + vendor
        + "\n"
        + "version : "
        + version
        + "\n"
        + "package : "
        + vnfPackageLocation
        + "\n"
        + "depl flavour : "
        + deploymentFlavour
        + "\n";
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }
}
