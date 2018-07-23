package it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-07-23T16:31:35.952+02:00")

public class ApiException extends Exception{
    private int code;
    public ApiException (int code, String msg) {
        super(msg);
        this.code = code;
    }
}
