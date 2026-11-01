package com.example.caltrack.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.data.FrequentFood
import com.example.caltrack.data.filterSavedFoodsForSearch
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.SavedFood
import com.example.caltrack.ui.components.AmountField
import com.example.caltrack.ui.components.parseOptionalAmount
import com.example.caltrack.ui.formatAmount
import com.example.caltrack.ui.formatRounded
import com.example.caltrack.ui.parseAmount
import com.example.caltrack.ui.theme.FieldShape
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
    onSave: (FoodEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = entry.id == 0L
    // New entries start with empty fields instead of zeros
    fun prefill(value: Double) = if (isNew) "" else formatAmount(value)

    var name by rememberSaveable { mutableStateOf(entry.name) }
    var grams by rememberSaveable { mutableStateOf(prefill(entry.grams)) }
    var kcal by rememberSaveable { mutableStateOf(prefill(entry.kcal)) }
    var protein by rememberSaveable { mutableStateOf(prefill(entry.protein)) }
    var carbs by rememberSaveable { mutableStateOf(prefill(entry.carbs)) }
    var fat by rememberSaveable { mutableStateOf(prefill(entry.fat)) }
    val edited = editedEntry(entry, name, grams, kcal, protein, carbs, fat)

    // Picked suggestion: its per-100 g values follow the grams until a value is typed by hand
    var picked by remember { mutableStateOf<SavedFood?>(null) }
    fun fillFrom(food: SavedFood, portion: Double) {
        val factor = portion / 100.0
        kcal = formatAmount(food.kcalPer100g * factor)
        protein = formatAmount(food.proteinPer100g * factor)
        carbs = formatAmount(food.carbsPer100g * factor)
        fat = formatAmount(food.fatPer100g * factor)
    }
    fun pick(food: SavedFood) {
        picked = food
        name = food.name
        val portion = parseAmount(grams)?.takeIf { it > 0 } ?: food.defaultGrams.also { grams = formatAmount(it) }
        fillFrom(food, portion)
    }

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

    val save = { if (edited != null) close { onSave(edited) } }
    val gramsUnit = stringResource(R.string.grams_unit)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val nameFocus = remember { FocusRequester() }
        if (isNew) {
            LaunchedEffect(Unit) { nameFocus.requestFocus() }
        }

        // The sheet already pads itself above the keyboard; scrolling covers small screens
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (isNew) R.string.add_food else R.string.edit_food),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
            )
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
                        picked = null
                    },
                    suggestions = if (isNew) savedFoods else emptyList(),
                    picked = picked,
                    onPick = ::pick,
                    modifier = Modifier.focusRequester(nameFocus),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AmountField(
                        value = grams,
                        onValueChange = {
                            grams = it
                            val food = picked
                            val portion = parseAmount(it)
                            if (food != null && portion != null) fillFrom(food, portion)
                        },
                        label = stringResource(R.string.grams),
                        unit = gramsUnit,
                        modifier = Modifier.weight(1f),
                    )
                    AmountField(
                        value = kcal,
                        onValueChange = {
                            kcal = it
                            picked = null
                        },
                        label = stringResource(R.string.calories),
                        unit = stringResource(R.string.kcal),
                        modifier = Modifier.weight(1f),
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
                            picked = null
                        },
                        label = stringResource(R.string.protein),
                        unit = gramsUnit,
                        modifier = Modifier.weight(1f),
                        optional = true,
                    )
                    AmountField(
                        value = carbs,
                        onValueChange = {
                            carbs = it
                            picked = null
                        },
                        label = stringResource(R.string.carbs),
                        unit = gramsUnit,
                        modifier = Modifier.weight(1f),
                        optional = true,
                    )
                    AmountField(
                        value = fat,
                        onValueChange = {
                            fat = it
                            picked = null
                        },
                        label = stringResource(R.string.fat),
                        unit = gramsUnit,
                        modifier = Modifier.weight(1f),
                        optional = true,
                        imeAction = ImeAction.Done,
                        onDone = save,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = { close(onDismiss) }, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(onClick = save, shapes = ButtonDefaults.shapes(), enabled = edited != null) {
                        Text(stringResource(R.string.save))
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
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.frequent_foods),
            style = MaterialTheme.typography.titleMedium,
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
                )
            } else {
                FrequentFoodRow(foods = foods, onSearch = onOpenSearch, onSelect = onSelectFood)
            }
        }
    }
}

@Composable
private fun FrequentFoodRow(
    foods: List<FrequentFood>,
    onSearch: () -> Unit,
    onSelect: (SavedFood) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (foods.isEmpty()) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 88.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = stringResource(R.string.frequent_food_empty),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(foods, key = { SavedFood.nameKeyOf(it.food.name) }) { item ->
                    FrequentFoodCard(item = item, onClick = { onSelect(item.food) })
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
    }
}

@Composable
private fun FrequentFoodCard(item: FrequentFood, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(148.dp)
            .heightIn(min = 88.dp)
            .clickable(
                onClickLabel = stringResource(R.string.select_food, item.food.name),
                role = Role.Button,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.food.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            item.occurrenceCount?.let { count ->
                Text(
                    text = stringResource(R.string.frequent_food_count, count),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.amount_grams, formatAmount(item.food.defaultGrams)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val matches = remember(savedFoods, query) { filterSavedFoodsForSearch(savedFoods, query) }
    val container = MaterialTheme.colorScheme.surfaceContainerHigh

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
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_foods)) },
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
            Text(
                text = if (SavedFood.searchKeyOf(query).isEmpty()) {
                    stringResource(R.string.search_foods_empty)
                } else {
                    stringResource(R.string.meals_no_results, query.trim())
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    matches.forEach { food ->
                        FoodSearchResult(food = food, onClick = { onSelect(food) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodSearchResult(food: SavedFood, onClick: () -> Unit) {
    val portion = food.toEntry(food.defaultGrams)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.select_food, food.name),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.meal_portion,
                    formatAmount(food.defaultGrams),
                    formatRounded(portion.kcal),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.meal_macros,
                    formatRounded(portion.protein),
                    formatRounded(portion.carbs),
                    formatRounded(portion.fat),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
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
            label = { Text(stringResource(R.string.name)) },
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
                            text = stringResource(R.string.per_100g_kcal, formatAmount(food.kcalPer100g)),
                            style = MaterialTheme.typography.bodySmall,
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
    grams: String,
    kcal: String,
    protein: String,
    carbs: String,
    fat: String,
): FoodEntry? {
    return entry.copy(
        name = name.trim().ifEmpty { return null },
        grams = parseAmount(grams)?.takeIf { it > 0 } ?: return null,
        kcal = parseAmount(kcal) ?: return null,
        // Macros are optional: blank counts as 0
        protein = parseOptionalAmount(protein) ?: return null,
        carbs = parseOptionalAmount(carbs) ?: return null,
        fat = parseOptionalAmount(fat) ?: return null,
    )
}
