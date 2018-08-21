package it.nextworks.nfvmano.catalogue.plugins.mano;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by Marco Capitani on 21/08/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
@Entity
public class DummyMano extends MANO {

    // Only used for JPA, no functionality required

    public DummyMano() {
        // JPA
    }

    public DummyMano(String manoId, MANOType manoType) {
        super(manoId, manoType);
    }
}
