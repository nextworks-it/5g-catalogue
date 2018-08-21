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
package it.nextworks.nfvmano.catalogue.plugins.mano.osm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girtel.osmclient.OSMClient;
import com.girtel.osmclient.OSMComponent;
import com.girtel.osmclient.utils.HTTPResponse;
import it.nextworks.nfvmano.catalogue.OperationFailedException;
import it.nextworks.nfvmano.catalogue.messages.NsdChangeNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdDeletionNotificationMessage;
import it.nextworks.nfvmano.catalogue.messages.NsdOnBoardingNotificationMessage;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdDeletionNotification;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.PnfdOnboardingNotification;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOPlugin;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class OpenSourceMANOPlugin extends MANOPlugin {

	private static final String NSD_FILE_NAME = "cirros_2vnf_ns.tar.gz"; // Should be a filename in src/main/resources
	private static final String NSD_ID = "cirros_2vnf_ns";

	private static final String[] VNF_FILE_NAMES = {"cirros_vnf.tar.gz"}; // Should be a filename in src/main/resources
	private static final String[] VNFD_IDS = {"cirros_vnf"};

	private static final Logger log = LoggerFactory.getLogger(OpenSourceMANOPlugin.class);

	private OSMClient osmClient;
	private final OSMMano osm;

	public OpenSourceMANOPlugin(MANOType manoType, MANO mano, String kafkaBootstrapServers) {
		super(manoType, mano, kafkaBootstrapServers);
		if (MANOType.OSM != manoType) {
			throw new IllegalArgumentException("OSM plugin requires an OSM type MANO");
		}
		osm = (OSMMano) mano;
	}

	private final Executor callbacks = Executors.newCachedThreadPool();

	@Override
	public void init() {
		super.init();
		initOsmConnection();
	}

	public void initOsmConnection() {
		osmClient = new OSMClient(osm.getIpAddress(), osm.getUsername(), osm.getPassword(), osm.getProject());
		log.info(
				"OSM instance ad {}, user {}, project {} connected.",
				osm.getIpAddress(),
				osm.getUsername(),
				osm.getProject()
		);
	}

	@Override
	public void acceptNsdOnBoardingNotification(NsdOnBoardingNotificationMessage notification) {
		log.info(
				"Received NSD onboarding notificationfor NSD {} info id {}.",
				notification.getNsdId(), notification.getNsdInfoId()
		);
		log.debug("Body: {}", notification);
		try {
			List<String> tIds = new LinkedList<>();
			for (String vnfFileName : VNF_FILE_NAMES) {
				File vnf = loadFile(vnfFileName);
				tIds.add(onBoardPackage(vnf, UUID.randomUUID().toString()));
			}
			log.debug("Required VNFD onboarded, tIds: {} onboarding NSD", tIds);
			File nsd = loadFile(NSD_FILE_NAME);
			String tId = onBoardPackage(nsd, UUID.randomUUID().toString());
			log.info("Successfully uploaded nsd {} with info ID {}.",
					notification.getNsdId(),
					notification.getNsdInfoId()
			);
			log.debug("tId: {}", tId);
		} catch (OperationFailedException e) {
			log.error("Could not onboard NSD: %s", e.getMessage());
			log.debug("Error details: ", e);
		}
	}

	@Override
	public void acceptNsdChangeNotification(NsdChangeNotificationMessage notification) {
		log.info("Received NSD change notification.");
		log.debug("Body: {}", notification);
	}

	@Override
	public void acceptNsdDeletionNotification(NsdDeletionNotificationMessage notification) {
		log.info(
				"Received NSD deletion notification for NSD {} info id {}.",
				notification.getNsdId(), notification.getNsdInfoId()
		);
		log.debug("Body: {}", notification);
		try {
			deleteNsd(NSD_ID, "test_delete_nsd");
			for (String vnfdId : VNFD_IDS) {
				deleteVnfd(vnfdId, "test_delete_vnf");
			}
			log.info("Successfully deleted nsd {} with info ID {}.",
					notification.getNsdId(),
					notification.getNsdInfoId()
			);
		} catch (OperationFailedException e) {
			log.error("Could not delete NSD: %s", e.getMessage());
			log.debug("Error details: ", e);
		}
	}

	@Override
	public void acceptPnfdOnBoardingNotification(PnfdOnboardingNotification notification) {
		log.info("Received PNFD onboarding notification.");
		log.debug("Body: {}", notification);
	}

	@Override
	public void acceptPnfdDeletionNotification(PnfdDeletionNotification notification) {
		log.info("Received PNFD deletion notification.");
		log.debug("Body: {}", notification);
	}

	private File loadFile(String filename) {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(filename);
		if (resource == null) {
			throw new IllegalArgumentException(String.format("Invalid resource %s", filename));
		}
		return new File(resource.getFile());
	}

	String onBoardPackage(File file, String opId) throws OperationFailedException {
		log.info("Onboarding package, opId: {}.", opId);
		HTTPResponse httpResponse = osmClient.uploadPackage(file);
		return parseResponseReturning(httpResponse, opId, true);
	}

	void deleteNsd(String nsdId, String opId) throws OperationFailedException {
		log.info("Deleting nsd {}, opId: {}.", nsdId, opId);
		HTTPResponse httpResponse = osmClient.deleteNSD(nsdId);
		parseResponse(httpResponse, opId);
	}

	void deleteVnfd(String vnfdId, String opId) throws OperationFailedException {
		log.info("Deleting vnfd {}, opId: {}.", vnfdId, opId);
		HTTPResponse httpResponse = osmClient.deleteVNFD(vnfdId);
		parseResponse(httpResponse, opId);
	}


	List<String> getNsdIdList() {
		return osmClient.getNSDList().stream().map(OSMComponent::toString).collect(Collectors.toList());
	}


	List<String> getVnfdIdList() {
		return osmClient.getVNFDList().stream().map(OSMComponent::toString).collect(Collectors.toList());
	}

	private void parseResponse(HTTPResponse httpResponse, String opId) throws OperationFailedException {
		parseResponseReturning(httpResponse, opId, false);
		// Ignoring the return value
	}

	private String parseResponseReturning(HTTPResponse httpResponse, String opId, boolean transaction)
			throws OperationFailedException {
		if ((httpResponse.getCode() >= 300) || (httpResponse.getCode() < 200)) {
			log.error("Unexpected response code {}: {}. OpId",
					httpResponse.getCode(), httpResponse.getMessage(), opId);
			log.debug("Response content: {}.", httpResponse.getContent());
			throw new OperationFailedException(
					String.format("Unexpected code from MANO: %s", httpResponse.getCode())
			);
		}
		// else, code is 2XX
		log.info("Package onboarding successful. OpId: {}.", opId);
		if (transaction) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				TransactionResponse transactionResponse =
						mapper.readValue(httpResponse.getContent(), TransactionResponse.class);
				return transactionResponse.transactionId;
			} catch (IOException e) {
				log.error("Could not read transaction ID from response: {}.", e.getMessage());
				log.debug("Exception details: ", e);
				throw new OperationFailedException("Could not read transaction ID from MANO response");
			}
		}
		// else, return the content, which will probably go unused.
		return httpResponse.getContent();
	}

	private static class TransactionResponse {
		@JsonProperty("transaction_id")
		String transactionId;
	}

	// check op status
	// GET
	// https://10.0.8.26:8443/composer/api/package-import/jobs/877d9079-360a-46a8-b068-1f2b3a32c1a1?api_server=https://localhost
	// no body
}
