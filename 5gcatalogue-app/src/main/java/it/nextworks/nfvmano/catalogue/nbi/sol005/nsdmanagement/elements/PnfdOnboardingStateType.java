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
package it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The enumeration PnfdOnboardingStateType shall comply with the provisions
 * defined in Table 5.5.4.6-1 of GS-NFV SOL005. It indicates the onboarding
 * state of the individual PNF descriptor resource. CREATED = The PNF descriptor
 * resource is created. UPLOADING = The associated PNFD content is being
 * uploaded. PROCESSING = The associated PNFD content is being processed, e.g.
 * validation. ONBOARDED = The associated PNFD content is on-boarded.
 */
public enum PnfdOnboardingStateType {

	CREATED("CREATED"),

	UPLOADING("UPLOADING"),

	PROCESSING("PROCESSING"),

	ONBOARDING("ONBOARDING");

	private String value;

	PnfdOnboardingStateType(String value) {
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static PnfdOnboardingStateType fromValue(String text) {
		for (PnfdOnboardingStateType b : PnfdOnboardingStateType.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}
}
