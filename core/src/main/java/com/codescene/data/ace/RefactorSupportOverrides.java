
package com.codescene.data.ace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "max-input-loc",
    "code-smells"
})
@Generated("jsonschema2pojo")
public class RefactorSupportOverrides {

    /**
     * Maximum input loc allowed
     * 
     */
    @JsonProperty("max-input-loc")
    @JsonPropertyDescription("Maximum input loc allowed")
    private Integer maxInputLoc;
    /**
     * List of supported code-smells
     * 
     */
    @JsonProperty("code-smells")
    @JsonPropertyDescription("List of supported code-smells")
    private List<String> codeSmells = new ArrayList<String>();

    /**
     * Maximum input loc allowed
     * 
     */
    @JsonProperty("max-input-loc")
    public Optional<Integer> getMaxInputLoc() {
        return Optional.ofNullable(maxInputLoc);
    }

    /**
     * List of supported code-smells
     * 
     */
    @JsonProperty("code-smells")
    public Optional<List<String>> getCodeSmells() {
        return Optional.ofNullable(codeSmells);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.maxInputLoc == null)? 0 :this.maxInputLoc.hashCode()));
        result = ((result* 31)+((this.codeSmells == null)? 0 :this.codeSmells.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RefactorSupportOverrides) == false) {
            return false;
        }
        RefactorSupportOverrides rhs = ((RefactorSupportOverrides) other);
        return (((this.maxInputLoc == rhs.maxInputLoc)||((this.maxInputLoc!= null)&&this.maxInputLoc.equals(rhs.maxInputLoc)))&&((this.codeSmells == rhs.codeSmells)||((this.codeSmells!= null)&&this.codeSmells.equals(rhs.codeSmells))));
    }

}
