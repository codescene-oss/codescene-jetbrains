
package com.codescene.data.ace;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "category",
    "line"
})
@Generated("jsonschema2pojo")
public class RefactoringTarget {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("category")
    private String category;
    /**
     * Start line for the code smell.
     * (Required)
     * 
     */
    @JsonProperty("line")
    @JsonPropertyDescription("Start line for the code smell.")
    private Integer line;

    /**
     * No args constructor for use in serialization
     * 
     */
    public RefactoringTarget() {
    }

    /**
     * 
     * @param line
     *     Start line for the code smell.
     */
    public RefactoringTarget(String category, Integer line) {
        super();
        this.category = category;
        this.line = line;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("category")
    public String getCategory() {
        return category;
    }

    public RefactoringTarget withCategory(String category) {
        this.category = category;
        return this;
    }

    /**
     * Start line for the code smell.
     * (Required)
     * 
     */
    @JsonProperty("line")
    public Integer getLine() {
        return line;
    }

    public RefactoringTarget withLine(Integer line) {
        this.line = line;
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.category == null)? 0 :this.category.hashCode()));
        result = ((result* 31)+((this.line == null)? 0 :this.line.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RefactoringTarget) == false) {
            return false;
        }
        RefactoringTarget rhs = ((RefactoringTarget) other);
        return (((this.category == rhs.category)||((this.category!= null)&&this.category.equals(rhs.category)))&&((this.line == rhs.line)||((this.line!= null)&&this.line.equals(rhs.line))));
    }

}
