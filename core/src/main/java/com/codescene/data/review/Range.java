
package com.codescene.data.review;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "start-line",
    "start-column",
    "end-line",
    "end-column"
})
@Generated("jsonschema2pojo")
public class Range {

    /**
     * Range start line. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("start-line")
    @JsonPropertyDescription("Range start line. 1-indexed.")
    private Integer startLine;
    /**
     * Range start column. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("start-column")
    @JsonPropertyDescription("Range start column. 1-indexed.")
    private Integer startColumn;
    /**
     * Range end line. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("end-line")
    @JsonPropertyDescription("Range end line. 1-indexed.")
    private Integer endLine;
    /**
     * Range end column. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("end-column")
    @JsonPropertyDescription("Range end column. 1-indexed.")
    private Integer endColumn;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Range() {
    }

    /**
     * 
     * @param endLine
     *     Range end line. 1-indexed.
     * @param endColumn
     *     Range end column. 1-indexed.
     * @param startColumn
     *     Range start column. 1-indexed.
     * @param startLine
     *     Range start line. 1-indexed.
     */
    public Range(Integer startLine, Integer startColumn, Integer endLine, Integer endColumn) {
        super();
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    /**
     * Range start line. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("start-line")
    public Integer getStartLine() {
        return startLine;
    }

    /**
     * Range start line. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("start-line")
    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Range withStartLine(Integer startLine) {
        this.startLine = startLine;
        return this;
    }

    /**
     * Range start column. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("start-column")
    public Integer getStartColumn() {
        return startColumn;
    }

    /**
     * Range start column. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("start-column")
    public void setStartColumn(Integer startColumn) {
        this.startColumn = startColumn;
    }

    public Range withStartColumn(Integer startColumn) {
        this.startColumn = startColumn;
        return this;
    }

    /**
     * Range end line. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("end-line")
    public Integer getEndLine() {
        return endLine;
    }

    /**
     * Range end line. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("end-line")
    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public Range withEndLine(Integer endLine) {
        this.endLine = endLine;
        return this;
    }

    /**
     * Range end column. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("end-column")
    public Integer getEndColumn() {
        return endColumn;
    }

    /**
     * Range end column. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("end-column")
    public void setEndColumn(Integer endColumn) {
        this.endColumn = endColumn;
    }

    public Range withEndColumn(Integer endColumn) {
        this.endColumn = endColumn;
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.endLine == null)? 0 :this.endLine.hashCode()));
        result = ((result* 31)+((this.endColumn == null)? 0 :this.endColumn.hashCode()));
        result = ((result* 31)+((this.startColumn == null)? 0 :this.startColumn.hashCode()));
        result = ((result* 31)+((this.startLine == null)? 0 :this.startLine.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Range) == false) {
            return false;
        }
        Range rhs = ((Range) other);
        return (((((this.endLine == rhs.endLine)||((this.endLine!= null)&&this.endLine.equals(rhs.endLine)))&&((this.endColumn == rhs.endColumn)||((this.endColumn!= null)&&this.endColumn.equals(rhs.endColumn))))&&((this.startColumn == rhs.startColumn)||((this.startColumn!= null)&&this.startColumn.equals(rhs.startColumn))))&&((this.startLine == rhs.startLine)||((this.startLine!= null)&&this.startLine.equals(rhs.startLine))));
    }

}
