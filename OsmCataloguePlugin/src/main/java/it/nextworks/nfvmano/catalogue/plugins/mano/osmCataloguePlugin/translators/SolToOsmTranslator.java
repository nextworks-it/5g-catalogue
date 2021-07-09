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
package it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.translators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANOType;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.sol006.*;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.templates.VirtualLinkPair;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.VDUComputeNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.VDUVirtualBlockStorageNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpNode;
import it.nextworks.nfvmano.libs.osmr10DataModels.nsd.OsmNsdSol006;
import it.nextworks.nfvmano.libs.osmr10DataModels.vnfd.OsmVduSol006;
import it.nextworks.nfvmano.libs.osmr10DataModels.vnfd.OsmVnfdSol006;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.nsDescriptor.*;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.vnfDescriptor.*;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.vnfDescriptor.ConnectionPoint;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.vnfDescriptor.ScalingGroupDescriptor;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.vnfDescriptor.ScalingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;



public class SolToOsmTranslator {

    public static class OsmVnfdSol006Wrapper {

        @JsonProperty("vnfd")
        private final OsmVnfdSol006 vnfd;

        @JsonCreator
        public OsmVnfdSol006Wrapper(@JsonProperty("vnfd") OsmVnfdSol006 vnfd) { this.vnfd = vnfd; }

        public Vnfd getVnfd() { return vnfd; }
    }

    public static class OsmNsWrapper {

        @JsonProperty("nsd")
        private final OsmNsdsSol006Wrapper nsd;

        @JsonCreator
        public OsmNsWrapper(@JsonProperty("nsd") OsmNsdsSol006Wrapper nsd) { this.nsd = nsd; }

        public OsmNsdsSol006Wrapper getNsd() { return nsd; }
    }

    public static class OsmNsdsSol006Wrapper {

        @JsonProperty("nsd")
        private List<OsmNsdSol006> nsds;

        @JsonCreator
        public OsmNsdsSol006Wrapper(@JsonProperty("nsd") List<OsmNsdSol006> nsds) { this.nsds = nsds; }

        public void addNsd(OsmNsdSol006 nsd) {
            if(nsds == null)
                nsds = new ArrayList<>();

            nsds.add(nsd);
        }

        public List<OsmNsdSol006> getNsds() { return nsds; }
    }

    private static final Logger log = LoggerFactory.getLogger(SolToOsmTranslator.class);
    private static int interfacePosition = 0;

    /* VNF private functions */

    private static VNFDConnectionPointReference makeCPRef(String vnfdId, int memberIndex, String cpId) {
        VNFDConnectionPointReference cpr = new VNFDConnectionPointReference();
        cpr.setVnfdIdReference(vnfdId);
        cpr.setIndexReference(memberIndex);
        cpr.setVnfdConnectionPointReference(cpId);
        return cpr;
    }

    private static VLD makeVld(Map.Entry<String, NsVirtualLinkNode> vl, List<DescriptorTemplate> vnfds, Map<String, Map<ConstituentVNFD, String>> vlToVnfMapping, boolean useVimNetworkName) {
        VLD vld = new VLD();
        vld.setId(vl.getKey());
        vld.setName(vl.getKey());
        vld.setShortName(vl.getKey());
        if(vl.getValue().getProperties().isMgmtNet() && isMgmtNetwork(vlToVnfMapping.getOrDefault(vl.getKey(), Collections.emptyMap()), vnfds))
            vld.setMgmtNetwork(true);
        vld.setType("ELAN");
        if(useVimNetworkName)
            vld.setVimNetworkName(vl.getKey());
        vld.setVnfdConnectionPointReferences(
                vlToVnfMapping.getOrDefault(vl.getKey(), Collections.emptyMap()).entrySet()
                        .stream()
                        .map(e -> makeCPRef(
                                e.getKey().getVnfdIdentifierReference(),
                                e.getKey().getMemberVNFIndex(),
                                e.getValue()
                        ))
                        .collect(Collectors.toList())
        );

        return vld;
    }

