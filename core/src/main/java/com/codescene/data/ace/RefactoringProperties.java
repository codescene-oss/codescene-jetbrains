
package com.codescene.data.ace;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "added-code-smells",
    "removed-code-smells"
})
@Generated("jsonschema2pojo")
public class RefactoringProperties {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("added-code-smells")
    private List<String> addedCodeSmells = new ArrayList<String>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("removed-code-smells")
    private List<String> removedCodeSmells = new ArrayList<String>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("added-code-smells")
    public List<String> getAddedCodeSmells() {
        return addedCodeSmells;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("removed-code-smells")
    public List<String> getRemovedCodeSmells() {
        return removedCodeSmells;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.addedCodeSmells == null)? 0 :this.addedCodeSmells.hashCode()));
        result = ((result* 31)+((this.removedCodeSmells == null)? 0 :this.removedCodeSmells.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RefactoringProperties) == false) {
            return false;
        }
        RefactoringProperties rhs = ((RefactoringProperties) other);
        return (((this.addedCodeSmells == rhs.addedCodeSmells)||((this.addedCodeSmells!= null)&&this.addedCodeSmells.equals(rhs.addedCodeSmells)))&&((this.removedCodeSmells == rhs.removedCodeSmells)||((this.removedCodeSmells!= null)&&this.removedCodeSmells.equals(rhs.removedCodeSmells))));
    }

}
