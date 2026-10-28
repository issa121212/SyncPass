package com.rojas.syncpass.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import com.rojas.syncpass.core.util.FormatoFechaHora

/** Digits stay unformatted in the editing state, so deleting separators never traps the cursor. */
class FechaHoraVisualTransformation(private val hora: Boolean = false) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = if (hora) FormatoFechaHora.hora(text.text) else FormatoFechaHora.fecha(text.text)
        val cuts = if (hora) listOf(2) else listOf(4, 6)
        return TransformedText(AnnotatedString(formatted), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) =
                (offset + cuts.count { it <= offset && it < text.length }).coerceAtMost(formatted.length)
            override fun transformedToOriginal(offset: Int) =
                formatted.take(offset).count { it in '0'..'9' }.coerceAtMost(text.length)
        })
    }
}

@Composable
fun FechaCampo(value: String, onValueChange: (String) -> Unit, label: String,
               modifier: Modifier = Modifier, hora: Boolean = false) {
    OutlinedTextField(value = value,
        onValueChange = { onValueChange(FormatoFechaHora.digitos(it, if (hora) 4 else 8)) },
        label = { Text(label) }, placeholder = { Text(if (hora) "14:00" else "2026-12-04") },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = FechaHoraVisualTransformation(hora), modifier = modifier)
}
