package com.example.myapplication.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.SessionManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues

@Composable
fun ProfileScreen(
    context: Context,
    onLogout: () -> Unit
) {
    val session = remember { SessionManager(context) }
    val username = session.username ?: "未登录"
    val isSuperAdmin = session.isSuperAdmin

    Box(Modifier.fillMaxSize()) {
        // 顶部柔和渐变背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
        ) {

            // 个人信息卡片
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, shadowElevation = 2.dp) {
                        Image(
                            painter = painterResource(id = com.example.myapplication.R.drawable.app_logo),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(68.dp).clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text = username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        if (isSuperAdmin) {
                            AssistChip(onClick = {}, label = { Text("超级管理员") })
                        } else {
                            AssistChip(onClick = {}, label = { Text("普通用户") })
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // 操作按钮
            Button(
                onClick = {
                    session.clear()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("退出登录")
            }
        }
    }
}