    private static boolean isMgmtNetwork(Map<ConstituentVNFD, String> cpToVnfMapping, List<DescriptorTemplate> vnfds){
        boolean isMgmt = true;
        for(Map.Entry<ConstituentVNFD, String> entry : cpToVnfMapping.entrySet()){
            if(!isMgmt)
                break;
            for(DescriptorTemplate vnfd : vnfds){
                if(vnfd.getMetadata().getDescriptorId().equals(entry.getKey().getVnfdIdentifierReference())){
                    try {
                        Map<String, VnfExtCpNode> cpNodes = vnfd.getTopologyTemplate().getVnfExtCpNodes();
                        VnfExtCpNode cpNode = cpNodes.get(entry.getValue());
                        if(cpNode != null){
                            if(cpNode.getProperties().getVirtualNetworkInterfaceRequirements().isEmpty()){
                                isMgmt = false;
                                break;
                            }
                            Map<String, String> interfaceRequirements = cpNode.getProperties().getVirtualNetworkInterfaceRequirements().get(0).getNetworkInterfaceRequirements();
                            if(!interfaceRequirements.containsKey("isManagement") || !interfaceRequirements.get("isManagement").equalsIgnoreCase("true")){
                                isMgmt = false;
                                break;
                            }
                        }
                    }catch (MalformattedElementException e){
                        log.debug(null, e);
                    }
                }
            }
        }

        return isMgmt;
    }

    /* VNF private fuctions */

    private static ConnectionPoint makeCP(String cpName) {
        ConnectionPoint osmCp = new ConnectionPoint();
        osmCp.setName(cpName);
        osmCp.setType("VPORT");
        osmCp.setPortSecurityEnabled(false);

        return osmCp;
    }

    private static ManagementInterface makeMgmtInterface(Map<String, VnfExtCpNode> cpNodes) {

        ManagementInterface mgmtInt = new ManagementInterface();
        for (Map.Entry<String, VnfExtCpNode> cpNode : cpNodes.entrySet()) {
            //TODO check if getting the first element is correct
            if(cpNode.getValue().getProperties().getVirtualNetworkInterfaceRequirements().isEmpty())
                continue;
            Map<String, String> interfaceRequirements = cpNode.getValue().getProperties().getVirtualNetworkInterfaceRequirements().get(0).getNetworkInterfaceRequirements();
            if(interfaceRequirements.containsKey("isManagement") && interfaceRequirements.get("isManagement").equalsIgnoreCase("true")){
                mgmtInt.setCp(cpNode.getKey());
                break;//considering only the first cp found
            }
            /*
            List<String> exVirtualLinks = cpNode.getValue().getRequirements().getExternalVirtualLink();
            for (String exVirtualLink : exVirtualLinks)
                if (exVirtualLink.endsWith("_mgmt") || exVirtualLink.startsWith("mgmt_") || exVirtualLink.equalsIgnoreCase("default"))
                    mgmtInt.setCp(cpNode.getKey());
             */
        }

        //Set first cp as management
        if (mgmtInt.getCp() == null)
            mgmtInt.setCp(cpNodes.keySet().iterator().next());//set the first connection point a management if no defined

        return mgmtInt;
    }

    private static Interface makeVDUInterface(String cpName, VnfExtCpNode cpNode, String mgmtCp) {
        Interface osmVduInterface = new Interface();
        osmVduInterface.setExtConnPointRef(cpName);
        osmVduInterface.setName(cpName);
        osmVduInterface.setType("EXTERNAL");
        VirtualInterface virtualInterface = new VirtualInterface();
        virtualInterface.setType("VIRTIO");
        osmVduInterface.setVirtualInterface(virtualInterface);
        osmVduInterface.setPosition(++interfacePosition);
        if(cpName.equals(mgmtCp))
            osmVduInterface.setMgmtInterface(true);
        /*
        List<String> exVirtualLinks = cpNode.getRequirements().getExternalVirtualLink();
        for (String exVirtualLink : exVirtualLinks)
            if (exVirtualLink.endsWith("_mgmt") || exVirtualLink.startsWith("mgmt_") || exVirtualLink.equalsIgnoreCase("default"))
                osmVduInterface.setMgmtInterface(true);
        */
        return osmVduInterface;
    }

