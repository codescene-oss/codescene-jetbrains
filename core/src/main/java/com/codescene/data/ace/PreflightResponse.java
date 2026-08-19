
package com.codescene.data.ace;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "version",
    "file-types",
    "language-common",
    "language-specific"
})
@Generated("jsonschema2pojo")
public class PreflightResponse {

    /**
     * Version flag
     * (Required)
     * 
     */
    @JsonProperty("version")
    @JsonPropertyDescription("Version flag")
    private Double version;
    /**
     * Supported extensions
     * (Required)
     * 
     */
    @JsonProperty("file-types")
    @JsonPropertyDescription("Supported extensions")
    private List<String> fileTypes = new ArrayList<String>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("language-common")
    private RefactorSupport languageCommon;
    /**
     * Language specific overrides
     * (Required)
     * 
     */
    @JsonProperty("language-specific")
    @JsonPropertyDescription("Language specific overrides")
    private LanguageSpecific languageSpecific;

    /**
     * Version flag
     * (Required)
     * 
     */
    @JsonProperty("version")
    public Double getVersion() {
        return version;
    }

    /**
     * Supported extensions
     * (Required)
     * 
     */
    @JsonProperty("file-types")
    public List<String> getFileTypes() {
        return fileTypes;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("language-common")
    public RefactorSupport getLanguageCommon() {
        return languageCommon;
    }

    /**
     * Language specific overrides
     * (Required)
     * 
     */
    @JsonProperty("language-specific")
    public LanguageSpecific getLanguageSpecific() {
        return languageSpecific;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.languageCommon == null)? 0 :this.languageCommon.hashCode()));
        result = ((result* 31)+((this.version == null)? 0 :this.version.hashCode()));
        result = ((result* 31)+((this.languageSpecific == null)? 0 :this.languageSpecific.hashCode()));
        result = ((result* 31)+((this.fileTypes == null)? 0 :this.fileTypes.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof PreflightResponse) == false) {
            return false;
        }
        PreflightResponse rhs = ((PreflightResponse) other);
        return (((((this.languageCommon == rhs.languageCommon)||((this.languageCommon!= null)&&this.languageCommon.equals(rhs.languageCommon)))&&((this.version == rhs.version)||((this.version!= null)&&this.version.equals(rhs.version))))&&((this.languageSpecific == rhs.languageSpecific)||((this.languageSpecific!= null)&&this.languageSpecific.equals(rhs.languageSpecific))))&&((this.fileTypes == rhs.fileTypes)||((this.fileTypes!= null)&&this.fileTypes.equals(rhs.fileTypes))));
    }

}
