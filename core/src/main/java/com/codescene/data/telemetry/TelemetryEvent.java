
package com.codescene.data.telemetry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Note - the event can also contain arbitrary key-value pairs which will be sent as additional properties
 * 
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "event-name",
    "user-id",
    "editor-type",
    "extension-version",
    "internal"
})
@Generated("jsonschema2pojo")
public class TelemetryEvent {

    /**
     * Name of event, this should be unique for each tracked function.
     * (Required)
     * 
     */
    @JsonProperty("event-name")
    @JsonPropertyDescription("Name of event, this should be unique for each tracked function.")
    private String eventName;
    /**
     * Unique identifier of user. Could be CodeScene user id.
     * 
     */
    @JsonProperty("user-id")
    @JsonPropertyDescription("Unique identifier of user. Could be CodeScene user id.")
    private String userId;
    /**
     * Name of editor, for example VSCode.
     * (Required)
     * 
     */
    @JsonProperty("editor-type")
    @JsonPropertyDescription("Name of editor, for example VSCode.")
    private String editorType;
    /**
     * Version of CodeScene extension.
     * (Required)
     * 
     */
    @JsonProperty("extension-version")
    @JsonPropertyDescription("Version of CodeScene extension.")
    private String extensionVersion;
    /**
     * Set to true to mark the event as 'internal'. Used for filtering.
     * 
     */
    @JsonProperty("internal")
    @JsonPropertyDescription("Set to true to mark the event as 'internal'. Used for filtering.")
    private Boolean internal;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public TelemetryEvent() {
    }

    /**
     * 
     * @param internal
     *     Set to true to mark the event as 'internal'. Used for filtering.
     * @param eventName
     *     Name of event, this should be unique for each tracked function.
     * @param extensionVersion
     *     Version of CodeScene extension.
     * @param editorType
     *     Name of editor, for example VSCode.
     * @param userId
     *     Unique identifier of user. Could be CodeScene user id.
     */
    public TelemetryEvent(String eventName, String userId, String editorType, String extensionVersion, Boolean internal) {
        super();
        this.eventName = eventName;
        this.userId = userId;
        this.editorType = editorType;
        this.extensionVersion = extensionVersion;
        this.internal = internal;
    }

    /**
     * Name of event, this should be unique for each tracked function.
     * (Required)
     * 
     */
    @JsonProperty("event-name")
    public String getEventName() {
        return eventName;
    }

    /**
     * Name of event, this should be unique for each tracked function.
     * (Required)
     * 
     */
    @JsonProperty("event-name")
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public TelemetryEvent withEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    /**
     * Unique identifier of user. Could be CodeScene user id.
     * 
     */
    @JsonProperty("user-id")
    public Optional<String> getUserId() {
        return Optional.ofNullable(userId);
    }

    /**
     * Unique identifier of user. Could be CodeScene user id.
     * 
     */
    @JsonProperty("user-id")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public TelemetryEvent withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Name of editor, for example VSCode.
     * (Required)
     * 
     */
    @JsonProperty("editor-type")
    public String getEditorType() {
        return editorType;
    }

    /**
     * Name of editor, for example VSCode.
     * (Required)
     * 
     */
    @JsonProperty("editor-type")
    public void setEditorType(String editorType) {
        this.editorType = editorType;
    }

    public TelemetryEvent withEditorType(String editorType) {
        this.editorType = editorType;
        return this;
    }

    /**
     * Version of CodeScene extension.
     * (Required)
     * 
     */
    @JsonProperty("extension-version")
    public String getExtensionVersion() {
        return extensionVersion;
    }

    /**
     * Version of CodeScene extension.
     * (Required)
     * 
     */
    @JsonProperty("extension-version")
    public void setExtensionVersion(String extensionVersion) {
        this.extensionVersion = extensionVersion;
    }

    public TelemetryEvent withExtensionVersion(String extensionVersion) {
        this.extensionVersion = extensionVersion;
        return this;
    }

    /**
     * Set to true to mark the event as 'internal'. Used for filtering.
     * 
     */
    @JsonProperty("internal")
    public Optional<Boolean> getInternal() {
        return Optional.ofNullable(internal);
    }

    /**
     * Set to true to mark the event as 'internal'. Used for filtering.
     * 
     */
    @JsonProperty("internal")
    public void setInternal(Boolean internal) {
        this.internal = internal;
    }

    public TelemetryEvent withInternal(Boolean internal) {
        this.internal = internal;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public TelemetryEvent withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.eventName == null)? 0 :this.eventName.hashCode()));
        result = ((result* 31)+((this.extensionVersion == null)? 0 :this.extensionVersion.hashCode()));
        result = ((result* 31)+((this.internal == null)? 0 :this.internal.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.editorType == null)? 0 :this.editorType.hashCode()));
        result = ((result* 31)+((this.userId == null)? 0 :this.userId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TelemetryEvent) == false) {
            return false;
        }
        TelemetryEvent rhs = ((TelemetryEvent) other);
        return (((((((this.eventName == rhs.eventName)||((this.eventName!= null)&&this.eventName.equals(rhs.eventName)))&&((this.extensionVersion == rhs.extensionVersion)||((this.extensionVersion!= null)&&this.extensionVersion.equals(rhs.extensionVersion))))&&((this.internal == rhs.internal)||((this.internal!= null)&&this.internal.equals(rhs.internal))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.editorType == rhs.editorType)||((this.editorType!= null)&&this.editorType.equals(rhs.editorType))))&&((this.userId == rhs.userId)||((this.userId!= null)&&this.userId.equals(rhs.userId))));
    }

}
