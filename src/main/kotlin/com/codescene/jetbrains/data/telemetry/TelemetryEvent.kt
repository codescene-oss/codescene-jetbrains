package com.codescene.jetbrains.data.telemetry

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.lang.StringBuilder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "event-name", "user-id", "editor-type", "extension-version", "*"
)
class TelemetryEvent {
    /**
     * Name of event, this should be unique for each tracked function.
     * (Required)
     *
     */
    @JsonProperty("event-name")
    @JsonPropertyDescription("Name of event, this should be unique for each tracked function.")
    private var eventName: String? = null

    /**
     * Unique identifier of user. Could be CodeScene user id.
     *
     */
    @JsonProperty("user-id")
    @JsonPropertyDescription("Unique identifier of user. Could be CodeScene user id.")
    private var userId: String? = null

    /**
     * Name of editor, for example VSCode.
     * (Required)
     *
     */
    @JsonProperty("editor-type")
    @JsonPropertyDescription("Name of editor, for example VSCode.")
    private var editorType: String? = null

    /**
     * Version of CodeScene extension.
     * (Required)
     *
     */
    @JsonProperty("extension-version")
    @JsonPropertyDescription("Version of CodeScene extension.")
    private var extensionVersion: String? = null

    /**
     * Additional fields provided by the extension, with any key and value type.
     *
     */
    @JsonProperty("*")
    @JsonPropertyDescription("Additional fields provided by the extension, with any key and value type.")
    private var `__`: Any? = null

    /**
     * No args constructor for use in serialization
     *
     */
    constructor()

    /**
     *
     * @param __
     * Additional fields provided by the extension, with any key and value type.
     * @param eventName
     * Name of event, this should be unique for each tracked function.
     * @param extensionVersion
     * Version of CodeScene extension.
     * @param editorType
     * Name of editor, for example VSCode.
     * @param userId
     * Unique identifier of user. Could be CodeScene user id.
     */
    constructor(
        eventName: String?,
        userId: String?,
        editorType: String?,
        extensionVersion: String?,
        `__`: Any?
    ) : super() {
        this.eventName = eventName
        this.userId = userId
        this.editorType = editorType
        this.extensionVersion = extensionVersion
        this.`__` = `__`
    }

    /**
     * Name of event, this should be unique for each tracked function.
     * (Required)
     *
     */
    @JsonProperty("event-name")
    fun getEventName(): String? {
        return eventName
    }

    /**
     * Name of event, this should be unique for each tracked function.
     * (Required)
     *
     */
    @JsonProperty("event-name")
    fun setEventName(eventName: String?) {
        this.eventName = eventName
    }

    fun withEventName(eventName: String?): TelemetryEvent {
        this.eventName = eventName
        return this
    }

    /**
     * Unique identifier of user. Could be CodeScene user id.
     *
     */
    @JsonProperty("user-id")
    fun getUserId(): String? {
        return userId
    }

    /**
     * Unique identifier of user. Could be CodeScene user id.
     *
     */
    @JsonProperty("user-id")
    fun setUserId(userId: String?) {
        this.userId = userId
    }

    fun withUserId(userId: String?): TelemetryEvent {
        this.userId = userId
        return this
    }

    /**
     * Name of editor, for example VSCode.
     * (Required)
     *
     */
    @JsonProperty("editor-type")
    fun getEditorType(): String? {
        return editorType
    }

    /**
     * Name of editor, for example VSCode.
     * (Required)
     *
     */
    @JsonProperty("editor-type")
    fun setEditorType(editorType: String?) {
        this.editorType = editorType
    }

    fun withEditorType(editorType: String?): TelemetryEvent {
        this.editorType = editorType
        return this
    }

    /**
     * Version of CodeScene extension.
     * (Required)
     *
     */
    @JsonProperty("extension-version")
    fun getExtensionVersion(): String? {
        return extensionVersion
    }

    /**
     * Version of CodeScene extension.
     * (Required)
     *
     */
    @JsonProperty("extension-version")
    fun setExtensionVersion(extensionVersion: String?) {
        this.extensionVersion = extensionVersion
    }

    fun withExtensionVersion(extensionVersion: String?): TelemetryEvent {
        this.extensionVersion = extensionVersion
        return this
    }

    /**
     * Additional fields provided by the extension, with any key and value type.
     *
     */
    @JsonProperty("*")
    fun get__(): Any? {
        return `__`
    }

    /**
     * Additional fields provided by the extension, with any key and value type.
     *
     */
    @JsonProperty("*")
    fun set__(`__`: Any?) {
        this.`__` = `__`
    }

    fun with__(`__`: Any?): TelemetryEvent {
        this.`__` = `__`
        return this
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(TelemetryEvent::class.java.getName()).append('@')
            .append(Integer.toHexString(System.identityHashCode(this))).append('[')
        sb.append("eventName")
        sb.append('=')
        sb.append((if ((this.eventName == null)) "<null>" else this.eventName))
        sb.append(',')
        sb.append("userId")
        sb.append('=')
        sb.append((if ((this.userId == null)) "<null>" else this.userId))
        sb.append(',')
        sb.append("editorType")
        sb.append('=')
        sb.append((if ((this.editorType == null)) "<null>" else this.editorType))
        sb.append(',')
        sb.append("extensionVersion")
        sb.append('=')
        sb.append((if ((this.extensionVersion == null)) "<null>" else this.extensionVersion))
        sb.append(',')
        sb.append("__")
        sb.append('=')
        sb.append((if ((this.`__` == null)) "<null>" else this.`__`))
        sb.append(',')
        if (sb.get((sb.length - 1)) == ',') {
            sb.setCharAt((sb.length - 1), ']')
        } else {
            sb.append(']')
        }
        return sb.toString()
    }

    override fun hashCode(): Int {
        var result = 1
        result = ((result * 31) + (if ((this.`__` == null)) 0 else this.`__`.hashCode()))
        result = ((result * 31) + (if ((this.eventName == null)) 0 else this.eventName.hashCode()))
        result = ((result * 31) + (if ((this.extensionVersion == null)) 0 else this.extensionVersion.hashCode()))
        result = ((result * 31) + (if ((this.editorType == null)) 0 else this.editorType.hashCode()))
        result = ((result * 31) + (if ((this.userId == null)) 0 else this.userId.hashCode()))
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if ((other is TelemetryEvent) == false) {
            return false
        }
        val rhs = other
        return ((((((this.`__` === rhs.`__`) || ((this.`__` != null) && this.`__` == rhs.`__`)) && ((this.eventName === rhs.eventName) || ((this.eventName != null) && this.eventName == rhs.eventName))) && ((this.extensionVersion === rhs.extensionVersion) || ((this.extensionVersion != null) && this.extensionVersion == rhs.extensionVersion))) && ((this.editorType === rhs.editorType) || ((this.editorType != null) && this.editorType == rhs.editorType))) && ((this.userId === rhs.userId) || ((this.userId != null) && this.userId == rhs.userId)))
    }
}
