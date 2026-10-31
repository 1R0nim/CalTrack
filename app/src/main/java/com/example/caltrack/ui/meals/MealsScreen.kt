package com.example.caltrack.ui.meals

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caltrack.R
import com.example.caltrack.data.local.entity.SavedFood
import com.example.caltrack.ui.components.FoodAvatar
import com.example.caltrack.ui.components.ScreenTitle
import com.example.caltrack.ui.components.SwipeToDeleteBox
import com.example.caltrack.ui.formatAmount
import com.example.caltrack.ui.formatRounded
import com.example.caltrack.ui.theme.CalTrackTheme
import com.example.caltrack.ui.theme.ListItemShape
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * @param selectedDate the day shown on Azi; foods added here are logged on it.
 * @param contentPadding already includes the space the floating bar covers.
 */
@Composable
fun MealsRoute(
    viewModel: MealsViewModel,
    selectedDate: () -> LocalDate,
    contentPadding: PaddingValues,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) { viewModel.setQuery(query) }
    var sheetFoodId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Food brought back by Undo, so its card expands into place
    var restoredId by remember { mutableStateOf<Long?>(null) }

    fun add(food: SavedFood, grams: Double) {
        val added = viewModel.add(food, grams, selectedDate())
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

    MealsScreen(
        state = state,
        query = query,
        onQueryChange = { query = it },
        contentPadding = contentPadding,
        onAdd = { food -> add(food, food.defaultGrams) },
        onOpen = { food -> sheetFoodId = food.id },
        onDelete = ::deleteWithUndo,
        restoredId = restoredId,
        onRestoredShown = { restoredId = null },
    )

    // Looked up again on every change, so edits show up in the open sheet
    val sheetFood = sheetFoodId?.let { id -> state.allFoods.firstOrNull { it.id == id } }
    if (sheetFood != null) {
        // Adding and deleting also close the sheet; both are called once it has animated away
        MealSheet(
            food = sheetFood,
            onAdd = { grams ->
                sheetFoodId = null
                add(sheetFood, grams)
            },
            onUpdate = viewModel::update,
            onDelete = {
                sheetFoodId = null
                deleteWithUndo(sheetFood)
            },
            onDismiss = { sheetFoodId = null },
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
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val motionScheme = MaterialTheme.motionScheme
    Column(
        modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateEndPadding(layoutDirection),
            ),
    ) {
        ScreenTitle(stringResource(R.string.section_meals))
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        when {
            state.isLoading -> Unit
            state.allFoods.isEmpty() -> EmptyMessage(stringResource(R.string.meals_empty))
            state.foods.isEmpty() -> EmptyMessage(stringResource(R.string.meals_no_results, query.trim()))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.foods, key = { it.id }) { food ->
                    MealItem(
                        food = food,
                        onAdd = { onAdd(food) },
                        onOpen = { onOpen(food) },
                        onDelete = { onDelete(food) },
                        restored = food.id == restoredId,
                        onRestoredShown = onRestoredShown,
                        // Reorders as use counts change; removal is the swipe's own collapse
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
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
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
    val portion = food.toEntry()
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
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodAvatar(food.name)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.meal_portion, formatAmount(food.defaultGrams), formatRounded(portion.kcal)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = macrosLabel(portion.protein, portion.carbs, portion.fat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

/** "P 13 · C 40 · G 10" */
@Composable
fun macrosLabel(protein: Double, carbs: Double, fat: Double): String =
    stringResource(R.string.meal_macros, formatRounded(protein), formatRounded(carbs), formatRounded(fat))

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyMessage(text: String) {
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
                painter = painterResource(R.drawable.ic_restaurant),
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

private val previewFoods = listOf(
    SavedFood(id = 1, name = "Brânză telemea", kcalPer100g = 250.0, proteinPer100g = 17.0, carbsPer100g = 1.0, fatPer100g = 20.0, defaultGrams = 50.0, useCount = 9),
    SavedFood(id = 2, name = "Luca traditional", kcalPer100g = 250.0, proteinPer100g = 10.8, carbsPer100g = 33.3, fatPer100g = 8.3, defaultGrams = 120.0, useCount = 4),
    SavedFood(id = 3, name = "Piept de pui", kcalPer100g = 165.0, proteinPer100g = 31.0, carbsPer100g = 0.0, fatPer100g = 3.6, defaultGrams = 180.0, useCount = 2),
)

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, widthDp = 360)
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
