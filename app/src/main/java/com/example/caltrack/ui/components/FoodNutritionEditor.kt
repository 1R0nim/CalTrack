package com.example.caltrack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.caltrack.R
import com.example.caltrack.data.local.entity.FoodNutritionPer100
import com.example.caltrack.data.local.entity.FoodNutritionTotals
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import com.example.caltrack.data.local.entity.SavedFood
import com.example.caltrack.ui.formatAmountInput
import com.example.caltrack.ui.formatNutritionValue
import com.example.caltrack.ui.parseAmount
import com.example.caltrack.ui.theme.FieldShape

/** Raw editable values for the shared Today and Meals food editor. */
data class FoodEditorValues(
    val name: String,
    val amount: String,
    val quantityUnit: FoodQuantityUnit,
    val kcalPer100: String,
    val proteinPer100: String,
    val carbsPer100: String,
    val fatPer100: String,
)

/** Returns a positive quantity or null while it is missing or invalid. */
fun FoodEditorValues.parsedAmount(): Double? =
    parseAmount(amount)?.takeIf { it > 0.0 }

/** Blank optional nutrients remain zero; barcode fields marked required must be completed. */
fun FoodEditorValues.parsedNutrition(requiredNutrients: Set<String> = emptySet()): FoodNutritionPer100? {
    fun nutrient(value: String, key: String): Double? =
        if (value.isBlank() && key !in requiredNutrients) 0.0 else parseAmount(value)

    return FoodNutritionPer100(
        kcal = parseAmount(kcalPer100) ?: return null,
        protein = nutrient(proteinPer100, "protein") ?: return null,
        carbs = nutrient(carbsPer100, "carbohydrates") ?: return null,
        fat = nutrient(fatPer100, "fat") ?: return null,
    )
}

private fun FoodEditorValues.hasMissingRequiredNutrition(requiredNutrients: Set<String>): Boolean =
    ("kcal" in requiredNutrients && parseAmount(kcalPer100) == null) ||
        ("protein" in requiredNutrients && parseAmount(proteinPer100) == null) ||
        ("carbohydrates" in requiredNutrients && parseAmount(carbsPer100) == null) ||
        ("fat" in requiredNutrients && parseAmount(fatPer100) == null)

/**
 * One shared editor for journal entries and foods in the library. Nutrient fields always contain
 * reference values per 100 units; the compact preview shows the totals for [FoodEditorValues.amount].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodNutritionEditor(
    values: FoodEditorValues,
    onValuesChange: (FoodEditorValues) -> Unit,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    saving: Boolean = false,
    requiredNutrients: Set<String> = emptySet(),
    suggestions: List<SavedFood> = emptyList(),
    pickedSuggestion: SavedFood? = null,
    onPickSuggestion: (SavedFood) -> Unit = {},
    nameErrorMessage: String? = null,
    nameModifier: Modifier = Modifier,
) {
    val amount = values.parsedAmount()
    val nutrition = values.parsedNutrition(requiredNutrients)
    val totals = if (amount != null && nutrition != null) nutrition.forQuantity(amount) else null
    val quantityUnit = stringResource(
        if (values.quantityUnit == FoodQuantityUnit.GRAMS) {
            R.string.grams_unit
        } else {
            R.string.milliliters_unit
        },
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        FoodNameField(
            value = values.name,
            onValueChange = { onValuesChange(values.copy(name = it)) },
            suggestions = suggestions,
            picked = pickedSuggestion,
            onPick = onPickSuggestion,
            nameErrorMessage = nameErrorMessage,
            modifier = nameModifier,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.nutrition_basis),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = values.quantityUnit == FoodQuantityUnit.GRAMS,
                    onClick = {
                        onValuesChange(values.copy(quantityUnit = FoodQuantityUnit.GRAMS))
                    },
                    label = { Text(stringResource(R.string.grams_unit)) },
                )
                FilterChip(
                    selected = values.quantityUnit == FoodQuantityUnit.MILLILITERS,
                    onClick = {
                        onValuesChange(values.copy(quantityUnit = FoodQuantityUnit.MILLILITERS))
                    },
                    label = { Text(stringResource(R.string.milliliters_unit)) },
                )
            }
        }

        AmountField(
            value = values.amount,
            onValueChange = { onValuesChange(values.copy(amount = it)) },
            label = stringResource(R.string.quantity),
            unit = quantityUnit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            maxInputLength = 32,
        )

        AmountField(
            value = values.kcalPer100,
            onValueChange = { onValuesChange(values.copy(kcalPer100 = it)) },
            label = stringResource(R.string.calories),
            unit = stringResource(R.string.kcal),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            maxInputLength = 32,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AmountField(
                value = values.proteinPer100,
                onValueChange = { onValuesChange(values.copy(proteinPer100 = it)) },
                label = stringResource(R.string.protein),
                unit = stringResource(R.string.grams_unit),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium,
                optional = "protein" !in requiredNutrients,
                maxInputLength = 32,
            )
            AmountField(
                value = values.carbsPer100,
                onValueChange = { onValuesChange(values.copy(carbsPer100 = it)) },
                label = stringResource(R.string.carbs),
                unit = stringResource(R.string.grams_unit),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium,
                optional = "carbohydrates" !in requiredNutrients,
                maxInputLength = 32,
            )
            AmountField(
                value = values.fatPer100,
                onValueChange = { onValuesChange(values.copy(fatPer100 = it)) },
                label = stringResource(R.string.fat),
                unit = stringResource(R.string.grams_unit),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium,
                optional = "fat" !in requiredNutrients,
                imeAction = ImeAction.Done,
                onDone = onSave,
                maxInputLength = 32,
            )
        }

        AnimatedContent(targetState = totals, label = "food nutrition totals") { displayedTotals ->
            if (displayedTotals != null && amount != null) {
                NutritionTotalsSummary(
                    amount = amount,
                    unit = quantityUnit,
                    totals = displayedTotals,
                )
            }
        }

        if (requiredNutrients.isNotEmpty() && values.hasMissingRequiredNutrition(requiredNutrients)) {
            Text(
                text = stringResource(R.string.barcode_missing_editor_fields),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel, shapes = ButtonDefaults.shapes()) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onSave,
                enabled = saveEnabled && !saving,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.save), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun NutritionTotalsSummary(
    amount: Double,
    unit: String,
    totals: FoodNutritionTotals,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(
                R.string.calculated_nutrition_for,
                formatAmountInput(amount),
                unit,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.calculated_nutrition_values,
                formatNutritionValue(totals.kcal),
                formatNutritionValue(totals.protein),
                formatNutritionValue(totals.carbs),
                formatNutritionValue(totals.fat),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Name field that suggests saved foods matching what's typed (ignoring case and diacritics). */
@Composable
private fun FoodNameField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<SavedFood>,
    picked: SavedFood?,
    onPick: (SavedFood) -> Unit,
    nameErrorMessage: String?,
    modifier: Modifier,
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
                .take(5)
                .toList()
        }
    }
    val expanded = focused && !dismissed && picked == null && matches.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!it) dismissed = true },
    ) {
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
            supportingText = nameErrorMessage?.let { message -> { Text(message) } },
            isError = nameErrorMessage != null,
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
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(food.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = {
                        Text(
                            text = stringResource(R.string.per_100_quantity, food.quantityUnit.code) +
                                " · " + stringResource(
                                R.string.amount_kcal,
                                formatAmountInput(food.kcalPer100),
                            ),
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
