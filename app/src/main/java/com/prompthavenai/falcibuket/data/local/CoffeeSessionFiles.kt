package com.prompthavenai.falcibuket.data.local

import com.prompthavenai.falcibuket.data.model.CoffeeCupPhotoGeometry
import com.prompthavenai.falcibuket.data.model.CoffeeCupRegion
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Uygulama-özel oturum dosyalarının saf (Android'siz) yöneticisi: üç JPEG +
 * `session.json` metadata. Android `Context`'e bağlı değildir; böylece JVM
 * birim testleriyle gerçek davranış doğrulanabilir.
 *
 * Dayanıklılık:
 * - başlangıçta yetim `*.tmp` dosyaları temizlenir,
 * - bozuk/kısaltılmış `session.json` istisna fırlatmaz; fotoğraflar dosya
 *   adından türetilmiş revizyonla kurtarılır,
 * - geçerli fotoğraflar ve kayıtlı kalibrasyon geometrisi korunur.
 */
internal class CoffeeSessionFiles(private val dir: File) {

    fun load(): CoffeeSessionSnapshot {
        if (!dir.exists()) return CoffeeSessionSnapshot.EMPTY
        cleanOrphanTemps()
        val meta = readMeta()
        val regions = meta?.optJSONObject("regions")
        val photos = LinkedHashMap<CoffeeCupRegion, CoffeeSessionPhoto>()
        val geometries = LinkedHashMap<CoffeeCupRegion, CoffeeCupPhotoGeometry>()
        for (region in CoffeeCupRegion.entries) {
            val file = File(dir, fileName(region))
            if (!file.isFile || file.length() <= 0L) continue
            val regionMeta = regions?.optJSONObject(region.name)
            val revision = regionMeta?.optString("revision")?.takeIf { it.isNotBlank() }
                ?: deriveRevision(file)
            photos[region] = CoffeeSessionPhoto(region, file, revision)
            val geometry = readGeometry(regionMeta)
            if (geometry != null) geometries[region] = geometry
        }
        return CoffeeSessionSnapshot(photos, geometries)
    }

    fun savePhotoBytes(region: CoffeeCupRegion, jpeg: ByteArray): CoffeeSessionSnapshot {
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, fileName(region))
        val temp = File(dir, "${fileName(region)}.tmp")
        temp.writeBytes(jpeg)
        atomicReplace(temp, target)
        val meta = readMeta() ?: JSONObject()
        meta.put("version", VERSION)
        val regions = meta.optJSONObject("regions") ?: JSONObject().also { meta.put("regions", it) }
        val entry = JSONObject()
        entry.put("revision", UUID.randomUUID().toString())
        regions.put(region.name, entry)
        writeMeta(meta)
        return load()
    }

    fun saveGeometry(
        region: CoffeeCupRegion,
        geometry: CoffeeCupPhotoGeometry,
        expectedPhotoRevision: String? = null
    ): CoffeeSessionSnapshot {
        if (!geometry.isValid()) return load()
        val photoFile = File(dir, fileName(region))
        if (!photoFile.isFile || photoFile.length() <= 0L) return load()
        val meta = readMeta() ?: JSONObject()
        meta.put("version", VERSION)
        val regions = meta.optJSONObject("regions") ?: JSONObject().also { meta.put("regions", it) }
        val entry = regions.optJSONObject(region.name) ?: JSONObject().also { regions.put(region.name, it) }
        val storedRevision = entry.optString("revision").takeIf { it.isNotBlank() }
        val currentRevision = storedRevision ?: deriveRevision(photoFile)
        // Beklenen fotoğraf sürümü verildiyse ve uyuşmuyorsa eski geometriyi
        // yeni fotoğrafa uygulama.
        if (expectedPhotoRevision != null && expectedPhotoRevision != currentRevision) return load()
        if (storedRevision == null) entry.put("revision", currentRevision)
        entry.put("cx", geometry.rimCenterX.toDouble())
        entry.put("cy", geometry.rimCenterY.toDouble())
        entry.put("rx", geometry.rimRadiusX.toDouble())
        entry.put("ry", geometry.rimRadiusY.toDouble())
        entry.put("rot", geometry.rimRotation.toDouble())
        writeMeta(meta)
        return load()
    }

    fun clear() {
        if (dir.exists()) dir.deleteRecursively()
    }

    /** Yetim (yarım kalmış) geçici dosyaları sil; geçerli son dosyalara dokunma. */
    private fun cleanOrphanTemps() {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isFile && file.name.endsWith(TMP_SUFFIX)) {
                file.delete()
            }
        }
    }

    private fun readMeta(): JSONObject? {
        val file = File(dir, META_NAME)
        if (!file.isFile) return null
        return runCatching { JSONObject(file.readText()) }.getOrNull()
    }

    private fun writeMeta(meta: JSONObject) {
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, META_NAME)
        val temp = File(dir, "$META_NAME$TMP_SUFFIX")
        temp.writeText(meta.toString())
        atomicReplace(temp, target)
    }

    private fun atomicReplace(temp: File, target: File) {
        try {
            Files.move(
                temp.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: Exception) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
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

    companion object {
        const val META_NAME = "session.json"
        const val TMP_SUFFIX = ".tmp"
        const val VERSION = 1

        fun fileName(region: CoffeeCupRegion): String = when (region) {
            CoffeeCupRegion.LEFT_INNER -> "left.jpg"
            CoffeeCupRegion.CENTER_INNER -> "center.jpg"
            CoffeeCupRegion.RIGHT_INNER -> "right.jpg"
        }
    }
}
