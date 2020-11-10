package it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin.translators;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.nextworks.nfvmano.libs.common.enums.CpRole;
import it.nextworks.nfvmano.libs.common.enums.LayerProtocol;
import it.nextworks.nfvmano.libs.descriptors.capabilities.VirtualComputeCapability;
import it.nextworks.nfvmano.libs.descriptors.capabilities.VirtualComputeCapabilityProperties;
import it.nextworks.nfvmano.libs.descriptors.elements.*;
import it.nextworks.nfvmano.libs.descriptors.templates.*;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VDU.*;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpNode;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpProperties;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VnfExtCp.VnfExtCpRequirements;
import it.nextworks.nfvmano.libs.ifa.descriptors.common.elements.SwImageDesc;
import it.nextworks.nfvmano.libs.ifa.descriptors.common.elements.VirtualComputeDesc;
import it.nextworks.nfvmano.libs.ifa.descriptors.common.elements.VirtualStorageDesc;
import it.nextworks.nfvmano.libs.ifa.descriptors.nsd.Nsd;
import it.nextworks.nfvmano.libs.ifa.descriptors.vnfd.Vdu;
import it.nextworks.nfvmano.libs.ifa.descriptors.vnfd.VnfDf;
import it.nextworks.nfvmano.libs.ifa.descriptors.vnfd.VnfExtCpd;
import it.nextworks.nfvmano.libs.ifa.descriptors.vnfd.Vnfd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class IfaToSolTranslator {

    private static final Logger log = LoggerFactory.getLogger(IfaToSolTranslator.class);

    public static DescriptorTemplate generateVnfDescriptor(Vnfd vnfdIfa) throws IOException, IllegalArgumentException {
        String id = vnfdIfa.getVnfdId();
        if(id == null)
            throw new IllegalArgumentException("Descriptor without ID");
        String version = vnfdIfa.getVnfdVersion();
        String description = vnfdIfa.getVnfProductInfoDescription();
        String resourceVendor = vnfdIfa.getVnfProvider();
        String name = vnfdIfa.getVnfProductName();
        if(name == null)
            throw new IllegalArgumentException("Descriptor without name");
        String resourceVendorRelease = vnfdIfa.getVnfSoftwareVersion();
        VnfDf df = vnfdIfa.getDeploymentFlavour().get(0); //Consider only one df

        log.debug("Creating VnfExtCpNode");
        //Creating VnfExtCpNode and VirtualinkRequirements for SubstitutionMappingsRequirements
        LinkedHashMap<String, Node> nodeTemplates = new LinkedHashMap<>();
        List<VirtualLinkPair> virtualLink = new ArrayList<>();
        boolean mgmtFound = false;
        String cpName = null;
        for(VnfExtCpd cp : vnfdIfa.getVnfExtCpd()){
            cpName = cp.getCpdId();
            virtualLink.add(new VirtualLinkPair(cpName, cpName));
            List<LayerProtocol> layerProtocols = new ArrayList<>();
            layerProtocols.add(LayerProtocol.IPV4);
            List<CpProtocolData> protocolData = new ArrayList<>();
            protocolData.add(new CpProtocolData(LayerProtocol.IPV4, null));
            List<VirtualNetworkInterfaceRequirements> virtualNetworkInterfaceRequirements = new ArrayList<>();
            HashMap<String, String> interfaceRequirements = new HashMap<>();
            it.nextworks.nfvmano.libs.ifa.descriptors.common.elements.AddressData addressData = cp.getAddressData().get(0); //Consider only one address data
            if(addressData.isManagement()) {
                mgmtFound = true;
                interfaceRequirements.put("isManagement", "true");
            }
            VirtualNetworkInterfaceRequirements virtualNetworkInterfaceRequirement = new VirtualNetworkInterfaceRequirements(null, null, false, interfaceRequirements, null);
            virtualNetworkInterfaceRequirements.add(virtualNetworkInterfaceRequirement);
            VnfExtCpProperties cpProperties = new VnfExtCpProperties(null, layerProtocols, CpRole.LEAF, cp.getDescription(), protocolData, false, virtualNetworkInterfaceRequirements);
            List<String> externalVirtualLink = new ArrayList<>();
            externalVirtualLink.add(cpName);
            VnfExtCpRequirements cpRequirements = new  VnfExtCpRequirements(externalVirtualLink, null);
            nodeTemplates.put(cpName, new VnfExtCpNode(null, cpProperties, cpRequirements));//type: "tosca.nodes.nfv.VnfExtCp"
        }

        //select ta random cp as mgmt
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

        List<Vdu> vduNodes = vnfdIfa.getVdu();
        for(Vdu vduNode : vduNodes) {
            SwImageDesc imageDesc = vduNode.getSwImageDesc();
            VirtualComputeDesc virtualComputeDesc = vnfdIfa.getVirtualComputeDesc().stream().filter(computeDesc -> vduNode.getVirtualComputeDesc().equals(computeDesc.getVirtualComputeDescId())).findAny().orElse(null);
            if(virtualComputeDesc == null)//VDU without virtualComputeDesc specified
                continue;
            List<VirtualStorageDesc> virtualStorageDescList = vnfdIfa.getVirtualStorageDesc().stream().filter(storageDesc -> vduNode.getVirtualStorageDesc().contains(storageDesc.getStorageId())).collect(Collectors.toList());
            String imageName = imageDesc.getSwImage();
            Integer vRam = virtualComputeDesc.getVirtualMemory().getVirtualMemSize();
            Integer vCpu = virtualComputeDesc.getVirtualCpu().getNumVirtualCpu();

            String vduId = vduNode.getVduId();
            log.debug("Creating VDUVirtualBlockStorageNode for vdu {}", vduId);
            for(VirtualStorageDesc virtualStorageDesc : virtualStorageDescList) {
                //Creating  VDUVirtualBlockStorageNode
                VirtualBlockStorageData virtualBlockStorageData = new VirtualBlockStorageData(virtualStorageDesc.getSizeOfStorage(), null, false);
                VDUVirtualBlockStorageProperties bsProperties = new VDUVirtualBlockStorageProperties(virtualBlockStorageData, null);
                nodeTemplates.put(virtualStorageDesc.getStorageId(), new VDUVirtualBlockStorageNode(null, bsProperties));//type: "tosca.nodes.nfv.Vdu.VirtualBlockStorage"
            }

            log.debug("Creating VDUComputeNode for vdu {}", vduId);
            //Creating VDUComputeNode
            it.nextworks.nfvmano.libs.ifa.descriptors.vnfd.VduProfile ifaVduProfile = df.getVduProfile().stream().filter(profile -> profile.getVduId().equals(vduId)).findAny().orElse(null);
            if(ifaVduProfile == null) //VDU profile not specified
                continue;
            VduProfile vduProfile = new VduProfile(ifaVduProfile.getMinNumberOfInstances(), ifaVduProfile.getMaxNumberOfInstances());
            SwImageData swImageData = new SwImageData(imageName, resourceVendorRelease, imageDesc.getChecksum(), null, null, imageDesc.getMinDisk(), imageDesc.getMinRam(), imageDesc.getSize(), imageDesc.getOperatingSystem(), null);
            VDUComputeProperties vduProperties = new VDUComputeProperties(vduId, null, null, null, null, null, null, vduProfile, swImageData);
            VirtualComputeCapabilityProperties vccProperties = new VirtualComputeCapabilityProperties(null, null, null, new VirtualMemory(vRam, null, null, false), new VirtualCpu(null, null, vCpu, null, null, null, null), null);
            VirtualComputeCapability virtualComputeCapability = new VirtualComputeCapability(vccProperties);
            VDUComputeCapabilities vduCapabilities = new VDUComputeCapabilities(virtualComputeCapability);
            List<String> storages = virtualStorageDescList.stream().map(VirtualStorageDesc::getStorageId).collect(Collectors.toList());
            VDUComputeRequirements vduRequirements = new VDUComputeRequirements(null, storages);
            nodeTemplates.put(vduId, new VDUComputeNode(null, vduProperties, vduCapabilities, vduRequirements));//type: "tosca.nodes.nfv.Vdu.Compute"
        }

        log.debug("Creating VNFNode");
        //Creating VNFNode
        String defaultIl = df.getDefaultInstantiationLevelId();
        if(defaultIl == null){
            it.nextworks.nfvmano.libs.ifa.descriptors.vnfd.InstantiationLevel il = df.getInstantiationLevel().get(0); //Take the first il as default
            if(il != null)
                defaultIl = il.getLevelId();
        }
        VnfProfile vnfProfile = new VnfProfile(defaultIl, null, null);
        VNFProperties vnfProperties = new VNFProperties(id, version, resourceVendor, name, resourceVendorRelease, name, description, null, null, null, null, null, null, null, df.getFlavourId(), df.getDescription(), vnfProfile);
        nodeTemplates.put(name, new VNFNode(null, name, vnfProperties, null, null, null));//type: "tosca.nodes.nfv.VNF"

        //Creating SubstitutionMappings
        SubstitutionMappingsRequirements requirements = new SubstitutionMappingsRequirements(null, virtualLink);
        SubstitutionMappings substitutionMappings = new SubstitutionMappings(null, "tosca.nodes.nfv.VNF", null, requirements, null);

        //Creating TopologyTemplate
        TopologyTemplate topologyTemplate = new TopologyTemplate(null, substitutionMappings, null, nodeTemplates, null, null);

        //Creating Metadata
        Metadata metadata = new Metadata(id, resourceVendor, version);

        //Creating DescriptorTemplate
        return new DescriptorTemplate("tosca_sol001_v0_10", null, description, metadata, null, null, null, topologyTemplate);
    }

    public static DescriptorTemplate generateNsDescriptor(Nsd nsdIfa) throws IOException, IllegalArgumentException {
        DescriptorTemplate nsd = null;
        return nsd;
    }
}
