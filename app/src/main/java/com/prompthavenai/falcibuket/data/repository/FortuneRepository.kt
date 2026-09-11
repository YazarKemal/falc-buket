package com.prompthavenai.falcibuket.data.repository

import com.prompthavenai.falcibuket.data.model.HistoryEntry
import com.prompthavenai.falcibuket.data.model.Reading
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Mock fortune repository. Sonraki milestone'da DeepSeek API çağrıları bu
 * interface arkasına eklenecek; UI sadece bu sınıfa bağımlı kalacak.
 */
interface FortuneRepository {
    fun analyzeReading(type: String, question: String?): Flow<Reading>
    val history: Flow<List<HistoryEntry>>
    fun saveReading(reading: Reading)
}

class MockFortuneRepository : FortuneRepository {

    private val saved = MutableStateFlow(defaultHistory())

    override fun analyzeReading(type: String, question: String?): Flow<Reading> = flow {
        delay(2000)
        emit(mockReading(type, question))
    }

    override val history = saved.asStateFlow()

    override fun saveReading(reading: Reading) {
        saved.value = saved.value + HistoryEntry("${reading.type} Falı", "Bugün", saved.value.firstOrNull()?.imageRes ?: 0)
    }

    private fun mockReading(type: String, question: String?) = Reading(
        id = type + System.currentTimeMillis(),
        type = type,
        dateLabel = "Bugün",
        generalEnergy = "Bugün etrafında güçlü bir mistik enerji var. Ay'ın konumu sezgilerini keskinleştiriyor; " +
            "zihinsel olarak açık olduğun her alanda beklenmedik bir netlik yakalayabilirsin. Yorgunluk ne olursa olsun " +
            "iç sesin bugün doğru yöne işaret ediyor.",
        love = "Kalbinle ilgili sorular yakın bir sürede tatmin olacak şekilde düzenleniyor. Seninle aynı frekansta " +
            "yaşayan biri, görünmeyen bir katkı sunuyor. Bir ilişki varsa küçük bir jest büyük bir yumuşamaya dönüşebilir.",
        career = "Kariyer ve para hattında önünde iki ayrı kapı beliriyor. Aceleci bir seçim yapmadan önce sabırlı bir " +
            "konuşma seni doğru kapıya yaklaştıracak. Maddi konularda bir kaynak, beklentinden daha yavaş ama daha sağlam " +
            "şekilde geliyor.",
        nearFuture = "Önümüzdeki birkaç gün içinde bir mesaj ya da davet seni şaşırtabilir. Geçmişte yarıda bıraktığın " +
            "bir konu yeniden gündeme gelirken, bu sefer kendini daha hazır hissedeceksin.",
        highlight = "Önümüzdeki birkaç hafta içinde beklediğin bir konuda iletişim veya yeni bir haber öne çıkıyor. " +
            "Buket bu işareti özellikle güçlü görüyor: aniden gelen bir 'merhaba' planlarının yönünü değiştirebilir."
    )

    private fun defaultHistory() = listOf(
        HistoryEntry("Bugünün Falı", "Bugün", com.prompthavenai.falcibuket.R.drawable.result_background),
        HistoryEntry("Aşk Falı", "2 gün önce", com.prompthavenai.falcibuket.R.drawable.love_fortune),
        HistoryEntry("Kahve Falı", "1 hafta önce", com.prompthavenai.falcibuket.R.drawable.coffee_fortune)
    )
}
