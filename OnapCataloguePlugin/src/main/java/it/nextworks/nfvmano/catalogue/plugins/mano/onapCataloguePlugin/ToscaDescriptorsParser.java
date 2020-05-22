package it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.OnapNsDescriptor;
import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.OnapObject;
import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.OnapVnfDescriptor;
import it.nextworks.nfvmano.libs.common.enums.CpRole;
import it.nextworks.nfvmano.libs.common.enums.FlowPattern;
import it.nextworks.nfvmano.libs.common.enums.LayerProtocol;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.capabilities.VirtualComputeCapability;
import it.nextworks.nfvmano.libs.descriptors.capabilities.VirtualComputeCapabilityProperties;
import it.nextworks.nfvmano.libs.descriptors.elements.*;
import it.nextworks.nfvmano.libs.descriptors.interfaces.LcmOperation;
import it.nextworks.nfvmano.libs.descriptors.interfaces.Vnflcm;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSProperties;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkProperties;
import it.nextworks.nfvmano.libs.descriptors.templates.*;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.*;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFInterfaces;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFRequirements;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ToscaDescriptorsParser {

    private static final Logger log = LoggerFactory.getLogger(ToscaDescriptorsParser.class);

    public static DescriptorTemplate generateVnfDescriptor(String descriptorPath) throws IOException, IllegalArgumentException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        OnapVnfDescriptor onapVnfDescriptor = mapper.readValue(new File(descriptorPath), OnapVnfDescriptor.class);
        Map<String, Object> onapMetadata = onapVnfDescriptor.getMetadata();
        String id = (String)onapMetadata.get("UUID");
        if(id == null)
            throw new IllegalArgumentException("Descriptor without ID");
        String version = "1.0";
        String description = (String)onapMetadata.get("description");
        String resourceVendor = (String)onapMetadata.get("resourceVendor");
        String name = (String)onapMetadata.get("name");
        if(name == null)
            throw new IllegalArgumentException("Descriptor without name");
        String resourceVendorRelease = (String)onapMetadata.get("resourceVendorRelease");
        String imageName = onapVnfDescriptor.getImageName();
        Integer vRam = 1024;//TODO onapVnfDescriptor.getRam();
        Integer vCpu = 1;//TODO onapVnfDescriptor.getCpu();
        Integer storage = 1;//TODO onapVnfDescriptor.getStorage();
        Map<String, String> cpLinkAssociations = onapVnfDescriptor.getConnectionPointLinkAssociations();
        if(cpLinkAssociations.size() == 0)
            throw new IllegalArgumentException("Descriptor without connection points");
        String mgmtCp = "cp_name";//TODO onapVnfDescriptor.getMgmtCp();

        log.debug("Creating VnfExtCpNode");
        //Creating VnfExtCpNode and VirtualinkRequirements for SubstitutionMappingsRequirements
        LinkedHashMap<String, Node> nodeTemplates = new LinkedHashMap<>();
        List<VirtualLinkPair> virtualLink = new ArrayList<>();
        boolean mgmtFound = false;
        String cpName = null;
        for(String cp : cpLinkAssociations.keySet()){
            cpName = cp;
            virtualLink.add(new VirtualLinkPair(cp, cp));
            List<LayerProtocol> layerProtocols = new ArrayList<>();
            layerProtocols.add(LayerProtocol.IPV4);
            List<CpProtocolData> protocolData = new ArrayList<>();
            protocolData.add(new CpProtocolData(LayerProtocol.IPV4, null));
            List<VirtualNetworkInterfaceRequirements> virtualNetworkInterfaceRequirements = new ArrayList<>();
            HashMap<String, String> interfaceRequirements = new HashMap<>();
            if(cp.equalsIgnoreCase(mgmtCp)) {
                mgmtFound = true;
                interfaceRequirements.put("isManagement", "true");
            }
            VirtualNetworkInterfaceRequirements virtualNetworkInterfaceRequirement = new VirtualNetworkInterfaceRequirements(null, null, false, interfaceRequirements, null);
            virtualNetworkInterfaceRequirements.add(virtualNetworkInterfaceRequirement);
            VnfExtCpProperties cpProperties = new VnfExtCpProperties(null, layerProtocols, CpRole.LEAF, cp, protocolData, false, virtualNetworkInterfaceRequirements);
            List<String> externalVirtualLink = new ArrayList<>();
            externalVirtualLink.add(cp);
            VnfExtCpRequirements cpRequirements = new  VnfExtCpRequirements(externalVirtualLink, null);
            nodeTemplates.put(cp, new VnfExtCpNode(null, cpProperties, cpRequirements));//type: "tosca.nodes.nfv.VnfExtCp"
        }

        //select ta random cp ad mgmt
        if(!mgmtFound){
            List<LayerProtocol> layerProtocols = new ArrayList<>();
            layerProtocols.add(LayerProtocol.IPV4);
            List<CpProtocolData> protocolData = new ArrayList<>();
            protocolData.add(new CpProtocolData(LayerProtocol.IPV4, null));
            List<VirtualNetworkInterfaceRequirements> virtualNetworkInterfaceRequirements = new ArrayList<>();
            HashMap<String, String> interfaceRequirements = new HashMap<>();
            interfaceRequirements.put("isManagement", "true");
            VirtualNetworkInterfaceRequirements virtualNetworkInterfaceRequirement = new VirtualNetworkInterfaceRequirements(null, null, false, interfaceRequirements, null);
            virtualNetworkInterfaceRequirements.add(virtualNetworkInterfaceRequirement);
            VnfExtCpProperties cpProperties = new VnfExtCpProperties(null, layerProtocols, CpRole.LEAF, cpName, protocolData, false, virtualNetworkInterfaceRequirements);
            List<String> externalVirtualLink = new ArrayList<>();
            externalVirtualLink.add(cpName);
            VnfExtCpRequirements cpRequirements = new  VnfExtCpRequirements(externalVirtualLink, null);
            nodeTemplates.replace(cpName, new VnfExtCpNode(null, cpProperties, cpRequirements));
        }

        //For the moment consider only single VDU
        log.debug("Creating VDUVirtualBlockStorageNode");
        //Creating  VDUVirtualBlockStorageNode
        VirtualBlockStorageData virtualBlockStorageData = new VirtualBlockStorageData(storage, null, false);
        VDUVirtualBlockStorageProperties bsProperties = new VDUVirtualBlockStorageProperties(virtualBlockStorageData, null);
        nodeTemplates.put(name + "_storage", new VDUVirtualBlockStorageNode(null, bsProperties));//type: "tosca.nodes.nfv.Vdu.VirtualBlockStorage"

        log.debug("Creating VDUComputeNode");
        //Creating VDUComputeNode
        VduProfile vduProfile = new VduProfile(1, 1);
        SwImageData swImageData = new SwImageData(imageName, resourceVendorRelease, null, null, null, null, null, null, null, null);
        VDUComputeProperties vduProperties = new VDUComputeProperties(name + "_vdu", null, null, null, null, null, null, vduProfile, swImageData);
        VirtualComputeCapabilityProperties vccProperties = new VirtualComputeCapabilityProperties(null, null, null, new VirtualMemory(vRam, null, null, false), new VirtualCpu(null, null, vCpu, null, null, null, null), null);
        VirtualComputeCapability virtualComputeCapability = new VirtualComputeCapability(vccProperties);
        VDUComputeCapabilities vduCapabilities = new VDUComputeCapabilities(virtualComputeCapability);
        List<String> storages = new ArrayList<>();
        storages.add(name + "_storage");
        VDUComputeRequirements vduRequirements = new VDUComputeRequirements(null, storages);
        nodeTemplates.put(name + "_vdu", new VDUComputeNode(null, vduProperties, vduCapabilities, vduRequirements));//type: "tosca.nodes.nfv.Vdu.Compute"

        log.debug("Creating VNFNode");
        //Creating VNFNode
        VNFProperties vnfProperties = new VNFProperties(id, version, resourceVendor, name, resourceVendorRelease, name, description, null, null, null, null, null, null, null, name + "_flavor", name + " flavor", null);
        nodeTemplates.put(name + "_vnf", new VNFNode(null, name, vnfProperties, null, null, null));//type: "tosca.nodes.nfv.VNF"

        //Creating SubstitutionMappings
        SubstitutionMappingsRequirements requirements = new SubstitutionMappingsRequirements(null, virtualLink);
        SubstitutionMappings substitutionMappings = new SubstitutionMappings(null, "tosca.nodes.nfv.VNF", null, requirements, null);

        //Creating TopologyTemplate
        TopologyTemplate topologyTemplate = new TopologyTemplate(null, substitutionMappings, null, nodeTemplates, null, null);

        //Creating Metadata
        Metadata metadata = new Metadata(id, resourceVendor, version);

        //Creating DescriptorTemplate
        DescriptorTemplate vnfd = new DescriptorTemplate("tosca_sol001_v0_10", null, description, metadata, null, null, null, topologyTemplate);

        return vnfd;
    }

    public static DescriptorTemplate generateNsDescriptor(String descriptorPath) throws IOException, IllegalArgumentException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        OnapNsDescriptor onapNsDescriptor = mapper.readValue(new File(descriptorPath), OnapNsDescriptor.class);
        Map<String, Object> nsMetadata = onapNsDescriptor.getMetadata();
        String nsdId = (String)nsMetadata.get("UUID");
        if(nsdId == null)
            throw new IllegalArgumentException("Ns Descriptor without ID");
        String invariantId = (String)nsMetadata.get("invariantUUID");
        if(invariantId == null)
            throw new IllegalArgumentException("Ns Descriptor without invariant ID");
        String nsdName = (String)nsMetadata.get("name");
        if(nsdName == null)
            throw new IllegalArgumentException("Ns Descriptor without invariant name");
        String nsdVersion = "1.0";
        String nsdDescription = (String)nsMetadata.get("description");
        String nsdVendor = "ONAP";

        String[] splits = descriptorPath.split("/");
        String definitionsFolder = "";
        for(int i = 0; i < splits.length - 1; i++)
            definitionsFolder = definitionsFolder.concat(splits[i] + "/");
        //Map <nodeName, descriptors>
        Map<String, OnapVnfDescriptor> nodeDescriptorMapping = new HashMap<>();
        //Map <nodeName, vfIdentifier>
        Map<String, String> nodeVfIdentifierMapping = onapNsDescriptor.getVFIdentifiers();
        for(Map.Entry<String, String> vfIdentifier : nodeVfIdentifierMapping.entrySet())
            nodeDescriptorMapping.put(vfIdentifier.getKey(), mapper.readValue(new File(definitionsFolder + "resource-" + vfIdentifier.getValue() + "-template.yml"), OnapVnfDescriptor.class));

        LinkedHashMap<String, Node> nodeTemplates = new LinkedHashMap<>();

        log.debug("Creating NsVirtualLinkNodes");
        List<String> virtualLinkNames = new ArrayList<>();
        Map<String, Map<String, String>> virtualLinkRequirements = new HashMap<>();
        for(OnapVnfDescriptor  vnfDescriptor : nodeDescriptorMapping.values()) {
            Map<String, Object> vnfMetadata = vnfDescriptor.getMetadata();
            String vnfdId = (String)vnfMetadata.get("UUID");
            if(vnfdId == null)
                throw new IllegalArgumentException("Vnf Descriptor without ID");
            virtualLinkRequirements.put(vnfdId, vnfDescriptor.getConnectionPointLinkAssociations());
        }

        Set<String> vLinkNames = new HashSet<>();
        for(Map.Entry<String, Map<String, String>> virtualLinkRequirement : virtualLinkRequirements.entrySet()){
            Map<String, String> cpLinkAssociations = virtualLinkRequirement.getValue();
            for(Map.Entry<String, String> cpLinkAssociation : cpLinkAssociations.entrySet())
                vLinkNames.add(cpLinkAssociation.getValue());
        }

        for(String vLinkName : vLinkNames) {
            List<LayerProtocol> layerProtocols = new ArrayList<>();
            layerProtocols.add(LayerProtocol.IPV4);
            ConnectivityType connectivityType = new ConnectivityType(layerProtocols, FlowPattern.LINE);
            VlProfile vlProfile = new VlProfile(new LinkBitrateRequirements(1000000, 10000), new LinkBitrateRequirements(100000, 10000), null, null);
            NsVirtualLinkProperties vLinkProperties = new NsVirtualLinkProperties(null, vLinkName, vlProfile, connectivityType, null);
            nodeTemplates.put(vLinkName, new NsVirtualLinkNode(null, vLinkProperties, null));//type: "tosca.nodes.nfv.NsVirtualLink"
            virtualLinkNames.add(vLinkName);
        }

        log.debug("Creating VNFNodes");
        for(Map.Entry<String, OnapVnfDescriptor> nodeEntry : nodeDescriptorMapping.entrySet()){
            Map<String, Object> vnfMetadata = nodeEntry.getValue().getMetadata();
            String vnfdId = (String)vnfMetadata.get("UUID");
            if(vnfdId == null)
                throw new IllegalArgumentException("Vnf Descriptor without ID");
            String vnfdVersion = "1.0";
            String vnfdDescription = (String)vnfMetadata.get("description");
            String vnfdResourceVendor = (String)vnfMetadata.get("resourceVendor");
            String vnfdName = (String)vnfMetadata.get("name");
            if(vnfdName == null)
                throw new IllegalArgumentException("Vnf Descriptor without name");
            String resourceVendorRelease = (String)vnfMetadata.get("resourceVendorRelease");
            VNFProperties vnfProperties = new VNFProperties(vnfdId, vnfdVersion, vnfdResourceVendor, vnfdName, resourceVendorRelease, vnfdName, vnfdDescription, null, null, null, null, null, null, null, vnfdName + "_flavor", vnfdName + " flavor", null);
            VNFRequirements vnfRequirements = new VNFRequirements(virtualLinkRequirements.get(vnfdId));
            nodeTemplates.put(nodeEntry.getKey(), new VNFNode(null, vnfdName, vnfProperties, vnfRequirements, null, null));//type: "tosca.nodes.nfv.VNF"
        }

        log.debug("Creating NSNode");
        //Creating NSNode
        NSProperties nsProperties = new NSProperties(nsdId, nsdVendor, nsdVersion, nsdName, invariantId);
        nodeTemplates.put(nsdName + "_ns", new NSNode(null, nsProperties, null));//type: "tosca.nodes.nfv.NS"

        //Creating SubstitutionMappings
        List<VirtualLinkPair> virtualLinkPairs = new ArrayList<>();
        SubstitutionMappingsRequirements requirements = new SubstitutionMappingsRequirements(null, virtualLinkPairs);
        SubstitutionMappings substitutionMappings = new SubstitutionMappings(null, "tosca.nodes.nfv.NS", null, requirements, null);

        //Creating TopologyTemplate
        TopologyTemplate topologyTemplate = new TopologyTemplate(null, substitutionMappings, null, nodeTemplates, null, null);

        //Creating Metadata
        Metadata metadata = new Metadata(nsdId, nsdVendor, nsdVersion);

        //Creating DescriptorTemplate
        DescriptorTemplate nsd = new DescriptorTemplate("tosca_sol001_v0_10", null, nsdDescription, metadata, null, null, null, topologyTemplate);

        return nsd;
    }
}
