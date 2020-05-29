package it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin;

import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OnapDriver {

    private static final Logger log = LoggerFactory.getLogger(OnapDriver.class);

    private String baseUrl;

    private RestTemplate restTemplate;

    public OnapDriver(String ipAddress, String port) {
        baseUrl = String.format("http://%s:%s/service_specification", ipAddress, port);
        this.restTemplate= new RestTemplate(new BufferingClientHttpRequestFactory(
                new SimpleClientHttpRequestFactory()
        ));
    }

    public List<String> getNsIds() throws FailedOperationException{
        HttpEntity<?> getEntity = new HttpEntity<>(new HttpHeaders());
        try {
            ResponseEntity<List<String>> httpResponse = restTemplate.exchange(baseUrl, HttpMethod.GET, getEntity, new ParameterizedTypeReference<List<String>>() {});
            if(httpResponse.getStatusCode() == HttpStatus.OK)
                return new ArrayList<>(httpResponse.getBody());
            else
                return null;
        }catch (HttpClientErrorException e) {
            throw new FailedOperationException("Error during Ns Ids retrieval : Client error");
        } catch (HttpServerErrorException e) {
            throw new FailedOperationException("Error during Ns Ids retrieval : Server error");
        } catch (UnknownHttpStatusCodeException e) {
            throw new FailedOperationException("Error during Ns Ids retrieval: Unknown error");
        } catch (Exception e) {
            throw new FailedOperationException("Generic error while interacting with Onap instance at url: " + baseUrl);
        }
    }

    public File getNsPackage(String nsId, File onapDir) throws FailedOperationException, IOException {
        String url = String.format("%s/%s", baseUrl, nsId);
        try {
            byte[] downloadedBytes = restTemplate.getForObject(url, byte[].class);
            File nsPackage = new File(onapDir, nsId + ".zip");
            FileOutputStream fos = new FileOutputStream(nsPackage);
            fos.write(downloadedBytes);
            fos.close();
            return nsPackage;
        }catch (HttpClientErrorException e) {
            throw new FailedOperationException("Error during Ns Package retrieval : Client error");
        } catch (HttpServerErrorException e) {
            throw new FailedOperationException("Error during Ns Package retrieval : Server error");
        } catch (UnknownHttpStatusCodeException e) {
            throw new FailedOperationException("Error during Ns Package retrieval: Unknown error");
        } catch (Exception e) {
            throw new FailedOperationException("Generic error while interacting with Onap instance at url: " + baseUrl);
        }
    }
}
