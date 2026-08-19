
package com.codescene.data.delta;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "change-type",
    "category",
    "description",
    "line"
})
@Generated("jsonschema2pojo")
public class ChangeDetail {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("change-type")
    private ChangeDetail.ChangeType changeType;
    /**
     * Code smell category, for example Complex Method
     * (Required)
     * 
     */
    @JsonProperty("category")
    @JsonPropertyDescription("Code smell category, for example Complex Method")
    private String category;
    /**
     * Detailed description about what caused the code health to go down.
     * (Required)
     * 
     */
    @JsonProperty("description")
    @JsonPropertyDescription("Detailed description about what caused the code health to go down.")
    private String description;
    /**
     * Line number of this change. 1-indexed. Note that for 'fixed'
     *     changes, the line only indicates where the issue was before the change.
     * 
     */
    @JsonProperty("line")
    @JsonPropertyDescription("Line number of this change. 1-indexed. Note that for 'fixed'\n    changes, the line only indicates where the issue was before the change.")
    private Integer line;

    /**
     * No args constructor for use in serialization
     * 
     */
    public ChangeDetail() {
    }

    /**
     * 
     * @param line
     *     Line number of this change. 1-indexed. Note that for 'fixed'
     *         changes, the line only indicates where the issue was before the change.
     * @param description
     *     Detailed description about what caused the code health to go down.
     * @param category
     *     Code smell category, for example Complex Method.
     */
    public ChangeDetail(ChangeDetail.ChangeType changeType, String category, String description, Integer line) {
        super();
        this.changeType = changeType;
        this.category = category;
        this.description = description;
        this.line = line;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("change-type")
    public ChangeDetail.ChangeType getChangeType() {
        return changeType;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("change-type")
    public void setChangeType(ChangeDetail.ChangeType changeType) {
        this.changeType = changeType;
    }

    public ChangeDetail withChangeType(ChangeDetail.ChangeType changeType) {
        this.changeType = changeType;
        return this;
    }

    /**
     * Code smell category, for example Complex Method
     * (Required)
     * 
     */
    @JsonProperty("category")
    public String getCategory() {
        return category;
    }

    /**
     * Code smell category, for example Complex Method
     * (Required)
     * 
     */
    @JsonProperty("category")
    public void setCategory(String category) {
        this.category = category;
    }

    public ChangeDetail withCategory(String category) {
        this.category = category;
        return this;
    }

    /**
     * Detailed description about what caused the code health to go down.
     * (Required)
     * 
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     * Detailed description about what caused the code health to go down.
     * (Required)
     * 
     */
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public ChangeDetail withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Line number of this change. 1-indexed. Note that for 'fixed'
     *     changes, the line only indicates where the issue was before the change.
     * 
     */
    @JsonProperty("line")
    public Optional<Integer> getLine() {
        return Optional.ofNullable(line);
    }

    /**
     * Line number of this change. 1-indexed. Note that for 'fixed'
     *     changes, the line only indicates where the issue was before the change.
     * 
     */
    @JsonProperty("line")
    public void setLine(Integer line) {
        this.line = line;
    }

    public ChangeDetail withLine(Integer line) {
        this.line = line;
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.category == null)? 0 :this.category.hashCode()));
        result = ((result* 31)+((this.line == null)? 0 :this.line.hashCode()));
        result = ((result* 31)+((this.changeType == null)? 0 :this.changeType.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ChangeDetail) == false) {
            return false;
        }
        ChangeDetail rhs = ((ChangeDetail) other);
        return (((((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description)))&&((this.category == rhs.category)||((this.category!= null)&&this.category.equals(rhs.category))))&&((this.line == rhs.line)||((this.line!= null)&&this.line.equals(rhs.line))))&&((this.changeType == rhs.changeType)||((this.changeType!= null)&&this.changeType.equals(rhs.changeType))));
    }

    @Generated("jsonschema2pojo")
    public enum ChangeType {

        INTRODUCED("introduced"),
        DEGRADED("degraded"),
        IMPROVED("improved"),
        FIXED("fixed"),
        __EMPTY__("");
        private final String value;
        private final static Map<String, ChangeDetail.ChangeType> CONSTANTS = new HashMap<String, ChangeDetail.ChangeType>();

        static {
            for (ChangeDetail.ChangeType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ChangeType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static ChangeDetail.ChangeType fromValue(String value) {
            ChangeDetail.ChangeType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
