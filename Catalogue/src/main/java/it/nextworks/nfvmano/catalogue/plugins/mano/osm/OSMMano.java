package it.nextworks.nfvmano.catalogue.plugins.mano.osm;

import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;

import javax.persistence.Entity;

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
        // JPA
    }

    public OSMMano(String manoId, String ipAddress, String username, String password, String project) {
        super(manoId, MANOType.OSM);
        this.ipAddress = ipAddress;
        this.username = username;
        this.password = password;
        this.project = project;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getProject() {
        return project;
    }
}
