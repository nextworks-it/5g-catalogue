package it.nextworks.nfvmano.catalogue.plugins.mano.osm.elements;

import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;

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

    public NsdBuilder() {

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
            Map<String, List<ConstituentVnfd>> vlToVnfMapping
    ) {
        return new Vld()
                .setId(vlId)
                .setName(vlId)
                .setShortName(vlId)
                .setMgmtNetwork(vlId.endsWith("_mgmt") ? "true" : "false") // TODO
                .setType("ELAN")
                .setVnfdConnectionPointRef(
                        vlToVnfMapping.getOrDefault(vlId, Collections.emptyList())
                                .stream()
                                .map(vnf -> makeCPRef(
                                        vnf.getVnfdIdRef(),
                                        vnf.getMemberVnfIndex(),
                                        "eth0"
                                ))
                                .collect(Collectors.toList())
                );
    }

    void parseDescriptorTemplate(DescriptorTemplate template) throws MalformattedElementException {
        dt = template;

        if (!(dt.getTopologyTemplate().getNSNodes().size() == 1)) {
            throw new IllegalArgumentException("Too many Nsds");
        }

        NSNode nsd = dt.getTopologyTemplate().getNSNodes().values().iterator().next();

        Map<String, List<ConstituentVnfd>> vlToVnfMapping = new HashMap<>();

        List<ConstituentVnfd> constituentVnfds = dt.getTopologyTemplate().getVNFNodes().values()
                .stream()
                .map(vnf -> {
                            ConstituentVnfd output = new ConstituentVnfd()
                                    .setVnfdIdRef(vnf.getProperties().getDescriptorId());
                            vnf.getRequirements().getVirtualLink().forEach(
                                    vlId -> {
                                        vlToVnfMapping.putIfAbsent(vlId, new ArrayList<>());
                                        vlToVnfMapping.get(vlId).add(output);
                                    }
                            );
                            return output;
                        }
                )
                .collect(Collectors.toList());
        for (int i = 0; i < constituentVnfds.size(); i++) {
            constituentVnfds.get(i).setVnfdIdRef(String.valueOf(i));
        }

        List<Vld> vlds = dt.getTopologyTemplate().getNsVirtualLinkNodes().entrySet()
                .stream()
                .map(e -> makeVld(e.getKey(), e.getValue(), vlToVnfMapping))
                .collect(Collectors.toList());

        List<Nsd> nsds = new ArrayList<>();
        nsds.add(
                new Nsd()
                        .setId(dt.getMetadata().getDescriptorId())
                        .setName(nsd.getProperties().getName())
                        .setShortName(nsd.getProperties().getName())
                        .setDescription(nsd.getProperties().getName())
                        .setVendor(nsd.getProperties().getDesigner())
                        .setVersion(nsd.getProperties().getVersion())
                        .setLogo("osm_2x.png") // TODO get logo?
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
