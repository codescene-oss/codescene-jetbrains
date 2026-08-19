
package com.codescene.data.delta;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "line",
    "column"
})
@Generated("jsonschema2pojo")
public class Position {

    /**
     * Line number. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("line")
    @JsonPropertyDescription("Line number. 1-indexed.")
    private Integer line;
    /**
     * Column number. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("column")
    @JsonPropertyDescription("Column number. 1-indexed.")
    private Integer column;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Position() {
    }

    /**
     * 
     * @param line
     *     Line number. 1-indexed.
     * @param column
     *     Column number. 1-indexed.
     */
    public Position(Integer line, Integer column) {
        super();
        this.line = line;
        this.column = column;
    }

    /**
     * Line number. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("line")
    public Integer getLine() {
        return line;
    }

    /**
     * Line number. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("line")
    public void setLine(Integer line) {
        this.line = line;
    }

    public Position withLine(Integer line) {
        this.line = line;
        return this;
    }

    /**
     * Column number. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("column")
    public Integer getColumn() {
        return column;
    }

    /**
     * Column number. 1-indexed.
     * (Required)
     * 
     */
    @JsonProperty("column")
    public void setColumn(Integer column) {
        this.column = column;
    }

    public Position withColumn(Integer column) {
        this.column = column;
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.column == null)? 0 :this.column.hashCode()));
        result = ((result* 31)+((this.line == null)? 0 :this.line.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Position) == false) {
            return false;
        }
        Position rhs = ((Position) other);
        return (((this.column == rhs.column)||((this.column!= null)&&this.column.equals(rhs.column)))&&((this.line == rhs.line)||((this.line!= null)&&this.line.equals(rhs.line))));
    }

}
