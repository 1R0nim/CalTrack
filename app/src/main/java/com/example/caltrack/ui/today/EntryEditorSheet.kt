package com.example.caltrack.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.data.BarcodeLookupUiState
import com.example.caltrack.data.BarcodeProduct
import com.example.caltrack.data.FrequentFood
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import com.example.caltrack.data.filterSavedFoodsForSearch
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.SavedFood
import com.example.caltrack.ui.components.AmountField
import com.example.caltrack.ui.components.parseOptionalAmount
import com.example.caltrack.ui.formatAmount
import com.example.caltrack.ui.formatRounded
import com.example.caltrack.ui.parseAmount
import com.example.caltrack.ui.theme.CardShape
import com.example.caltrack.ui.theme.FieldShape
import com.example.caltrack.ui.theme.calTrackSurfaceBorder
import com.example.caltrack.ui.theme.calTrackSurfaceOutline
import com.example.caltrack.ui.theme.ListItemShape
import kotlinx.coroutines.launch

private const val MaxSuggestions = 5

/**
 * Add/edit form for one entry. A new entry can be filled from frequent or searched saved foods,
 * then reviewed and explicitly saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorSheet(
    entry: FoodEntry,
    frequentFoods: List<FrequentFood>,
    savedFoods: List<SavedFood>,
    barcodeLookupState: BarcodeLookupUiState,
    onBarcodeScanned: (String) -> Unit,
    onClearBarcodeLookup: () -> Unit,
    onSave: (FoodEntry, BarcodeProduct?) -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = entry.id == 0L
    fun prefill(value: Double) = if (isNew) "" else formatAmount(value)

    var name by rememberSaveable { mutableStateOf(entry.name) }
    var amount by rememberSaveable { mutableStateOf(prefill(entry.amount)) }
    var quantityUnit by remember { mutableStateOf(entry.quantityUnit) }
    var kcal by rememberSaveable { mutableStateOf(prefill(entry.kcal)) }
    var protein by rememberSaveable { mutableStateOf(prefill(entry.protein)) }
    var carbs by rememberSaveable { mutableStateOf(prefill(entry.carbs)) }
    var fat by rememberSaveable { mutableStateOf(prefill(entry.fat)) }
    var pickedFood by remember { mutableStateOf<SavedFood?>(null) }
    var selectedBarcodeProduct by remember { mutableStateOf<BarcodeProduct?>(null) }
    var scannedProductForSave by remember { mutableStateOf<BarcodeProduct?>(null) }
    var scannerUnavailable by rememberSaveable { mutableStateOf(false) }

    fun fillFrom(food: SavedFood, portion: Double) {
        val factor = portion / 100.0
        kcal = formatAmount(food.kcalPer100 * factor)
        protein = formatAmount(food.proteinPer100 * factor)
        carbs = formatAmount(food.carbsPer100 * factor)
        fat = formatAmount(food.fatPer100 * factor)
    }

    fun fillFrom(product: BarcodeProduct, portion: Double) {
        val factor = portion / 100.0
        kcal = product.nutrition.kcalPer100?.let { formatAmount(it * factor) }.orEmpty()
        protein = product.nutrition.proteinPer100?.let { formatAmount(it * factor) }.orEmpty()
        carbs = product.nutrition.carbsPer100?.let { formatAmount(it * factor) }.orEmpty()
        fat = product.nutrition.fatPer100?.let { formatAmount(it * factor) }.orEmpty()
    }

    fun recalculate(amountText: String) {
        val portion = parseAmount(amountText)?.takeIf { it > 0 } ?: return
        pickedFood?.let { fillFrom(it, portion) }
        selectedBarcodeProduct?.let { fillFrom(it, portion) }
    }

    fun pick(food: SavedFood) {
        pickedFood = food
        selectedBarcodeProduct = null
        scannedProductForSave = null
        quantityUnit = food.quantityUnit
        name = food.name
        val portion = parseAmount(amount)?.takeIf { it > 0 }
            ?: food.defaultAmount.also { amount = formatAmount(it) }
        fillFrom(food, portion)
    }

    fun applyProduct(product: BarcodeProduct, unit: FoodQuantityUnit) {
        pickedFood = null
        selectedBarcodeProduct = product
        scannedProductForSave = product
        quantityUnit = unit
        name = product.editorName
        val portion = product.defaultAmount(unit)
        amount = formatAmount(portion)
        fillFrom(product, portion)
    }

    val missingImportedNutrients = scannedProductForSave?.nutrition?.missingNutrients.orEmpty()
    val edited = editedEntry(
        entry = entry,
        name = name,
        amount = amount,
        quantityUnit = quantityUnit,
        kcal = kcal,
        protein = protein,
        carbs = carbs,
        fat = fat,
        requiredNutrients = missingImportedNutrients,
    )

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    // Animates the sheet away before handing back; repeated taps meanwhile are ignored
    fun close(then: () -> Unit) {
        if (closing) return
        closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }

    val save = { if (edited != null) close { onSave(edited, scannedProductForSave) } }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val nameFocus = remember { FocusRequester() }
    val editorScrollState = rememberScrollState()
    val amountUnit = stringResource(
        if (quantityUnit == FoodQuantityUnit.GRAMS) R.string.grams_unit else R.string.milliliters_unit,
    )

    fun closeSearch() {
        searchQuery = ""
        searchOpen = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun selectFood(food: SavedFood) {
        pick(food)
        closeSearch()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.calTrackSurfaceOutline(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        sheetMaxWidth = 560.dp,
    ) {
        if (isNew) {
            LaunchedEffect(Unit) { nameFocus.requestFocus() }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(editorScrollState)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(if (isNew) R.string.add_food else R.string.edit_food),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { close(onDismiss) },
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close_add_food),
                    )
                }
            }

            if (isNew) {
                FrequentFoodsSection(
                    foods = frequentFoods,
                    savedFoods = savedFoods,
                    searchOpen = searchOpen,
                    query = searchQuery,
                    onOpenSearch = { searchOpen = true },
                    onQueryChange = { searchQuery = it },
                    onCloseSearch = ::closeSearch,
                    onSelectFood = ::selectFood,
                    barcodeLookupState = barcodeLookupState,
                    scannerUnavailable = scannerUnavailable,
                    onBarcodeScanned = { barcode ->
                        scannerUnavailable = false
                        searchQuery = ""
                        searchOpen = false
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onBarcodeScanned(barcode)
                    },
                    onScannerError = { scannerUnavailable = true },
                    onUseProduct = { product, unit ->
                        applyProduct(product, unit)
                        if (searchOpen) closeSearch() else {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                    onManualEntry = {
                        onClearBarcodeLookup()
                        scannerUnavailable = false
                        focusManager.clearFocus()
                        nameFocus.requestFocus()
                        keyboardController?.show()
                    },
                    onTryBarcodeAgain = onClearBarcodeLookup,
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NameField(
                    value = name,
                    onValueChange = {
                        name = it
                        pickedFood = null
                    },
                    suggestions = if (isNew) savedFoods else emptyList(),
                    picked = pickedFood,
                    onPick = ::pick,
                    modifier = Modifier.focusRequester(nameFocus),
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        Color.Transparent
                    },
                    border = calTrackSurfaceBorder(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AmountField(
                                value = amount,
                                onValueChange = {
                                    amount = it
                                    recalculate(it)
                                },
                                label = stringResource(R.string.quantity),
                                unit = amountUnit,
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium,
                            )
                            AmountField(
                                value = kcal,
                                onValueChange = {
                                    kcal = it
                                    pickedFood = null
                                    selectedBarcodeProduct = null
                                },
                                label = stringResource(R.string.calories),
                                unit = stringResource(R.string.kcal),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AmountField(
                                value = protein,
                                onValueChange = {
                                    protein = it
                                    pickedFood = null
                                    selectedBarcodeProduct = null
                                },
                                label = stringResource(R.string.protein),
                                unit = stringResource(R.string.grams_unit),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                optional = true,
                            )
                            AmountField(
                                value = carbs,
                                onValueChange = {
                                    carbs = it
                                    pickedFood = null
                                    selectedBarcodeProduct = null
                                },
                                label = stringResource(R.string.carbs),
                                unit = stringResource(R.string.grams_unit),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                optional = true,
                            )
                            AmountField(
                                value = fat,
                                onValueChange = {
                                    fat = it
                                    pickedFood = null
                                    selectedBarcodeProduct = null
                                },
                                label = stringResource(R.string.fat),
                                unit = stringResource(R.string.grams_unit),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                optional = true,
                                imeAction = ImeAction.Done,
                                onDone = save,
                            )
                        }
                        if (missingImportedNutrients.isNotEmpty() && edited == null) {
                            Text(
                                text = stringResource(R.string.barcode_missing_editor_fields),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { close(onDismiss) }, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = save,
                        enabled = edited != null,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.save), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequentFoodsSection(
    foods: List<FrequentFood>,
    savedFoods: List<SavedFood>,
    searchOpen: Boolean,
    query: String,
    onOpenSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onSelectFood: (SavedFood) -> Unit,
    barcodeLookupState: BarcodeLookupUiState,
    scannerUnavailable: Boolean,
    onBarcodeScanned: (String) -> Unit,
    onScannerError: () -> Unit,
    onUseProduct: (BarcodeProduct, FoodQuantityUnit) -> Unit,
    onManualEntry: () -> Unit,
    onTryBarcodeAgain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.frequent_foods),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        AnimatedContent(targetState = searchOpen, label = "frequent-food-search") { searching ->
            if (searching) {
                FoodSearchPanel(
                    savedFoods = savedFoods,
                    query = query,
                    onQueryChange = onQueryChange,
                    onClose = onCloseSearch,
                    onSelect = onSelectFood,
                    onBarcodeScanned = onBarcodeScanned,
                    onScannerError = onScannerError,
                )
            } else {
                FrequentFoodRow(
                    foods = foods,
                    onSearch = onOpenSearch,
                    onSelect = onSelectFood,
                    onBarcodeScanned = onBarcodeScanned,
                    onScannerError = onScannerError,
                )
            }
        }
        BarcodeLookupPanel(
            state = barcodeLookupState,
            scannerUnavailable = scannerUnavailable,
            onUseProduct = onUseProduct,
            onTryAgain = onTryBarcodeAgain,
            onManualEntry = onManualEntry,
        )
    }
}

@Composable
private fun FrequentFoodRow(
    foods: List<FrequentFood>,
    onSearch: () -> Unit,
    onSelect: (SavedFood) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onScannerError: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (foods.isEmpty()) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp),
                shape = ListItemShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = calTrackSurfaceBorder(),
            ) {
                Text(
                    text = stringResource(R.string.frequent_food_empty),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(foods, key = { _, item -> SavedFood.nameKeyOf(item.food.name) }) { index, item ->
                    val cardShape = RoundedCornerShape(
                        topStart = if (index == 0) 14.dp else 4.dp,
                        bottomStart = if (index == 0) 14.dp else 4.dp,
                        topEnd = if (index == foods.lastIndex) 14.dp else 4.dp,
                        bottomEnd = if (index == foods.lastIndex) 14.dp else 4.dp,
                    )
                    FrequentFoodCard(
                        item = item,
                        shape = cardShape,
                        onClick = { onSelect(item.food) },
                    )
                }
            }
        }
        FilledTonalIconButton(
            onClick = onSearch,
            modifier = Modifier.size(48.dp),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = stringResource(R.string.search_foods),
            )
        }
        BarcodeScannerButton(
            onBarcodeScanned = onBarcodeScanned,
            onScannerError = onScannerError,
        )
    }
}

@Composable
private fun FrequentFoodCard(item: FrequentFood, shape: Shape, onClick: () -> Unit) {
    val portion = item.food.toEntry(item.food.defaultAmount)
    Surface(
        modifier = Modifier
            .width(128.dp)
            .heightIn(min = 60.dp)
            .clickable(
                onClickLabel = stringResource(R.string.select_food, item.food.name),
                role = Role.Button,
                onClick = onClick,
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = calTrackSurfaceBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = item.food.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.amount_kcal, formatRounded(portion.kcal)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FoodSearchPanel(
    savedFoods: List<SavedFood>,
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onSelect: (SavedFood) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onScannerError: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val matches = remember(savedFoods, query) { filterSavedFoodsForSearch(savedFoods, query) }
    val container = MaterialTheme.colorScheme.surfaceContainerLow

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .focusRequester(focusRequester)
                    .calTrackSurfaceOutline(FieldShape),
                placeholder = {
                    Text(stringResource(R.string.search_foods), style = MaterialTheme.typography.bodyMedium)
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onQueryChange("") },
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = FieldShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = container,
                    unfocusedContainerColor = container,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )
            BarcodeScannerButton(
                onBarcodeScanned = onBarcodeScanned,
                onScannerError = onScannerError,
            )
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp),
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_search),
                )
            }
        }
        if (matches.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                shape = ListItemShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = calTrackSurfaceBorder(),
            ) {
                Text(
                    text = if (SavedFood.searchKeyOf(query).isEmpty()) {
                        stringResource(R.string.search_foods_empty)
                    } else {
                        stringResource(R.string.meals_no_results, query.trim())
                    },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Surface(
                shape = ListItemShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = calTrackSurfaceBorder(),
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 288.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    matches.forEachIndexed { index, food ->
                        FoodSearchResult(food = food, onClick = { onSelect(food) })
                        if (index != matches.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp, end = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodSearchResult(food: SavedFood, onClick: () -> Unit) {
    val portion = food.toEntry(food.defaultAmount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.select_food, food.name),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProductImage(food.imageUrl, food.name, size = 40.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            food.brand?.takeIf { it.isNotBlank() && !it.equals(food.name, ignoreCase = true) }?.let { brand ->
                Text(
                    text = brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(
                    R.string.meal_portion,
                    formatAmount(food.defaultAmount),
                    food.quantityUnit.code,
                    formatRounded(portion.kcal),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    R.string.meal_macros,
                    formatRounded(portion.protein),
                    formatRounded(portion.carbs),
                    formatRounded(portion.fat),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp),
        )
    }
}

/** Name field that suggests saved foods matching what's typed (ignoring case and diacritics). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<SavedFood>,
    picked: SavedFood?,
    onPick: (SavedFood) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val matches = remember(value, suggestions) {
        val key = SavedFood.searchKeyOf(value)
        if (key.isEmpty()) {
            emptyList()
        } else {
            suggestions.asSequence()
                .filter { SavedFood.searchKeyOf(it.name).contains(key) }
                .take(MaxSuggestions)
                .toList()
        }
    }
    val expanded = focused && !dismissed && picked == null && matches.isNotEmpty()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!it) dismissed = true }) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                dismissed = false
                onValueChange(it)
            },
            modifier = modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            label = { Text(stringResource(R.string.name), style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { dismissed = true }) {
            matches.forEach { food ->
                DropdownMenuItem(
                    text = { Text(food.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = {
                        Text(
                            text = stringResource(R.string.per_100_quantity, food.quantityUnit.code) +
                                " · " + stringResource(R.string.amount_kcal, formatAmount(food.kcalPer100)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { onPick(food) },
                )
            }
        }
    }
}

/** [entry] with the form values applied, or null while a required field is missing or invalid. */
private fun editedEntry(
    entry: FoodEntry,
    name: String,
    amount: String,
    quantityUnit: FoodQuantityUnit,
    kcal: String,
    protein: String,
    carbs: String,
    fat: String,
    requiredNutrients: List<String> = emptyList(),
): FoodEntry? {
    val parsedKcal = parseAmount(kcal) ?: return null
    val parsedProtein = if ("protein" in requiredNutrients) parseAmount(protein) ?: return null
        else parseOptionalAmount(protein) ?: return null
    val parsedCarbs = if ("carbohydrates" in requiredNutrients) parseAmount(carbs) ?: return null
        else parseOptionalAmount(carbs) ?: return null
    val parsedFat = if ("fat" in requiredNutrients) parseAmount(fat) ?: return null
        else parseOptionalAmount(fat) ?: return null
    return entry.copy(
        name = name.trim().ifEmpty { return null },
        amount = parseAmount(amount)?.takeIf { it > 0 } ?: return null,
        quantityUnit = quantityUnit,
        kcal = parsedKcal,
        protein = parsedProtein,
        carbs = parsedCarbs,
        fat = parsedFat,
    )
}
