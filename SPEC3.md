# SPEC3：主体抠图 + 全屏识别结果页 + 发音 + 音标

## 目标

将 CameraScreen 的识别结果展示从"底部浮层 + 多候选词列表"升级为
"全屏确认页 + 主体抠图高亮 + 三按钮决策区 + 发音按钮 + 音标展示"。

---

## 架构决策

| 决策点 | 方案 | 备注 |
|--------|------|------|
| 抠图引擎 | MediaPipe `tasks-vision:0.10.29` + `selfie_segmenter.tflite` | float16，244 KB，打包进 APK，完全离线 |
| 结果页实现 | CameraScreen 内部状态机，不新建 Navigation Route | 避免 Bitmap 序列化问题 |
| 候选词数量 | 只取 top-1 标签 | 原 top-3 列表废弃 |
| 模型放置 | `app/src/main/assets/selfie_segmenter.tflite` | 已放入 |
| 发音引擎 | Android 内置 `TextToSpeech`（`android.speech.tts`） | 无额外依赖，完全离线 |
| 音标数据 | 硬编码映射表 `LabelToPhoneticMap.kt` | 与 LabelToChineseMap 同结构，覆盖所有可识别标签 |

---

## 状态机

```kotlin
sealed class CamState {
    object Viewfinder : CamState()
    object Processing : CamState()
    data class Result(
        val capturedBitmap: Bitmap,
        val displayBitmap: Bitmap?,  // null → fallback（显示原图）
        val english: String,
        val chinese: String,
        val phonetic: String,        // e.g. "/ˈæpəl/"，查不到时为 "/ — /"
    ) : CamState()
}
```

流转：`Viewfinder` → 按拍照 → `Processing` → 完成 → `Result`

- Result 中：重拍 → `Viewfinder`；加入词本 → `onWordAdded` + `onBack`；放弃 → `onBack`
- 识别无结果 / 失败 → 回到 `Viewfinder`（不切换 Result）

---

## 文件清单

| 文件 | 操作 | 状态 |
|------|------|------|
| `gradle/libs.versions.toml` | 新增 mediapipe 版本 + library 条目 | ✅ 完成 |
| `app/build.gradle.kts` | 新增 `implementation(libs.mediapipe.tasks.vision)` | ✅ 完成 |
| `app/src/main/assets/selfie_segmenter.tflite` | 手动下载放入（float16，244 KB） | ✅ 完成 |
| `app/src/main/java/com/paici/app/data/SubjectSegmentationProcessor.kt` | 新建 | ✅ 完成 |
| `app/src/main/java/com/paici/app/ui/camera/LabelToChineseMap.kt` | 不改动，继续复用 | — |
| `app/src/main/java/com/paici/app/ui/camera/LabelToPhoneticMap.kt` | 新建，IPA 音标映射表 | ✅ 完成 |
| `app/src/main/java/com/paici/app/ui/camera/CameraScreen.kt` | 大幅重写（含发音 + 音标） | ✅ 完成 |

---

## 依赖变更

**`gradle/libs.versions.toml`** 新增：
```toml
[versions]
mediapipeTasksVision = "0.10.29"

[libraries]
mediapipe-tasks-vision = { group = "com.google.mediapipe", name = "tasks-vision", version.ref = "mediapipeTasksVision" }
```

**`app/build.gradle.kts`** 新增：
```kotlin
implementation(libs.mediapipe.tasks.vision)
```

`TextToSpeech` 无需新增依赖，属于 Android SDK 标准库（`android.speech.tts`）。

---

## SubjectSegmentationProcessor

路径：`app/src/main/java/com/paici/app/data/SubjectSegmentationProcessor.kt`

**关键 API 说明（0.10.x 实际用法）：**
- Options 类是 `ImageSegmenter.ImageSegmenterOptions`（中间类），**不是** 顶层的 `ImageSegmenterOptions`
- MPImage → Bitmap 转换使用 `BitmapExtractor.extract(mpImage)`，**不是** `mpImage.asBitmap()`

**处理流程：**
1. 初始化 `ImageSegmenter`（IMAGE 模式，category mask 输出，confidence mask 关闭）
2. 模型文件缺失或初始化异常 → `segmenter = null`，后续调用全部返回 null（fallback）
3. `suspend fun process(bitmap):`
   - 缩放到 256×256（模型输入尺寸）
   - `segmenter.segment(BitmapImageBuilder(scaled).build())`
   - `BitmapExtractor.extract(result.categoryMask().get())` 得到 256×256 mask bitmap
   - 放大 mask 到原图尺寸；遍历像素，`Color.red(pixel) > 0` 为主体（category=1），填白色；其余透明
   - PorterDuff DST_IN 抠出前景 → `fgBitmap`（透明背景）
   - 前景像素占比 < 5% → 返回 null（fallback）
   - 从 fgBitmap 生成白色剪影（`PorterDuffColorFilter(WHITE, SRC_IN)`）
   - 合成 displayBitmap：glow（`BlurMaskFilter 24f, alpha 180`）+ outline（`BlurMaskFilter 6f, alpha 230`）+ fgBitmap
