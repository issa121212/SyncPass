package com.rojas.syncpass.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rojas.syncpass.domain.model.codigoPublicoVisible

@Composable
fun CodigoPublicoLabel(codigo: String?, syncState: String) {
    Text(codigoPublicoVisible(codigo, syncState), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
