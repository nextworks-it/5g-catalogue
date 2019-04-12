package it.nextworks.nfvmano.catalogue.engine.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.*;
import it.nextworks.nfvmano.catalogue.repos.ContentType;
import it.nextworks.nfvmano.libs.common.exceptions.NotPermittedOperationException;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.*;

@Entity
public class PnfdInfoResource {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID pnfdId;
    private String pnfdName;
    private String pnfdVersion;
    private String pnfdProvider;
    private UUID pnfdInvariantId;
    private PnfdOnboardingStateType pnfdOnboardingState;
    private PnfdUsageStateType pnfdUsageState;

    private ContentType contentType;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private List<String> pnfdFilename = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private Map<String, String> userDefinedData = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private Map<String, NotificationResource> acknowledgedOnboardOpConsumers = new HashMap<>();

    public PnfdInfoResource() {
    }

    public PnfdInfoResource(Map<String, String> userDefinedData) {
        if (userDefinedData != null) this.userDefinedData = userDefinedData;
        pnfdOnboardingState = PnfdOnboardingStateType.CREATED;
        pnfdUsageState = PnfdUsageStateType.NOT_IN_USE;
        contentType = ContentType.UNSPECIFIED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPnfdId() {
        return pnfdId;
    }

    public String getPnfdName() {
        return pnfdName;
    }

    public String getPnfdVersion() {
        return pnfdVersion;
    }

    public String getPnfdProvider() {
        return pnfdProvider;
    }

    public UUID getPnfdInvariantId() {
        return pnfdInvariantId;
    }

    public PnfdOnboardingStateType getPnfdOnboardingState() {
        return pnfdOnboardingState;
    }

    public PnfdUsageStateType getPnfdUsageState() {
        return pnfdUsageState;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public List<String> getPnfdFilename() {
        return pnfdFilename;
    }

    public Map<String, String> getUserDefinedData() {
        return userDefinedData;
    }

    public Map<String, NotificationResource> getAcknowledgedOnboardOpConsumers() {
        return acknowledgedOnboardOpConsumers;
    }

    public void setPnfdId(UUID pnfdId) {
        this.pnfdId = pnfdId;
    }

    public void setPnfdName(String pnfdName) {
        this.pnfdName = pnfdName;
    }

    public void setPnfdVersion(String pnfdVersion) {
        this.pnfdVersion = pnfdVersion;
    }

    public void setPnfdProvider(String pnfdProvider) {
        this.pnfdProvider = pnfdProvider;
    }

    public void setPnfdInvariantId(UUID pnfdInvariantId) {
        this.pnfdInvariantId = pnfdInvariantId;
    }

    public void setPnfdOnboardingState(PnfdOnboardingStateType pnfdOnboardingState) {
        this.pnfdOnboardingState = pnfdOnboardingState;
    }

    public void setPnfdUsageState(PnfdUsageStateType pnfdUsageState) {
        this.pnfdUsageState = pnfdUsageState;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public void setPnfdFilename(List<String> pnfdFilename) {
        this.pnfdFilename = pnfdFilename;
    }

    public void addPnfdFilename(String filename) {
        this.pnfdFilename.add(filename);
    }

    public void setUserDefinedData(Map<String, String> userDefinedData) {
        this.userDefinedData = userDefinedData;
    }

    public void setAcknowledgedOnboardOpConsumers(Map<String, NotificationResource> acknowledgedOnboardOpConsumers) {
        this.acknowledgedOnboardOpConsumers = acknowledgedOnboardOpConsumers;
    }

    public void isDeletable() throws NotPermittedOperationException {
        if (pnfdUsageState != PnfdUsageStateType.NOT_IN_USE)
            throw new NotPermittedOperationException("PNFD info " + this.id + " cannot be deleted because IN USE");
    }
}
