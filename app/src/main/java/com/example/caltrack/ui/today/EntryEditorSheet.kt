package com.example.caltrack.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialExpressiveTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.example.caltrack.ui.components.FoodEditorValues
import com.example.caltrack.ui.components.FoodNutritionEditor
import com.example.caltrack.ui.components.parsedAmount
import com.example.caltrack.ui.components.parsedNutrition
import com.example.caltrack.ui.formatAmount
import com.example.caltrack.ui.formatAmountInput
import com.example.caltrack.ui.formatRounded
import com.example.caltrack.ui.parseAmount
import com.example.caltrack.ui.theme.FieldShape
import com.example.caltrack.ui.theme.calTrackSurfaceBorder
import com.example.caltrack.ui.theme.calTrackSurfaceOutline
import com.example.caltrack.ui.theme.ListItemShape
import kotlinx.coroutines.launch

/**
 * Add/edit form for one entry. A new entry can be filled from frequent or searched saved foods,
 * then reviewed and explicitly saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorSheet(
    entry: FoodEntry,
    isAiPrefilled: Boolean,
    initiallyMissingNutrients: Set<String>,
    frequentFoods: List<FrequentFood>,
    savedFoods: List<SavedFood>,
    barcodeLookupState: BarcodeLookupUiState,
    onBarcodeScanned: (String) -> Unit,
    onClearBarcodeLookup: () -> Unit,
    onSave: (FoodEntry, BarcodeProduct?) -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = entry.id == 0L
    val addFoodColorScheme = MaterialTheme.colorScheme
    val addFoodMotionScheme = MaterialTheme.motionScheme
    val addFoodTypography = MaterialTheme.typography

    fun prefillPer100(value: Double, nutrient: String): String {
        if (isNew && !isAiPrefilled || nutrient in initiallyMissingNutrients) return ""
        val per100 = if (entry.amount > 0.0) value * 100.0 / entry.amount else value
        return formatAmountInput(per100)
    }

    var name by rememberSaveable(entry.id, isAiPrefilled) { mutableStateOf(entry.name) }
    var amount by rememberSaveable(entry.id, isAiPrefilled) {
        mutableStateOf(if (isNew && !isAiPrefilled) "" else formatAmountInput(entry.amount))
    }
    var quantityUnit by rememberSaveable(entry.id, isAiPrefilled) { mutableStateOf(entry.quantityUnit) }
    var kcal by rememberSaveable(entry.id, isAiPrefilled, initiallyMissingNutrients) { mutableStateOf(prefillPer100(entry.kcal, "kcal")) }
    var protein by rememberSaveable(entry.id, isAiPrefilled, initiallyMissingNutrients) { mutableStateOf(prefillPer100(entry.protein, "protein")) }
    var carbs by rememberSaveable(entry.id, isAiPrefilled, initiallyMissingNutrients) { mutableStateOf(prefillPer100(entry.carbs, "carbohydrates")) }
    var fat by rememberSaveable(entry.id, isAiPrefilled, initiallyMissingNutrients) { mutableStateOf(prefillPer100(entry.fat, "fat")) }
    var pickedFood by remember { mutableStateOf<SavedFood?>(null) }
    var scannedProductForSave by remember { mutableStateOf<BarcodeProduct?>(null) }
    var scannerUnavailable by rememberSaveable { mutableStateOf(false) }

    fun pick(food: SavedFood) {
        pickedFood = food
        scannedProductForSave = null
        quantityUnit = food.quantityUnit
        name = food.name
        if (parseAmount(amount)?.takeIf { it > 0.0 } == null) {
            amount = formatAmountInput(food.defaultAmount)
        }
        kcal = formatAmountInput(food.kcalPer100)
        protein = formatAmountInput(food.proteinPer100)
        carbs = formatAmountInput(food.carbsPer100)
        fat = formatAmountInput(food.fatPer100)
    }

    fun applyProduct(product: BarcodeProduct, unit: FoodQuantityUnit) {
        pickedFood = null
        scannedProductForSave = product
        quantityUnit = unit
        name = product.editorName
        amount = formatAmountInput(product.defaultAmount(unit))
        kcal = product.nutrition.kcalPer100?.let(::formatAmountInput).orEmpty()
        protein = product.nutrition.proteinPer100?.let(::formatAmountInput).orEmpty()
        carbs = product.nutrition.carbsPer100?.let(::formatAmountInput).orEmpty()
        fat = product.nutrition.fatPer100?.let(::formatAmountInput).orEmpty()
    }

    val editorValues = FoodEditorValues(
        name = name,
        amount = amount,
        quantityUnit = quantityUnit,
        kcalPer100 = kcal,
        proteinPer100 = protein,
        carbsPer100 = carbs,
        fatPer100 = fat,
    )
    val missingImportedNutrients = scannedProductForSave?.nutrition?.missingNutrients.orEmpty() + initiallyMissingNutrients
    val edited = editedEntry(entry, editorValues, missingImportedNutrients.toSet())

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

    MaterialExpressiveTheme(
        colorScheme = addFoodColorScheme,
        motionScheme = addFoodMotionScheme,
        typography = addFoodTypography,
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            sheetMaxWidth = 560.dp,
        ) {
        // Like Mese, the keyboard only opens once a field is tapped
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(editorScrollState)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(if (isNew) R.string.add_food else R.string.edit_food),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
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

            FoodNutritionEditor(
                values = editorValues,
                onValuesChange = { updated ->
                    if (updated.name != name) pickedFood = null
                    name = updated.name
                    amount = updated.amount
                    quantityUnit = updated.quantityUnit
                    kcal = updated.kcalPer100
                    protein = updated.proteinPer100
                    carbs = updated.carbsPer100
                    fat = updated.fatPer100
                },
                saveEnabled = edited != null,
                onSave = save,
                onCancel = { close(onDismiss) },
                modifier = Modifier.padding(horizontal = 20.dp),
                requiredNutrients = missingImportedNutrients.toSet(),
                suggestions = if (isNew) savedFoods else emptyList(),
                pickedSuggestion = pickedFood,
                onPickSuggestion = ::pick,
                nameModifier = Modifier.focusRequester(nameFocus),
            )
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
            .padding(start = 20.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedContent(targetState = searchOpen, label = "frequent-food-search") { searching ->
            if (searching) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.frequent_foods),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FoodSearchPanel(
                        savedFoods = savedFoods,
                        query = query,
                        onQueryChange = onQueryChange,
                        onClose = onCloseSearch,
                        onSelect = onSelectFood,
                        onBarcodeScanned = onBarcodeScanned,
                        onScannerError = onScannerError,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.frequent_foods),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FrequentFoodRow(
                        foods = foods,
                        onSearch = onOpenSearch,
                        onSelect = onSelectFood,
                        onBarcodeScanned = onBarcodeScanned,
                        onScannerError = onScannerError,
                    )
                }
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (foods.isEmpty()) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 76.dp),
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
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 76.dp)
                    .clipToBounds(),
                contentPadding = PaddingValues(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun FrequentFoodCard(item: FrequentFood, shape: Shape, onClick: () -> Unit) {
    val portion = item.food.toEntry(item.food.defaultAmount)
    Surface(
        modifier = Modifier
            .width(140.dp)
            .heightIn(min = 76.dp)
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.food.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.meal_portion,
                    formatAmount(item.food.defaultAmount),
                    item.food.quantityUnit.code,
                    formatRounded(portion.kcal),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.occurrenceCount?.let { count ->
                Text(
                    text = pluralStringResource(R.plurals.frequent_food_uses, count, count),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
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
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                matches.forEach { food ->
                    FoodSearchResult(food = food, onClick = { onSelect(food) })
                }
            }
        }
    }
}

@Composable
private fun FoodSearchResult(food: SavedFood, onClick: () -> Unit) {
    val portion = food.toEntry(food.defaultAmount)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.select_food, food.name),
                role = Role.Button,
                onClick = onClick,
            ),
        shape = ListItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = calTrackSurfaceBorder(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProductImage(food.imageUrl, food.name, size = 44.dp)
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
                    .padding(end = 4.dp)
                    .size(24.dp),
            )
        }
    }
}

/** [entry] with base values scaled to the entered journal quantity. */
private fun editedEntry(
    entry: FoodEntry,
    values: FoodEditorValues,
    requiredNutrients: Set<String>,
): FoodEntry? {
    val amount = values.parsedAmount() ?: return null
    val totals = values.parsedNutrition(requiredNutrients)?.forQuantity(amount) ?: return null
    return entry.copy(
        name = values.name.trim().ifEmpty { return null },
        amount = amount,
        quantityUnit = values.quantityUnit,
        kcal = totals.kcal,
        protein = totals.protein,
        carbs = totals.carbs,
        fat = totals.fat,
    )
}
