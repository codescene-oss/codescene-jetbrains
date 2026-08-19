
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
    "message",
    "lines",
    "columns"
})
@Generated("jsonschema2pojo")
public class ReasonDetails {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("message")
    private String message;
    /**
     *  2-tuple pointing to the start-line and end-line of the issue. 0-based.
     * (Required)
     * 
     */
    @JsonProperty("lines")
    @JsonPropertyDescription("2-tuple pointing to the start-line and end-line of the issue. 0-based.")
    private List<Integer> lines = new ArrayList<Integer>();
    /**
     *  2-tuple pointing to the start-col and end-col of the issue. 0-based.
     * (Required)
     * 
     */
    @JsonProperty("columns")
    @JsonPropertyDescription("2-tuple pointing to the start-col and end-col of the issue. 0-based.")
    private List<Integer> columns = new ArrayList<Integer>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     *  2-tuple pointing to the start-line and end-line of the issue. 0-based.
     * (Required)
     * 
     */
    @JsonProperty("lines")
    public List<Integer> getLines() {
        return lines;
    }

    /**
     *  2-tuple pointing to the start-col and end-col of the issue. 0-based.
     * (Required)
     * 
     */
    @JsonProperty("columns")
    public List<Integer> getColumns() {
        return columns;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.message == null)? 0 :this.message.hashCode()));
        result = ((result* 31)+((this.lines == null)? 0 :this.lines.hashCode()));
        result = ((result* 31)+((this.columns == null)? 0 :this.columns.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ReasonDetails) == false) {
            return false;
        }
        ReasonDetails rhs = ((ReasonDetails) other);
        return ((((this.message == rhs.message)||((this.message!= null)&&this.message.equals(rhs.message)))&&((this.lines == rhs.lines)||((this.lines!= null)&&this.lines.equals(rhs.lines))))&&((this.columns == rhs.columns)||((this.columns!= null)&&this.columns.equals(rhs.columns))));
    }

}
