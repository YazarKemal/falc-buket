package com.prompthavenai.falcibuket.data.local

import android.content.Context
import android.net.Uri
import com.prompthavenai.falcibuket.data.model.CoffeeCupPhotoGeometry
import com.prompthavenai.falcibuket.data.model.CoffeeCupRegion
import com.prompthavenai.falcibuket.data.remote.ImageCompressor
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Kalıcılaştırılmış tek bir bölge fotoğrafı (uygulama-özel depolama). */
data class CoffeeSessionPhoto(
    val region: CoffeeCupRegion,
    val file: File,
    val revision: String
)

/**
 * Süreç ölümünden sonra geri yüklenen oturum anlık görüntüsü.
 * Yalnızca uygulama-özel dosya yolları içerir; harici Uri veya sağlayıcı
 * bilgisi saklanmaz ve loglanmaz.
 */
data class CoffeeSessionSnapshot(
    val photos: Map<CoffeeCupRegion, CoffeeSessionPhoto>,
    val geometries: Map<CoffeeCupRegion, CoffeeCupPhotoGeometry>
) {
    fun isPresent(region: CoffeeCupRegion): Boolean = photos.containsKey(region)

    fun geometryFor(region: CoffeeCupRegion): CoffeeCupPhotoGeometry? = geometries[region]

    /** Atlasın hangi fotoğraf/geometri sürümüne göre kurulduğunu belirten imza. */
    fun revisionSignature(): String = CoffeeCupRegion.entries.joinToString("|") { region ->
        val photo = photos[region]?.revision ?: "-"
        val geo = geometries[region]
        val geoKey = if (geo == null) "-" else "${geo.rimCenterX},${geo.rimCenterY},${geo.rimRadiusX},${geo.rimRadiusY},${geo.rimRotation}"
        "$photo:$geoKey"
    }

    val presentCount: Int get() = photos.size

    companion object {
        val EMPTY = CoffeeSessionSnapshot(emptyMap(), emptyMap())
    }
}

/**
 * Üç bölge fotoğrafını ve kalibrasyonlarını uygulama-özel kalıcı depolamada
 * tutar. Fotoğraflar seçilir seçilmez sanitize edilmiş JPEG olarak kopyalanır;
 * süreç yeniden başladığında hazır bölgeler otomatik geri yüklenir.
 */
object CoffeeSessionStore {

    private const val DIR_NAME = "coffee_session"
    private const val META_NAME = "session.json"
    private const val VERSION = 1

    /** Tüm okuma-değiştirme-yazma işlemlerini serileştirir (kayıp güncelleme yok). */
    private val lock = Any()

    fun sessionDir(context: Context): File = File(context.filesDir, DIR_NAME)

    fun photoFile(context: Context, region: CoffeeCupRegion): File =
        File(sessionDir(context), fileName(region))

    private fun fileName(region: CoffeeCupRegion): String = when (region) {
        CoffeeCupRegion.LEFT_INNER -> "left.jpg"
        CoffeeCupRegion.CENTER_INNER -> "center.jpg"
        CoffeeCupRegion.RIGHT_INNER -> "right.jpg"
    }

    fun load(context: Context): CoffeeSessionSnapshot = synchronized(lock) {
        loadLocked(context)
    }

    private fun loadLocked(context: Context): CoffeeSessionSnapshot {
        val dir = sessionDir(context)
        if (!dir.exists()) return CoffeeSessionSnapshot.EMPTY
        val meta = readMeta(context)
        val photos = LinkedHashMap<CoffeeCupRegion, CoffeeSessionPhoto>()
        val geometries = LinkedHashMap<CoffeeCupRegion, CoffeeCupPhotoGeometry>()
        for (region in CoffeeCupRegion.entries) {
            val file = photoFile(context, region)
            if (!file.exists() || file.length() <= 0L) continue
            val regionMeta = meta?.optJSONObject(region.name)
            val revision = regionMeta?.optString("revision")?.takeIf { it.isNotBlank() }
                ?: deriveRevision(file)
            photos[region] = CoffeeSessionPhoto(region, file, revision)
            val geometry = readGeometry(regionMeta)
            if (geometry != null) geometries[region] = geometry
        }
        return CoffeeSessionSnapshot(photos, geometries)
    }

