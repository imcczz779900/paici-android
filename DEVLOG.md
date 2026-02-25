# 拍词 开发日志

记录每次开发会话中完成的工作，按时间倒序排列。

---

## 2026-02-25

### SPEC.md 审查与修复

审查了 SPEC.md 的质量，发现并修复了以下问题：

- **代码块格式损坏**：第 4 节缺少闭合 ` ``` `，导致后续所有 Markdown 标题和列表在渲染时全部失效。已补上。
- **字段名不一致**：SPEC 中写的是 `text` / `meaning`，而 `Word.kt` 实际实现为 `english` / `chinese`。已将 SPEC 改为与代码一致。
- **删除教程内容**：原文末尾的"你接下来该怎么做"和"你现在这个状态的评价"属于写给用户的教程说明，不属于技术规格文档，已整体删除。
- **ResultScreen 方案明确**：3.2 节和导航列表对 ResultScreen 是否独立存在描述不一致，明确为"识别结果在 CameraScreen 内以状态展示，不单独建 ResultScreen"。

### SPEC.md 补充遗漏内容

在审查后发现规格文档存在实现层面的空白，追加了以下内容：

- **第 2 节**：加入 CameraX（`camera-camera2`、`camera-lifecycle`、`camera-view`）和 ML Kit Image Labeling 的 Gradle artifact 名称。
- **第 3.2 节**：明确需要在 `AndroidManifest.xml` 声明 `CAMERA` 权限并在运行时动态申请。
- **第 3.2 节**：新增"中文释义策略"段落，说明 ML Kit 只返回英文标签，第一阶段用硬编码映射表（50–100 词），找不到时显示"—"占位。
- **第 4 节**：数据模型补入 `imageUrl: String? = null` 字段，与已实现的 `Word.kt` 对齐。

---

### CameraScreen 完整实现

完成了从首页→拍照→显示单词→加入词本的最小闭环。

#### 新增文件

**`AppViewModel.kt`**
- 持有全局内存单词列表（`mutableStateListOf<Word>`）
- 提供 `addWord(english, chinese)`，自动去重（不区分大小写）和自增 ID
- 生命周期与 Activity 绑定，屏幕旋转不丢失数据

**`ui/camera/LabelToChineseMap.kt`**
- 约 130 个常见英文标签→中文的硬编码映射表
- 覆盖食物、动物、家具、电子产品、衣物、交通、自然等类别
- Key 为小写英文，与 `label.text.lowercase()` 对应

**`ui/camera/CameraScreen.kt`**
- 启动时自动申请相机权限（`ActivityResultContracts.RequestPermission`）
- 权限通过后通过 `LaunchedEffect` 将 CameraX 绑定到 Activity 生命周期
- 拍照流程：`ImageCapture.takePicture()` → `ImageProxy.toBitmap()` → ML Kit `ImageLabeling`
- ML Kit 结果取置信度 ≥ 65% 的前 3 个标签，查映射表得中文
- 底部展示识别结果列表，每项有"+ 加入"按钮；点击后变为"已加入"文字（本次会话去重）
- 拍照期间显示 `CircularProgressIndicator`，防止重复点击

#### 修改文件

| 文件 | 改动内容 |
|---|---|
| `gradle/libs.versions.toml` | 新增 `camerax = "1.3.4"`、`mlkitImageLabeling = "17.0.9"` 及对应 library 条目 |
| `app/build.gradle.kts` | 添加 CameraX 三件套和 ML Kit 的 `implementation` |
| `AndroidManifest.xml` | 声明 `android.permission.CAMERA` 和 `uses-feature` |
| `AppNavigation.kt` | 新增 `Screen.Camera`，函数签名加入 `AppViewModel` 参数，`WordListScreen` 改用 ViewModel 的真实单词列表 |
| `MainActivity.kt` | 用 `by viewModels()` 创建 `AppViewModel`，传入 `AppNavigation` |

---

### Room 数据库接入 + 词典 API 回填

将内存存储替换为 SQLite（通过 Room），并为映射表未收录的词异步获取中文释义。

#### 新增文件

**`data/WordDao.kt`**
- `getAllWords(): Flow<List<Word>>`：返回实时流，Room 每次写入自动推送新快照
- `insertWord(word): Long`：返回新行 rowId，重复（english 唯一索引）时返回 -1
- `updateChinese(id, chinese)`：供词典 API 回填使用

**`data/AppDatabase.kt`**
- Room 数据库单例，持有 `WordDao`
- 数据库名：`paici_database`，version 1

**`data/TranslationService.kt`**
- 使用 MyMemory 免费翻译 API（无需 API Key）
- `translateToChineseOrDash(english)`：在 `Dispatchers.IO` 协程中执行 HTTP 请求，解析 JSON，异常时返回"—"
- 不引入任何新依赖，使用 Android 内置的 `HttpURLConnection` 和 `org.json.JSONObject`

#### 修改文件

| 文件 | 改动内容 |
|---|---|
| `libs.versions.toml` | 新增 `ksp = "2.0.21-1.0.27"`、`room = "2.6.1"` 及对应 library / plugin 条目 |
| `app/build.gradle.kts` | 新增 KSP 插件、`room-runtime`、`room-ktx`、`ksp(room-compiler)` |
| `AndroidManifest.xml` | 新增 `INTERNET` 权限（词典 API 需要） |
| `data/Word.kt` | 加上 `@Entity`、`@PrimaryKey(autoGenerate = true)`、`@Index(unique = true)`；id 默认值改为 0；移除不再需要的 `sampleWords` |
| `AppViewModel.kt` | 改为 `AndroidViewModel`，`words` 从 `mutableStateListOf` 改为 `StateFlow<List<Word>>`（由 Room Flow + `stateIn` 驱动）；`addWord` 在 `viewModelScope` 中执行插入，若中文为"—"则后台调用 `TranslationService` 并回填 |
| `navigation/AppNavigation.kt` | `WordListScreen` 的 words 参数改为 `viewModel.words.collectAsState()` |
| `ui/wordlist/WordListScreen.kt` | 移除 `sampleWords` 导入，默认参数改为 `emptyList()`，预览改用本地定义的列表 |

#### 词典 API 回填流程

```
用户点击"+ 加入"
  → ViewModel.addWord(english, chinese)
    → Room 插入（返回 rowId）
    → if chinese == "—"
        → TranslationService.translate(english)  [Dispatchers.IO]
          → 成功：dao.updateChinese(rowId, translated)
          → 失败：保留"—"，不重试
    → WordListScreen 通过 Flow 自动刷新
```

---

### 手动编辑中文释义

为单词列表中中文为"—"的单词提供手动编辑入口。

#### 修改文件

| 文件 | 改动内容 |
|---|---|
| `AppViewModel.kt` | 新增 `updateChinese(id, chinese)`，调用 `dao.updateChinese` |
| `ui/wordlist/WordListScreen.kt` | 新增 `onUpdateChinese` 参数；`WordCard` 当 `chinese == "—"` 时右侧显示红色"—"和铅笔图标；点击弹出 `AlertDialog`，含 `OutlinedTextField` 供输入；确认后回调 `onUpdateChinese` |
| `navigation/AppNavigation.kt` | `WordListScreen` 新增 `onUpdateChinese = { id, chinese -> viewModel.updateChinese(id, chinese) }` |

#### 交互流程

```
单词列表显示 "—"（红色）+ 铅笔图标
  → 用户点击铅笔图标
    → 弹出对话框「为「xxx」添加中文释义」
      → 用户输入中文，点击「确定」
        → onUpdateChinese(id, chinese)
          → AppViewModel.updateChinese(id, chinese)
            → dao.updateChinese(id, chinese)
              → Room Flow 推送新快照 → 列表自动刷新
```

---

## 待完成

（暂无）
