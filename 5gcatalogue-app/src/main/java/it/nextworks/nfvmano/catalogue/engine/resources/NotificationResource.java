/*
* Copyright 2018 Nextworks s.r.l.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
