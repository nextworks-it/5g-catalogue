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
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;

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
