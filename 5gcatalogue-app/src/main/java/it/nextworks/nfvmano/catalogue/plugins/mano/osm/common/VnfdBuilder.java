package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.vnfDescriptor.*;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.VDUComputeNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.VDUVirtualBlockStorageNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


public class VnfdBuilder {

    private static final Logger log = LoggerFactory.getLogger(VnfdBuilder.class);
    private final File defaultLogo;
    private OsmVNFPackage osmPackage;
    private DescriptorTemplate dt;
    private int interfacesPosition;

    @Autowired
    private FileSystemStorageService storageService;

    public VnfdBuilder(File defaultLogo) {
        this.defaultLogo = defaultLogo;
        this.interfacesPosition = 0;
    }

    private ConnectionPoint makeCP(String cpName){
        ConnectionPoint osmCp = new ConnectionPoint();
        osmCp.setName(cpName);
        osmCp.setType("VPORT");
        osmCp.setPortSecurityEnabled(false);
        return osmCp;
    }

    private ManagementInterface makeMgmtInterface(Map<String, VnfExtCpNode> cpNodes){
        ManagementInterface mgmtInt = new ManagementInterface();
        for (Map.Entry<String, VnfExtCpNode> cpNode : cpNodes.entrySet()){
            List<String> exVirtualLinks = cpNode.getValue().getRequirements().getExternalVirtualLink();
            for(String exVirtualLink : exVirtualLinks)
                if(exVirtualLink.endsWith("_mgmt"))
                    mgmtInt.setCp(cpNode.getKey());
        }
        return mgmtInt;
    }

    private Interface makeVDUInterface(String cpName, VnfExtCpNode cpNode){
        Interface osmVduInterface = new Interface();
        osmVduInterface.setExtConnPointRef(cpName);
        osmVduInterface.setName(cpName);
        osmVduInterface.setType("EXTERNAL");
        osmVduInterface.setVirtualInterface(new VirtualInterface("VIRTIO"));
        osmVduInterface.setPosition(++interfacesPosition);
        List<String> exVirtualLinks = cpNode.getRequirements().getExternalVirtualLink();
        for(String exVirtualLink : exVirtualLinks)
            if(exVirtualLink.endsWith("_mgmt"))
                osmVduInterface.setMgmtInterface(true);
        return osmVduInterface;
    }

    private VDU makeVDU (VDUComputeNode vdu, Map<String, VDUVirtualBlockStorageNode> blockStorageNodes, List<Interface> interfaces){
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

    private List<ScalingGroupDescriptor> makeScalingGroupDescriptor(VDUComputeNode vdu){
        List<ScalingGroupDescriptor> descriptors = new ArrayList<>();
        ScalingGroupDescriptor scalingByOne = new ScalingGroupDescriptor();
        scalingByOne.setMinInstanceCount(vdu.getProperties().getVduProfile().getMinNumberOfInstances() - 1);
        scalingByOne.setMaxInstanceCount(vdu.getProperties().getVduProfile().getMaxNumberOfInstances() - 1);
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
        dt = template;

        if (!(dt.getTopologyTemplate().getVNFNodes().size() == 1)) {
            throw new IllegalArgumentException("Too many Vnfds");
        }

        Map<String, VnfExtCpNode> cpNodes = dt.getTopologyTemplate().getVnfExtCpNodes();
        List<ConnectionPoint> osmCps = cpNodes.entrySet()
                .stream()
                .map(e -> makeCP(e.getKey()))
                .collect(Collectors.toList());

        ManagementInterface osmMgmtInterface = makeMgmtInterface(cpNodes);

        List<Interface> osmVduInterfaces = cpNodes.entrySet()
                .stream()
                .map(e -> makeVDUInterface(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        Map<String, VDUVirtualBlockStorageNode> blockStorageNodes = dt.getTopologyTemplate().getVDUBlockStorageNodes();
        Map<String, VDUComputeNode> vduComputeNodes = dt.getTopologyTemplate().getVDUComputeNodes();
        List<VDU> osmVdus = vduComputeNodes.entrySet()
                .stream()
                .map(e -> makeVDU(e.getValue(), blockStorageNodes, osmVduInterfaces))
                .collect(Collectors.toList());

        // For the moment considers only one Vdu per VNF
        VDUComputeNode vdu = vduComputeNodes.values().iterator().next();
        List<ScalingGroupDescriptor> scalingGroupDescriptors = null;
        if((vdu.getProperties().getVduProfile().getMaxNumberOfInstances() - vdu.getProperties().getVduProfile().getMinNumberOfInstances()) > 0)
            scalingGroupDescriptors = makeScalingGroupDescriptor(vdu);

        VNFNode vnfd = dt.getTopologyTemplate().getVNFNodes().values().iterator().next();
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
        osmVnfd.setVersion(vnfd.getProperties().getSoftwareVersion());
        osmVnfd.setLogo(defaultLogo.getName());//TODO add meaningful logo?
        osmVnfd.setConnectionPoints(osmCps);
        osmVnfd.setManagementInterface(osmMgmtInterface);
        // Set cloud-init file
        osmVdus.get(0).setCloudInitFile(vnfd.getInterfaces().getVnflcm().getInstantiate().getImplementation());
        osmVnfd.setVduList(osmVdus);
        osmVnfd.setScalingGroupDescriptor(scalingGroupDescriptors);

        vnfds.add(osmVnfd);

        VNFDCatalog vnfdCatalog = new VNFDCatalog();
        vnfdCatalog.setVnfd(vnfds);
        osmPackage = new OsmVNFPackage().setVnfdCatalog(vnfdCatalog);
    }

    public OsmVNFPackage getPackage() {
        return osmPackage;
    }
}