    private static VDU makeVDU(VDUComputeNode vdu, Map<String, VDUVirtualBlockStorageNode> blockStorageNodes, List<Interface> interfaces) {
        VDU osmVdu = new VDU();
        String requiredBlockStorageName = vdu.getRequirements().getVirtualStorage().get(0);
        VDUVirtualBlockStorageNode requiredBlockStorage = blockStorageNodes.get(requiredBlockStorageName);
        osmVdu.setId(vdu.getProperties().getName());
        osmVdu.setName(vdu.getProperties().getName());
        osmVdu.setDescription(vdu.getProperties().getName());
        osmVdu.setCount(vdu.getProperties().getVduProfile().getMinNumberOfInstances());
        if(vdu.getProperties().getSwImageData() == null)
            osmVdu.setImage(requiredBlockStorage.getProperties().getSwImageData().getImageName());
        else
            osmVdu.setImage(vdu.getProperties().getSwImageData().getImageName());
        osmVdu.setInterfaces(interfaces);
        VMFlavor flavor = new VMFlavor();
        flavor.setMemoryMb(vdu.getCapabilities().getVirtualCompute().getProperties().getVirtualMemory().getVirtualMemSize());
        flavor.setStorageGb(requiredBlockStorage.getProperties().getVirtualBlockStorageData().getSizeOfStorage());
        flavor.setVcpuCount(vdu.getCapabilities().getVirtualCompute().getProperties().getVirtualCpu().getNumVirtualCpu());
        osmVdu.setVmFlavor(flavor);

        return osmVdu;
    }

    private static List<ScalingGroupDescriptor> makeScalingGroupDescriptor(VDUComputeNode vdu) {
        List<ScalingGroupDescriptor> descriptors = new ArrayList<>();
        ScalingGroupDescriptor scalingByOne = new ScalingGroupDescriptor();
        scalingByOne.setMinInstanceCount(0);
        scalingByOne.setMaxInstanceCount(vdu.getProperties().getVduProfile().getMaxNumberOfInstances() - vdu.getProperties().getVduProfile().getMinNumberOfInstances());
        scalingByOne.setName("scale_by_one");
        List<ScalingPolicy> scalingPolicies = new ArrayList<>();
        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setName("manual_scale");
        scalingPolicy.setScalingType("manual");
        scalingPolicy.setEnabled(true);
        scalingPolicies.add(scalingPolicy);
        scalingByOne.setScalingPolicies(scalingPolicies);
        List<VduReference> vdusRef = new ArrayList<>();
        VduReference vduRef = new VduReference();
        vduRef.setVduIdRef(vdu.getProperties().getName());
        vdusRef.add(vduRef);
        scalingByOne.setVduList(vdusRef);
        descriptors.add(scalingByOne);

        return descriptors;
    }

