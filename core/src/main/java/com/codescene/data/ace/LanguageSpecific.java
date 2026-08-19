
package com.codescene.data.ace;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Language specific overrides
 * 
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({

})
@Generated("jsonschema2pojo")
public class LanguageSpecific {

    @JsonIgnore
    private Map<String, RefactorSupportOverrides> additionalProperties = new LinkedHashMap<String, RefactorSupportOverrides>();

    @JsonAnyGetter
    public Map<String, RefactorSupportOverrides> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, RefactorSupportOverrides value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof LanguageSpecific) == false) {
            return false;
        }
        LanguageSpecific rhs = ((LanguageSpecific) other);
        return ((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties)));
    }

}
