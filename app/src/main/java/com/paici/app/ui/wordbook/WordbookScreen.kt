package com.paici.app.ui.wordbook

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paici.app.data.Word
import com.paici.app.ui.theme.拍词Theme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordbookScreen(
    words: List<Word>,
    onDayClick: (date: String) -> Unit,
    onCameraClick: () -> Unit,
) {
    val grouped = remember(words) {
        words
            .groupBy { word ->
                Instant.ofEpochMilli(word.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .entries
            .sortedByDescending { it.key }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("词汇本") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        if (grouped.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text  = "还没有单词",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text  = "拍一张照片开始学习吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onCameraClick) {
                        Icon(
                            imageVector        = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("去拍照识词")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier        = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = grouped,
                    key   = { entry -> entry.key.toString() },
                ) { (date, dayWords) ->
                    DayCard(
                        date    = date,
                        words   = dayWords,
                        onClick = { onDayClick(date.toString()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    date: LocalDate,
    words: List<Word>,
    onClick: () -> Unit,
) {
    val dateDisplay = "${date.monthValue}月${date.dayOfMonth}日"
    val thumbnail   = words.first().english

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            // 缩略图：当天第一个英文单词
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = thumbnail,
                    style     = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color     = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis,
                    modifier  = Modifier.padding(horizontal = 6.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = dateDisplay,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = "${words.size} 个单词",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier           = Modifier.size(16.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── 预览 ──────────────────────────────────────────────────────────────────────

private val previewWords = listOf(
    Word(id = 1, english = "apple",  chinese = "苹果", createdAt = System.currentTimeMillis()),
    Word(id = 2, english = "book",   chinese = "书",   createdAt = System.currentTimeMillis() - 86_400_000L),
    Word(id = 3, english = "camera", chinese = "相机", createdAt = System.currentTimeMillis() - 86_400_000L),
)

@Preview(name = "有数据 - 亮色", showBackground = true)
@Preview(name = "有数据 - 暗色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WordbookScreenPreview() {
    拍词Theme {
        WordbookScreen(words = previewWords, onDayClick = {}, onCameraClick = {})
    }
}

@Preview(name = "空状态", showBackground = true)
@Composable
private fun WordbookScreenEmptyPreview() {
    拍词Theme {
        WordbookScreen(words = emptyList(), onDayClick = {}, onCameraClick = {})
    }
}
