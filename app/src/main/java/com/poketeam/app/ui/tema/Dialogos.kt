package com.poketeam.app.ui.tema

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Pergunta antes de apagar algo que não tem volta. */
@Composable
fun ConfirmarExclusao(titulo: String, texto: String, acao: String = "Excluir", aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = { aoCancelar(); aoConfirmar() }) { Text(acao, color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } },
    )
}
