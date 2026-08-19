
package com.codescene.data.ace;

import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "cached?"
})
@Generated("jsonschema2pojo")
public class Metadata {

    @JsonProperty("cached?")
    private Boolean cached;

    @JsonProperty("cached?")
    public Optional<Boolean> getCached() {
        return Optional.ofNullable(cached);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.cached == null)? 0 :this.cached.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Metadata) == false) {
            return false;
        }
        Metadata rhs = ((Metadata) other);
        return ((this.cached == rhs.cached)||((this.cached!= null)&&this.cached.equals(rhs.cached)));
    }

}
