
package com.codescene.data.delta;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "function",
    "change-details"
})
@Generated("jsonschema2pojo")
public class FunctionFinding {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("function")
    private Function function;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("change-details")
    private List<ChangeDetail> changeDetails = new ArrayList<ChangeDetail>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public FunctionFinding() {
    }

    public FunctionFinding(Function function, List<ChangeDetail> changeDetails) {
        super();
        this.function = function;
        this.changeDetails = changeDetails;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("function")
    public Function getFunction() {
        return function;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("function")
    public void setFunction(Function function) {
        this.function = function;
    }

    public FunctionFinding withFunction(Function function) {
        this.function = function;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("change-details")
    public List<ChangeDetail> getChangeDetails() {
        return changeDetails;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("change-details")
    public void setChangeDetails(List<ChangeDetail> changeDetails) {
        this.changeDetails = changeDetails;
    }

    public FunctionFinding withChangeDetails(List<ChangeDetail> changeDetails) {
        this.changeDetails = changeDetails;
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.function == null)? 0 :this.function.hashCode()));
        result = ((result* 31)+((this.changeDetails == null)? 0 :this.changeDetails.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof FunctionFinding) == false) {
            return false;
        }
        FunctionFinding rhs = ((FunctionFinding) other);
        return (((this.function == rhs.function)||((this.function!= null)&&this.function.equals(rhs.function)))&&((this.changeDetails == rhs.changeDetails)||((this.changeDetails!= null)&&this.changeDetails.equals(rhs.changeDetails))));
    }

}
