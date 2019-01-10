package it.nextworks.nfvmano.libs.osmr4Client.utilities;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

/**
 * This class represents a HTTP Response
 *
 */
public class HttpResponse {

    private Integer code;
    private String  message;
    private String  content;
    private Path    receivedFilePath;

    /**
     * Constructor
     *
     * @param code HTTP code
     * @param message HTTP message
     * @param content HTTP content
     * @param receivedFilePath Local path of the downloaded file, if any
     */
    private HttpResponse(Integer code, String message, String content, Path receivedFilePath) {
        this.code = code;
        this.message = message;
        this.content = content;
        this.receivedFilePath = receivedFilePath;
    }

    /**
     * Obtains HTTP code, e.c. 200
     * @return HTTP code
     */
    public Integer getCode()
    {
        return code;
    }

    /**
     * Obtains HTTP message, e.c. OK
     * @return HTTP message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Obtains HTTP response content
     * @return HTTP response content
     */
    public String getContent()
    {
        return content;
    }

    /**
     * Obtains the local path of the received file
     * @return the local path of the received file
     */
    public Path getFilePath()
    {
        return receivedFilePath;
    }

    @Override
    public String toString() {

        return "Status : (" + code + ", " + message + ")\r\n" +
                "Content : \n " + content + "\r\n";
    }

    /**
     * Obtains a HTTP Response from a HTTP Connection
     * @param conn HTTP Connection to process
     * @return HTTP Response
     */
    public static HttpResponse getResponseFromHTTPConnection(HttpURLConnection conn) {

        BufferedReader in;
        int code = 0;
        String message = "";
        StringBuilder content = new StringBuilder();
        Path filePath = null;

        try {
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            String line;

            if(code < HTTP_BAD_REQUEST) {
                if(conn.getContentType().equals("application/zip")){
                    String [] url = conn.getURL().toString().split("/");
                    filePath = Paths.get("/tmp/" + url[url.length - 2] + ".tar.gz");
                    Files.copy(conn.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                else {
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = in.readLine()) != null)
                        content.append(line);
                }
            }
            else {
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                while ((line = in.readLine()) != null)
                    content.append(line);
                in.close();
            }
            conn.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new HttpResponse(code, message, content.toString().replaceAll("\\{}", "\"empty\""), filePath);
    }
}
