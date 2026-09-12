package com.prompthavenai.falcibuket.ui.components

import androidx.annotation.RawRes
import com.prompthavenai.falcibuket.R

/**
 * Kahve Falı akışındaki video varlıklarının tek merkezi eşlemesi.
 * Yalnızca gerçekten var olan dosyalar referans edilir.
 */
object CoffeeVideoAssets {
    /** Coffee giriş ekranı: sessiz, döngülü arka plan/hero. */
    @RawRes
    val ENTRY_LOOP: Int = R.raw.coffee_entry_loop

    /** Coffee analiz/yükleniyor ekranı: gerçek istek sürerken sessiz döngü. */
    @RawRes
    val ANALYSIS_LOOP: Int = R.raw.coffee_analysis_loop

    /**
     * İleride eklenecek Coffee sonuç videosu için temiz kanca.
     * Üçüncü varlık sağlanıp incelenene kadar `null` kalır; bu yüzden bugün
     * var olmayan bir kaynağa referans verilmez. (Ambient loop mu, tek
     * seferlik reveal mi olacağı sonra kararlaştırılacak.)
     */
    @RawRes
    val RESULT_VIDEO: Int? = null
}
