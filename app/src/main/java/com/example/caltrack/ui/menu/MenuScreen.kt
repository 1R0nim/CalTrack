package com.example.caltrack.ui.menu

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.data.AppSettings
import com.example.caltrack.data.ThemeMode
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
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
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

/** Connected Expressive button group; icon above the label so all three fit on a 360 dp screen. */
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
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}
