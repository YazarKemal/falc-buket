package com.prompthavenai.falcibuket.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.prompthavenai.falcibuket.BuildConfig
import com.prompthavenai.falcibuket.data.local.CoffeeSessionSnapshot
import com.prompthavenai.falcibuket.data.model.CoffeeAtlasRetryPolicy
import com.prompthavenai.falcibuket.data.model.CoffeeCanonicalReconstruction as C
import com.prompthavenai.falcibuket.data.model.CoffeeCupAtlas
import com.prompthavenai.falcibuket.data.model.CoffeeCupPhotoGeometry
import com.prompthavenai.falcibuket.data.model.CoffeeCupRegion
import com.prompthavenai.falcibuket.data.model.CoffeeGeometryGate
import com.prompthavenai.falcibuket.data.model.CoffeePhotometric
import com.prompthavenai.falcibuket.data.model.CoffeeRimEstimator
import com.prompthavenai.falcibuket.data.model.PhotoAlignment
import com.prompthavenai.falcibuket.data.model.RimEstimate
import kotlinx.coroutines.CancellationException
import java.io.File
import kotlin.math.abs

/**
 * V4 KANONİK FİNCAN-UZAYI atlas üreticisi (Android sınırı).
 *
 * Kalıcı oturumdan üç fotoğrafı okur → rim elipsini tespit eder/kalibrasyonu
 * kullanır → her birini kanonik diske düzeltir → yan diskleri merkeze hizalar →
 * kabul-kapılı fotometrik düzeltme → ÜÇ diski TEK kanonik diske füzyonlar →
 * zemin+duvar atlasını üretir. Hiçbir görsel ağa gönderilmez.
 */
object CoffeeCupAtlasBuilder {

    private const val TAG = "FalciBuketAtlas"
    private val buildLock = Any()

    class Metrics(
        val confidences: FloatArray,
        val rimStatuses: Array<RimEstimate.Status?>,
        val lcBefore: Float,
        val lcAfter: Float,
        val lcAccepted: Boolean,
        val crBefore: Float,
        val crAfter: Float,
        val crAccepted: Boolean,
        val canonicalCoverage: Float,
        val floorCoverage: Float,
        val wallCoverage: Float,
        val present: BooleanArray,
        val needsCalibration: BooleanArray,
        val alignments: FloatArray
    )

    class BuildResult(val atlas: Bitmap?, val metrics: Metrics?)

    /**
     * Sabit kalite profilleri. Normal yol (sampleSize=1) değişmez; düşük-bellek
     * yolu yalnızca OOM sonrası TEK kez denenir.
     */
    private class BuildResolution(
        val diskSize: Int,
        val atlasWidth: Int,
        val atlasHeight: Int,
        val decodeSampleSize: Int
    ) {
        companion object {
            val NORMAL = BuildResolution(
                C.DISK_SIZE, CoffeeCupAtlas.ATLAS_WIDTH, CoffeeCupAtlas.ATLAS_HEIGHT, 1
            )
            val LOW_MEMORY = BuildResolution(
                C.DISK_SIZE / 2, CoffeeCupAtlas.ATLAS_WIDTH / 2, CoffeeCupAtlas.ATLAS_HEIGHT / 2, 2
            )
        }
    }

    fun build(context: Context, snapshot: CoffeeSessionSnapshot): BuildResult = synchronized(buildLock) {
        val run = CoffeeAtlasRetryPolicy.run(
            normal = { attempt(context, snapshot, BuildResolution.NORMAL) },
            retry = { attempt(context, snapshot, BuildResolution.LOW_MEMORY) },
            onEvent = { event -> Log.w(TAG, event) }
        )
        run.value ?: BuildResult(null, null)
    }

