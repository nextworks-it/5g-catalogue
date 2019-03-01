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
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.common;

import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.catalogue.storage.FileSystemStorageService;
import it.nextworks.nfvmano.catalogue.plugins.mano.osm.common.nsDescriptor.*;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.templates.VirtualLinkPair;
import it.nextworks.nfvmano.libs.descriptors.vnfd.nodes.VNF.VNFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class NsdBuilder {

    private static final Logger log = LoggerFactory.getLogger(NsdBuilder.class);
    private final File defaultLogo;
    private OsmNsdPackage osmPackage;
    private DescriptorTemplate dt;

    @Autowired
    private FileSystemStorageService storageService;

    public NsdBuilder(File defaultLogo) {
        this.defaultLogo = defaultLogo;
    }

    private VnfdConnectionPointRef makeCPRef(String vnfdId, int memberIndex, String cpId) {
        return new VnfdConnectionPointRef()
                .setVnfdIdRef(vnfdId)
                .setMemberVnfIndexRef(memberIndex)
                .setVnfdConnectionPointRef(cpId);
    }

    private Vld makeVld(
            String vlId,
            NsVirtualLinkNode link,
            Map<String, Map<ConstituentVnfd, String>> vlToVnfMapping
    ) {
        return new Vld()
                .setId(vlId)
                .setName(vlId)
                .setShortName(vlId)
                .setMgmtNetwork(vlId.endsWith("_mgmt") || vlId.startsWith("mgmt_") ? "true" : "false") // TODO
                .setType("ELAN")
                .setVimNetworkName(vlId)
                .setVnfdConnectionPointRef(
                        vlToVnfMapping.getOrDefault(vlId, Collections.emptyMap()).entrySet()
                                .stream()
                                .map(e -> makeCPRef(
                                        e.getKey().getVnfdIdRef(),
                                        e.getKey().getMemberVnfIndex(),
                                        e.getValue()
                                ))
                                .collect(Collectors.toList())
                );
    }

    public void parseDescriptorTemplate(DescriptorTemplate template, List<DescriptorTemplate> vnfds, MANOType manoType) throws MalformattedElementException {
        dt = template;

        if (!(dt.getTopologyTemplate().getNSNodes().size() == 1)) {
            throw new IllegalArgumentException("Too many Nsds");
        }

        NSNode nsd = dt.getTopologyTemplate().getNSNodes().values().iterator().next();

        Map<String, Map<ConstituentVnfd, String>> vlToVnfMapping = new HashMap<>();

        List<ConstituentVnfd> constituentVnfds = new ArrayList<>();

        Map<String, VNFNode> vnfNodes = dt.getTopologyTemplate().getVNFNodes();

        for (Map.Entry<String, VNFNode> vnfNode : vnfNodes.entrySet()) {
            String vnfdId = vnfNode.getValue().getProperties().getDescriptorId();

            for (DescriptorTemplate vnfd : vnfds) {
                String id = vnfd.getTopologyTemplate().getVNFNodes().entrySet().iterator().next().getValue().getProperties().getDescriptorId();
                if (vnfdId.equalsIgnoreCase(id)) {
                    log.debug("Matching vnfdId {} in NSD with vnfdId {} in VNFDs list", vnfdId, id);

                    ConstituentVnfd constituentVnfd = new ConstituentVnfd().setVnfdIdRef(vnfdId);
                    constituentVnfds.add(constituentVnfd);

                    List<String> vls = vnfNode.getValue().getRequirements().getVirtualLink();

                    for (String vlId : vls) {
                        vlToVnfMapping.putIfAbsent(vlId, new HashMap<>());
                        List<VirtualLinkPair> pairs = vnfd.getTopologyTemplate().getSubstituitionMappings().getRequirements().getVirtualLink();
                        for (VirtualLinkPair pair : pairs) {
                            if (pair.getVl().equalsIgnoreCase(vlId)) {
                                String cpId = pair.getCp();
                                vlToVnfMapping.get(vlId).put(constituentVnfd, cpId);
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
            log.debug("Constituent VNFD: " + constituentVnfds.get(i).getVnfdIdRef());
            constituentVnfds.get(i).setMemberVnfIndex(i + 1);
        }

        List<Vld> vlds = dt.getTopologyTemplate().getNsVirtualLinkNodes().entrySet()
                .stream()
                .map(e -> makeVld(e.getKey(), e.getValue(), vlToVnfMapping))
                .collect(Collectors.toList());

        List<Nsd> nsds = new ArrayList<>();
        Nsd osmNsd = new Nsd()
                .setId(nsd.getProperties().getDescriptorId())
                .setShortName(nsd.getProperties().getName())
                .setDescription(dt.getDescription()) // TODO: this is not ideal.
                .setVendor(nsd.getProperties().getDesigner())
                .setVersion(nsd.getProperties().getVersion())
                .setLogo(defaultLogo.getName()) // TODO get logo?
                .setConstituentVnfd(constituentVnfds)
                .setVld(vlds);
        if (manoType == MANOType.OSMR3)
            osmNsd.setName(nsd.getProperties().getDescriptorId());
        else if (manoType == MANOType.OSMR4)
            osmNsd.setName(nsd.getProperties().getName());
        nsds.add(osmNsd);

        NsdCatalog nsdCatalog = new NsdCatalog().setNsd(nsds);
        osmPackage = new OsmNsdPackage().setNsdCatalog(nsdCatalog);
    }

    public OsmNsdPackage getPackage() {
        return osmPackage;
    }
}
