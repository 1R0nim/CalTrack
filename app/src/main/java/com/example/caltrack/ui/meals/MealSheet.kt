package com.example.caltrack.ui.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.data.FoodDataSource
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import com.example.caltrack.data.local.entity.SavedFood
import com.example.caltrack.ui.components.AmountField
import com.example.caltrack.ui.components.FoodAvatar
import com.example.caltrack.ui.components.FoodEditorValues
import com.example.caltrack.ui.components.FoodNutritionEditor
import com.example.caltrack.ui.components.parsedAmount
import com.example.caltrack.ui.components.parsedNutrition
import com.example.caltrack.ui.formatAmount
import com.example.caltrack.ui.formatAmountInput
import com.example.caltrack.ui.formatRounded
import com.example.caltrack.ui.parseAmount
import com.example.caltrack.ui.theme.FieldShape
import com.example.caltrack.ui.theme.calTrackSurfaceOutline
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

private val QuickAmounts = listOf(50, 100, 150, 200)

/**
 * Long-press sheet for a saved food: pick a portion and add it, or edit / delete the food.
 * [onAdd] and [onDelete] are called once the sheet has animated away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealSheet(
    food: SavedFood,
    onAdd: (amount: Double) -> Unit,
    onUpdate: (SavedFood) -> Deferred<Boolean>,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    fun close(then: () -> Unit) {
        if (closing) return
        closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        // The sheet already pads itself above the keyboard; scrolling covers small screens
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            if (editing) {
                EditMealContent(food = food, onUpdate = onUpdate, onDone = { editing = false })
            } else {
                AddMealContent(
                    food = food,
                    onAdd = { amount -> close { onAdd(amount) } },
                    onEdit = { editing = true },
                    onDelete = { confirmDelete = true },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            modifier = Modifier.calTrackSurfaceOutline(MaterialTheme.shapes.extraLarge),
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.delete_meal_title, food.name)) },
            text = { Text(stringResource(R.string.delete_meal_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        close(onDelete)
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** Creation sheet for a library food; saving it never adds a journal entry. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavedFoodSheet(
    food: SavedFood,
    requiredNutrients: Set<String>,
    onSave: (SavedFood) -> Deferred<Boolean>,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    fun close(saved: Boolean) {
        if (closing) return
        closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (saved) onSaved()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { close(saved = false) },
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            EditMealContent(
                food = food,
                onUpdate = onSave,
                onDone = { close(saved = true) },
                onCancel = { close(saved = false) },
                requiredNutrients = requiredNutrients,
            )
        }
    }
}

@Composable
private fun AddMealContent(
    food: SavedFood,
    onAdd: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf(formatAmount(food.defaultAmount)) }
    val portion = parseAmount(amount)?.takeIf { it > 0 }
    // Recomputed on every keystroke
    val values = food.toEntry(portion ?: 0.0)
    val add = { if (portion != null) onAdd(portion) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FoodAvatar(food.name)
            Spacer(Modifier.width(14.dp))
            Text(
                text = food.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AmountField(
                value = amount,
                onValueChange = { amount = it },
                label = stringResource(R.string.quantity),
                unit = food.quantityUnit.code,
                modifier = Modifier.weight(1f),
                imeAction = ImeAction.Done,
                onDone = add,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatRounded(values.kcal),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.kcal),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Text(
                    text = macrosLabel(values.protein, values.carbs, values.fat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        QuickAmountsGroup(
            selected = portion,
            unit = food.quantityUnit.code,
            onSelect = { amount = it.toString() },
        )
        Button(
            onClick = add,
            enabled = portion != null,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add), style = MaterialTheme.typography.titleMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.edit), maxLines = 1)
            }
            OutlinedButton(onClick = onDelete, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.delete_from_meals),
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Connected Expressive button group with common portions; the one matching the field is checked. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuickAmountsGroup(selected: Double?, unit: String, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        QuickAmounts.forEachIndexed { index, value ->
            ToggleButton(
                checked = selected == value.toDouble(),
                onCheckedChange = { onSelect(value) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    QuickAmounts.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
            ) {
                Text(stringResource(R.string.quantity_amount, value.toString(), unit), maxLines = 1)
            }
        }
    }
}

/** The same reference-value editor is used for new and existing library foods. */
@Composable
private fun EditMealContent(
    food: SavedFood,
    onUpdate: (SavedFood) -> Deferred<Boolean>,
    onDone: () -> Unit,
    onCancel: () -> Unit = onDone,
    requiredNutrients: Set<String> = emptySet(),
) {
    val isNew = food.id == 0L
    val isManualCreation = isNew && food.dataSource == FoodDataSource.MANUAL
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(food.id) {
        mutableStateOf(if (isManualCreation) "" else food.name)
    }
    var amount by rememberSaveable(food.id) { mutableStateOf(formatAmountInput(food.defaultAmount)) }
    var kcal by rememberSaveable(food.id) {
        mutableStateOf(if (isManualCreation || "kcal" in requiredNutrients) "" else formatAmountInput(food.kcalPer100))
    }
    var protein by rememberSaveable(food.id) {
        mutableStateOf(if (isManualCreation || "protein" in requiredNutrients) "" else formatAmountInput(food.proteinPer100))
    }
    var carbs by rememberSaveable(food.id) {
        mutableStateOf(if (isManualCreation || "carbohydrates" in requiredNutrients) "" else formatAmountInput(food.carbsPer100))
    }
    var fat by rememberSaveable(food.id) {
        mutableStateOf(if (isManualCreation || "fat" in requiredNutrients) "" else formatAmountInput(food.fatPer100))
    }
    var quantityUnit by rememberSaveable(food.id) { mutableStateOf(food.quantityUnit) }
    var nameTaken by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val editorValues = FoodEditorValues(
        name = name,
        amount = amount,
        quantityUnit = quantityUnit,
        kcalPer100 = kcal,
        proteinPer100 = protein,
        carbsPer100 = carbs,
        fatPer100 = fat,
    )
    val updated = editedFood(food, editorValues, requiredNutrients)
    val save = {
        if (updated != null && !saving) {
            saving = true
            scope.launch {
                if (onUpdate(updated).await()) onDone() else nameTaken = true
                saving = false
            }
        }
    }

    FoodNutritionEditor(
        values = editorValues,
        onValuesChange = { next ->
            if (next.name != name) nameTaken = false
            name = next.name
            amount = next.amount
            quantityUnit = next.quantityUnit
            kcal = next.kcalPer100
            protein = next.proteinPer100
            carbs = next.carbsPer100
            fat = next.fatPer100
        },
        saveEnabled = updated != null,
        saving = saving,
        onSave = save,
        onCancel = onCancel,
        title = stringResource(if (isNew) R.string.add_food else R.string.edit_food),
        requiredNutrients = requiredNutrients,
        nameErrorMessage = if (nameTaken) {
            stringResource(if (isNew) R.string.food_already_saved else R.string.name_taken)
        } else {
            null
        },
    )
}

/** [food] with reference values and the selected default quantity applied. */
private fun editedFood(
    food: SavedFood,
    values: FoodEditorValues,
    requiredNutrients: Set<String>,
): SavedFood? {
    val amount = values.parsedAmount() ?: return null
    val nutrition = values.parsedNutrition(requiredNutrients) ?: return null
    return food.copy(
        name = values.name.trim().ifEmpty { return null },
        kcalPer100 = nutrition.kcal,
        proteinPer100 = nutrition.protein,
        carbsPer100 = nutrition.carbs,
        fatPer100 = nutrition.fat,
        defaultAmount = amount,
        quantityUnit = values.quantityUnit,
    )
}
