package com.codescene.jetbrains.data.telemetry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "event-name",
        "user-id",
        "editor-type",
        "extension-version",
        "*"
})
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
     * Additional fields provided by the extension, with any key and value type.
     *
     */
    @JsonProperty("*")
    @JsonPropertyDescription("Additional fields provided by the extension, with any key and value type.")
    private Object __;

    /**
     * No args constructor for use in serialization
     *
     */
    public TelemetryEvent() {
    }

    /**
     *
     * @param __
     *     Additional fields provided by the extension, with any key and value type.
     * @param eventName
     *     Name of event, this should be unique for each tracked function.
     * @param extensionVersion
     *     Version of CodeScene extension.
     * @param editorType
     *     Name of editor, for example VSCode.
     * @param userId
     *     Unique identifier of user. Could be CodeScene user id.
     */
    public TelemetryEvent(String eventName, String userId, String editorType, String extensionVersion, Object __) {
        super();
        this.eventName = eventName;
        this.userId = userId;
        this.editorType = editorType;
        this.extensionVersion = extensionVersion;
        this.__ = __;
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
    public String getUserId() {
        return userId;
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
     * Additional fields provided by the extension, with any key and value type.
     *
     */
    @JsonProperty("*")
    public Object get__() {
        return __;
    }

    /**
     * Additional fields provided by the extension, with any key and value type.
     *
     */
    @JsonProperty("*")
    public void set__(Object __) {
        this.__ = __;
    }

    public TelemetryEvent with__(Object __) {
        this.__ = __;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TelemetryEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("eventName");
        sb.append('=');
        sb.append(((this.eventName == null)?"<null>":this.eventName));
        sb.append(',');
        sb.append("userId");
        sb.append('=');
        sb.append(((this.userId == null)?"<null>":this.userId));
        sb.append(',');
        sb.append("editorType");
        sb.append('=');
        sb.append(((this.editorType == null)?"<null>":this.editorType));
        sb.append(',');
        sb.append("extensionVersion");
        sb.append('=');
        sb.append(((this.extensionVersion == null)?"<null>":this.extensionVersion));
        sb.append(',');
        sb.append("__");
        sb.append('=');
        sb.append(((this.__ == null)?"<null>":this.__));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.__ == null)? 0 :this.__.hashCode()));
        result = ((result* 31)+((this.eventName == null)? 0 :this.eventName.hashCode()));
        result = ((result* 31)+((this.extensionVersion == null)? 0 :this.extensionVersion.hashCode()));
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
        return ((((((this.__ == rhs.__)||((this.__!= null)&&this.__.equals(rhs.__)))&&((this.eventName == rhs.eventName)||((this.eventName!= null)&&this.eventName.equals(rhs.eventName))))&&((this.extensionVersion == rhs.extensionVersion)||((this.extensionVersion!= null)&&this.extensionVersion.equals(rhs.extensionVersion))))&&((this.editorType == rhs.editorType)||((this.editorType!= null)&&this.editorType.equals(rhs.editorType))))&&((this.userId == rhs.userId)||((this.userId!= null)&&this.userId.equals(rhs.userId))));
    }

}