    public static OsmNSPackage generateNsDescriptor(DescriptorTemplate nsdSol, List<DescriptorTemplate> vnfds, File logo, boolean useVimNetworkName, MANOType manoType) throws MalformattedElementException {
        if (!(nsdSol.getTopologyTemplate().getNSNodes().size() == 1)) {
            throw new IllegalArgumentException("Too many Nsds");
        }
        NSNode nsNode = nsdSol.getTopologyTemplate().getNSNodes().values().iterator().next();

        Map<String, Map<ConstituentVNFD, String>> vlToVnfMapping = new HashMap<>();

        List<ConstituentVNFD> constituentVnfds = new ArrayList<>();

        Map<String, VNFNode> vnfNodes = nsdSol.getTopologyTemplate().getVNFNodes();

        for (Map.Entry<String, VNFNode> vnfNode : vnfNodes.entrySet()) {
            String vnfdId = vnfNode.getValue().getProperties().getDescriptorId();

            for (DescriptorTemplate vnfd : vnfds) {
                String id = vnfd.getTopologyTemplate().getVNFNodes().entrySet().iterator().next().getValue().getProperties().getDescriptorId();
                if (vnfdId.equalsIgnoreCase(id)) {
                    log.debug("Matching vnfdId {} in NSD with vnfdId {} in VNFDs list", vnfdId, id);

                    ConstituentVNFD constituentVnfd = new ConstituentVNFD();
                    constituentVnfd.setVnfdIdentifierReference(vnfdId);
                    constituentVnfds.add(constituentVnfd);

                    Map<String, String> vLinksAssociations = vnfNode.getValue().getRequirements().getVirtualLink();
                    for (Map.Entry<String, String> entry : vLinksAssociations.entrySet()) {
                        vlToVnfMapping.putIfAbsent(entry.getValue(), new HashMap<>());
                        List<VirtualLinkPair> pairs = vnfd.getTopologyTemplate().getSubstituitionMappings().getRequirements().getVirtualLink();
                        for (VirtualLinkPair pair : pairs) {
                            if (pair.getVl().equalsIgnoreCase(entry.getKey())) {
                                String cpId = pair.getCp();
                                vlToVnfMapping.get(entry.getValue()).put(constituentVnfd, cpId);
                            }
                        }
                    }
                }
            }
        }

        /*List<ConstituentVnfd> constituentVnfds = dt.getTopologyTemplate().getVNFNodes().values()
                .stream()
                .map(vnf -> {
                            ConstituentVnfd output = new ConstituentVnfd()
                                    .setVnfdIdRef(vnf.getProperties().getDescriptorId());

                            vnf.getRequirements().getVirtualLink().forEach(
                                    input -> {
                                        String[] split = input.split("/");
                                        if (!(split.length == 2)) {
                                            throw new IllegalArgumentException(String.format(
                                                    "Illegal vl requirement %s: wrong split",
                                                    input
                                            ));
                                        }
                                        String vlId = split[0];
                                        String cpId = split[1];
                                        vlToVnfMapping.putIfAbsent(vlId, new HashMap<>());
                                        vlToVnfMapping.get(vlId).put(output, cpId);
                                    }
                            );
                            return output;
                        }
                )
                .collect(Collectors.toList());*/
        for (int i = 0; i < constituentVnfds.size(); i++) {
            log.debug("Constituent VNFD: " + constituentVnfds.get(i).getVnfdIdentifierReference());
            constituentVnfds.get(i).setMemberVNFIndex(i + 1);
        }

        List<VLD> vlds = nsdSol.getTopologyTemplate().getNsVirtualLinkNodes().entrySet()
                .stream()
                .map(e -> makeVld(e, vnfds, vlToVnfMapping, useVimNetworkName))
                .collect(Collectors.toList());
        //check if a mgmt is set, if not set the first vld found ad mgmt
        List<Boolean> isMgmtNetworks = vlds.stream().map(VLD::isMgmtNetwork).collect(Collectors.toList());
        if(!isMgmtNetworks.contains(true))
            vlds.get(0).setMgmtNetwork(true);
        List<NSDescriptor> nsds = new ArrayList<>();
        NSDescriptor osmNsd = new NSDescriptor();
        osmNsd.setId(nsNode.getProperties().getDescriptorId());
        osmNsd.setShortName(nsNode.getProperties().getName());
        osmNsd.setDescription(nsdSol.getDescription()); // TODO: this is not ideal.
        osmNsd.setVendor(nsNode.getProperties().getDesigner());
        osmNsd.setVersion(nsNode.getProperties().getVersion());
        osmNsd.setLogo(logo.getName()); // TODO get logo?
        osmNsd.setConstituentVNFDs(constituentVnfds);
        osmNsd.setVldList(vlds);
        if (manoType == MANOType.OSMR3)
            osmNsd.setName(nsNode.getProperties().getDescriptorId());
        else if (manoType == MANOType.OSMR4)
            osmNsd.setName(nsNode.getProperties().getName());
        nsds.add(osmNsd);

        NSDCatalog nsdCatalog = new NSDCatalog();
        nsdCatalog.setNsds(nsds);
        return new OsmNSPackage().setNsdCatalog(nsdCatalog);
    }

