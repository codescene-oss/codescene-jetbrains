
package com.codescene.data.review;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "category",
    "highlight-range",
    "details"
})
@Generated("jsonschema2pojo")
public class CodeSmell {

    /**
     * Name of codesmell.
     * (Required)
     * 
     */
    @JsonProperty("category")
    @JsonPropertyDescription("Name of codesmell.")
    private String category;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("highlight-range")
    private Range highlightRange;
    /**
     * Details about codesmell, for example nesting depth.
     * (Required)
     * 
     */
    @JsonProperty("details")
    @JsonPropertyDescription("Details about codesmell, for example nesting depth.")
    private String details;

    /**
     * No args constructor for use in serialization
     * 
     */
    public CodeSmell() {
    }

    /**
     * 
     * @param highlightRange
     *     Range for highlighting this code smell.
     * @param details
     *     Details about codesmell, for example nesting depth.
     * @param category
     *     Name of codesmell.
     */
    public CodeSmell(String category, Range highlightRange, String details) {
        super();
        this.category = category;
        this.highlightRange = highlightRange;
        this.details = details;
    }

    /**
     * Name of codesmell.
     * (Required)
     * 
     */
    @JsonProperty("category")
    public String getCategory() {
        return category;
    }

    /**
     * Name of codesmell.
     * (Required)
     * 
     */
    @JsonProperty("category")
    public void setCategory(String category) {
        this.category = category;
    }

    public CodeSmell withCategory(String category) {
        this.category = category;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("highlight-range")
    public Range getHighlightRange() {
        return highlightRange;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("highlight-range")
    public void setHighlightRange(Range highlightRange) {
        this.highlightRange = highlightRange;
    }

    public CodeSmell withHighlightRange(Range highlightRange) {
        this.highlightRange = highlightRange;
        return this;
    }

    /**
     * Details about codesmell, for example nesting depth.
     * (Required)
     * 
     */
    @JsonProperty("details")
    public String getDetails() {
        return details;
    }

    /**
     * Details about codesmell, for example nesting depth.
     * (Required)
     * 
     */
    @JsonProperty("details")
    public void setDetails(String details) {
        this.details = details;
    }

    public CodeSmell withDetails(String details) {
        this.details = details;
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.category == null)? 0 :this.category.hashCode()));
        result = ((result* 31)+((this.highlightRange == null)? 0 :this.highlightRange.hashCode()));
        result = ((result* 31)+((this.details == null)? 0 :this.details.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CodeSmell) == false) {
            return false;
        }
        CodeSmell rhs = ((CodeSmell) other);
        return ((((this.category == rhs.category)||((this.category!= null)&&this.category.equals(rhs.category)))&&((this.highlightRange == rhs.highlightRange)||((this.highlightRange!= null)&&this.highlightRange.equals(rhs.highlightRange))))&&((this.details == rhs.details)||((this.details!= null)&&this.details.equals(rhs.details))));
    }

}
