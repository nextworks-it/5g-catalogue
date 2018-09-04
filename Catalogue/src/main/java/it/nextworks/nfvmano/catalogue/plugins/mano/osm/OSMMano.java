package it.nextworks.nfvmano.catalogue.plugins.mano.osm;

import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Marco Capitani on 20/08/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
@Entity
public class OSMMano extends MANO {

    private String ipAddress;
    private String username;
    private String password;
    private String project;

    public OSMMano() {
        // JPA only
    }

    public OSMMano(String manoId, String ipAddress, String username, String password, String project) {
        super(manoId, MANOType.OSM);
        this.ipAddress = ipAddress;
        this.username = username;
        this.password = password;
        this.project = project;
    }

    @JsonProperty("ipAddress")
    public String getIpAddress() {
        return ipAddress;
    }

    @JsonProperty("username")
    public String getUsername() {
        return username;
    }

    @JsonProperty("password")
    public String getPassword() {
        return password;
    }

    @JsonProperty("project")
    public String getProject() {
        return project;
    }
    
    public void isValid() throws MalformattedElementException {
    	if (this.ipAddress == null)
    		throw new MalformattedElementException("OSMMano without ipAddress");
    	if (this.username == null)
    		throw new MalformattedElementException("OSMMano without username");
    	if (this.password == null)
    		throw new MalformattedElementException("OSMMano without password");
    }
}
