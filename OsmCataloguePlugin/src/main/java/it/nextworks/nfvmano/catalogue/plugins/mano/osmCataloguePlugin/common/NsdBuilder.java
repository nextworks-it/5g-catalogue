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
package it.nextworks.nfvmano.catalogue.plugins.mano.osmCataloguePlugin.common;

import it.nextworks.nfvmano.catalogue.plugins.cataloguePlugin.mano.MANOType;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.templates.VirtualLinkPair;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import it.nextworks.nfvmano.libs.osmr4PlusDataModel.nsDescriptor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class NsdBuilder {

    private static final Logger log = LoggerFactory.getLogger(NsdBuilder.class);
    private final File defaultLogo;
    private OsmNSPackage osmPackage;
    private DescriptorTemplate dt;

    public NsdBuilder(File defaultLogo) {
        this.defaultLogo = defaultLogo;
    }

    private VNFDConnectionPointReference makeCPRef(String vnfdId, int memberIndex, String cpId) {
        VNFDConnectionPointReference cpr = new VNFDConnectionPointReference();
        cpr.setVnfdIdReference(vnfdId);
        cpr.setIndexReference(memberIndex);
        cpr.setVnfdConnectionPointReference(cpId);
        return cpr;
    }

    private VLD makeVld(
            String vlId,
            NsVirtualLinkNode link,
            Map<String, Map<ConstituentVNFD, String>> vlToVnfMapping
    ) {
        VLD vld = new VLD();
        vld.setId(vlId);
        vld.setName(vlId);
        vld.setShortName(vlId);
        vld.setMgmtNetwork(vlId.endsWith("_mgmt") || vlId.startsWith("mgmt_") || vlId.equalsIgnoreCase("default")); // TODO
        vld.setType("ELAN");
        vld.setVimNetworkName(vlId);
        vld.setVnfdConnectionPointReferences(
                vlToVnfMapping.getOrDefault(vlId, Collections.emptyMap()).entrySet()
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

    public void parseDescriptorTemplate(DescriptorTemplate template, List<DescriptorTemplate> vnfds, MANOType manoType) throws MalformattedElementException {
        dt = template;

        if (!(dt.getTopologyTemplate().getNSNodes().size() == 1)) {
            throw new IllegalArgumentException("Too many Nsds");
        }

        NSNode nsd = dt.getTopologyTemplate().getNSNodes().values().iterator().next();

        Map<String, Map<ConstituentVNFD, String>> vlToVnfMapping = new HashMap<>();

        List<ConstituentVNFD> constituentVnfds = new ArrayList<>();

        Map<String, VNFNode> vnfNodes = dt.getTopologyTemplate().getVNFNodes();

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

        List<VLD> vlds = dt.getTopologyTemplate().getNsVirtualLinkNodes().entrySet()
                .stream()
                .map(e -> makeVld(e.getKey(), e.getValue(), vlToVnfMapping))
                .collect(Collectors.toList());

        List<NSDescriptor> nsds = new ArrayList<>();
        NSDescriptor osmNsd = new NSDescriptor();
        osmNsd.setId(nsd.getProperties().getDescriptorId());
        osmNsd.setShortName(nsd.getProperties().getName());
        osmNsd.setDescription(dt.getDescription()); // TODO: this is not ideal.
        osmNsd.setVendor(nsd.getProperties().getDesigner());
        osmNsd.setVersion(nsd.getProperties().getVersion());
        osmNsd.setLogo(defaultLogo.getName()); // TODO get logo?
        osmNsd.setConstituentVNFDs(constituentVnfds);
        osmNsd.setVldList(vlds);
        if (manoType == MANOType.OSMR3)
            osmNsd.setName(nsd.getProperties().getDescriptorId());
        else if (manoType == MANOType.OSMR4)
            osmNsd.setName(nsd.getProperties().getName());
        nsds.add(osmNsd);

        NSDCatalog nsdCatalog = new NSDCatalog();
        nsdCatalog.setNsds(nsds);
        osmPackage = new OsmNSPackage().setNsdCatalog(nsdCatalog);
    }

    public OsmNSPackage getPackage() {
        return osmPackage;
    }
}