    /** Seçilen fotoğrafı hemen sanitize edilmiş JPEG olarak kalıcılaştırır. */
    fun savePhoto(context: Context, region: CoffeeCupRegion, uri: Uri): CoffeeSessionSnapshot =
        synchronized(lock) {
            val processed = ImageCompressor.process(context, uri, maxEdge = com.prompthavenai.falcibuket.data.model.CoffeeCupAtlas.PHOTO_MAX_EDGE)
            val dir = sessionDir(context)
            if (!dir.exists()) dir.mkdirs()
            val target = photoFile(context, region)
            val temp = File(dir, "${fileName(region)}.tmp")
            temp.writeBytes(processed.jpeg)
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            val meta = readMeta(context) ?: JSONObject()
            meta.put("version", VERSION)
            val regions = meta.optJSONObject("regions") ?: JSONObject().also { meta.put("regions", it) }
            val entry = JSONObject()
            entry.put("revision", UUID.randomUUID().toString())
            regions.put(region.name, entry)
            writeMeta(context, meta)
            loadLocked(context)
        }

    fun saveGeometry(
        context: Context,
        region: CoffeeCupRegion,
        geometry: CoffeeCupPhotoGeometry
    ): CoffeeSessionSnapshot = synchronized(lock) {
        val dir = sessionDir(context)
        if (!dir.exists()) dir.mkdirs()
        val meta = readMeta(context) ?: JSONObject()
        meta.put("version", VERSION)
        val regions = meta.optJSONObject("regions") ?: JSONObject().also { meta.put("regions", it) }
        val entry = regions.optJSONObject(region.name) ?: JSONObject().also { regions.put(region.name, it) }
        entry.put("cx", geometry.rimCenterX.toDouble())
        entry.put("cy", geometry.rimCenterY.toDouble())
        entry.put("rx", geometry.rimRadiusX.toDouble())
        entry.put("ry", geometry.rimRadiusY.toDouble())
        entry.put("rot", geometry.rimRotation.toDouble())
        writeMeta(context, meta)
        loadLocked(context)
    }

    /** Tüm oturum fotoğraflarını ve metadatasını siler. */
    fun clear(context: Context) = synchronized(lock) {
        val dir = sessionDir(context)
        if (dir.exists()) dir.deleteRecursively()
    }

    private fun readGeometry(regionMeta: JSONObject?): CoffeeCupPhotoGeometry? {
        if (regionMeta == null) return null
        if (!regionMeta.has("cx") || !regionMeta.has("cy") || !regionMeta.has("rx") || !regionMeta.has("ry")) {
            return null
        }
        val geometry = CoffeeCupPhotoGeometry(
            rimCenterX = regionMeta.optDouble("cx").toFloat(),
            rimCenterY = regionMeta.optDouble("cy").toFloat(),
            rimRadiusX = regionMeta.optDouble("rx").toFloat(),
            rimRadiusY = regionMeta.optDouble("ry").toFloat(),
            rimRotation = regionMeta.optDouble("rot", 0.0).toFloat()
        )
        return if (geometry.isValid()) geometry else null
    }

    private fun deriveRevision(file: File): String = "${file.length()}-${file.lastModified()}"

    private fun readMeta(context: Context): JSONObject? {
        val file = File(sessionDir(context), META_NAME)
        if (!file.exists()) return null
        return runCatching { JSONObject(file.readText()) }.getOrNull()
    }

    private fun writeMeta(context: Context, meta: JSONObject) {
        val dir = sessionDir(context)
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, META_NAME)
        val temp = File(dir, "$META_NAME.tmp")
        temp.writeText(meta.toString())
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }
}
