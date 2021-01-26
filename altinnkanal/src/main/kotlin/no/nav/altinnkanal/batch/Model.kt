package no.nav.altinnkanal.batch

import com.fasterxml.jackson.annotation.JsonProperty

data class ArData(
    @JsonProperty("callId")
    var callId: String = "",
    @JsonProperty("archiveReference")
    var archiveReference: String = "",
    @JsonProperty("archiveTimeStamp")
    var archiveTimeStamp: String = "",
    @JsonProperty("serviceCode")
    var serviceCode: String = "",
    @JsonProperty("serviceEditionCode")
    var serviceEditionCode: String = "",
    @JsonProperty("dataFormatId")
    var dataFormatId: String = "",
    @JsonProperty("dataFormatVersion")
    var dataFormatVersion: String = "",
    @JsonProperty("reportee")
    var reportee: String = "",
    @JsonProperty("numberOfForms")
    var numberOfForms: Int = 0,
    @JsonProperty("numberOfAttachments")
    var numberOfAttachments: Int = 0,
    @JsonProperty("formData")
    var formData: String = "",
    @JsonProperty("attachments")
    var attachments: List<Attachment> = mutableListOf(),
    @JsonProperty("metadata")
    var metadata: Map<String, String> = mutableMapOf()
)

data class Attachment(
    @JsonProperty("filename")
    var filename: String = "",
    @JsonProperty("attachmentType")
    var attachmentType: String = "",
    @JsonProperty("attachmentTypeName")
    var attachmentTypeName: String = "",
    @JsonProperty("encrypted")
    var encrypted: Boolean = false,
    @JsonProperty("dataBase64")
    var dataBase64: String = ""
)
