package com.example.weather.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.weather.core.config.validateAmapApiKey

/** 首次启动或修改配置时展示的高德 API Key 输入页。 */
@Composable
fun ApiKeySetupScreen(
    initialValue: String,
    canCancel: Boolean,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var input by remember(initialValue) { mutableStateOf(initialValue) }
    var error by remember { mutableStateOf<String?>(null) }
    var showKey by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Card(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = if (canCancel) "修改 API Key" else "欢迎使用 Weather",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it.trim()
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("高德  API Key") },
                        singleLine = true,
                        isError = error != null,
                        supportingText = {
                            Text(error ?: "Key 仅保存在本机，不会上传到网络")
                        },
                        visualTransformation = if (showKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    )

                    TextButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "隐藏 Key" else "显示 Key")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (canCancel) {
                            TextButton(onClick = onCancel) {
                                Text("取消")
                            }
                        }
                        Button(
                            onClick = {
                                val validationError = validateAmapApiKey(input)
                                if (validationError == null) {
                                    onSave(input.trim())
                                } else {
                                    error = validationError
                                }
                            },
                        ) {
                            Text(if (canCancel) "保存并重新加载" else "保存并开始使用")
                        }
                    }
                }
            }

        }
    }
}
