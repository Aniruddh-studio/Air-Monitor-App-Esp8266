package com.example.aqisensor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.aqisensor.R
import com.example.aqisensor.ui.theme.AccentGreen

/**
 * Modal dialog that lets the user update the saved device IP address.
 * Accessible via the ⚙ icon in the top app bar.
 *
 * @param currentIp  Pre-filled with the currently saved IP.
 * @param onSave     Called with the new IP string when the user confirms.
 * @param onDismiss  Called when the user cancels or taps outside.
 */
@Composable
fun SettingsDialog(
    currentIp: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var ipText by remember { mutableStateOf(currentIp) }
    var error  by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    fun validate(): Boolean {
        // Accept plain IP (192.168.1.5) or IP:port (192.168.1.5:8080)
        val valid = ipText.trim().matches(Regex("""\d{1,3}(\.\d{1,3}){3}(:\d{1,5})?"""))
        error = !valid
        return valid
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text  = stringResource(R.string.title_settings),
                    style = MaterialTheme.typography.headlineMedium,
                )

                OutlinedTextField(
                    value          = ipText,
                    onValueChange  = { ipText = it; error = false },
                    label          = { Text(stringResource(R.string.label_device_ip)) },
                    placeholder    = { Text(stringResource(R.string.hint_ip_address)) },
                    isError        = error,
                    supportingText = if (error) {
                        { Text("Enter IP or IP:port (e.g. 192.168.1.5:8080)") }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction    = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        if (validate()) onSave(ipText.trim())
                    }),
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            keyboard?.hide()
                            if (validate()) onSave(ipText.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    ) {
                        Text(
                            text  = stringResource(R.string.action_save),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}
