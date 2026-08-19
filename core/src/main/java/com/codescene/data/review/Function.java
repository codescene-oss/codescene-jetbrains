
package com.codescene.data.review;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "function",
    "range",
    "code-smells"
})
@Generated("jsonschema2pojo")
public class Function {

    /**
     * The name of the function which has codesmell(s).
     * (Required)
     * 
     */
    @JsonProperty("function")
    @JsonPropertyDescription("The name of the function which has codesmell(s).")
    private String function;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("range")
    private Range range;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("code-smells")
    private List<CodeSmell> codeSmells = new ArrayList<CodeSmell>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Function() {
    }

    /**
     * 
     * @param function
     *     The name of the function which has codesmell(s).
     * @param range
     *     Range within the code where the smell occurs.
     */
    public Function(String function, Range range, List<CodeSmell> codeSmells) {
        super();
        this.function = function;
        this.range = range;
        this.codeSmells = codeSmells;
    }

    /**
     * The name of the function which has codesmell(s).
     * (Required)
     * 
     */
    @JsonProperty("function")
    public String getFunction() {
        return function;
    }

    /**
     * The name of the function which has codesmell(s).
     * (Required)
     * 
     */
    @JsonProperty("function")
    public void setFunction(String function) {
        this.function = function;
    }

    public Function withFunction(String function) {
        this.function = function;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("range")
    public Range getRange() {
        return range;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("range")
    public void setRange(Range range) {
        this.range = range;
    }

    public Function withRange(Range range) {
        this.range = range;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("code-smells")
    public List<CodeSmell> getCodeSmells() {
        return codeSmells;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("code-smells")
    public void setCodeSmells(List<CodeSmell> codeSmells) {
        this.codeSmells = codeSmells;
    }

    public Function withCodeSmells(List<CodeSmell> codeSmells) {
        this.codeSmells = codeSmells;
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.range == null)? 0 :this.range.hashCode()));
        result = ((result* 31)+((this.codeSmells == null)? 0 :this.codeSmells.hashCode()));
        result = ((result* 31)+((this.function == null)? 0 :this.function.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Function) == false) {
            return false;
        }
        Function rhs = ((Function) other);
        return ((((this.range == rhs.range)||((this.range!= null)&&this.range.equals(rhs.range)))&&((this.codeSmells == rhs.codeSmells)||((this.codeSmells!= null)&&this.codeSmells.equals(rhs.codeSmells))))&&((this.function == rhs.function)||((this.function!= null)&&this.function.equals(rhs.function))));
    }

}
