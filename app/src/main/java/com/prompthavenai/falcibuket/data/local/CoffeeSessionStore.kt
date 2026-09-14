package com.prompthavenai.falcibuket.data.local

import android.content.Context
import android.net.Uri
import com.prompthavenai.falcibuket.data.model.CoffeeCupPhotoGeometry
import com.prompthavenai.falcibuket.data.model.CoffeeCupRegion
import com.prompthavenai.falcibuket.data.remote.ImageCompressor
import java.io.File

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
 *
 * Dosya/JSON mantığı [CoffeeSessionFiles] içindedir (Android'siz, test edilebilir);
 * bu nesne yalnızca Context köprüsü ve görüntü sıkıştırmayı sağlar.
 */
object CoffeeSessionStore {

    private const val DIR_NAME = "coffee_session"

    /** Tüm okuma-değiştirme-yazma işlemlerini serileştirir (kayıp güncelleme yok). */
    private val lock = Any()

    fun sessionDir(context: Context): File = File(context.filesDir, DIR_NAME)

    fun photoFile(context: Context, region: CoffeeCupRegion): File =
        File(sessionDir(context), CoffeeSessionFiles.fileName(region))

    private fun files(context: Context): CoffeeSessionFiles = CoffeeSessionFiles(sessionDir(context))

    fun load(context: Context): CoffeeSessionSnapshot = synchronized(lock) {
        files(context).load()
    }

    /** Seçilen fotoğrafı hemen sanitize edilmiş JPEG olarak kalıcılaştırır. */
    fun savePhoto(context: Context, region: CoffeeCupRegion, uri: Uri): CoffeeSessionSnapshot =
        synchronized(lock) {
            val processed = ImageCompressor.process(
                context, uri,
                maxEdge = com.prompthavenai.falcibuket.data.model.CoffeeCupAtlas.PHOTO_MAX_EDGE
            )
            files(context).savePhotoBytes(region, processed.jpeg)
        }

    fun saveGeometry(
        context: Context,
        region: CoffeeCupRegion,
        geometry: CoffeeCupPhotoGeometry,
        expectedPhotoRevision: String? = null
    ): CoffeeSessionSnapshot = synchronized(lock) {
        files(context).saveGeometry(region, geometry, expectedPhotoRevision)
    }

    /** Tüm oturum fotoğraflarını ve metadatasını siler. */
    fun clear(context: Context) = synchronized(lock) {
        files(context).clear()
    }
}
