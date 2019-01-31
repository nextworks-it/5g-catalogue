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
package it.nextworks.nfvmano.catalogue.engine;

import it.nextworks.nfvmano.catalogue.repos.DescriptorTemplateRepository;
import it.nextworks.nfvmano.catalogue.repos.TopologyTemplateRepository;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NS.NSNode;
import it.nextworks.nfvmano.libs.descriptors.nsd.nodes.NsVirtualLink.NsVirtualLinkNode;
import it.nextworks.nfvmano.libs.descriptors.templates.DescriptorTemplate;
import it.nextworks.nfvmano.libs.descriptors.templates.Metadata;
import it.nextworks.nfvmano.libs.descriptors.templates.TopologyTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DbPersistencyHandler {

    private static final Logger log = LoggerFactory.getLogger(DbPersistencyHandler.class);

    @Autowired
    private DescriptorTemplateRepository templateRepo;

    @Autowired
    private TopologyTemplateRepository topologyRepo;

    public DbPersistencyHandler() {
    }

    @SuppressWarnings("unused")
    public UUID createNsd(DescriptorTemplate source) throws AlreadyExistingEntityException, MalformattedElementException {
        log.debug("Saving descriptor template in DB");
        source.isValid();
        validateNsdFormat(source);

        //here we assume that each descriptor should be uniquely identified by the triple descriptor ID, vendor, version
        String descriptorId = source.getMetadata().getDescriptorId();
        String vendor = source.getMetadata().getVendor();
        String version = source.getMetadata().getVersion();
        log.debug("NSD ID " + descriptorId + " - Vendor " + vendor + " - Version " + version);
        if (templateRepo.findByMetadataDescriptorIdAndMetadataVendorAndMetadataVersion(descriptorId, vendor, version).isPresent()) {
            log.error("NSD already available in DB. Impossible to create a new one.");
            throw new AlreadyExistingEntityException("NSD already available in DB. Impossible to create a new one." + "NSD ID " + descriptorId + " - Vendor " + vendor + " - Version " + version);
        }

        DescriptorTemplate target = new DescriptorTemplate(source.getToscaDefinitionsVersion(),
                source.getToscaDefaultNamespace(),
                source.getDescription(),
                new Metadata(descriptorId, vendor, version));

        templateRepo.saveAndFlush(target);
        log.debug("Descriptor template saved");

        TopologyTemplate topologyTarget = new TopologyTemplate(target);
        topologyRepo.saveAndFlush(topologyTarget);
        log.debug("Topology template saved");

        TopologyTemplate topologySource = source.getTopologyTemplate();

        @SuppressWarnings("unused")
        Map<String, NsVirtualLinkNode> nsVirtualLinks = topologySource.getNsVirtualLinkNodes();
        Map<String, NSNode> nsNodes = topologySource.getNSNodes();

        //TODO: save the rest of the NSD

        UUID nsdId = target.getId();
        return nsdId;

    }

    public DescriptorTemplate getNsd(UUID internalNsdId) throws NotExistingEntityException {
        log.debug("Retrieving NSD with internal ID " + internalNsdId);
        Optional<DescriptorTemplate> nsd = templateRepo.findById(internalNsdId);
        if (nsd.isPresent()) return nsd.get();
        else {
            log.debug("Descriptor Template with internal ID " + internalNsdId + " not found in DB.");
            throw new NotExistingEntityException("Descriptor Template with internal ID " + internalNsdId + " not found in DB.");
        }
    }

    public synchronized void deleteNsd(UUID internalNsdId) throws NotExistingEntityException {
        log.debug("Removing NSD with internal ID " + internalNsdId);
        DescriptorTemplate dt = getNsd(internalNsdId);
        this.templateRepo.delete(dt);
        log.debug("NSD removed from DB.");
    }


    private void validateNsdFormat(DescriptorTemplate source) throws MalformattedElementException {
        TopologyTemplate topologySource = source.getTopologyTemplate();
        if (topologySource == null)
            throw new MalformattedElementException("NSD descriptor template without topology template");
        Map<String, NSNode> nsNodes = topologySource.getNSNodes();
        if ((nsNodes == null) || (nsNodes.size() != 1))
            throw new MalformattedElementException("NS node template not available or multiple.");
    }

}