    public static OsmVNFPackage generateVnfDescriptor(DescriptorTemplate vnfdSol, File logo, MANOType manoType) throws MalformattedElementException {
        if (vnfdSol.getTopologyTemplate().getVNFNodes().size() == 0)
            throw new IllegalArgumentException("No VNF defined");

        if (vnfdSol.getTopologyTemplate().getVNFNodes().size() != 1)
            throw new IllegalArgumentException("Too many VNF defined");

        Map<String, VnfExtCpNode> cpNodes = vnfdSol.getTopologyTemplate().getVnfExtCpNodes();
        if (cpNodes.size() == 0)
            throw new IllegalArgumentException("No Connection Points defined");

        List<ConnectionPoint> osmCps = cpNodes.keySet()
                .stream()
                .map(SolToOsmTranslator::makeCP)
                .collect(Collectors.toList());

        ManagementInterface osmMgmtInterface = makeMgmtInterface(cpNodes);

        //clean static counter
        interfacePosition = 0;
        List<Interface> osmVduInterfaces = cpNodes.entrySet()
                .stream()
                .map(e -> makeVDUInterface(e.getKey(), e.getValue(), osmMgmtInterface.getCp()))
                .collect(Collectors.toList());

        Map<String, VDUVirtualBlockStorageNode> blockStorageNodes = vnfdSol.getTopologyTemplate().getVDUBlockStorageNodes();
        if (blockStorageNodes.size() == 0)
            throw new IllegalArgumentException("No Blocks Storage defined");

        Map<String, VDUComputeNode> vduComputeNodes = vnfdSol.getTopologyTemplate().getVDUComputeNodes();
        if (vduComputeNodes.size() == 0)
            throw new IllegalArgumentException("No VDU defined");

        List<VDU> osmVdus = vduComputeNodes.values()
                .stream()
                .map(vduComputeNode -> makeVDU(vduComputeNode, blockStorageNodes, osmVduInterfaces))
                .collect(Collectors.toList());

        VDUComputeNode vdu = vduComputeNodes.values().iterator().next();// For the moment considers only one VDU per VNF
        List<ScalingGroupDescriptor> scalingGroupDescriptors = null;
        if ((vdu.getProperties().getVduProfile().getMaxNumberOfInstances() - vdu.getProperties().getVduProfile().getMinNumberOfInstances()) > 0)
            scalingGroupDescriptors = makeScalingGroupDescriptor(vdu);

        // Create VNFD
        VNFNode vnfd = vnfdSol.getTopologyTemplate().getVNFNodes().values().iterator().next();
        List<VNFDescriptor> vnfds = new ArrayList<>();
        VNFDescriptor osmVnfd = new VNFDescriptor();

        osmVnfd.setId(vnfd.getProperties().getDescriptorId());

        if (manoType == MANOType.OSMR3)
            osmVnfd.setName(vnfd.getProperties().getDescriptorId());
        else if (manoType == MANOType.OSMR4)
            osmVnfd.setName(vnfd.getProperties().getProductName());

        osmVnfd.setShortName(vnfd.getProperties().getProductName());
        osmVnfd.setDescription(vnfd.getProperties().getProductInfoDescription());
        osmVnfd.setVendor(vnfd.getProperties().getProvider());
        osmVnfd.setVersion(vnfd.getProperties().getDescriptorVersion());
        osmVnfd.setLogo(logo.getName());//TODO add meaningful logo?
        osmVnfd.setConnectionPoints(osmCps);
        osmVnfd.setManagementInterface(osmMgmtInterface);
        osmVnfd.setVduList(osmVdus);
        osmVnfd.setScalingGroupDescriptor(scalingGroupDescriptors);

        // Set cloud-init file if any
        if (vnfd.getInterfaces() != null &&
                vnfd.getInterfaces().getVnflcm() != null &&
                vnfd.getInterfaces().getVnflcm().getInstantiate() != null)
            osmVdus.get(0).setCloudInitFile(vnfd.getInterfaces().getVnflcm().getInstantiate().getImplementation());

        vnfds.add(osmVnfd);

        // Create VNF Package
        VNFDCatalog vnfdCatalog = new VNFDCatalog();
        vnfdCatalog.setVnfd(vnfds);
        return new OsmVNFPackage().setVnfdCatalog(vnfdCatalog);
    }

    private static void makeDf(OsmVnfdSol006 osmVnfd) {
        List<VnfdDf> vnfdDfs = osmVnfd.getDf();
        if(vnfdDfs == null)
            vnfdDfs = new ArrayList<>();
        if(vnfdDfs.isEmpty()) {
            String vduId = osmVnfd.getVdu().get(0).getId();
            VnfdDf vnfdDf = new VnfdDf()
                    .id("default_df")
                    .vduProfile(Collections.singletonList(new VnfdDfVduProfileItem()
                            .id(vduId)
                            .minNumberOfInstances("1")
                            .maxNumberOfInstances("1")))
                    .instantiationLevel(Collections.singletonList(new VnfdInstantiationlevel()
                            .id("il-1")
                            .vduLevel(Collections.singletonList(new VnfdVdulevel().vduId(vduId).numberOfInstances("1")))))
                    .defaultInstantiationLevel("il-1");

            osmVnfd.setDf(Collections.singletonList(vnfdDf));
        } else {
            for(VnfdDf vnfdDf : vnfdDfs) {
                VnfdLcmoperationsconfiguration vnfdLcmoperationsconfiguration = vnfdDf.getLcmOperationsConfiguration();
                if(vnfdLcmoperationsconfiguration == null)
                    continue;
                VnfdLcmoperationsconfigurationOperatevnfopconfig operateVnfOpConfig =
                        vnfdLcmoperationsconfiguration.getOperateVnfOpConfig();
                if(operateVnfOpConfig == null)
                    continue;
                // TODO add Day1-2 object for operateVnfOpConfig
            }
        }
    }

