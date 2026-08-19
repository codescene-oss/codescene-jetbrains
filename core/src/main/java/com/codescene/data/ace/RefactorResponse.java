
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
    "declarations",
    "code",
    "refactoring-properties",
    "reasons",
    "confidence",
    "metadata",
    "trace-id",
    "credits-info"
})
@Generated("jsonschema2pojo")
public class RefactorResponse {

    /**
     * Optional declarations
     * 
     */
    @JsonProperty("declarations")
    @JsonPropertyDescription("Optional declarations")
    private String declarations;
    /**
     * Refactored code
     * (Required)
     * 
     */
    @JsonProperty("code")
    @JsonPropertyDescription("Refactored code")
    private String code;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("refactoring-properties")
    private RefactoringProperties refactoringProperties;
    /**
     * List of reasons for refactoring failure
     * (Required)
     * 
     */
    @JsonProperty("reasons")
    @JsonPropertyDescription("List of reasons for refactoring failure")
    private List<Reason> reasons = new ArrayList<Reason>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("confidence")
    private Confidence confidence;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("metadata")
    private Metadata metadata;
    /**
     * Trace id for the request, use for debugging requests
     * (Required)
     * 
     */
    @JsonProperty("trace-id")
    @JsonPropertyDescription("Trace id for the request, use for debugging requests")
    private String traceId;
    @JsonProperty("credits-info")
    private CreditsInfo creditsInfo;

    /**
     * Optional declarations
     * 
     */
    @JsonProperty("declarations")
    public Optional<String> getDeclarations() {
        return Optional.ofNullable(declarations);
    }

    /**
     * Refactored code
     * (Required)
     * 
     */
    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("refactoring-properties")
    public RefactoringProperties getRefactoringProperties() {
        return refactoringProperties;
    }

    /**
     * List of reasons for refactoring failure
     * (Required)
     * 
     */
    @JsonProperty("reasons")
    public List<Reason> getReasons() {
        return reasons;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("confidence")
    public Confidence getConfidence() {
        return confidence;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("metadata")
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Trace id for the request, use for debugging requests
     * (Required)
     * 
     */
    @JsonProperty("trace-id")
    public String getTraceId() {
        return traceId;
    }

    @JsonProperty("credits-info")
    public Optional<CreditsInfo> getCreditsInfo() {
        return Optional.ofNullable(creditsInfo);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.traceId == null)? 0 :this.traceId.hashCode()));
        result = ((result* 31)+((this.metadata == null)? 0 :this.metadata.hashCode()));
        result = ((result* 31)+((this.code == null)? 0 :this.code.hashCode()));
        result = ((result* 31)+((this.reasons == null)? 0 :this.reasons.hashCode()));
        result = ((result* 31)+((this.refactoringProperties == null)? 0 :this.refactoringProperties.hashCode()));
        result = ((result* 31)+((this.confidence == null)? 0 :this.confidence.hashCode()));
        result = ((result* 31)+((this.creditsInfo == null)? 0 :this.creditsInfo.hashCode()));
        result = ((result* 31)+((this.declarations == null)? 0 :this.declarations.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RefactorResponse) == false) {
            return false;
        }
        RefactorResponse rhs = ((RefactorResponse) other);
        return (((((((((this.traceId == rhs.traceId)||((this.traceId!= null)&&this.traceId.equals(rhs.traceId)))&&((this.metadata == rhs.metadata)||((this.metadata!= null)&&this.metadata.equals(rhs.metadata))))&&((this.code == rhs.code)||((this.code!= null)&&this.code.equals(rhs.code))))&&((this.reasons == rhs.reasons)||((this.reasons!= null)&&this.reasons.equals(rhs.reasons))))&&((this.refactoringProperties == rhs.refactoringProperties)||((this.refactoringProperties!= null)&&this.refactoringProperties.equals(rhs.refactoringProperties))))&&((this.confidence == rhs.confidence)||((this.confidence!= null)&&this.confidence.equals(rhs.confidence))))&&((this.creditsInfo == rhs.creditsInfo)||((this.creditsInfo!= null)&&this.creditsInfo.equals(rhs.creditsInfo))))&&((this.declarations == rhs.declarations)||((this.declarations!= null)&&this.declarations.equals(rhs.declarations))));
    }

}
