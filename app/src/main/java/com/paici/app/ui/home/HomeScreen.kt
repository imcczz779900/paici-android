package com.paici.app.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paici.app.ui.theme.拍词Theme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onCameraClick: () -> Unit,
    wordCount: Int = 0,
) {
    val today = remember { LocalDate.now() }
    val dateText = remember(today) {
        val dow = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
        "${today.monthValue}月${today.dayOfMonth}日，$dow"
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(36.dp))

            // ── 日期 + 问候 + 引导文案 ────────────────────────────────────────
            Text(
                text  = dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = "你好",
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "拍张照片，发现一个新单词",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.weight(1f))

            // ── 插画占位 ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    modifier           = Modifier.size(80.dp),
                    tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(Modifier.weight(1f))

            // ── 主按钮 ────────────────────────────────────────────────────────
            Button(
                onClick  = onCameraClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    modifier           = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = "拍照识词",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── 统计 ──────────────────────────────────────────────────────────
            Text(
                text  = "已收录 $wordCount 个单词",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── 预览 ──────────────────────────────────────────────────────────────────────

@Preview(name = "亮色", showBackground = true)
@Preview(
    name = "暗色",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenPreview() {
    拍词Theme {
        HomeScreen(onCameraClick = {}, wordCount = 12)
    }
}
