package it.nextworks.nfvmano.catalogue.common;

import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.ProblemDetails;

public class Utilities {

	public Utilities() {
		// TODO Auto-generated constructor stub
	}

	public static ProblemDetails buildProblemDetails(int status, String details) {
		ProblemDetails pd = new ProblemDetails();
		pd.setDetail(details);
		pd.setStatus(status);
		return pd;
	}
	
}
