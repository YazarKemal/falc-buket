package com.prompthavenai.falcibuket.ui.screens.flows

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.ReadingFlowScaffold
import com.prompthavenai.falcibuket.ui.components.ReadingPreviewNotice
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.Rose
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextCream
import com.prompthavenai.falcibuket.ui.theme.TextMuted
import java.time.LocalDate

/** Simple birth input payload (no formatted date strings). */
data class PersonInput(
    val name: String,
    val date: String,
    val time: String,
    val place: String
)

private val datePattern = Regex("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})$")

/**
 * Validates a GG.AA.YYYY birth date. Returns a user-facing Turkish error
 * message, or null when the date is a valid past/present calendar date.
 */
fun validateBirthDate(input: String): String? {
    val value = input.trim()
    if (value.isEmpty()) return "Doğum tarihi gerekli"
    val match = datePattern.matchEntire(value) ?: return "Tarihi GG.AA.YYYY biçiminde gir"
    val day = match.groupValues[1].toInt()
    val month = match.groupValues[2].toInt()
    val year = match.groupValues[3].toInt()
    val today = LocalDate.now()
    if (year < 1900 || year > today.year) return "Yıl 1900 ile ${today.year} arasında olmalı"
    val date = runCatching { LocalDate.of(year, month, day) }.getOrNull()
        ?: return "Geçersiz takvim tarihi"
    if (date.isAfter(today)) return "Doğum tarihi gelecekte olamaz"
    return null
}

/** Reusable birth-data flow. Used by BIRTH_DATA and TWO_PERSON_DATA types. */
@Composable
fun GenericBirthDataScreen(
    title: String,
    subtitle: String,
    twoPeople: Boolean,
    cta: String,
    @DrawableRes artworkRes: Int?,
    onBack: () -> Unit,
    onSubmit: (PersonInput, PersonInput?) -> Unit
) {
    var name1 by rememberSaveable { mutableStateOf("") }
    var date1 by rememberSaveable { mutableStateOf("") }
    var time1 by rememberSaveable { mutableStateOf("") }
    var place1 by rememberSaveable { mutableStateOf("") }

    var name2 by rememberSaveable { mutableStateOf("") }
    var date2 by rememberSaveable { mutableStateOf("") }
    var time2 by rememberSaveable { mutableStateOf("") }
    var place2 by rememberSaveable { mutableStateOf("") }

    val dateError1 = if (date1.isBlank()) null else validateBirthDate(date1)
    val dateError2 = if (twoPeople && date2.isNotBlank()) validateBirthDate(date2) else null
    val firstValid = date1.isNotBlank() && validateBirthDate(date1) == null
    val secondValid = !twoPeople || (date2.isNotBlank() && validateBirthDate(date2) == null)

    ReadingFlowScaffold(title = title, subtitle = subtitle, onBack = onBack) {
        if (artworkRes != null) {
            Image(
                painterResource(artworkRes), null,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(20.dp))
        }

        PersonFields(
            header = if (twoPeople) "Birinci Kişi" else "Doğum Bilgileri",
            name = name1, onName = { name1 = it },
            date = date1, onDate = { date1 = it },
            dateError = dateError1,
            time = time1, onTime = { time1 = it },
            place = place1, onPlace = { place1 = it }
        )

        if (twoPeople) {
            Spacer(Modifier.height(20.dp))
            PersonFields(
                header = "İkinci Kişi",
                name = name2, onName = { name2 = it },
                date = date2, onDate = { date2 = it },
                dateError = dateError2,
                time = time2, onTime = { time2 = it },
                place = place2, onPlace = { place2 = it }
            )
        }

        Spacer(Modifier.height(20.dp))
        GoldButton(
            cta,
            enabled = firstValid && secondValid,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSubmit(
                    PersonInput(name1.trim(), date1.trim(), time1.trim(), place1.trim()),
                    if (twoPeople) PersonInput(name2.trim(), date2.trim(), time2.trim(), place2.trim()) else null
                )
            }
        )
        Spacer(Modifier.height(12.dp))
        ReadingPreviewNotice()
    }
}

@Composable
private fun PersonFields(
    header: String,
    name: String,
    onName: (String) -> Unit,
    date: String,
    onDate: (String) -> Unit,
    dateError: String?,
    time: String,
    onTime: (String) -> Unit,
    place: String,
    onPlace: (String) -> Unit
) {
    Text(header, style = MaterialTheme.typography.titleMedium, color = Gold)
    Spacer(Modifier.height(10.dp))
    BirthField(value = name, onValueChange = onName, label = "İsim (opsiyonel)")
    Spacer(Modifier.height(10.dp))
    BirthField(value = date, onValueChange = onDate, label = "Doğum tarihi (GG.AA.YYYY)", error = dateError)
    Spacer(Modifier.height(10.dp))
    BirthField(value = time, onValueChange = onTime, label = "Doğum saati (opsiyonel)")
    Spacer(Modifier.height(10.dp))
    BirthField(value = place, onValueChange = onPlace, label = "Doğum yeri (opsiyonel)")
}

@Composable
private fun BirthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = TextMuted) },
        singleLine = true,
        isError = error != null,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfacePlum,
            unfocusedContainerColor = SurfacePlum.copy(alpha = 0.6f),
            focusedBorderColor = Gold,
            unfocusedBorderColor = Gold.copy(alpha = 0.3f),
            errorBorderColor = Rose,
            focusedTextColor = TextCream,
            unfocusedTextColor = TextCream,
            cursorColor = Gold
        ),
        modifier = Modifier.fillMaxWidth()
    )
    if (error != null) {
        Spacer(Modifier.height(4.dp))
        Text(error, style = MaterialTheme.typography.labelMedium, color = Rose)
    }
}
