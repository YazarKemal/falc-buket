package com.prompthavenai.falcibuket.data

import com.prompthavenai.falcibuket.data.model.AdditionalInputs
import com.prompthavenai.falcibuket.data.model.CardSelection
import com.prompthavenai.falcibuket.data.model.FortuneType
import com.prompthavenai.falcibuket.data.model.ImagePayloadDescriptor
import com.prompthavenai.falcibuket.data.model.ImageReference
import com.prompthavenai.falcibuket.data.model.ImageReferenceKind
import com.prompthavenai.falcibuket.data.model.ImageRole
import com.prompthavenai.falcibuket.data.model.PersonData
import com.prompthavenai.falcibuket.data.model.PlaceData
import com.prompthavenai.falcibuket.data.model.ReadingFamily
import com.prompthavenai.falcibuket.data.model.ReadingRequest
import com.prompthavenai.falcibuket.data.model.SymbolSelection
import com.prompthavenai.falcibuket.data.remote.ReadingRequestMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingRequestContractTest {

    @Test
    fun `all 28 types map to the 7 families with expected counts`() {
        val byFamily = FortuneType.entries.groupBy { ReadingFamily.of(it) }
        assertEquals(28, FortuneType.entries.size)
        assertEquals(4, byFamily[ReadingFamily.VISION]?.size)
        assertEquals(5, byFamily[ReadingFamily.CARDS]?.size)
        assertEquals(1, byFamily[ReadingFamily.DREAM_TEXT]?.size)
        assertEquals(6, byFamily[ReadingFamily.ASTROLOGY_CALCULATED]?.size)
        assertEquals(1, byFamily[ReadingFamily.COMPATIBILITY]?.size)
        assertEquals(5, byFamily[ReadingFamily.SYMBOL_CAST]?.size)
        assertEquals(6, byFamily[ReadingFamily.QUESTION_INTUITION]?.size)
        assertEquals(ReadingFamily.entries.size, byFamily.size)
    }

    @Test
    fun `wire payload uses the backend contract field names`() {
        val request = ReadingRequest(
            readingType = FortuneType.TAROT,
            requestId = "req-42",
            question = "  Yakın geleceğim?  ",
            selectedCards = listOf(CardSelection(position = 0, cardId = "the-fool", reversed = true)),
            selectedSymbols = listOf(SymbolSelection(symbolId = "fehu", position = 1)),
            birthData = PersonData(
                birthDate = "1990-03-12",
                name = "Ayşe",
                birthTime = "07:45",
                timeZone = "Europe/Istanbul",
                place = PlaceData(label = "İstanbul", latitude = 41.0, longitude = 29.0)
            ),
            imagePayload = listOf(
                ImagePayloadDescriptor(
                    assetId = "cup-1",
                    role = ImageRole.CUP,
                    mimeType = "image/jpeg",
                    byteCount = 1024L,
                    reference = ImageReference(ImageReferenceKind.LOCAL_ATTACHMENT, "attachment-1")
                )
            ),
            additionalInputs = AdditionalInputs(deckId = "rws", deckVersion = "1", spreadId = "three")
        )

        val payload = ReadingRequestMapper.toWirePayload(request)

        assertEquals(1, payload["schemaVersion"])
        assertEquals("req-42", payload["requestId"])
        assertEquals("tarot", payload["readingType"])
        assertEquals("Yakın geleceğim?", payload["question"])

        @Suppress("UNCHECKED_CAST")
        val card = (payload["selectedCards"] as List<Map<String, Any>>)[0]
        assertEquals(0, card["position"])
        assertEquals("the-fool", card["cardId"])
        assertEquals(true, card["reversed"])

        @Suppress("UNCHECKED_CAST")
        val symbol = (payload["selectedSymbols"] as List<Map<String, Any>>)[0]
        assertEquals("fehu", symbol["symbolId"])
        assertEquals(1, symbol["position"])

        @Suppress("UNCHECKED_CAST")
        val birth = payload["birthData"] as Map<String, Any>
        assertEquals("1990-03-12", birth["birthDate"])
        assertEquals("07:45", birth["birthTime"])
        assertEquals("Europe/Istanbul", birth["timeZone"])

        @Suppress("UNCHECKED_CAST")
        val image = (payload["imagePayload"] as List<Map<String, Any>>)[0]
        assertEquals("cup", image["role"])
        @Suppress("UNCHECKED_CAST")
        val reference = image["reference"] as Map<String, Any>
        assertEquals("localAttachment", reference["kind"])
        assertEquals("attachment-1", reference["id"])

        @Suppress("UNCHECKED_CAST")
        val extra = payload["additionalInputs"] as Map<String, Any>
        assertEquals("rws", extra["deckId"])
        assertEquals("three", extra["spreadId"])
    }

    @Test
    fun `blank and empty optional fields are omitted from the payload`() {
        val request = ReadingRequest(
            readingType = FortuneType.LOVE,
            requestId = "req-1",
            question = "   "
        )
        val payload = ReadingRequestMapper.toWirePayload(request)

        assertFalse(payload.containsKey("question"))
        assertFalse(payload.containsKey("selectedCards"))
        assertFalse(payload.containsKey("selectedSymbols"))
        assertFalse(payload.containsKey("imagePayload"))
        assertFalse(payload.containsKey("birthData"))
        assertFalse(payload.containsKey("additionalInputs"))
        assertTrue(payload.containsKey("readingType"))
    }
}
