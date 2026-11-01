package com.example.caltrack.ui.meals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caltrack.R
import com.example.caltrack.data.BarcodeLookupUiState
import com.example.caltrack.data.BarcodeProduct
import com.example.caltrack.data.FoodDataSource
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import com.example.caltrack.data.local.entity.SavedFood
import com.example.caltrack.ui.components.FoodAvatar
import com.example.caltrack.ui.today.TodayCardArtwork
import com.example.caltrack.ui.today.BarcodeLookupPanel
import com.example.caltrack.ui.today.BarcodeScannerButton
import com.example.caltrack.ui.today.ProductImage
import com.example.caltrack.ui.components.SwipeToDeleteBox
import com.example.caltrack.ui.formatAmount
import com.example.caltrack.ui.formatRounded
import com.example.caltrack.ui.theme.CalTrackTheme
import com.example.caltrack.ui.theme.ListItemShape
import com.example.caltrack.ui.theme.SmallCardShape
import com.example.caltrack.ui.theme.calTrackSurfaceOutline
import com.example.caltrack.ui.theme.calTrackSurfaceBorder
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * [selectedDate] is the day shown on Today; quick-add logs existing library foods on that date.
 * Barcode lookup state is shared with the existing Today scanner and Open Food Facts flow.
 */
@Composable
fun MealsRoute(
    viewModel: MealsViewModel,
    selectedDate: () -> LocalDate,
    barcodeLookupState: BarcodeLookupUiState,
    onBarcodeScanned: (String) -> Unit,
    onClearBarcodeLookup: () -> Unit,
    contentPadding: PaddingValues,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) { viewModel.setQuery(query) }
    var sheetFoodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorFood by remember { mutableStateOf<SavedFood?>(null) }
    var requiredNutrients by remember { mutableStateOf(emptySet<String>()) }
    var scannerUnavailable by rememberSaveable { mutableStateOf(false) }
    var restoredId by remember { mutableStateOf<Long?>(null) }

    fun add(food: SavedFood, amount: Double) {
        val added = viewModel.add(food, amount, selectedDate())
        scope.launch {
            val undo = onShowSnackbar(resources.getString(R.string.meal_added), resources.getString(R.string.undo))
            if (undo) viewModel.undoAdd(added)
        }
    }

    fun deleteWithUndo(food: SavedFood) {
        viewModel.delete(food)
        scope.launch {
            val undo = onShowSnackbar(
                resources.getString(R.string.entry_deleted, food.name),
                resources.getString(R.string.undo),
            )
            if (undo) {
                restoredId = food.id
                viewModel.restore(food)
            }
        }
    }

    fun startManualCreation() {
        onClearBarcodeLookup()
        scannerUnavailable = false
        requiredNutrients = emptySet()
        editorFood = blankFoodDraft()
    }

    fun startProductReview(product: BarcodeProduct, unit: FoodQuantityUnit) {
        val existing = state.allFoods.firstOrNull { it.barcode == product.barcode }
        requiredNutrients = if (existing == null) product.nutrition.missingNutrients.toSet() else emptySet()
        editorFood = existing ?: product.toSavedFood(unit)
        onClearBarcodeLookup()
    }

    MealsScreen(
        state = state,
        query = query,
        onQueryChange = { query = it },
        contentPadding = contentPadding,
        onAdd = { food -> add(food, food.defaultAmount) },
        onOpen = { food -> sheetFoodId = food.id },
        onDelete = ::deleteWithUndo,
        restoredId = restoredId,
        onRestoredShown = { restoredId = null },
        onAddManualFood = ::startManualCreation,
        barcodeLookupState = barcodeLookupState,
        scannerUnavailable = scannerUnavailable,
        onBarcodeScanned = {
            scannerUnavailable = false
            onBarcodeScanned(it)
        },
        onScannerError = { scannerUnavailable = true },
        onUseProduct = ::startProductReview,
        onTryBarcodeAgain = {
            scannerUnavailable = false
            onClearBarcodeLookup()
        },
        onManualEntry = ::startManualCreation,
    )

    val sheetFood = sheetFoodId?.let { id -> state.allFoods.firstOrNull { it.id == id } }
    if (sheetFood != null) {
        MealSheet(
            food = sheetFood,
            onAdd = { amount ->
                sheetFoodId = null
                add(sheetFood, amount)
            },
            onUpdate = viewModel::update,
            onDelete = {
                sheetFoodId = null
                deleteWithUndo(sheetFood)
            },
            onDismiss = { sheetFoodId = null },
        )
    }

    editorFood?.let { food ->
        AddSavedFoodSheet(
            food = food,
            requiredNutrients = requiredNutrients,
            onSave = { savedFood ->
                if (savedFood.id == 0L) viewModel.create(savedFood) else viewModel.update(savedFood)
            },
            onSaved = {
                scope.launch { onShowSnackbar(resources.getString(R.string.food_saved), null) }
            },
            onDismiss = { editorFood = null },
        )
    }
}

