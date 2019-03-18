package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common;

import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor.*;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.VDUComputeNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.VDUVirtualBlockStorageNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class VnfdBuilder {

    private static final Logger log = LoggerFactory.getLogger(VnfdBuilder.class);

    private final File logo;
    private OsmVNFPackage osmPackage;
    private int interfacesPosition;

    public VnfdBuilder(File logo) {

        this.logo = logo;
        this.interfacesPosition = 0;
    }

    private ConnectionPoint makeCP(String cpName) {

        ConnectionPoint osmCp = new ConnectionPoint();
        osmCp.setName(cpName);
        osmCp.setType("VPORT");
        osmCp.setPortSecurityEnabled(false);

        return osmCp;
    }

    private ManagementInterface makeMgmtInterface(Map<String, VnfExtCpNode> cpNodes) {

        ManagementInterface mgmtInt = new ManagementInterface();
        for (Map.Entry<String, VnfExtCpNode> cpNode : cpNodes.entrySet()) {
            List<String> exVirtualLinks = cpNode.getValue().getRequirements().getExternalVirtualLink();
            for (String exVirtualLink : exVirtualLinks)
                if (exVirtualLink.endsWith("_mgmt") || exVirtualLink.startsWith("mgmt_"))
                    mgmtInt.setCp(cpNode.getKey());
        }

        //Set first cp as management
        if (mgmtInt.getCp() == null)
            mgmtInt.setCp(cpNodes.keySet().iterator().next());

        return mgmtInt;
    }

    private Interface makeVDUInterface(String cpName, VnfExtCpNode cpNode) {

        Interface osmVduInterface = new Interface();
        osmVduInterface.setExtConnPointRef(cpName);
        osmVduInterface.setName(cpName);
        osmVduInterface.setType("EXTERNAL");
        osmVduInterface.setVirtualInterface(new VirtualInterface("VIRTIO"));
        osmVduInterface.setPosition(++interfacesPosition);
        List<String> exVirtualLinks = cpNode.getRequirements().getExternalVirtualLink();
        for (String exVirtualLink : exVirtualLinks)
            if (exVirtualLink.endsWith("_mgmt") || exVirtualLink.startsWith("mgmt_"))
                osmVduInterface.setMgmtInterface(true);

        return osmVduInterface;
    }

    private VDU makeVDU(VDUComputeNode vdu, Map<String, VDUVirtualBlockStorageNode> blockStorageNodes, List<Interface> interfaces) {

        VDU osmVdu = new VDU();
        String requiredBlockStorageName = vdu.getRequirements().getVirtualStorage().get(0);
        VDUVirtualBlockStorageNode requiredBlockStorage = blockStorageNodes.get(requiredBlockStorageName);
        osmVdu.setId(vdu.getProperties().getName());
        osmVdu.setName(vdu.getProperties().getName());
        osmVdu.setDescription(vdu.getProperties().getName());
        osmVdu.setCount(vdu.getProperties().getVduProfile().getMinNumberOfInstances());
        osmVdu.setImage(requiredBlockStorage.getProperties().getSwImageData().getImageName());
        osmVdu.setInterfaces(interfaces);
        osmVdu.setVmFlavor(new VMFlavor(vdu.getCapabilities().getVirtualCompute().getProperties().getVirtualCpu().getNumVirtualCpu(),
                vdu.getCapabilities().getVirtualCompute().getProperties().getVirtualMemory().getVirtualMemSize(),
                requiredBlockStorage.getProperties().getVirtualBlockStorageData().getSizeOfStorage()));

        return osmVdu;
    }

    private List<ScalingGroupDescriptor> makeScalingGroupDescriptor(VDUComputeNode vdu) {

        List<ScalingGroupDescriptor> descriptors = new ArrayList<>();
        ScalingGroupDescriptor scalingByOne = new ScalingGroupDescriptor();
        scalingByOne.setMinInstanceCount(0);
        scalingByOne.setMaxInstanceCount(vdu.getProperties().getVduProfile().getMaxNumberOfInstances() - vdu.getProperties().getVduProfile().getMinNumberOfInstances());
        scalingByOne.setName("scale_by_one");
        List<ScalingPolicy> scalingPolicies = new ArrayList<>();
        ScalingPolicy scalingPolicy = new ScalingPolicy("manual_scale", "manual");
        scalingPolicies.add(scalingPolicy);
        scalingByOne.setScalingPolicies(scalingPolicies);
        List<VduReference> vdusRef = new ArrayList<>();
        VduReference vduRef = new VduReference(vdu.getProperties().getName());
        vdusRef.add(vduRef);
        scalingByOne.setVduList(vdusRef);
        descriptors.add(scalingByOne);

        return descriptors;
    }

    public void parseDescriptorTemplate(DescriptorTemplate template, MANOType manoType) throws MalformattedElementException {

        if (template.getTopologyTemplate().getVNFNodes().size() == 0)
            throw new IllegalArgumentException("No VNF defined");

        if (template.getTopologyTemplate().getVNFNodes().size() != 1)
            throw new IllegalArgumentException("Too many VNF defined");

        Map<String, VnfExtCpNode> cpNodes = template.getTopologyTemplate().getVnfExtCpNodes();
        if (cpNodes.size() == 0)
            throw new IllegalArgumentException("No Connection Points defined");

        List<ConnectionPoint> osmCps = cpNodes.entrySet()
                .stream()
                .map(e -> makeCP(e.getKey()))
                .collect(Collectors.toList());

        ManagementInterface osmMgmtInterface = makeMgmtInterface(cpNodes);

        List<Interface> osmVduInterfaces = cpNodes.entrySet()
                .stream()
                .map(e -> makeVDUInterface(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        Map<String, VDUVirtualBlockStorageNode> blockStorageNodes = template.getTopologyTemplate().getVDUBlockStorageNodes();
        if (blockStorageNodes.size() == 0)
            throw new IllegalArgumentException("No Blocks Storage defined");

        Map<String, VDUComputeNode> vduComputeNodes = template.getTopologyTemplate().getVDUComputeNodes();
        if (vduComputeNodes.size() == 0)
            throw new IllegalArgumentException("No VDU defined");

        List<VDU> osmVdus = vduComputeNodes.entrySet()
                .stream()
                .map(e -> makeVDU(e.getValue(), blockStorageNodes, osmVduInterfaces))
                .collect(Collectors.toList());

        VDUComputeNode vdu = vduComputeNodes.values().iterator().next();// For the moment considers only one VDU per VNF
        List<ScalingGroupDescriptor> scalingGroupDescriptors = null;
        if ((vdu.getProperties().getVduProfile().getMaxNumberOfInstances() - vdu.getProperties().getVduProfile().getMinNumberOfInstances()) > 0)
            scalingGroupDescriptors = makeScalingGroupDescriptor(vdu);

        // Create VNFD
        VNFNode vnfd = template.getTopologyTemplate().getVNFNodes().values().iterator().next();
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
        osmPackage = new OsmVNFPackage().setVnfdCatalog(vnfdCatalog);
    }

    public OsmVNFPackage getPackage() {
        return osmPackage;
    }
}
