# 拍词

用手机拍一张照片，识别主体物体，自动展示英文单词、IPA 音标、中文释义，并可一键朗读和加入个人词汇本。

---

## 功能

| 功能 | 说明 |
|------|------|
| 📷 拍照识词 | 对准物体拍照，ML Kit 自动识别主体标签 |
| ✂️ 主体抠图 | MediaPipe 离线分割，高亮主体并叠加白色描边与光晕 |
| 🔤 英文 + 音标 | 展示英文单词与 IPA 音标（约 130 个常见标签） |
| 🔊 发音 | 点击喇叭按钮，系统 TTS 朗读英文 |
| 🀄 中文释义 | 内置中文映射表，无需网络 |
| 📖 词汇本 | 加入词本后本地持久化，随时复习 |

---

## 截图


<img width="1080" height="2400" alt="Screenshot_20260226_013045" src="https://github.com/user-attachments/assets/c60acbef-d133-40c5-b029-8d0405676f95" />

<img width="1080" height="2400" alt="Screenshot_20260226_013218" src="https://github.com/user-attachments/assets/f36bcfa5-08ad-4436-993d-ef0b6b3f3675" />

<img width="1080" height="2400" alt="Screenshot_20260226_013229" src="https://github.com/user-attachments/assets/eb3d27be-962f-44ce-93cc-bd1b8cad9f6c" />

---
## 技术栈

| 层次 | 技术 |
|------|------|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| 相机 | CameraX 1.3.4 |
| 图像识别 | ML Kit Image Labeling 17.0.9 |
| 主体分割 | MediaPipe tasks-vision 0.10.29 + `selfie_segmenter.tflite`（离线） |
| 发音 | Android TextToSpeech（系统内置） |
| 数据存储 | JSON 文件持久化 |
| 导航 | Navigation Compose 2.8.0 |

---

## 运行要求

- Android 8.0（API 26）及以上
- 需要相机权限
- **无需网络**（识别、分割、发音、释义全部离线）

---

## 构建步骤

```bash
# 1. 克隆仓库
git clone https://github.com/imcczz779900/paici-android.git
cd paici-android

# 2. 用 Android Studio 打开，或直接命令行构建
./gradlew assembleDebug

# 3. 安装到设备
./gradlew installDebug
```

> 模型文件 `selfie_segmenter.tflite` 已包含在 `app/src/main/assets/` 中，无需额外下载。
> 若分割失败（主体占比 < 5%）会自动 fallback 显示原图，不影响其他功能。

---

## 项目结构

```
app/src/main/java/com/paici/app/
├── data/
│   ├── SubjectSegmentationProcessor.kt   # MediaPipe 抠图
│   ├── TranslationService.kt             # 在线翻译（备用）
│   ├── Word.kt                           # 数据模型
│   └── WordRepository.kt                 # JSON 持久化
├── navigation/
│   └── AppNavigation.kt
├── ui/
│   ├── camera/
│   │   ├── CameraScreen.kt               # 拍照 + 识别 + 结果页
│   │   ├── LabelToChineseMap.kt          # 英文 → 中文映射
│   │   └── LabelToPhoneticMap.kt         # 英文 → IPA 映射
│   ├── home/
│   ├── wordbook/
│   └── theme/
└── AppViewModel.kt
```

---

## 识别结果页流程

```
拍照
 └→ Processing（识别中…）
     ├→ 识别成功 → Result 全屏页
     │    ├ 抠图展示（或 fallback 原图）
     │    ├ 英文 / 音标 / 中文
     │    ├ 发音按钮
     │    └ [重拍]  [✓ 加入词本]  [放弃]
     └→ 无结果 / 失败 → 返回取景器
```

---

## License

[MIT](LICENSE)
