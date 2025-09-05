package com.example.myapplication.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.SessionManager
import com.example.myapplication.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun LoginScreen(
    context: Context,
    onLoginSuccess: () -> Unit,
    onNavigateRegister: (() -> Unit)? = null
) {
    val session = remember { SessionManager(context) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var captchaId by remember { mutableStateOf("") }
    var captchaQuestion by remember { mutableStateOf("") }
    var captchaText by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surface
        )
    )

    fun refreshCaptcha() {
        val client = OkHttpClient()
        val url = "https://fc-${BuildConfig.EMAS_SPACE_ID}.next.bspapp.com/captcha"
        scope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    val req = Request.Builder().url(url).get()
                        .addHeader(BuildConfig.EMAS_SECRET_HEADER, BuildConfig.EMAS_SECRET)
                        .build()
                    client.newCall(req).execute().use { resp -> resp.body?.string() ?: "" }
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
            error = "请输入用户名与密码"
        } else if (!loading) {
            error = null
            loading = true
            val client = OkHttpClient()
            val qs = "username=" + URLEncoder.encode(username, "UTF-8") +
                    "&password=" + URLEncoder.encode(password, "UTF-8") +
                    "&captchaId=" + URLEncoder.encode(captchaId, "UTF-8") +
                    "&captchaText=" + URLEncoder.encode(captchaText, "UTF-8")
            val url = "https://fc-${BuildConfig.EMAS_SPACE_ID}.next.bspapp.com/login?" + qs
            scope.launch {
                try {
                    val textAndOk = withContext(Dispatchers.IO) {
                        val req = Request.Builder()
                            .url(url)
                            .get()
                            .addHeader(BuildConfig.EMAS_SECRET_HEADER, BuildConfig.EMAS_SECRET)
                            .build()
                        client.newCall(req).execute().use { resp ->
                            val text = resp.body?.string() ?: ""
                            text to resp.isSuccessful
                        }
                    }
                    val text = textAndOk.first
                    val ok = textAndOk.second
                    if (ok && text.contains("\"success\":true")) {
                        val token = Regex("\"token\":\"(.*?)\"").find(text)?.groupValues?.getOrNull(1)
                        val role = Regex("\"role\":\"(.*?)\"").find(text)?.groupValues?.getOrNull(1)
                        session.username = username
                        if (!token.isNullOrBlank()) session.token = token
                        session.isSuperAdmin = (role == "super_admin")
                        loading = false
                        onLoginSuccess()
                    } else {
                        loading = false
                        error = "登录失败：$text"
                        refreshCaptcha()
                    }
                } catch (t: Throwable) {
                    loading = false
                    error = t.message ?: t.toString()
                    refreshCaptcha()
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(bgBrush),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 4.dp
                ) {
                    Image(
                        painter = painterResource(id = com.example.myapplication.R.drawable.app_logo),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "登录",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("用户名") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("密码") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        submit()
                    })
                )

                // 验证码放在密码框下面
                Spacer(modifier = Modifier.height(12.dp))
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
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                disabledContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.width(140.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        submit()
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("登录中...")
                    } else {
                        Text("登录")
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onNavigateRegister?.invoke() }) {
                        Text("去注册")
                    }
                }

                error?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(msg, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}


