
package com.codescene.data.delta;

import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "name",
    "range"
})
@Generated("jsonschema2pojo")
public class Function {

    /**
     * Name of function
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Name of function")
    private String name;
    @JsonProperty("range")
    private Range range;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Function() {
    }

    /**
     * 
     * @param name
     *     Name of function.
     * @param range
     *     Full range of the function.
     */
    public Function(String name, Range range) {
        super();
        this.name = name;
        this.range = range;
    }

    /**
     * Name of function
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Name of function
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public Function withName(String name) {
        this.name = name;
        return this;
    }

    @JsonProperty("range")
    public Optional<Range> getRange() {
        return Optional.ofNullable(range);
    }

    @JsonProperty("range")
    public void setRange(Range range) {
        this.range = range;
    }

    public Function withRange(Range range) {
        this.range = range;
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.range == null)? 0 :this.range.hashCode()));
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
        return (((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.range == rhs.range)||((this.range!= null)&&this.range.equals(rhs.range))));
    }

}
