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
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextCream
import com.prompthavenai.falcibuket.ui.theme.TextMuted

/** Simple birth input payload (no formatted date strings). */
data class PersonInput(
    val name: String,
    val date: String,
    val time: String,
    val place: String
)

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
            time = time1, onTime = { time1 = it },
            place = place1, onPlace = { place1 = it }
        )

        if (twoPeople) {
            Spacer(Modifier.height(20.dp))
            PersonFields(
                header = "İkinci Kişi",
                name = name2, onName = { name2 = it },
                date = date2, onDate = { date2 = it },
                time = time2, onTime = { time2 = it },
                place = place2, onPlace = { place2 = it }
            )
        }

        Spacer(Modifier.height(20.dp))
        GoldButton(
            cta,
            enabled = date1.isNotBlank() && (!twoPeople || date2.isNotBlank()),
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
    time: String,
    onTime: (String) -> Unit,
    place: String,
    onPlace: (String) -> Unit
) {
    Text(header, style = MaterialTheme.typography.titleMedium, color = Gold)
    Spacer(Modifier.height(10.dp))
    BirthField(value = name, onValueChange = onName, label = "İsim (opsiyonel)")
    Spacer(Modifier.height(10.dp))
    BirthField(value = date, onValueChange = onDate, label = "Doğum tarihi (GG.AA.YYYY)")
    Spacer(Modifier.height(10.dp))
    BirthField(value = time, onValueChange = onTime, label = "Doğum saati (opsiyonel)")
    Spacer(Modifier.height(10.dp))
    BirthField(value = place, onValueChange = onPlace, label = "Doğum yeri (opsiyonel)")
}

@Composable
private fun BirthField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = TextMuted) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfacePlum,
            unfocusedContainerColor = SurfacePlum.copy(alpha = 0.6f),
            focusedBorderColor = Gold,
            unfocusedBorderColor = Gold.copy(alpha = 0.3f),
            focusedTextColor = TextCream,
            unfocusedTextColor = TextCream,
            cursorColor = Gold
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
