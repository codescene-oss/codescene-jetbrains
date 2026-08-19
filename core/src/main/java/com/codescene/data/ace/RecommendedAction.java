
package com.codescene.data.ace;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "description",
    "details"
})
@Generated("jsonschema2pojo")
public class RecommendedAction {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("description")
    private String description;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("details")
    private String details;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("details")
    public String getDetails() {
        return details;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.details == null)? 0 :this.details.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecommendedAction) == false) {
            return false;
        }
        RecommendedAction rhs = ((RecommendedAction) other);
        return (((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description)))&&((this.details == rhs.details)||((this.details!= null)&&this.details.equals(rhs.details))));
    }

}
