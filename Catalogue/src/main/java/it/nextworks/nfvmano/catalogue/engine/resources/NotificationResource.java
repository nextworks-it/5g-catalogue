package it.nextworks.nfvmano.catalogue.engine.resources;

import javax.persistence.Embeddable;

import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

@Embeddable
public class NotificationResource {
	
	private String nsdInfoId;
	private OperationStatus opStatus;
	
	public NotificationResource() {

	}

	public NotificationResource(String nsdInfoId, OperationStatus opStatus) {
		this.nsdInfoId = nsdInfoId;
		this.opStatus = opStatus;
	}

	public String getNsdInfoId() {
		return nsdInfoId;
	}

	public void setNsdInfoId(String nsdInfoId) {
		this.nsdInfoId = nsdInfoId;
	}

	public OperationStatus getOpStatus() {
		return opStatus;
	}

	public void setOpStatus(OperationStatus opStatus) {
		this.opStatus = opStatus;
	}	
}
