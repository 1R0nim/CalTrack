package com.example.caltrack.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.example.caltrack.ui.parseAmount
import com.example.caltrack.ui.theme.FieldShape

/** Decimal field with a unit suffix; rejects anything that isn't a number while typing. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.isAmountInput()) onValueChange(it) },
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = if (optional) ({ Text("0") }) else null,
        suffix = { Text(unit) },
        isError = value.isNotEmpty() && parseAmount(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        shape = FieldShape,
    )
}

/** Digits with at most one decimal separator; keyboards differ on "," vs ".". */
private fun String.isAmountInput(): Boolean =
    length <= 7 && all { it in '0'..'9' || it == '.' || it == ',' } && count { it == '.' || it == ',' } <= 1

/** Blank counts as 0; null when the text isn't a number. */
fun parseOptionalAmount(text: String): Double? = if (text.isBlank()) 0.0 else parseAmount(text)
