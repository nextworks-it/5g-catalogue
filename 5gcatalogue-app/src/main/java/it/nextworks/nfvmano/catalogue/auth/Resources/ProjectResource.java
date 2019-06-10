package it.nextworks.nfvmano.catalogue.auth.Resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class ProjectResource {

    UUID projectId;
    String projectDescription;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    List<String> users = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    List<String> nsds = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    List<String> pnfds = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    List<String> vnfPackages = new ArrayList<>();
    @Id
    @GeneratedValue
    private UUID id;

    public ProjectResource() {
    }

    public ProjectResource(UUID projectId, String projectDescription) {
        this.projectId = projectId;
        this.projectDescription = projectDescription;
    }

    public ProjectResource(UUID projectId, String projectDescription, List<String> users) {
        this.projectId = projectId;
        this.projectDescription = projectDescription;
        this.users = users;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public List<String> getNsds() {
        return nsds;
    }

    public void setNsds(List<String> nsds) {
        this.nsds = nsds;
    }

    public List<String> getPnfds() {
        return pnfds;
    }

    public void setPnfds(List<String> pnfds) {
        this.pnfds = pnfds;
    }

    public List<String> getVnfPackages() {
        return vnfPackages;
    }

    public void setVnfPackages(List<String> vnfPackages) {
        this.vnfPackages = vnfPackages;
    }

    public boolean isDeletable() {
        if (!users.isEmpty() || !nsds.isEmpty() || !pnfds.isEmpty() || !vnfPackages.isEmpty()) {
            return false;
        }
        return true;
    }
}
