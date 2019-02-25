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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANO;
import it.nextworks.nfvmano.catalogue.plugins.mano.MANOType;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private Map<String, String> osmIdTranslation;

    public OSMMano() {
        // JPA only
    }

    public OSMMano(String manoId, String ipAddress, String username, String password, String project, MANOType manoType) {
        super(manoId, manoType);
        this.ipAddress = ipAddress;
        this.username = username;
        this.password = password;
        this.project = project;
        this.osmIdTranslation = new HashMap<>();
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

    public Map<String, String> getOsmIdTranslation() { return osmIdTranslation; }

    public void isValid() throws MalformattedElementException {
        if (this.ipAddress == null)
            throw new MalformattedElementException("OSMMano without ipAddress");
        if (this.username == null)
            throw new MalformattedElementException("OSMMano without username");
        if (this.password == null)
            throw new MalformattedElementException("OSMMano without password");
    }
}
