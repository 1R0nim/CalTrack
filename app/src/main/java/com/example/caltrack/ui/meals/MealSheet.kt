package com.example.caltrack.ui.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.example.caltrack.data.local.entity.SavedFood
import com.example.caltrack.ui.components.AmountField
import com.example.caltrack.ui.components.FoodAvatar
import com.example.caltrack.ui.components.parseOptionalAmount
import com.example.caltrack.ui.formatAmount
import com.example.caltrack.ui.formatRounded
import com.example.caltrack.ui.parseAmount
import com.example.caltrack.ui.theme.FieldShape
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

private val QuickGrams = listOf(50, 100, 150, 200)

/**
 * Long-press sheet for a saved food: pick a portion and add it, or edit / delete the food.
 * [onAdd] and [onDelete] are called once the sheet has animated away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealSheet(
    food: SavedFood,
    onAdd: (grams: Double) -> Unit,
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
                    onAdd = { grams -> close { onAdd(grams) } },
                    onEdit = { editing = true },
                    onDelete = { confirmDelete = true },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
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

@Composable
private fun AddMealContent(
    food: SavedFood,
    onAdd: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var grams by rememberSaveable { mutableStateOf(formatAmount(food.defaultGrams)) }
    val portion = parseAmount(grams)?.takeIf { it > 0 }
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
                value = grams,
                onValueChange = { grams = it },
                label = stringResource(R.string.grams),
                unit = stringResource(R.string.grams_unit),
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
        QuickGramsGroup(selected = portion, onSelect = { grams = it.toString() })
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
private fun QuickGramsGroup(selected: Double?, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        QuickGrams.forEachIndexed { index, value ->
            ToggleButton(
                checked = selected == value.toDouble(),
                onCheckedChange = { onSelect(value) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    QuickGrams.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
            ) {
                Text(stringResource(R.string.grams_amount, value), maxLines = 1)
            }
        }
    }
}

/** Name and per-100 g values; the portion stays whatever was last logged. */
@Composable
private fun EditMealContent(
    food: SavedFood,
    onUpdate: (SavedFood) -> Deferred<Boolean>,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(food.name) }
    var kcal by rememberSaveable { mutableStateOf(formatAmount(food.kcalPer100g)) }
    var protein by rememberSaveable { mutableStateOf(formatAmount(food.proteinPer100g)) }
    var carbs by rememberSaveable { mutableStateOf(formatAmount(food.carbsPer100g)) }
    var fat by rememberSaveable { mutableStateOf(formatAmount(food.fatPer100g)) }
    var nameTaken by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val gramsUnit = stringResource(R.string.grams_unit)

    val updated = editedFood(food, name, kcal, protein, carbs, fat)
    val save = {
        if (updated != null && !saving) {
            saving = true
            scope.launch {
                if (onUpdate(updated).await()) onDone() else nameTaken = true
                saving = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.edit_meal),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameTaken = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.name)) },
            isError = nameTaken,
            supportingText = if (nameTaken) ({ Text(stringResource(R.string.name_taken)) }) else null,
            singleLine = true,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
        )
        Text(
            text = stringResource(R.string.per_100g),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        AmountField(
            value = kcal,
            onValueChange = { kcal = it },
            label = stringResource(R.string.calories),
            unit = stringResource(R.string.kcal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AmountField(
                value = protein,
                onValueChange = { protein = it },
                label = stringResource(R.string.protein),
                unit = gramsUnit,
                modifier = Modifier.weight(1f),
                optional = true,
            )
            AmountField(
                value = carbs,
                onValueChange = { carbs = it },
                label = stringResource(R.string.carbs),
                unit = gramsUnit,
                modifier = Modifier.weight(1f),
                optional = true,
            )
            AmountField(
                value = fat,
                onValueChange = { fat = it },
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
            TextButton(onClick = onDone, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
            Button(onClick = save, shapes = ButtonDefaults.shapes(), enabled = updated != null && !saving) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

/** [food] with the form values applied, or null while a required field is missing or invalid. */
private fun editedFood(
    food: SavedFood,
    name: String,
    kcal: String,
    protein: String,
    carbs: String,
    fat: String,
): SavedFood? {
    return food.copy(
        name = name.trim().ifEmpty { return null },
        kcalPer100g = parseAmount(kcal) ?: return null,
        proteinPer100g = parseOptionalAmount(protein) ?: return null,
        carbsPer100g = parseOptionalAmount(carbs) ?: return null,
        fatPer100g = parseOptionalAmount(fat) ?: return null,
    )
}
