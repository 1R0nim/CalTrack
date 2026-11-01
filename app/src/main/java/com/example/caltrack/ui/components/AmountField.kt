package com.example.caltrack.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
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
    textStyle: TextStyle? = null,
    maxInputLength: Int = 7,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.isAmountInput(maxInputLength)) onValueChange(it) },
        modifier = modifier,
        label = {
            if (textStyle == null) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(label, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        placeholder = if (optional) ({ Text("0") }) else null,
        suffix = {
            if (textStyle == null) Text(unit) else Text(unit, style = textStyle)
        },
        isError = value.isNotEmpty() && parseAmount(value) == null,
        textStyle = textStyle ?: LocalTextStyle.current,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        shape = FieldShape,
    )
}

/** Digits with at most one decimal separator; keyboards differ on "," vs ".". */
private fun String.isAmountInput(maxInputLength: Int): Boolean =
    length <= maxInputLength &&
        all { it in '0'..'9' || it == '.' || it == ',' } &&
        count { it == '.' || it == ',' } <= 1

/** Blank counts as 0; null when the text isn't a number. */
fun parseOptionalAmount(text: String): Double? = if (text.isBlank()) 0.0 else parseAmount(text)
