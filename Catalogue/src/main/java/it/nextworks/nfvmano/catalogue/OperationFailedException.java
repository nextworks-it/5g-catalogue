package it.nextworks.nfvmano.catalogue;

/**
 * Created by Marco Capitani on 21/08/18.
 *
 * @author Marco Capitani <m.capitani AT nextworks.it>
 */
@SuppressWarnings("serial")
public class OperationFailedException extends Exception {

    public OperationFailedException(String message) {
        super(message);
    }
}