@Composable
fun MealsScreen(
    state: MealsUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    contentPadding: PaddingValues,
    onAdd: (SavedFood) -> Unit,
    onOpen: (SavedFood) -> Unit,
    onDelete: (SavedFood) -> Unit,
    restoredId: Long?,
    onRestoredShown: () -> Unit,
    onAddManualFood: () -> Unit = {},
    barcodeLookupState: BarcodeLookupUiState = BarcodeLookupUiState.Idle,
    scannerUnavailable: Boolean = false,
    onBarcodeScanned: (String) -> Unit = {},
    onScannerError: () -> Unit = {},
    onUseProduct: (BarcodeProduct, FoodQuantityUnit) -> Unit = { _, _ -> },
    onTryBarcodeAgain: () -> Unit = {},
    onManualEntry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val motionScheme = MaterialTheme.motionScheme
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val horizontalInset = if (maxWidth >= 600.dp) 28.dp else 16.dp
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + horizontalInset,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + horizontalInset,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "meals-header") { MealsHeader() }
            item(key = "meals-search") {
                SearchField(query = query, onQueryChange = onQueryChange)
            }
            item(key = "meals-actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onAddManualFood,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_food), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    BarcodeScannerButton(
                        onBarcodeScanned = onBarcodeScanned,
                        onScannerError = onScannerError,
                    )
                }
            }
            if (scannerUnavailable || barcodeLookupState !is BarcodeLookupUiState.Idle) {
                item(key = "barcode-lookup") {
                    BarcodeLookupPanel(
                        state = barcodeLookupState,
                        scannerUnavailable = scannerUnavailable,
                        onUseProduct = onUseProduct,
                        onTryAgain = onTryBarcodeAgain,
                        onManualEntry = onManualEntry,
                    )
                }
            }

            when {
                state.isLoading -> item(key = "foods-loading") { FoodsLoadingMessage() }
                state.allFoods.isEmpty() -> item(key = "foods-empty") {
                    EmptyMessage(stringResource(R.string.meals_empty))
                }
                state.foods.isEmpty() -> item(key = "search-empty") {
                    EmptyMessage(
                        text = stringResource(R.string.meals_no_results, query.trim()),
                        icon = R.drawable.ic_search,
                    )
                }
                else -> items(state.foods, key = { it.id }) { food ->
                    MealItem(
                        food = food,
                        onAdd = { onAdd(food) },
                        onOpen = { onOpen(food) },
                        onDelete = { onDelete(food) },
                        restored = food.id == restoredId,
                        onRestoredShown = onRestoredShown,
                        modifier = Modifier.animateItem(
                            fadeInSpec = null,
                            placementSpec = motionScheme.defaultSpatialSpec(),
                            fadeOutSpec = motionScheme.fastEffectsSpec(),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MealsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.section_meals),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.meals_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoodsLoadingMessage() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.foods_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val container = MaterialTheme.colorScheme.surfaceContainer
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .calTrackSurfaceOutline(CircleShape),
        placeholder = { Text(stringResource(R.string.search_meals)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }, shapes = IconButtonDefaults.shapes()) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.clear_search))
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

/** Card that logs the food on tap (with a small pulse), opens the sheet on long press, swipes away. */
@Composable
private fun MealItem(
    food: SavedFood,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    restored: Boolean,
    onRestoredShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnDelete by rememberUpdatedState(onDelete)
    val deleteLabel = stringResource(R.string.delete_from_meals)
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) { if (restored) onRestoredShown() }

    fun addWithPulse() {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onAdd()
        scope.launch {
            pulse.animateTo(1.04f, spring(stiffness = Spring.StiffnessHigh))
            pulse.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    SwipeToDeleteBox(
        onDelete = { currentOnDelete() },
        expandOnEnter = restored,
        // Outside the swipe's clip, so the pulse isn't cut off at the edges
        modifier = modifier.graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
        },
    ) {
        MealCard(
            food = food,
            onClick = ::addWithPulse,
            onLongClick = onOpen,
            modifier = Modifier.semantics {
                customActions = listOf(CustomAccessibilityAction(deleteLabel) { currentOnDelete(); true })
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealCard(
    food: SavedFood,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val portion = food.toEntry()
    val foodHash = food.name.trim().lowercase().hashCode()
    val accentContainer = when (Math.floorMod(foodHash, 3)) {
        0 -> colors.primaryContainer
        1 -> colors.secondaryContainer
        else -> colors.tertiaryContainer
    }
    val cardColor = lerp(colors.surfaceContainerLow, accentContainer, 0.38f)
    val artworkIndex = Math.floorMod(foodHash, 6)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(ListItemShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = stringResource(R.string.add),
                onLongClickLabel = stringResource(R.string.edit),
            ),
        shape = ListItemShape,
        color = cardColor,
        contentColor = colors.onSurface,
        border = calTrackSurfaceBorder(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            TodayCardArtwork(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp),
                size = 96.dp,
                alpha = 0.055f,
                shapeIndex = artworkIndex,
                rotation = 12f + Math.floorMod(artworkIndex * 17, 47),
            )
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (food.imageUrl.isNullOrBlank()) {
                    FoodAvatar(food.name, expressiveTodayPalette = true)
                } else {
                    ProductImage(
                        url = food.imageUrl,
                        description = food.name,
                        size = 48.dp,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.meal_portion,
                            formatAmount(food.defaultAmount),
                            food.quantityUnit.code,
                            formatRounded(portion.kcal),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NutrientMacroCard(
                            abbreviation = "P",
                            value = portion.protein,
                            containerColor = colors.tertiaryContainer,
                            contentColor = colors.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        NutrientMacroCard(
                            abbreviation = "C",
                            value = portion.carbs,
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        NutrientMacroCard(
                            abbreviation = "G",
                            value = portion.fat,
                            containerColor = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onClick, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_meal, food.name),
                    )
                }
            }
        }
    }
}

@Composable
private fun NutrientMacroCard(
    abbreviation: String,
    value: Double,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 32.dp),
        shape = SmallCardShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = abbreviation,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = formatRounded(value),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.grams_unit),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

/** "P 13 · C 40 · G 10" */
@Composable
fun macrosLabel(protein: Double, carbs: Double, fat: Double): String =
    stringResource(R.string.meal_macros, formatRounded(protein), formatRounded(carbs), formatRounded(fat))

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyMessage(text: String, icon: Int = R.drawable.ic_restaurant) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun blankFoodDraft() = SavedFood(
    name = "",
    kcalPer100 = 0.0,
    proteinPer100 = 0.0,
    carbsPer100 = 0.0,
    fatPer100 = 0.0,
    defaultAmount = 100.0,
    quantityUnit = FoodQuantityUnit.GRAMS,
    dataSource = FoodDataSource.MANUAL,
)

private fun BarcodeProduct.toSavedFood(unit: FoodQuantityUnit) = SavedFood(
    name = editorName,
    kcalPer100 = nutrition.kcalPer100 ?: 0.0,
    proteinPer100 = nutrition.proteinPer100 ?: 0.0,
    carbsPer100 = nutrition.carbsPer100 ?: 0.0,
    fatPer100 = nutrition.fatPer100 ?: 0.0,
    defaultAmount = defaultAmount(unit),
    barcode = barcode,
    quantityUnit = unit,
    dataSource = dataSource,
    brand = brand,
    productQuantity = productQuantity,
    servingSize = servingSize,
    imageUrl = imageUrl,
)

private val previewFoods = listOf(
    SavedFood(id = 1, name = "Brânză telemea", kcalPer100 = 250.0, proteinPer100 = 17.0, carbsPer100 = 1.0, fatPer100 = 20.0, defaultAmount = 50.0, useCount = 9),
    SavedFood(id = 2, name = "Luca traditional", kcalPer100 = 250.0, proteinPer100 = 10.8, carbsPer100 = 33.3, fatPer100 = 8.3, defaultAmount = 120.0, useCount = 4),
    SavedFood(id = 3, name = "Piept de pui", kcalPer100 = 165.0, proteinPer100 = 31.0, carbsPer100 = 0.0, fatPer100 = 3.6, defaultAmount = 180.0, useCount = 2),
)

@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES or android.content.res.Configuration.UI_MODE_TYPE_NORMAL, widthDp = 360)
@Preview(name = "Light", widthDp = 360)
@Composable
private fun MealsScreenPreview() {
    CalTrackTheme {
        Surface {
            MealsScreen(
                state = MealsUiState(foods = previewFoods, allFoods = previewFoods, isLoading = false),
                query = "",
                onQueryChange = {},
                contentPadding = PaddingValues(0.dp),
                onAdd = {},
                onOpen = {},
                onDelete = {},
                restoredId = null,
                onRestoredShown = {},
            )
        }
    }
}
