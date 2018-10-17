package it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.KeyValuePairs;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.NsdOperationalStateType;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * NsdInfoModifications
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-07-23T16:31:35.952+02:00")

public class NsdInfoModifications {
	@JsonProperty("nsdOperationalState")
	private NsdOperationalStateType nsdOperationalState = null;

	@JsonProperty("userDefinedData")
	private KeyValuePairs userDefinedData = null;

	public NsdInfoModifications nsdOperationalState(NsdOperationalStateType nsdOperationalState) {
		this.nsdOperationalState = nsdOperationalState;
		return this;
	}

	/**
	 * Get nsdOperationalState
	 * 
	 * @return nsdOperationalState
	 **/
	@ApiModelProperty(value = "")

	@Valid

	public NsdOperationalStateType getNsdOperationalState() {
		return nsdOperationalState;
	}

	public void setNsdOperationalState(NsdOperationalStateType nsdOperationalState) {
		this.nsdOperationalState = nsdOperationalState;
	}

	public NsdInfoModifications userDefinedData(KeyValuePairs userDefinedData) {
		this.userDefinedData = userDefinedData;
		return this;
	}

	/**
	 * Get userDefinedData
	 * 
	 * @return userDefinedData
	 **/
	@ApiModelProperty(value = "")

	@Valid

	public KeyValuePairs getUserDefinedData() {
		return userDefinedData;
	}

	public void setUserDefinedData(KeyValuePairs userDefinedData) {
		this.userDefinedData = userDefinedData;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NsdInfoModifications nsdInfoModifications = (NsdInfoModifications) o;
		return Objects.equals(this.nsdOperationalState, nsdInfoModifications.nsdOperationalState)
				&& Objects.equals(this.userDefinedData, nsdInfoModifications.userDefinedData);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nsdOperationalState, userDefinedData);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class NsdInfoModifications {\n");

		sb.append("    nsdOperationalState: ").append(toIndentedString(nsdOperationalState)).append("\n");
		sb.append("    userDefinedData: ").append(toIndentedString(userDefinedData)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}
