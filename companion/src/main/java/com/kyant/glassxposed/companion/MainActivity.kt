package com.kyant.glassxposed.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SystemUI targets", style = MaterialTheme.typography.titleMedium)
        ToggleRow("Status bar", settings.enabledStatusBar) { v -> update { it.copy(enabledStatusBar = v) } }
        ToggleRow("Notification shade", settings.enabledShade) { v -> update { it.copy(enabledShade = v) } }
        ToggleRow("Quick settings panel", settings.enabledQsPanel) { v -> update { it.copy(enabledQsPanel = v) } }
        ToggleRow("Navigation bar", settings.enabledNavBar) { v -> update { it.copy(enabledNavBar = v) } }
        ToggleRow("Lock screen", settings.enabledLockscreen) { v -> update { it.copy(enabledLockscreen = v) } }
        ToggleRow("Volume dialog", settings.enabledVolumeDialog) { v -> update { it.copy(enabledVolumeDialog = v) } }

        HorizontalDivider()
        Text("Blur + refraction (from AndroidLiquidGlass)", style = MaterialTheme.typography.titleMedium)
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

        HorizontalDivider()
        Text("Grain + tint (from Haze)", style = MaterialTheme.typography.titleMedium)
        ToggleRow("Enable grain + tint", settings.useNoiseTint) { v -> update { it.copy(useNoiseTint = v) } }
        if (settings.useNoiseTint) {
            SliderRow("Grain amount", settings.noiseAlpha, 0f, 0.3f) { v -> update { it.copy(noiseAlpha = v) } }
            SliderRow("Tint opacity", settings.tintAlpha, 0f, 255f) { v -> update { it.copy(tintAlpha = v) } }
        }

        HorizontalDivider()
        Text("Other apps — experimental, whole-window blur only", style = MaterialTheme.typography.titleMedium)
        Text(
            "This blurs a listed app's ENTIRE screen (text included) — there's no way " +
                "to target just a toolbar/nav element in an app we don't control the " +
                "source of. Only useful for specific cases, not general \"make this app glassy\". " +
                "Comma-separated package names, e.g. com.android.camera2,com.google.android.apps.photos",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = settings.otherAppsPackages,
            onValueChange = { v -> update { it.copy(otherAppsPackages = v) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default,
            label = { Text("Package names") }
        )
        SliderRow("Blur radius for these apps", settings.otherAppsBlurRadius, 0f, 60f) { v ->
            update { it.copy(otherAppsBlurRadius = v) }
        }
        Text(
            "Remember: LSPosed only hooks apps you've also added to this module's " +
                "scope in LSPosed Manager. Listing a package here alone does nothing.",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()
        Text(
            "Changes apply the next time the hooked view re-lays-out (usually near-instant " +
                "for SystemUI, next onResume for other apps). If nothing changes, restart " +
                "SystemUI from LSPosed Manager's module page, or reboot.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${"%.2f".format(value)}")
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}
