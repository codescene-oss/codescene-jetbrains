
package com.codescene.data.ace;

import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "skip-cache",
    "token"
})
@Generated("jsonschema2pojo")
public class RefactoringOptions {

    @JsonProperty("skip-cache")
    private Boolean skipCache;
    /**
     * Optional access token for non-freemium access
     * 
     */
    @JsonProperty("token")
    @JsonPropertyDescription("Optional access token for non-freemium access")
    private String token;

    @JsonProperty("skip-cache")
    public Optional<Boolean> getSkipCache() {
        return Optional.ofNullable(skipCache);
    }

    @JsonProperty("skip-cache")
    public void setSkipCache(Boolean skipCache) {
        this.skipCache = skipCache;
    }

    /**
     * Optional access token for non-freemium access
     * 
     */
    @JsonProperty("token")
    public Optional<String> getToken() {
        return Optional.ofNullable(token);
    }

    /**
     * Optional access token for non-freemium access
     * 
     */
    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.skipCache == null)? 0 :this.skipCache.hashCode()));
        result = ((result* 31)+((this.token == null)? 0 :this.token.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RefactoringOptions) == false) {
            return false;
        }
        RefactoringOptions rhs = ((RefactoringOptions) other);
        return (((this.skipCache == rhs.skipCache)||((this.skipCache!= null)&&this.skipCache.equals(rhs.skipCache)))&&((this.token == rhs.token)||((this.token!= null)&&this.token.equals(rhs.token))));
    }

}
