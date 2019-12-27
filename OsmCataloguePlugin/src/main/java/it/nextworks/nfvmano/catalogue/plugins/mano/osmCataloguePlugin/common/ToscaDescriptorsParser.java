package it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.libs.common.enums.CpRole;
import it.nextworks.nfvmano.libs.common.enums.FlowPattern;
import it.nextworks.nfvmano.libs.common.enums.LayerProtocol;
import it.nextworks.nfvmano.libs.descriptors.capabilities.VirtualComputeCapability;
import it.nextworks.nfvmano.libs.descriptors.capabilities.VirtualComputeCapabilityProperties;
import it.nextworks.nfvmano.libs.descriptors.elements.*;
import it.nextworks.nfvmano.libs.descriptors.interfaces.LcmOperation;
import it.nextworks.nfvmano.libs.descriptors.interfaces.Vnflcm;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSProperties;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSRequirements;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkProperties;
import it.nextworks.nfvmano.libs.descriptors.templates.*;
import it.nextworks.nfvmano.libs.descriptors.templates.Node;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.*;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFInterfaces;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFRequirements;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpRequirements;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.nsDescriptor.*;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.osmManagement.OsmInfoObject;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.vnfDescriptor.*;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.vnfDescriptor.ConnectionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
            List<VirtualNetworkInterfaceRequirements> virtualNetworkInterfaceRequirements = new ArrayList<>();
            HashMap<String, String> interfaceRequirements = new HashMap<>();
            if(osmVnfDescriptor.getManagementInterface().getCp().equalsIgnoreCase(cp.getName()))
                interfaceRequirements.put("isManagement", "true");
            VirtualNetworkInterfaceRequirements virtualNetworkInterfaceRequirement = new VirtualNetworkInterfaceRequirements(null, null, false, interfaceRequirements, null);
            virtualNetworkInterfaceRequirements.add(virtualNetworkInterfaceRequirement);
            VnfExtCpProperties cpProperties = new VnfExtCpProperties(null, layerProtocols, CpRole.LEAF, cp.getName(), protocolData, false, virtualNetworkInterfaceRequirements);
            List<String> externalVirtualLink = new ArrayList<>();
            externalVirtualLink.add(cp.getName());
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
        if(osmVnfDescriptor.getName() == null && osmVnfDescriptor.getShortName() == null)
            throw new IllegalArgumentException("No VNFD name defined");
        String vnfNodeName = osmVnfDescriptor.getName();
        if(vnfNodeName == null)
            vnfNodeName = osmVnfDescriptor.getShortName();
        nodeTemplates.put(vnfNodeName + "_storage", new VDUVirtualBlockStorageNode("tosca.nodes.nfv.Vdu.VirtualBlockStorage", bsProperties));

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
        storages.add(vnfNodeName + "_storage");
        VDUComputeRequirements vduRequirements = new VDUComputeRequirements(null, storages);
        nodeTemplates.put(vdu.getName(), new VDUComputeNode("tosca.nodes.nfv.Vdu.Compute", vduProperties, vduCapabilities, vduRequirements));

        log.debug("Creating VNFNode");
        //Creating VNFNode
        if(osmVnfDescriptor.getId() == null)
            throw new IllegalArgumentException("No VNFD ID defined");
        String vnfVersion = osmVnfDescriptor.getVersion();
        if(vnfVersion == null)
            vnfVersion = "1.0";
        String vnfVendor = osmVnfDescriptor.getVendor();
        if(vnfVendor == null)
            vnfVendor = "Undefined";
        VNFProperties vnfProperties = new VNFProperties(osmVnfDescriptor.getId(), vnfVersion, vnfVendor, vnfNodeName, "1.0", vnfNodeName, osmVnfDescriptor.getDescription(), null, null, null, null, null, null, null, vnfNodeName + "_flavor", vnfNodeName + " flavor", null);
        VNFInterfaces vnfInterfaces = null;
        if(vdu.getCloudInitFile() != null)
            vnfInterfaces = new VNFInterfaces(null, new Vnflcm(new LcmOperation(vdu.getCloudInitFile()), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        nodeTemplates.put(vnfNodeName + "_VNF", new VNFNode("tosca.nodes.nfv.VNF", vnfNodeName, vnfProperties, null, null, vnfInterfaces));

        //Creating SubstitutionMappings
        SubstitutionMappingsRequirements requirements = new SubstitutionMappingsRequirements(null, virtualLink);
        SubstitutionMappings substitutionMappings = new SubstitutionMappings(null, "tosca.nodes.nfv.VNF", null, requirements, null);

        //Creating TopologyTemplate
        TopologyTemplate topologyTemplate = new TopologyTemplate(null, substitutionMappings, null, nodeTemplates, null, null);

        //Creating Metadata
        Metadata metadata = new Metadata(osmVnfDescriptor.getId(), vnfVendor, vnfVersion);

        //Creating DescriptorTemplate
        DescriptorTemplate vnfd = new DescriptorTemplate("tosca_simple_yaml", null, "Descriptor generated by 5G Apps & Services Catalogue", metadata, null, null, null, topologyTemplate);

        return vnfd;
    }

    public static DescriptorTemplate generateNsDescriptor(String osmNsDescriptorPath, List<OsmInfoObject> vnfInfoList, List<TranslationInformation> translationInformationList, Path rootDir) throws IOException, IllegalArgumentException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        OsmNSPackage osmNsPkg = mapper.readValue(new File(osmNsDescriptorPath), OsmNSPackage.class);

        if(osmNsPkg.getNsdCatalog().getNsds() == null || osmNsPkg.getNsdCatalog().getNsds().size() == 0)
            throw new IllegalArgumentException("No NS defined");
        if(osmNsPkg.getNsdCatalog().getNsds().size() != 1)
            throw new IllegalArgumentException("Too many NS defined");

        NSDescriptor osmNsDescriptor = osmNsPkg.getNsdCatalog().getNsds().get(0);

        LinkedHashMap<String, Node> nodeTemplates = new LinkedHashMap<>();

        log.debug("Creating NsVirtualLinkNodes");
        List<ConstituentVNFD> vnfdList = osmNsDescriptor.getConstituentVNFDs();
        List<VLD> vlds = osmNsDescriptor.getVldList();
        List<String> virtualLinkNames = new ArrayList<>();
        Map<String, Map<String, String>> virtualLinkRequirements = new HashMap<>();
        for(ConstituentVNFD vnfd : vnfdList)
            virtualLinkRequirements.put(vnfd.getVnfdIdentifierReference(), new HashMap<>());
        for(VLD vld : vlds) {
            if(vld.getId() == null)
                throw new IllegalArgumentException("No VLD ID defined");
            String vLinkName;
            if(vld.getVimNetworkName() != null)
                vLinkName = vld.getVimNetworkName();
            else {
                if(vld.isMgmtNetwork())
                    vLinkName = vld.getId() + "_mgmt";
                else
                    vLinkName = vld.getId();
            }
            List<LayerProtocol> layerProtocols = new ArrayList<>();
            layerProtocols.add(LayerProtocol.IPV4);
            ConnectivityType connectivityType = new ConnectivityType(layerProtocols, FlowPattern.LINE);
            VlProfile vlProfile = new VlProfile(new LinkBitrateRequirements(1000000, 10000), new LinkBitrateRequirements(100000, 10000), null, null);
            NsVirtualLinkProperties vLinkProperties = new NsVirtualLinkProperties(null, vLinkName, vlProfile, connectivityType, null);
            nodeTemplates.put(vLinkName, new NsVirtualLinkNode("tosca.nodes.nfv.NsVirtualLink", vLinkProperties, null));
            virtualLinkNames.add(vLinkName);
            List<VNFDConnectionPointReference> vnfdConnectionPointReferenceList = vld.getVnfdConnectionPointReferences();
            for(VNFDConnectionPointReference vnfdConnectionPointReference : vnfdConnectionPointReferenceList){
                virtualLinkRequirements.get(vnfdConnectionPointReference.getVnfdIdReference()).put(vnfdConnectionPointReference.getVnfdConnectionPointReference(), vLinkName);//Assume TOSCA VNFD has virtual link requirements {cp: cp_name, vl: cp_name}
            }
        }

        log.debug("Creating VNFNodes");
        for(ConstituentVNFD vnfd : vnfdList){
            String osmVnfDescriptorPath = rootDir.toString();
            for(OsmInfoObject vnfObject : vnfInfoList){
                if(vnfObject.getDescriptorId().equals(vnfd.getVnfdIdentifierReference())) {
                    osmVnfDescriptorPath = osmVnfDescriptorPath.concat("/" + vnfObject.getAdmin().getStorage().getDescriptor());
                    break;
                }
            }
            OsmVNFPackage osmVnfPkg = mapper.readValue(new File(osmVnfDescriptorPath), OsmVNFPackage.class);
            VNFDescriptor osmVnfDescriptor = osmVnfPkg.getVnfdCatalog().getVnfd().get(0);//For the moment consider only one VNF present
            List<TranslationInformation> filteredTranslationInformationList = translationInformationList.stream().filter(t -> t.getOsmDescriptorId().equals(osmVnfDescriptor.getId()) && t.getDescriptorVersion().equals(osmVnfDescriptor.getVersion())).collect(Collectors.toList());
            String catVnfDescriptorId = osmVnfDescriptor.getId();
            if(filteredTranslationInformationList.size() != 0)
                catVnfDescriptorId = filteredTranslationInformationList.get(0).getCatDescriptorId();
            String vnfNodeName = osmVnfDescriptor.getName();
            if(vnfNodeName == null)
                vnfNodeName = osmVnfDescriptor.getShortName();
            VNFProperties vnfProperties = new VNFProperties(catVnfDescriptorId, osmVnfDescriptor.getVersion(), osmVnfDescriptor.getVendor(), vnfNodeName, "1.0", vnfNodeName, osmVnfDescriptor.getDescription(), null, null, null, null, null, null, null, vnfNodeName + "_flavor", vnfNodeName + " flavor", null);
            VNFRequirements vnfRequirements = new VNFRequirements(virtualLinkRequirements.get(vnfd.getVnfdIdentifierReference()));
            nodeTemplates.put(vnfNodeName, new VNFNode("tosca.nodes.nfv.VNF", vnfNodeName, vnfProperties, vnfRequirements, null, null));
        }

        log.debug("Creating NSNode");
        //Creating NSNode
        if(osmNsDescriptor.getName() == null && osmNsDescriptor.getShortName() == null)
            throw new IllegalArgumentException("No NSD name defined");
        if(osmNsDescriptor.getId() == null)
            throw new IllegalArgumentException("No NSD ID defined");
        String nsdVersion = osmNsDescriptor.getVersion();
        if(nsdVersion == null)
            nsdVersion = "1.0";
        String nsdVendor = osmNsDescriptor.getVendor();
        if(nsdVendor == null)
            nsdVendor = "Undefined";
        NSRequirements nsRequirements = new NSRequirements(virtualLinkNames);
        String nsNodeName = osmNsDescriptor.getName();
        if(nsNodeName == null)
            nsNodeName = osmNsDescriptor.getShortName();
        NSProperties nsProperties = new NSProperties(osmNsDescriptor.getId(), nsdVendor, nsdVersion, nsNodeName, osmNsDescriptor.getId());
        nodeTemplates.put(nsNodeName + "_NS", new NSNode("tosca.nodes.nfv.NS", nsProperties, nsRequirements));

        //Creating SubstitutionMappings
        List<VirtualLinkPair> virtualLinkPairs = new ArrayList<>();
        SubstitutionMappingsRequirements requirements = new SubstitutionMappingsRequirements(null, virtualLinkPairs);
        SubstitutionMappings substitutionMappings = new SubstitutionMappings(null, "tosca.nodes.nfv.NS", null, requirements, null);

        //Creating TopologyTemplate
        TopologyTemplate topologyTemplate = new TopologyTemplate(null, substitutionMappings, null, nodeTemplates, null, null);

        //Creating Metadata
        Metadata metadata = new Metadata(osmNsDescriptor.getId(), nsdVendor, nsdVersion);

        //Creating DescriptorTemplate
        DescriptorTemplate nsd = new DescriptorTemplate("tosca_simple_yaml", null, "Descriptor generated by 5G Apps & Services Catalogue", metadata, null, null, null, topologyTemplate);

        return nsd;
    }
}
