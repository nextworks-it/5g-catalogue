package it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin.model;

import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.SoObjectType;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

public class SoNsQueryResponse {

    private List<SoNsInfoObject> queryResult = new ArrayList<>();

    public List<SoNsInfoObject> getQueryResult() {
        return queryResult;
    }

    public void setQueryResult(List<SoNsInfoObject> queryResult) {
        this.queryResult = queryResult;
    }
}
