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
package it.nextworks.nfvmano.catalogue.plugins.mano;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class MANO {

	@Id
	@GeneratedValue
	private Long id;

	private String manoId;
	private MANOType manoType;

	public MANO() {
		// JPA only
	}

	public MANO(String manoId, MANOType manoType) {
		this.manoId = manoId;
		this.manoType = manoType;
	}

	public String getManoId() {
		return manoId;
	}

	public void setManoId(String manoId) {
		this.manoId = manoId;
	}

	public MANOType getManoType() {
		return manoType;
	}

	public void setManoType(MANOType manoType) {
		this.manoType = manoType;
	}
}
