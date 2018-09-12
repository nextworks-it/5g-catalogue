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
package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Marco Capitani on 10/09/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
public class NsdBuilder {

    private OsmNsdPackage osmPackage;
    private DescriptorTemplate dt;
    private final File defaultLogo;

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
                .setMgmtNetwork(vlId.endsWith("_mgmt") ? "true" : "false") // TODO
                .setType("ELAN")
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

    public void parseDescriptorTemplate(DescriptorTemplate template) throws MalformattedElementException {
        dt = template;

        if (!(dt.getTopologyTemplate().getNSNodes().size() == 1)) {
            throw new IllegalArgumentException("Too many Nsds");
        }

        NSNode nsd = dt.getTopologyTemplate().getNSNodes().values().iterator().next();

        Map<String, Map<ConstituentVnfd, String>> vlToVnfMapping = new HashMap<>();

        List<ConstituentVnfd> constituentVnfds = dt.getTopologyTemplate().getVNFNodes().values()
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
                .collect(Collectors.toList());
        for (int i = 0; i < constituentVnfds.size(); i++) {
            constituentVnfds.get(i).setMemberVnfIndex(i + 1);
        }

        List<Vld> vlds = dt.getTopologyTemplate().getNsVirtualLinkNodes().entrySet()
                .stream()
                .map(e -> makeVld(e.getKey(), e.getValue(), vlToVnfMapping))
                .collect(Collectors.toList());

        List<Nsd> nsds = new ArrayList<>();
        nsds.add(
                new Nsd()
                        .setId(nsd.getProperties().getDescriptorId())
                        .setName(nsd.getProperties().getDescriptorId())
                        .setShortName(nsd.getProperties().getName())
                        .setDescription(dt.getDescription()) // TODO: this is not ideal.
                        .setVendor(nsd.getProperties().getDesigner())
                        .setVersion(nsd.getProperties().getVersion())
                        .setLogo(defaultLogo.getName()) // TODO get logo?
                        .setConstituentVnfd(constituentVnfds)
                        .setVld(vlds)
        );
        NsdCatalog nsdCatalog = new NsdCatalog().setNsd(nsds);
        osmPackage = new OsmNsdPackage().setNsdCatalog(nsdCatalog);
    }

    public OsmNsdPackage getPackage() {
        return osmPackage;
    }
}
