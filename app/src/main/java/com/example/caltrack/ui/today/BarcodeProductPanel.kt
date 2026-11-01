package com.example.caltrack.ui.today

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.example.caltrack.R
import com.example.caltrack.data.BarcodeLookupFailure
import com.example.caltrack.data.BarcodeLookupUiState
import com.example.caltrack.data.BarcodeProduct
import com.example.caltrack.data.FoodDataSource
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import com.example.caltrack.ui.theme.calTrackSurfaceBorder
import com.example.caltrack.ui.formatAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
internal fun BarcodeScannerButton(
    onBarcodeScanned: (String) -> Unit,
    onScannerError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    val options = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .enableAutoZoom()
            .build()
    }

    androidx.compose.material3.FilledTonalIconButton(
        onClick = {
            if (scanning) return@FilledTonalIconButton
            scanning = true
            try {
                GmsBarcodeScanning.getClient(context, options).startScan()
                    .addOnSuccessListener { barcode ->
                        scanning = false
                        barcode.rawValue?.let(onBarcodeScanned) ?: onScannerError()
                    }
                    .addOnCanceledListener { scanning = false }
                    .addOnFailureListener {
                        scanning = false
                        onScannerError()
                    }
            } catch (_: Exception) {
                scanning = false
                onScannerError()
            }
        },
        modifier = modifier.size(48.dp),
        shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_barcode),
            contentDescription = stringResource(R.string.scan_barcode),
        )
    }
}

@Composable
internal fun BarcodeLookupPanel(
    state: BarcodeLookupUiState,
    scannerUnavailable: Boolean,
    onUseProduct: (BarcodeProduct, FoodQuantityUnit) -> Unit,
    onTryAgain: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = scannerUnavailable || state !is BarcodeLookupUiState.Idle,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (scannerUnavailable) {
            Text(
                text = stringResource(R.string.barcode_scanner_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        when (state) {
            BarcodeLookupUiState.Idle -> Unit
            BarcodeLookupUiState.Loading -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.large,
                border = calTrackSurfaceBorder(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.barcode_lookup_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is BarcodeLookupUiState.Found -> ProductPreview(
                product = state.product,
                onUseProduct = onUseProduct,
            )
            is BarcodeLookupUiState.NotFound -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.large,
                border = calTrackSurfaceBorder(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.barcode_product_not_found), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = state.barcode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onTryAgain, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.barcode_scan_again))
                        }
                        Button(onClick = onManualEntry, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.barcode_manual_entry))
                        }
                    }
                }
            }
            is BarcodeLookupUiState.Failed -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.large,
                border = calTrackSurfaceBorder(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(state.reason.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onTryAgain,
                            enabled = state.reason != BarcodeLookupFailure.INVALID_BARCODE,
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(R.string.barcode_scan_again))
                        }
                        Button(onClick = onManualEntry, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.barcode_manual_entry))
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ProductPreview(
    product: BarcodeProduct,
    onUseProduct: (BarcodeProduct, FoodQuantityUnit) -> Unit,
) {
    val detectedUnit = product.nutrition.quantityUnit ?: product.quantityUnitHint
    var selectedUnit by remember(product.barcode) { mutableStateOf(detectedUnit) }
    val ready = selectedUnit != null
    val hasMissing = product.nutrition.missingNutrients.isNotEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
                border = calTrackSurfaceBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductImage(product.imageUrl, product.editorName.ifBlank { product.barcode })
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = product.editorName.ifBlank { stringResource(R.string.barcode_product_name_unavailable) },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    product.brand?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = stringResource(R.string.barcode_value, product.barcode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            product.productQuantity?.let {
                Text(stringResource(R.string.barcode_package_quantity, it), style = MaterialTheme.typography.bodySmall)
            }
            product.servingSize?.let {
                Text(stringResource(R.string.barcode_serving_size, it), style = MaterialTheme.typography.bodySmall)
            }
            if (product.nutrition.quantityUnit == null) {
                Text(
                    text = stringResource(R.string.barcode_choose_unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedUnit == FoodQuantityUnit.GRAMS,
                        onClick = { selectedUnit = FoodQuantityUnit.GRAMS },
                        label = { Text(stringResource(R.string.grams_unit)) },
                    )
                    FilterChip(
                        selected = selectedUnit == FoodQuantityUnit.MILLILITERS,
                        onClick = { selectedUnit = FoodQuantityUnit.MILLILITERS },
                        label = { Text(stringResource(R.string.milliliters_unit)) },
                    )
                }
            }
            selectedUnit?.let { unit ->
                Text(
                    text = stringResource(R.string.barcode_nutrition_basis, unit.code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                NutrientLine(stringResource(R.string.calories), product.nutrition.kcalPer100?.let(::formatAmount), stringResource(R.string.kcal))
                NutrientLine(stringResource(R.string.protein), product.nutrition.proteinPer100?.let(::formatAmount), stringResource(R.string.grams_unit))
                NutrientLine(stringResource(R.string.carbs), product.nutrition.carbsPer100?.let(::formatAmount), stringResource(R.string.grams_unit))
                NutrientLine(stringResource(R.string.fat), product.nutrition.fatPer100?.let(::formatAmount), stringResource(R.string.grams_unit))
            }
            if (hasMissing) {
                Text(
                    text = stringResource(R.string.barcode_incomplete_nutrition),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = stringResource(
                    if (product.dataSource == FoodDataSource.OPEN_FOOD_FACTS) {
                        R.string.barcode_source_open_food_facts
                    } else {
                        R.string.barcode_source_edited
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.barcode_community_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { selectedUnit?.let { onUseProduct(product, it) } },
                enabled = ready,
                modifier = Modifier.fillMaxWidth(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.barcode_use_product))
            }
        }
    }
}

@Composable
private fun NutrientLine(label: String, amount: String?, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = amount?.let { stringResource(R.string.barcode_nutrient_amount, it, unit) }
                ?: stringResource(R.string.barcode_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = if (amount == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun ProductImage(url: String?, description: String, size: Dp = 64.dp) {
    val imageShape = RoundedCornerShape(if (size <= 40.dp) 8.dp else 16.dp)
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = url) {
        value = if (url == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 4_000
                }
                try {
                    connection.inputStream.use(BitmapFactory::decodeStream)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = description,
            modifier = Modifier.size(size).clip(imageShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(size).clip(imageShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_restaurant),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun BarcodeLookupFailure.messageRes(): Int = when (this) {
    BarcodeLookupFailure.INVALID_BARCODE -> R.string.barcode_invalid
    BarcodeLookupFailure.NETWORK_UNAVAILABLE -> R.string.barcode_network_error
    BarcodeLookupFailure.TIMEOUT -> R.string.barcode_timeout
    BarcodeLookupFailure.RATE_LIMITED -> R.string.barcode_rate_limited
    BarcodeLookupFailure.SERVER_UNAVAILABLE -> R.string.barcode_service_unavailable
    BarcodeLookupFailure.UNKNOWN -> R.string.barcode_lookup_error
}
