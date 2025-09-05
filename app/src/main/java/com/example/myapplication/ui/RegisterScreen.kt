package com.example.myapplication.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

@Composable
fun RegisterScreen(
    context: Context,
    onBackToLogin: () -> Unit,
    onRegistered: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var captchaId by remember { mutableStateOf("") }
    var captchaQuestion by remember { mutableStateOf("") }
    var captchaText by remember { mutableStateOf("") }

    fun refreshCaptcha() {
        val client = OkHttpClient()
        val url = "https://fc-${BuildConfig.EMAS_SPACE_ID}.next.bspapp.com/captcha"
        scope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    val req = Request.Builder().url(url).get()
                        .addHeader(BuildConfig.EMAS_SECRET_HEADER, BuildConfig.EMAS_SECRET)
                        .build()
                    client.newCall(req).execute().use { it.body?.string() ?: "" }
                }
                val id = Regex("\"captchaId\":\"(.*?)\"").find(res)?.groupValues?.getOrNull(1) ?: ""
                val q = Regex("\"question\":\"(.*?)\"").find(res)?.groupValues?.getOrNull(1) ?: ""
                captchaId = id
                captchaQuestion = q
                captchaText = ""
            } catch (_: Throwable) { }
        }
    }

    LaunchedEffect(Unit) { refreshCaptcha() }

    fun submit() {
        if (username.isBlank() || password.isBlank()) {
            error = "请输入用户名和密码"
            return
        }
        if (loading) return
        loading = true
        val qs = "username=" + URLEncoder.encode(username, "UTF-8") +
                "&password=" + URLEncoder.encode(password, "UTF-8") +
                "&captchaId=" + URLEncoder.encode(captchaId, "UTF-8") +
                "&captchaText=" + URLEncoder.encode(captchaText, "UTF-8")
        val url = "https://fc-${BuildConfig.EMAS_SPACE_ID}.next.bspapp.com/register?" + qs
        val client = OkHttpClient()
        scope.launch {
            try {
                val textAndOk = withContext(Dispatchers.IO) {
                    val req = Request.Builder().url(url).get()
                        .addHeader(BuildConfig.EMAS_SECRET_HEADER, BuildConfig.EMAS_SECRET)
                        .build()
                    client.newCall(req).execute().use { resp ->
                        val text = resp.body?.string() ?: ""
                        text to resp.isSuccessful
                    }
                }
                val ok = textAndOk.second
                val text = textAndOk.first
                if (ok && text.contains("\"success\":true")) {
                    loading = false
                    onRegistered()
                } else {
                    loading = false
                    error = "注册失败：$text"
                    refreshCaptcha()
                }
            } catch (t: Throwable) {
                loading = false
                error = t.message ?: t.toString()
                refreshCaptcha()
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(Modifier.padding(24.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("注册", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("用户名") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Spacer(Modifier.height(12.dp))
                if (captchaQuestion.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = captchaQuestion)
                        Spacer(Modifier.width(12.dp))
                        TextButton(onClick = { refreshCaptcha() }) { Text("换一张") }
                        Spacer(Modifier.width(12.dp))
                        TextField(
                            value = captchaText,
                            onValueChange = { captchaText = it },
                            placeholder = { Text("输入结果") },
                            singleLine = true,
                            modifier = Modifier.width(140.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                disabledContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { submit() }, enabled = !loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(if (loading) "提交中..." else "提交注册")
                }
                TextButton(onClick = onBackToLogin, modifier = Modifier.align(Alignment.End)) { Text("返回登录") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}


