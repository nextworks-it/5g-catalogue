package it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.libs.common.enums.CpRole;
import it.nextworks.nfvmano.libs.common.enums.LayerProtocol;
import it.nextworks.nfvmano.libs.descriptors.capabilities.VirtualComputeCapability;
import it.nextworks.nfvmano.libs.descriptors.capabilities.VirtualComputeCapabilityProperties;
import it.nextworks.nfvmano.libs.descriptors.elements.*;
import it.nextworks.nfvmano.libs.descriptors.interfaces.LcmOperation;
import it.nextworks.nfvmano.libs.descriptors.interfaces.Vnflcm;
import it.nextworks.nfvmano.libs.descriptors.templates.*;
import it.nextworks.nfvmano.libs.descriptors.templates.Node;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.*;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFInterfaces;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpRequirements;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.vnfDescriptor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ToscaDescriptorsParser {

    private static final Logger log = LoggerFactory.getLogger(ToscaDescriptorsParser.class);

    public static DescriptorTemplate generateVnfDescriptor(String osmVnfDescriptorPath) throws IOException, IllegalArgumentException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        OsmVNFPackage osmVnfPkg = mapper.readValue(new File(osmVnfDescriptorPath), OsmVNFPackage.class);

        if(osmVnfPkg.getVnfdCatalog().getVnfd() == null || osmVnfPkg.getVnfdCatalog().getVnfd().size() == 0)
            throw new IllegalArgumentException("No VNF defined");
        if(osmVnfPkg.getVnfdCatalog().getVnfd().size() != 1)
            throw new IllegalArgumentException("Too many VNF defined");

        VNFDescriptor osmVnfDescriptor = osmVnfPkg.getVnfdCatalog().getVnfd().get(0);

        log.debug("Creating VnfExtCpNode");
        //Creating VnfExtCpNode and VirtualinkRequirements for SubstitutionMappingsRequirements
        LinkedHashMap<String, Node> nodeTemplates = new LinkedHashMap<>();
        List<VirtualLinkPair> virtualLink = new ArrayList<>();
        //TODO add this check in the data model validate?
        if(osmVnfDescriptor.getConnectionPoints() == null || osmVnfDescriptor.getConnectionPoints().size() == 0)
            throw new IllegalArgumentException("No Connection Points defined");
        for(ConnectionPoint cp : osmVnfDescriptor.getConnectionPoints()){
            if(cp.getName() == null)
                throw new IllegalArgumentException("No Connection Point name defined");
            virtualLink.add(new VirtualLinkPair(cp.getName(), cp.getName()));
            List<LayerProtocol> layerProtocols = new ArrayList<>();
            layerProtocols.add(LayerProtocol.IPV4);
            List<CpProtocolData> protocolData = new ArrayList<>();
            protocolData.add(new CpProtocolData(LayerProtocol.IPV4, null));
            VnfExtCpProperties cpProperties = new VnfExtCpProperties(null, layerProtocols, CpRole.LEAF, cp.getName(), protocolData, false, null);
            List<String> externalVirtualLink = new ArrayList<>();
            externalVirtualLink.add(cp.getName() + "_net");
            VnfExtCpRequirements cpRequirements = new  VnfExtCpRequirements(externalVirtualLink, null);
            nodeTemplates.put(cp.getName(), new VnfExtCpNode("tosca.nodes.nfv.VnfExtCp", cpProperties, cpRequirements));
        }

        if(osmVnfDescriptor.getVduList() == null || osmVnfDescriptor.getVduList().size() == 0)
            throw new IllegalArgumentException("No VDU defined");
        //For the moment consider only single VDU
        if(osmVnfDescriptor.getVduList().size() != 1)
            throw new IllegalArgumentException("Multiple VDU are not currently supported");
        VDU vdu = osmVnfDescriptor.getVduList().get(0);
        log.debug("Creating VDUVirtualBlockStorageNode");
        //Creating  VDUVirtualBlockStorageNode
        if(vdu.getVmFlavor() == null || vdu.getVmFlavor().getStorageGb() == null || vdu.getVmFlavor().getMemoryMb() == null || vdu.getVmFlavor().getVcpuCount() == null)
            throw new IllegalArgumentException("No VM Flavor defined");
        VirtualBlockStorageData virtualBlockStorageData = new VirtualBlockStorageData(vdu.getVmFlavor().getStorageGb(), null, false);
        if(vdu.getImage() == null)
            throw new IllegalArgumentException("No Image Data defined");
        SwImageData swImageData = new SwImageData(vdu.getImage(), "1.0", null, null, null, null, null, null, null, null);
        VDUVirtualBlockStorageProperties bsProperties = new VDUVirtualBlockStorageProperties(virtualBlockStorageData, swImageData);
        if(osmVnfDescriptor.getName() == null)
            throw new IllegalArgumentException("No VNFD name defined");
        nodeTemplates.put(osmVnfDescriptor.getName() + "_storage", new VDUVirtualBlockStorageNode("tosca.nodes.nfv.Vdu.VirtualBlockStorage", bsProperties));

        log.debug("Creating VDUComputeNode");
        //Creating VDUComputeNode
        //For the moment consider only single scalingGroupDescriptor
        Integer minInstanceCount = (vdu.getCount() == null) ? 1 : vdu.getCount();
        Integer maxInstanceCount = minInstanceCount;
        if(osmVnfDescriptor.getScalingGroupDescriptor() != null && osmVnfDescriptor.getScalingGroupDescriptor().get(0).getMaxInstanceCount() != null)
            maxInstanceCount = osmVnfDescriptor.getScalingGroupDescriptor().get(0).getMaxInstanceCount() + minInstanceCount;
        VduProfile vduProfile = new VduProfile(minInstanceCount, maxInstanceCount);
        if(vdu.getName() == null)
            throw new IllegalArgumentException("No VDU name defined");
        VDUComputeProperties vduProperties = new VDUComputeProperties(vdu.getName(), null, null, null, null, null, null, vduProfile, null);
        VirtualComputeCapabilityProperties vccProperties = new VirtualComputeCapabilityProperties(null, null, null, new VirtualMemory(vdu.getVmFlavor().getMemoryMb(), null, null, false), new VirtualCpu(null, null, vdu.getVmFlavor().getVcpuCount(), null, null, null, null), null);
        VirtualComputeCapability virtualComputeCapability = new VirtualComputeCapability(vccProperties);
        VDUComputeCapabilities vduCapabilities = new VDUComputeCapabilities(virtualComputeCapability);
        List<String> storages = new ArrayList<>();
        storages.add(osmVnfDescriptor.getName() + "_storage");
        VDUComputeRequirements vduRequirements = new VDUComputeRequirements(null, storages);
        nodeTemplates.put(vdu.getName(), new VDUComputeNode("tosca.nodes.nfv.Vdu.Compute", vduProperties, vduCapabilities, vduRequirements));

        log.debug("Creating VNFNode");
        //Creating VNFNode
        if(osmVnfDescriptor.getId() == null)
            throw new IllegalArgumentException("No VNFD ID defined");
        if(osmVnfDescriptor.getVersion() == null)
            throw new IllegalArgumentException("No VNFD version defined");
        if(osmVnfDescriptor.getVendor() == null)
            throw new IllegalArgumentException("No VNFD vendor defined");
        VNFProperties vnfProperties = new VNFProperties(osmVnfDescriptor.getId(), osmVnfDescriptor.getVersion(), osmVnfDescriptor.getVendor(), osmVnfDescriptor.getName(), "1.0", osmVnfDescriptor.getName(), osmVnfDescriptor.getDescription(), null, null, null, null, null, null, null, osmVnfDescriptor.getName() + "_flavor", osmVnfDescriptor.getName() + " flavor", null);
        VNFInterfaces vnfInterfaces = null;
        if(vdu.getCloudInitFile() != null)
            vnfInterfaces = new VNFInterfaces(null, new Vnflcm(new LcmOperation(vdu.getCloudInitFile()), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        nodeTemplates.put(osmVnfDescriptor.getName() + "_VNF", new VNFNode("tosca.nodes.nfv.VNF", osmVnfDescriptor.getName(), vnfProperties, null, null, vnfInterfaces));

        //Creating SubstitutionMappings
        SubstitutionMappingsRequirements requirements = new SubstitutionMappingsRequirements(null, virtualLink);
        SubstitutionMappings substitutionMappings = new SubstitutionMappings(null, "tosca.nodes.nfv.VNF", null, requirements, null);

        //Creating TopologyTemplate
        TopologyTemplate topologyTemplate = new TopologyTemplate(null, substitutionMappings, null, nodeTemplates, null, null);

        //Creating Metadata
        Metadata metadata = new Metadata(osmVnfDescriptor.getId(), osmVnfDescriptor.getVendor(), osmVnfDescriptor.getVersion());

        //Creating DescriptorTemplate
        DescriptorTemplate vnfd = new DescriptorTemplate("tosca_simple_yaml", null, "Descriptor generated by 5G Apps & Services Catalogue", metadata, null, null, null, topologyTemplate);

        return vnfd;
    }

    public static DescriptorTemplate generateNsDescriptor(String osmNsDescriptorPath) throws IOException, IllegalArgumentException {
        return null;
    }
}
