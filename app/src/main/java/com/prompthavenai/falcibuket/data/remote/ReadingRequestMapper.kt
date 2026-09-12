package com.prompthavenai.falcibuket.data.remote

import com.prompthavenai.falcibuket.data.model.AdditionalInputs
import com.prompthavenai.falcibuket.data.model.CardSelection
import com.prompthavenai.falcibuket.data.model.ImagePayloadDescriptor
import com.prompthavenai.falcibuket.data.model.ImageReferenceKind
import com.prompthavenai.falcibuket.data.model.ImageRole
import com.prompthavenai.falcibuket.data.model.PersonData
import com.prompthavenai.falcibuket.data.model.PlaceData
import com.prompthavenai.falcibuket.data.model.ReadingRequest
import com.prompthavenai.falcibuket.data.model.SymbolSelection
import java.util.UUID

/** Stable id so a retried submit is recognized as the same operation. */
fun newReadingRequestId(): String = UUID.randomUUID().toString()

/**
 * Pure mapping from the generic domain [ReadingRequest] to the Firebase
 * callable wire payload. No Android graphics types, no image bytes and no I/O
 * live here; attachment bytes are resolved separately in the media layer.
 */
object ReadingRequestMapper {

    fun toWirePayload(request: ReadingRequest): Map<String, Any> {
        val payload = LinkedHashMap<String, Any>()
        payload["schemaVersion"] = request.schemaVersion
        payload["requestId"] = request.requestId
        payload["readingType"] = request.readingType.id

        request.question.clean()?.let { payload["question"] = it }

        if (request.selectedCards.isNotEmpty()) {
            payload["selectedCards"] = request.selectedCards.map { it.toWire() }
        }
        if (request.selectedSymbols.isNotEmpty()) {
            payload["selectedSymbols"] = request.selectedSymbols.map { it.toWire() }
        }
        request.birthData?.let { payload["birthData"] = it.toWire() }
        request.secondPersonData?.let { payload["secondPersonData"] = it.toWire() }
        if (request.imagePayload.isNotEmpty()) {
            payload["imagePayload"] = request.imagePayload.map { it.toWire() }
        }
        request.additionalInputs?.let { inputs ->
            inputs.toWire().takeIf { it.isNotEmpty() }?.let { payload["additionalInputs"] = it }
        }

        return payload
    }

    private fun CardSelection.toWire(): Map<String, Any> {
        val map = LinkedHashMap<String, Any>()
        map["position"] = position
        cardId.clean()?.let { map["cardId"] = it }
        if (reversed) map["reversed"] = true
        return map
    }

    private fun SymbolSelection.toWire(): Map<String, Any> = linkedMapOf(
        "symbolId" to (symbolId.clean() ?: symbolId),
        "position" to position
    )

    private fun PersonData.toWire(): Map<String, Any> {
        val map = LinkedHashMap<String, Any>()
        name.clean()?.let { map["name"] = it }
        map["birthDate"] = birthDate.trim()
        birthTime.clean()?.let { map["birthTime"] = it }
        timeZone.clean()?.let { map["timeZone"] = it }
        place?.let { map["place"] = it.toWire() }
        return map
    }

    private fun PlaceData.toWire(): Map<String, Any> {
        val map = LinkedHashMap<String, Any>()
        label.clean()?.let { map["label"] = it }
        latitude?.let { map["latitude"] = it }
        longitude?.let { map["longitude"] = it }
        return map
    }

    private fun ImagePayloadDescriptor.toWire(): Map<String, Any> = linkedMapOf(
        "assetId" to assetId.trim(),
        "role" to role.wireValue,
        "mimeType" to mimeType.trim(),
        "byteCount" to byteCount,
        "reference" to linkedMapOf(
            "kind" to reference.kind.wireValue,
            "id" to reference.id.trim()
        )
    ).also { map ->
        width?.let { map["width"] = it }
        height?.let { map["height"] = it }
    }

    private fun AdditionalInputs.toWire(): Map<String, Any> {
        val map = LinkedHashMap<String, Any>()
        dreamText.clean()?.let { map["dreamText"] = it }
        deckId.clean()?.let { map["deckId"] = it }
        deckVersion.clean()?.let { map["deckVersion"] = it }
        spreadId.clean()?.let { map["spreadId"] = it }
        castMethod.clean()?.let { map["castMethod"] = it }
        targetDate.clean()?.let { map["targetDate"] = it }
        return map
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private val ImageRole.wireValue: String
        get() = when (this) {
            ImageRole.PRIMARY -> "primary"
            ImageRole.CUP -> "cup"
            ImageRole.SAUCER -> "saucer"
        }

    private val ImageReferenceKind.wireValue: String
        get() = when (this) {
            ImageReferenceKind.LOCAL_ATTACHMENT -> "localAttachment"
            ImageReferenceKind.PRIVATE_OBJECT -> "privateObject"
        }
}
