package com.paici.app.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paici.app.ui.theme.拍词Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            // ── 语言 ──────────────────────────────────────────────────────────
            Text(
                text     = "语言",
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("母语") },
                    trailingContent = {
                        Text(
                            text  = "中文",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("学习语言") },
                    trailingContent = {
                        Text(
                            text  = "English",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            // ── 反馈 ──────────────────────────────────────────────────────────
            Text(
                text     = "反馈",
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 4.dp),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("意见反馈") })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(headlineContent = { Text("给我们评分") })
            }
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
private fun SettingsScreenPreview() {
    拍词Theme { SettingsScreen() }
}
