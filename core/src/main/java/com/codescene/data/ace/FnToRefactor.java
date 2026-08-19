
package com.codescene.data.ace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * A structure for use in subsequent calls to the refactor endpoint.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "name",
    "range",
    "body",
    "file-type",
    "function-type",
    "refactoring-targets"
})
@Generated("jsonschema2pojo")
public class FnToRefactor {

    /**
     * Function name (for presentation)
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Function name (for presentation)")
    private String name;
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
    @JsonProperty("body")
    private String body;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("file-type")
    private String fileType;
    @JsonProperty("function-type")
    private String functionType;
    /**
     * List of refactoring targets (code-smells).
     * (Required)
     * 
     */
    @JsonProperty("refactoring-targets")
    @JsonPropertyDescription("List of refactoring targets (code-smells).")
    private List<RefactoringTarget> refactoringTargets = new ArrayList<RefactoringTarget>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public FnToRefactor() {
    }

    /**
     * 
     * @param refactoringTargets
     *     List of refactoring targets (code-smells).
     * @param name
     *     Function name (for presentation).
     * @param range
     *     Range of the function. Use to keep track of what code to replace in the original file.
     */
    public FnToRefactor(String name, Range range, String body, String fileType, String functionType, List<RefactoringTarget> refactoringTargets) {
        super();
        this.name = name;
        this.range = range;
        this.body = body;
        this.fileType = fileType;
        this.functionType = functionType;
        this.refactoringTargets = refactoringTargets;
    }

    /**
     * Function name (for presentation)
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public FnToRefactor withName(String name) {
        this.name = name;
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

    public FnToRefactor withRange(Range range) {
        this.range = range;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    public FnToRefactor withBody(String body) {
        this.body = body;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("file-type")
    public String getFileType() {
        return fileType;
    }

    public FnToRefactor withFileType(String fileType) {
        this.fileType = fileType;
        return this;
    }

    @JsonProperty("function-type")
    public Optional<String> getFunctionType() {
        return Optional.ofNullable(functionType);
    }

    public FnToRefactor withFunctionType(String functionType) {
        this.functionType = functionType;
        return this;
    }

    /**
     * List of refactoring targets (code-smells).
     * (Required)
     * 
     */
    @JsonProperty("refactoring-targets")
    public List<RefactoringTarget> getRefactoringTargets() {
        return refactoringTargets;
    }

    public FnToRefactor withRefactoringTargets(List<RefactoringTarget> refactoringTargets) {
        this.refactoringTargets = refactoringTargets;
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.range == null)? 0 :this.range.hashCode()));
        result = ((result* 31)+((this.functionType == null)? 0 :this.functionType.hashCode()));
        result = ((result* 31)+((this.body == null)? 0 :this.body.hashCode()));
        result = ((result* 31)+((this.refactoringTargets == null)? 0 :this.refactoringTargets.hashCode()));
        result = ((result* 31)+((this.fileType == null)? 0 :this.fileType.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof FnToRefactor) == false) {
            return false;
        }
        FnToRefactor rhs = ((FnToRefactor) other);
        return (((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.range == rhs.range)||((this.range!= null)&&this.range.equals(rhs.range))))&&((this.functionType == rhs.functionType)||((this.functionType!= null)&&this.functionType.equals(rhs.functionType))))&&((this.body == rhs.body)||((this.body!= null)&&this.body.equals(rhs.body))))&&((this.refactoringTargets == rhs.refactoringTargets)||((this.refactoringTargets!= null)&&this.refactoringTargets.equals(rhs.refactoringTargets))))&&((this.fileType == rhs.fileType)||((this.fileType!= null)&&this.fileType.equals(rhs.fileType))));
    }

}
