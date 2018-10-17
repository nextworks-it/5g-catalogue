package it.nextworks.nfvmano.catalogue.engine.resources;

import javax.persistence.Embeddable;

import it.nextworks.nfvmano.catalogue.messages.CatalogueMessageType;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;

@Embeddable
public class NotificationResource {
	
	private String nsdInfoId;
	private CatalogueMessageType operation;
	private OperationStatus opStatus;
	
	public NotificationResource() {

	}

	public NotificationResource(String nsdInfoId, CatalogueMessageType operation, OperationStatus opStatus) {
		this.nsdInfoId = nsdInfoId;
		this.operation = operation;
		this.opStatus = opStatus;
	}

	public String getNsdInfoId() {
		return nsdInfoId;
	}

	public void setNsdInfoId(String nsdInfoId) {
		this.nsdInfoId = nsdInfoId;
	}
	
	public CatalogueMessageType getOperation() {
		return operation;
	}

	public void setOperation(CatalogueMessageType operation) {
		this.operation = operation;
	}

	public OperationStatus getOpStatus() {
		return opStatus;
	}

	public void setOpStatus(OperationStatus opStatus) {
		this.opStatus = opStatus;
	}	
}