4. 任意异常 → catch → 返回 null

---

## 音标：LabelToPhoneticMap

路径：`app/src/main/java/com/paici/app/ui/camera/LabelToPhoneticMap.kt`

结构与 `LabelToChineseMap.kt` 完全一致——Key 为小写英文，与 `label.text.lowercase()` 对应；
Value 为 IPA 字符串（带斜杠），查不到时调用方显示 `"/ — /"`。

```kotlin
val labelToPhonetic: Map<String, String> = mapOf(
    "apple"      to "/ˈæpəl/",
    "banana"     to "/bəˈnɑːnə/",
    "dog"        to "/dɒɡ/",
    "cat"        to "/kæt/",
    // …覆盖 LabelToChineseMap 中所有词条
)
```

覆盖范围与 `LabelToChineseMap` 完全对齐（约 130 词），确保同一个标签
在中文映射里有就在音标映射里也有。

---

## 发音：TextToSpeech 接入

**无新依赖**，使用 `android.speech.tts.TextToSpeech`。

**在 CameraScreen 中的初始化：**
```kotlin
var ttsReady by remember { mutableStateOf(false) }
val tts = remember {
    TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
        }
    }
}
DisposableEffect(Unit) {
    onDispose { tts.shutdown() }
}
```

TTS 初始化是异步的，`ttsReady` 未置 true 前发音按钮保持 disabled。
不单独设置语言（默认继承系统语言），如需强制英音可在 `status == SUCCESS` 时加：
```kotlin
tts.language = Locale.US
```

**发音调用：**
```kotlin
tts.speak(state.english, TextToSpeech.QUEUE_FLUSH, null, "word_tts")
```

**Result 页按钮改动（原占位 → 可用）：**
```kotlin
// 之前
IconButton(onClick = { /* 发音占位 */ }) { ... }

// 之后
IconButton(
    onClick = { tts.speak(state.english, TextToSpeech.QUEUE_FLUSH, null, "word_tts") },
    enabled = ttsReady,
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.VolumeUp,  // 同时修复 deprecation warning
        contentDescription = "发音",
        tint = if (ttsReady) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
    )
}
```

---

## CameraScreen 完整改动（含发音 + 音标）

**在现有重写基础上，追加以下改动：**

### capture() 新增音标查询

```kotlin
val phonetic = labelToPhonetic[english] ?: "/ — /"
camState = CamState.Result(
    capturedBitmap = bitmap,
    displayBitmap  = displayBitmap,
    english        = english,
    chinese        = chinese,
    phonetic       = phonetic,   // ← 新增
)
```

### Result 页文字区（替换硬编码占位）

```kotlin
// 之前
Text("/ — /", style=bodyMedium, color=white.copy(0.5f))

// 之后
Text(state.phonetic, style=bodyMedium, color=white.copy(0.5f))
```

### 新增 TTS 相关状态与 DisposableEffect

见上方"发音：TextToSpeech 接入"节。`tts` 与 `ttsReady` 声明在
`CameraScreen` Composable 顶层，三个状态分支共享同一个实例。

---

## 模型文件

下载地址（float16 版，244 KB）：
```
https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/1/selfie_segmenter.tflite
```

放置路径：
```
app/src/main/assets/selfie_segmenter.tflite
```

> 未放入时：`SubjectSegmentationProcessor` 初始化捕获异常，`segmenter = null`，
> 所有 `process()` 调用返回 null，Result 页 fallback 显示原图，不崩溃。

---

## 编译状态

`./gradlew assembleDebug` → **BUILD SUCCESSFUL，零 warning**

剩余 warning（原有代码，不在本 SPEC 范围）：
- `AppNavigation.kt`：`Icons.Filled.MenuBook` 建议换 AutoMirrored 版本

---

## 待办

- [x] 下载并放入 `selfie_segmenter.tflite`（float16，244 KB）
- [x] 新建 `LabelToPhoneticMap.kt`（覆盖 ~130 词，IPA 格式）
- [x] `CamState.Result` 新增 `phonetic` 字段，capture() 中查表赋值
- [x] Result 页音标文字改为 `state.phonetic`（替换硬编码 `"/ — /"`）
- [x] `CameraScreen` 接入 `TextToSpeech`，发音按钮从占位变为可用
- [x] 修复 `Icons.Filled.VolumeUp` deprecation warning
- [ ] 真机测试：抠图效果、fallback 路径、发音、音标展示、三按钮交互
