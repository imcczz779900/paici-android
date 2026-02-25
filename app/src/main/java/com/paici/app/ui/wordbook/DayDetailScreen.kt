package com.paici.app.ui.wordbook

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paici.app.data.Word
import com.paici.app.ui.theme.拍词Theme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: String,
    words: List<Word>,
    onBack: () -> Unit,
    onUpdateChinese: (id: Int, chinese: String) -> Unit = { _, _ -> },
) {
    val displayDate = remember(date) {
        val d = LocalDate.parse(date)
        "${d.monthValue}月${d.dayOfMonth}日"
    }

    var editingWord by remember { mutableStateOf<Word?>(null) }
    var editText    by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayDate) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier        = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(words, key = { it.id }) { word ->
                WordCard(
                    word          = word,
                    onEditChinese = if (word.chinese == "—") {
                        {
                            editingWord = word
                            editText    = ""
                        }
                    } else null,
                )
            }
        }
    }

    editingWord?.let { word ->
        AlertDialog(
            onDismissRequest = { editingWord = null },
            title = { Text("编辑释义") },
            text = {
                Column {
                    Text(
                        text  = "为「${word.english}」添加中文释义",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = editText,
                        onValueChange = { editText = it },
                        placeholder   = { Text("输入中文释义") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editText.trim()
                        if (trimmed.isNotEmpty()) onUpdateChinese(word.id, trimmed)
                        editingWord = null
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { editingWord = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun WordCard(
    word: Word,
    onEditChinese: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Text(
                text  = word.english,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (word.chinese == "—" && onEditChinese != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    IconButton(
                        onClick  = onEditChinese,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "编辑释义",
                            modifier           = Modifier.size(16.dp),
                            tint               = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                Text(
                    text  = word.chinese,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── 预览 ──────────────────────────────────────────────────────────────────────

@Preview(name = "有数据", showBackground = true)
@Preview(name = "暗色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DayDetailScreenPreview() {
    拍词Theme {
        DayDetailScreen(
            date  = "2026-02-25",
            words = listOf(
                Word(id = 1, english = "apple",   chinese = "苹果"),
                Word(id = 2, english = "book",    chinese = "书"),
                Word(id = 3, english = "unknown", chinese = "—"),
            ),
            onBack = {},
        )
    }
}
