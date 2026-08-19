
package com.codescene.data.ace;

import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "used",
    "limit",
    "reset"
})
@Generated("jsonschema2pojo")
public class CreditsInfo {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("used")
    private Integer used;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("limit")
    private Integer limit;
    /**
     * Credit reset date in ISO-8601 format
     * 
     */
    @JsonProperty("reset")
    @JsonPropertyDescription("Credit reset date in ISO-8601 format")
    private String reset;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("used")
    public Integer getUsed() {
        return used;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("limit")
    public Integer getLimit() {
        return limit;
    }

    /**
     * Credit reset date in ISO-8601 format
     * 
     */
    @JsonProperty("reset")
    public Optional<String> getReset() {
        return Optional.ofNullable(reset);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.limit == null)? 0 :this.limit.hashCode()));
        result = ((result* 31)+((this.reset == null)? 0 :this.reset.hashCode()));
        result = ((result* 31)+((this.used == null)? 0 :this.used.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CreditsInfo) == false) {
            return false;
        }
        CreditsInfo rhs = ((CreditsInfo) other);
        return ((((this.limit == rhs.limit)||((this.limit!= null)&&this.limit.equals(rhs.limit)))&&((this.reset == rhs.reset)||((this.reset!= null)&&this.reset.equals(rhs.reset))))&&((this.used == rhs.used)||((this.used!= null)&&this.used.equals(rhs.used))));
    }

}
