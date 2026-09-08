package com.kyant.glassxposed.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var settings by remember { mutableStateOf(SettingsRepository.load(context)) }

    fun update(block: (SettingsRepository.Settings) -> SettingsRepository.Settings) {
        settings = block(settings)
        SettingsRepository.save(context, settings)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Liquid Glass — SystemUI targets", style = MaterialTheme.typography.titleMedium)

        ToggleRow("Status bar", settings.enabledStatusBar) { v -> update { it.copy(enabledStatusBar = v) } }
        ToggleRow("Notification shade", settings.enabledShade) { v -> update { it.copy(enabledShade = v) } }
        ToggleRow("Navigation bar", settings.enabledNavBar) { v -> update { it.copy(enabledNavBar = v) } }

        Text("Effect parameters", style = MaterialTheme.typography.titleMedium)

        SliderRow("Blur radius", settings.blurRadius, 0f, 60f) { v -> update { it.copy(blurRadius = v) } }
        SliderRow("Refraction height", settings.refractionHeight, 1f, 80f) { v -> update { it.copy(refractionHeight = v) } }
        SliderRow("Refraction amount", settings.refractionAmount, 0f, 60f) { v -> update { it.copy(refractionAmount = v) } }
        SliderRow("Depth effect", settings.depthEffect, 0f, 1f) { v -> update { it.copy(depthEffect = v) } }

        ToggleRow("Chromatic aberration (dispersion)", settings.useDispersion) { v -> update { it.copy(useDispersion = v) } }
        if (settings.useDispersion) {
            SliderRow("Chromatic aberration amount", settings.chromaticAberration, 0f, 20f) { v ->
                update { it.copy(chromaticAberration = v) }
            }
        }

        Text(
            "Changes apply the next time SystemUI re-lays-out the hooked views " +
                "(usually near-instant). If nothing changes, restart SystemUI from " +
                "LSPosed Manager's module page, or reboot.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${"%.1f".format(value)}")
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}