    /** Tek bir deneme. OOM çağırana (retry politikasına) bırakılır; diğer hatalar kontrollü. */
    private fun attempt(
        context: Context,
        snapshot: CoffeeSessionSnapshot,
        resolution: BuildResolution
    ): BuildResult {
        return try {
            buildInternal(context, snapshot, resolution)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "BUILD_FAILED stage=exception")
            BuildResult(null, null)
        }
    }

    private fun buildInternal(
        context: Context,
        snapshot: CoffeeSessionSnapshot,
        resolution: BuildResolution
    ): BuildResult {
        val buffers = arrayOfNulls<C.PixelBuffer>(3)
        val geometries = arrayOfNulls<CoffeeCupPhotoGeometry>(3)
        val confidences = FloatArray(3)
        val rimStatuses = arrayOfNulls<RimEstimate.Status>(3)
        val needsCalibration = BooleanArray(3)
        val present = BooleanArray(3)

        for (region in CoffeeCupRegion.entries) {
            val photo = snapshot.photos[region] ?: continue
            val index = CoffeeCupAtlas.regionIndex(region)
            val buffer = loadBuffer(photo.file, resolution.decodeSampleSize) ?: continue
            buffers[index] = buffer
            present[index] = true

            val manual = snapshot.geometryFor(region)
            if (manual != null && manual.isValid()) {
                geometries[index] = manual
                confidences[index] = 1f
                rimStatuses[index] = RimEstimate.Status.OK
            } else {
                val estimate = CoffeeRimEstimator.estimate(buffer)
                rimStatuses[index] = estimate.status
                confidences[index] = estimate.confidence
                val decision = CoffeeGeometryGate.select(null, estimate)
                if (decision.geometry != null) {
                    geometries[index] = decision.geometry
                } else {
                    needsCalibration[index] = true
                }
            }
        }

        if (!present.any()) {
            logMetrics(
                confidences, rimStatuses, 0f, 0f, false, 0f, 0f, false,
                0f, 0f, 0f, present, needsCalibration, FloatArray(3)
            )
            return BuildResult(null, null)
        }

        // Düşük güvenli otomatik geometriyle sessizce atlas KURMA. Kalibrasyon
        // gereken bir bölge varsa hiç düzeltme/füzyon yapmadan dur.
        if (needsCalibration.any { it }) {
            val metrics = Metrics(
                confidences, rimStatuses, 0f, 0f, false, 0f, 0f, false,
                0f, 0f, 0f, present, needsCalibration, FloatArray(3)
            )
            logMetrics(
                confidences, rimStatuses, 0f, 0f, false, 0f, 0f, false,
                0f, 0f, 0f, present, needsCalibration, FloatArray(3)
            )
            return BuildResult(null, metrics)
        }

        val disks = arrayOfNulls<C.CanonicalDisk>(3)
        for (i in 0..2) {
            val buffer = buffers[i] ?: continue
            val geometry = geometries[i] ?: continue
            disks[i] = C.rectify(buffer, geometry, resolution.diskSize)
            buffers[i] = null
        }

        val anchorIndex = if (disks[1] != null) 1 else disks.indexOfFirst { it != null }
        val alignments = arrayOf(
            PhotoAlignment.IDENTITY,
            PhotoAlignment.IDENTITY,
            PhotoAlignment.IDENTITY
        )
        val anchor = disks.getOrNull(anchorIndex)
        if (anchor != null) {
            for (i in 0..2) {
                if (i == anchorIndex) continue
                val disk = disks[i] ?: continue
                alignments[i] = C.estimateAlignment(anchor, disk)
            }
        }

        // Hizalamayı ÖNCE disklere uygula ki fotometrik kapı ve füzyon AYNI
        // hizalanmış veriyi görsün. Füzyonda artık identity kullanılır.
        val rotations = FloatArray(3) { alignments[it].contentRotation }
        for (i in 0..2) {
            val disk = disks[i] ?: continue
            if (abs(rotations[i]) > 1e-6f) {
                disks[i] = C.rotateDisk(disk, rotations[i])
                alignments[i] = PhotoAlignment.IDENTITY
            }
        }

        val lc = photometric(anchorIndex, 1, 0, disks)
        val cr = photometric(anchorIndex, 1, 2, disks)

        val fused = C.fuse(disks, alignments, resolution.diskSize)
        val atlas = C.renderAtlas(
            fused.disk, resolution.atlasWidth, resolution.atlasHeight, CoffeeCupAtlas.FLOOR_V
        )
        val coverage = C.measureCoverage(atlas, fused.disk)

        exportDebug(context, disks, fused, atlas)

        // Hiçbir disk düzeltilemediyse (tüm geometriler geçersiz) boş bir IVORY
        // atlası "başarı" gibi döndürme; kalibrasyon gerekli durumu bildir.
        val bitmap = if (coverage.first > 0f) {
            bufferToBitmap(atlas.atlas, atlas.width, atlas.height)
        } else {
            null
        }
        logMetrics(
            confidences, rimStatuses,
            lc.before, lc.after, lc.accepted,
            cr.before, cr.after, cr.accepted,
            coverage.first, coverage.second, coverage.third,
            present, needsCalibration, rotations
        )
        return BuildResult(
            bitmap,
            Metrics(
                confidences, rimStatuses,
                lc.before, lc.after, lc.accepted,
                cr.before, cr.after, cr.accepted,
                coverage.first, coverage.second, coverage.third,
                present, needsCalibration, rotations
            )
        )
    }

    private class PhotometricOutcome(val before: Float, val after: Float, val accepted: Boolean) {
        companion object {
            val NONE = PhotometricOutcome(0f, 0f, false)
        }
    }

    private fun photometric(
        anchorIndex: Int,
        anchorRegion: Int,
        sideIndex: Int,
        disks: Array<C.CanonicalDisk?>
    ): PhotometricOutcome {
        if (anchorIndex != anchorRegion) return PhotometricOutcome.NONE
        val anchor = disks[anchorIndex] ?: return PhotometricOutcome.NONE
        val side = disks[sideIndex] ?: return PhotometricOutcome.NONE
        val candidate = CoffeePhotometric.estimateCandidate(anchor, side)
        val decision = CoffeePhotometric.evaluateAndGate(anchor, side, candidate)
        if (decision.accepted) {
            disks[sideIndex] = CoffeePhotometric.applyToDisk(side, candidate)
        }
        return PhotometricOutcome(decision.before, decision.after, decision.accepted)
    }

    private fun loadBuffer(file: File, sampleSize: Int): C.PixelBuffer? {
        val decoded = if (sampleSize <= 1) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } ?: return null
        return try {
            bitmapToBuffer(decoded)
        } finally {
            decoded.recycle()
        }
    }

    private fun logMetrics(
        confidences: FloatArray,
        rimStatuses: Array<RimEstimate.Status?>,
        lcBefore: Float,
        lcAfter: Float,
        lcAccepted: Boolean,
        crBefore: Float,
        crAfter: Float,
        crAccepted: Boolean,
        canonicalCoverage: Float,
        floorCoverage: Float,
        wallCoverage: Float,
        present: BooleanArray,
        needsCalibration: BooleanArray,
        alignments: FloatArray
    ) {
        if (!BuildConfig.DEBUG) return
        val rim = (0..2).joinToString(" ") { i ->
            val status = rimStatuses[i]?.name ?: "ABSENT"
            "r$i=$status:${confidences[i]}"
        }
        val lc = if (lcAccepted) "before=${lcBefore} after=${lcAfter}" else "before=${lcBefore} REJECTED"
        val cr = if (crAccepted) "before=${crBefore} after=${crAfter}" else "before=${crBefore} REJECTED"
        val cal = needsCalibration.joinToString("") { if (it) "1" else "0" }
        val presentFlags = present.joinToString("") { if (it) "1" else "0" }
        val align = alignments.joinToString(",") { it.toString() }
        Log.d(
            TAG,
            "V4 RIM $rim PRESENT=$presentFlags CALIB=$cal ALIGN=$align " +
                "LC $lc CR $cr " +
                "COVERAGE canonical=${canonicalCoverage}% floor=${floorCoverage}% wall=${wallCoverage}%"
        )
    }

    // ---------------------------------------------------------------------
    // DEBUG dışa aktarımlar
    // ---------------------------------------------------------------------

    private fun exportDebug(
        context: Context,
        disks: Array<C.CanonicalDisk?>,
        fused: C.FusionResult,
        atlas: C.AtlasPixels
    ) {
        if (!BuildConfig.DEBUG) return
        try {
            val dir = context.cacheDir
            val names = arrayOf("debug_left_disk.png", "debug_center_disk.png", "debug_right_disk.png")
            for (i in 0..2) {
                val disk = disks[i]
                val target = File(dir, names[i])
                if (disk == null) {
                    target.delete()
                } else {
                    writePng(target, diskToBitmap(disk))
                }
            }
            writePng(File(dir, "debug_fused_disk.png"), diskToBitmap(fused.disk, markOrientation = true))
            val floorCartesian = C.renderFloorCartesian(fused.disk, fused.disk.size)
            writePng(File(dir, "debug_floor_reconstruction.png"), bufferToBitmap(floorCartesian, fused.disk.size, fused.disk.size))
            writePng(File(dir, "debug_wall_reconstruction.png"), bufferToBitmap(atlas.wall, atlas.width, atlas.height - atlas.floorRows))
            val atlasWithMarks = atlas.atlas.copyOf()
            drawOrientationMarksAtlas(atlasWithMarks, atlas.width, atlas.height, atlas.floorRows)
            writePng(File(dir, "debug_final_atlas.png"), bufferToBitmap(atlasWithMarks, atlas.width, atlas.height))
            writePng(File(dir, "debug_source_weights.png"), bufferToBitmap(C.renderWeights(fused.weights, fused.disk.size), fused.disk.size, fused.disk.size))
            val weightSummary = weightSummary(fused.weights, fused.disk.size)
            Log.d(TAG, "V4 WEIGHTS $weightSummary")
        } catch (e: Exception) {
            Log.w(TAG, "EXPORT_FAILED stage=exception")
        }
    }

    private fun writePng(file: File, bitmap: Bitmap) {
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
    }

    // ---------------------------------------------------------------------
    // Bitmap yardımcıları (Android sınırı)
    // ---------------------------------------------------------------------

    private fun bitmapToBuffer(bitmap: Bitmap): C.PixelBuffer {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        return C.PixelBuffer(w, h, pixels)
    }

    private fun bufferToBitmap(pixels: IntArray, width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

    private fun diskToBitmap(disk: C.CanonicalDisk, markOrientation: Boolean = false): Bitmap {
        val size = disk.size
        val pixels = IntArray(size * size)
        for (i in 0 until size * size) {
            pixels[i] = if (disk.valid[i]) disk.image.argb[i] else 0x00000000
        }
        if (markOrientation) drawOrientationMarksDisk(pixels, size)
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    private fun drawOrientationMarksDisk(pixels: IntArray, size: Int) {
        val marks = intArrayOf(
            C.argb(255, 0, 0), C.argb(0, 255, 0), C.argb(0, 128, 255), C.argb(255, 255, 0)
        )
        val twoPi = 2f * Math.PI.toFloat()
        for (m in 0..3) {
            val theta = m * (twoPi / 4f)
            for (r in 0..8) {
                val radius = 0.90f + r * 0.01f
                val dx = radius * kotlin.math.cos(theta)
                val dy = radius * kotlin.math.sin(theta)
                val px = ((dx + 1f) * 0.5f * size).toInt()
                val py = ((1f - dy) * 0.5f * size).toInt()
                for (ox in -2..2) {
                    for (oy in -2..2) {
                        val x = px + ox
                        val y = py + oy
                        if (x in 0 until size && y in 0 until size) pixels[y * size + x] = marks[m]
                    }
                }
            }
        }
    }

    private fun drawOrientationMarksAtlas(pixels: IntArray, width: Int, height: Int, floorRows: Int) {
        val marks = intArrayOf(
            C.argb(255, 0, 0), C.argb(0, 255, 0), C.argb(0, 128, 255), C.argb(255, 255, 0)
        )
        for (m in 0..3) {
            val x = ((m / 4f) * width).toInt().coerceIn(0, width - 1)
            for (y in 0 until height) {
                if (y < floorRows && (y % 24) < 8) pixels[y * width + x] = marks[m]
                if (y >= floorRows && ((y - floorRows) % 24) < 8) pixels[y * width + x] = marks[m]
            }
        }
    }

    private fun weightSummary(weights: FloatArray, size: Int): String {
        var sl = 0.0
        var sc = 0.0
        var sr = 0.0
        var n = 0
        for (i in 0 until size * size) {
            val l = weights[i * 3]
            val c = weights[i * 3 + 1]
            val r = weights[i * 3 + 2]
            if (l + c + r <= 0f) continue
            sl += l
            sc += c
            sr += r
            n++
        }
        if (n == 0) return "left=0.0 center=0.0 right=0.0"
        return "left=${sl / n} center=${sc / n} right=${sr / n}"
    }
}
