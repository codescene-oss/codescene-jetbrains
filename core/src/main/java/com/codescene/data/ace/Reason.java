
package com.codescene.data.ace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "summary",
    "details"
})
@Generated("jsonschema2pojo")
public class Reason {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("summary")
    private String summary;
    @JsonProperty("details")
    private List<ReasonDetails> details = new ArrayList<ReasonDetails>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("summary")
    public String getSummary() {
        return summary;
    }

    @JsonProperty("details")
    public Optional<List<ReasonDetails>> getDetails() {
        return Optional.ofNullable(details);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.summary == null)? 0 :this.summary.hashCode()));
        result = ((result* 31)+((this.details == null)? 0 :this.details.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Reason) == false) {
            return false;
        }
        Reason rhs = ((Reason) other);
        return (((this.summary == rhs.summary)||((this.summary!= null)&&this.summary.equals(rhs.summary)))&&((this.details == rhs.details)||((this.details!= null)&&this.details.equals(rhs.details))));
    }

}
