package com.example.aqisensor.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.aqisensor.R
import com.example.aqisensor.ui.theme.AccentGreen

/**
 * First-launch screen prompting the user to enter the device IP.
 * Displayed when DataStore has no saved IP.
 *
 * @param onSave  Called with the validated IP string when the user taps Save.
 */
@Composable
fun IpSetupScreen(
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ipText by remember { mutableStateOf("") }
    var error  by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    fun validate(): Boolean {
        // Accept plain IP (192.168.1.5) or IP:port (192.168.1.5:8080)
        val valid = ipText.trim().matches(Regex("""\d{1,3}(\.\d{1,3}){3}(:\d{1,5})?"""))
        error = !valid
        return valid
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // App icon placeholder (coloured badge)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "AQI",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AccentGreen,
                    )
                }

                Text(
                    text      = stringResource(R.string.setup_title),
                    style     = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text      = stringResource(R.string.setup_subtitle),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                OutlinedTextField(
                    value          = ipText,
                    onValueChange  = { ipText = it; error = false },
                    label          = { Text(stringResource(R.string.label_device_ip)) },
                    placeholder    = { Text(stringResource(R.string.hint_ip_address)) },
                    isError        = error,
                    supportingText = if (error) {
                        { Text("Enter IP or IP:port (e.g. 192.168.1.5 or 192.168.1.5:8080)") }
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

                Button(
                    onClick = {
                        keyboard?.hide()
                        if (validate()) onSave(ipText.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = AccentGreen),
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
