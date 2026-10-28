package com.rojas.syncpass.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

fun generarQrBitmap(contenido: String, tamano: Int = 640): Bitmap? = runCatching {
    val matriz: BitMatrix = MultiFormatWriter().encode(contenido, BarcodeFormat.QR_CODE, tamano, tamano)
    val bitmap = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.ARGB_8888)
    for (x in 0 until tamano) {
        for (y in 0 until tamano) {
            bitmap.setPixel(x, y, if (matriz[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    bitmap
}.getOrNull()

@Composable
fun QrDialog(
    contenido: String,
    titulo: String,
    onDismiss: () -> Unit,
) {
    val bitmap = remember(contenido) { generarQrBitmap(contenido) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Código QR",
                        modifier = Modifier.size(260.dp),
                    )
                } else {
                    Text("No se pudo generar el QR", color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Muestra este código al estudiante o voluntario de recepción. Mostrarlo no registra tu asistencia.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}
