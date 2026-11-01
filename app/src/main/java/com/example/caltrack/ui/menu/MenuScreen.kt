package com.example.caltrack.ui.menu

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.data.AppSettings
import com.example.caltrack.data.ThemeMode
import com.example.caltrack.ui.AiConnectionState
import com.example.caltrack.ui.components.ScreenTitle
import com.example.caltrack.ui.theme.CalTrackTheme
import com.example.caltrack.ui.theme.DynamicColorSupported
import com.example.caltrack.ui.theme.SmallCardShape
import com.example.caltrack.ui.theme.calTrackSurfaceBorder

/** [contentPadding] already includes the space the floating bar covers. */
@Composable
fun MenuScreen(
    settings: AppSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    aiConnectionState: AiConnectionState,
    onSaveGeminiApiKey: (String) -> Unit,
    onRemoveGeminiApiKey: () -> Unit,
    onTestGeminiConnection: () -> Unit,
    contentPadding: PaddingValues,
    aiConnectionDetail: String? = null,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    var geminiApiKey by remember { mutableStateOf("") }
    var showGeminiApiKey by remember { mutableStateOf(false) }
    val version = remember(context) { context.versionName() }
    val launcherIcon = remember(context) {
        val drawable = requireNotNull(context.getDrawable(R.mipmap.ic_launcher_round))
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(Canvas(bitmap))
        BitmapPainter(bitmap.asImageBitmap())
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding(),
            ),
    ) {
        ScreenTitle(stringResource(R.string.section_menu))

        SectionHeader(R.string.appearance)
        SettingsCard {
            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            ThemeModeGroup(selected = settings.themeMode, onSelect = onThemeModeChange)
        }
        Spacer(Modifier.height(4.dp))
        SettingsCard {
            DynamicColorRow(enabled = settings.dynamicColor, onChange = onDynamicColorChange)
        }

        SectionHeader(R.string.ai_assistant)
        SettingsCard {
            Text(stringResource(R.string.ai_provider), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.ai_provider_gemini),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.ai_byok_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = geminiApiKey,
                onValueChange = { geminiApiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.gemini_api_key)) },
                singleLine = true,
                visualTransformation = if (showGeminiApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showGeminiApiKey = !showGeminiApiKey }) {
                        Text(stringResource(if (showGeminiApiKey) R.string.hide_api_key else R.string.show_api_key))
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (geminiApiKey.isNotBlank()) {
                            onSaveGeminiApiKey(geminiApiKey)
                            geminiApiKey = ""
                        }
                    },
                    enabled = geminiApiKey.isNotBlank(),
                ) { Text(stringResource(R.string.save_api_key)) }
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
                    },
                ) { Text(stringResource(R.string.get_gemini_key)) }
            }
            if (settings.hasGeminiApiKey) {
                TextButton(onClick = onTestGeminiConnection, enabled = aiConnectionState != AiConnectionState.TESTING) {
                    Text(stringResource(if (aiConnectionState == AiConnectionState.TESTING) R.string.testing_connection else R.string.test_connection))
                }
                Text(
                    stringResource(aiConnectionState.statusLabel(settings.hasGeminiApiKey)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (aiConnectionState) {
                        AiConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                        AiConnectionState.INVALID_KEY, AiConnectionState.QUOTA, AiConnectionState.RATE_LIMITED, AiConnectionState.UNAVAILABLE, AiConnectionState.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (aiConnectionState != AiConnectionState.TESTING) {
                    aiConnectionDetail?.let { detail ->
                        Text(
                            stringResource(R.string.ai_error_detail, detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onRemoveGeminiApiKey) {
                    Text(stringResource(R.string.remove_api_key), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.ai_grounding_cost_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.ai_byok_security_limitation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeader(R.string.about)
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = launcherIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.version, version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun AiConnectionState.statusLabel(hasKey: Boolean): Int = when (this) {
    AiConnectionState.NOT_TESTED -> if (hasKey) R.string.ai_connection_not_tested else R.string.ai_key_missing
    AiConnectionState.TESTING -> R.string.testing_connection
    AiConnectionState.CONNECTED -> R.string.ai_connection_connected
    AiConnectionState.INVALID_KEY -> R.string.ai_connection_invalid_key
    AiConnectionState.QUOTA -> R.string.ai_connection_quota
    AiConnectionState.RATE_LIMITED -> R.string.ai_connection_rate_limited
    AiConnectionState.UNAVAILABLE -> R.string.ai_error_unavailable
    AiConnectionState.FAILED -> R.string.ai_connection_failed
}

@Composable
private fun SectionHeader(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = SmallCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = calTrackSurfaceBorder(),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

/** Connected expressive button group for System, Light and Dark preferences. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeModeGroup(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val modes = ThemeMode.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        modes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = mode == selected,
                onCheckedChange = { onSelect(mode) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 72.dp)
                    .semantics { role = Role.RadioButton },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(mode.icon), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(mode.label),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicColorRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val checked = enabled && DynamicColorSupported
    Row(
        modifier = Modifier.toggleable(
            value = checked,
            enabled = DynamicColorSupported,
            role = Role.Switch,
            onValueChange = onChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.dynamic_color), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    when {
                        !DynamicColorSupported -> R.string.dynamic_color_unsupported
                        checked -> R.string.dynamic_color_on
                        else -> R.string.dynamic_color_off
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            // The whole row toggles
            onCheckedChange = null,
            enabled = DynamicColorSupported,
            thumbContent = if (checked) {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            },
        )
    }
}

@get:StringRes
private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    }

@get:DrawableRes
private val ThemeMode.icon: Int
    get() = when (this) {
        ThemeMode.System -> R.drawable.ic_contrast
        ThemeMode.Light -> R.drawable.ic_light_mode
        ThemeMode.Dark -> R.drawable.ic_dark_mode
    }

private fun Context.versionName(): String {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    return info.versionName.orEmpty()
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, widthDp = 360)
@Preview(name = "Light", widthDp = 360)
@Composable
private fun MenuScreenPreview() {
    CalTrackTheme {
        Surface {
            MenuScreen(
                settings = AppSettings(),
                onThemeModeChange = {},
                onDynamicColorChange = {},
                aiConnectionState = AiConnectionState.NOT_TESTED,
                onSaveGeminiApiKey = {},
                onRemoveGeminiApiKey = {},
                onTestGeminiConnection = {},
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}
