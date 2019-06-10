package it.nextworks.nfvmano.catalogue.auth;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthUtilities {

    private static final Logger log = LoggerFactory.getLogger(AuthUtilities.class);

    public static void decodeJWT(String jwtToken) {
        log.debug("Going to decode JWT...");
        String[] split_string = jwtToken.split("\\.");
        String base64EncodedHeader = split_string[0];
        String base64EncodedBody = split_string[1];
        String base64EncodedSignature = split_string[2];

        Base64 base64Url = new Base64(true);
        String header = new String(base64Url.decode(base64EncodedHeader));
        log.debug("JWT Header: " + header);

        String body = new String(base64Url.decode(base64EncodedBody));
        log.debug("JWT Body: " + body);
    }
}
