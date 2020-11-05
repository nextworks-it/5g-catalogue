package it.nextworks.nfvmano.catalogue.plugins.mano.fivegrowthCataloguePlugin.model;

import it.nextworks.nfvmano.catalogue.plugins.mano.onapCataloguePlugin.model.SoObjectType;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

public class SoVnfQueryResponse {
    private List<SoVnfInfoObject> queryResult = new ArrayList<>();

    public List<SoVnfInfoObject> getQueryResult() {
        return queryResult;
    }

    public void setQueryResult(List<SoVnfInfoObject> queryResult) {
        this.queryResult = queryResult;
    }
}