    private static OsmVduSol006 convertVduToOsmVdu(VnfdVdu vdu) {
        OsmVduSol006 osmVdu = new OsmVduSol006();

        osmVdu.setVirtualStorageDesc(vdu.getVirtualStorageDesc());
        osmVdu.setBootData(vdu.getBootData());
        osmVdu.setDescription(vdu.getDescription());
        osmVdu.setVirtualComputeDesc(vdu.getVirtualComputeDesc());
        osmVdu.setIntCpd(vdu.getIntCpd());
        osmVdu.setConfigurableProperties(vdu.getConfigurableProperties());
        osmVdu.setId(vdu.getId());
        osmVdu.setBootOrder(vdu.getBootOrder());
        osmVdu.setNfviConstraint(vdu.getNfviConstraint());
        osmVdu.setMonitoringParameter(vdu.getMonitoringParameter());
        osmVdu.setSwImageDesc(vdu.getSwImageDesc());
        osmVdu.setName(vdu.getName());

        return osmVdu;
    }

    private static void setCloudInit(OsmVnfdSol006 osmVnfd, Map<String, File> cloudInitMap) {
        List<VnfdVdu> osmVdus = new ArrayList<>();

        for (VnfdVdu vdu : osmVnfd.getVdu()) {
            OsmVduSol006 osmVdu = convertVduToOsmVdu(vdu);
            String vduId = osmVdu.getId();
            if (cloudInitMap.containsKey(vduId))
                osmVdu.setCloudInitFile(cloudInitMap.get(vduId).getName());
            else
                log.debug("cloud.init mapping for VDU ID " + vduId + " not found in manifest, cloud init file not bound.");
            osmVdus.add(osmVdu);
        }

        osmVnfd.getVdu().clear();
        osmVnfd.getVdu().addAll(osmVdus);
    }

    public static OsmVnfdSol006Wrapper generateVnfDescriptor(Vnfd vnfd, Map<String, File> cloudInitMap)
            throws MalformattedElementException {

        OsmVnfdSol006 osmVnfd = new OsmVnfdSol006(vnfd);

        osmVnfd.setDescription(osmVnfd.getProductInfoDescription());

        List<ExtCpd> extCpds = osmVnfd.getExtCpd();
        if(extCpds == null || extCpds.isEmpty())
            throw new MalformattedElementException("No External Connection Points defined");
        List<String> mgmtCps = new ArrayList<>();
        String mgmtId = null;
        int i = 0;
        for(ExtCpd extCpd : extCpds) {
            List<VirtualNetworkInterfaceRequirementSchema> virtualNetworkInterfaceRequirements =
                    extCpd.getVirtualNetworkInterfaceRequirementSchemas();
            if(virtualNetworkInterfaceRequirements == null || virtualNetworkInterfaceRequirements.isEmpty())
                continue;

            int j = 0;
            for(VirtualNetworkInterfaceRequirementSchema virtualNetworkInterfaceRequirement : virtualNetworkInterfaceRequirements) {
                if(virtualNetworkInterfaceRequirement.getName() == null ||
                        virtualNetworkInterfaceRequirement.getName().isEmpty())
                    virtualNetworkInterfaceRequirement.setName("virtual-network-interface-requirement " + j + " of ext-cpd " + i);
                j++;
            }

            VirtualNetworkInterfaceRequirementSchema virtualNetworkInterfaceRequirement =
                    virtualNetworkInterfaceRequirements.get(0);
            List<NetworkInterfaceRequirementsSchema> networkInterfaceRequirements =
                    virtualNetworkInterfaceRequirement.getNetworkInterfaceRequirements();
            mgmtCps = networkInterfaceRequirements
                    .stream()
                    .filter(nir -> nir.getKey().equals("isManagement") &&
                            nir.getValue().equalsIgnoreCase("true"))
                    .map(NetworkInterfaceRequirementsSchema::getValue)
                    .collect(Collectors.toList());

            mgmtId = extCpd.getId();
            i++;
        }

        if(mgmtCps.size() == 0)
            throw new MalformattedElementException("Please define a management Connection Point");
        else if (mgmtCps.size() > 1)
            throw new MalformattedElementException("Multiple management Connection Points are not allowed");

        osmVnfd.setMgmtCp(mgmtId);

        makeDf(osmVnfd);

        if(!cloudInitMap.isEmpty())
            setCloudInit(osmVnfd, cloudInitMap);

        return new OsmVnfdSol006Wrapper(osmVnfd);
    }

    public static OsmNsWrapper generateNsDescriptor(Nsd nsd) {
        OsmNsdSol006 osmNsd = new OsmNsdSol006(nsd);
        osmNsd.setDescription(osmNsd.getName() + " version " + osmNsd.getVersion());
        return new OsmNsWrapper(new OsmNsdsSol006Wrapper(Collections.singletonList(osmNsd)));
    }
}
