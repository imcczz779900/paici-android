## 阶段二计划

### 约束（继承自 SPEC.md）
- 保持 Jetpack Compose + Material 3
- 不引入 Hilt / MVI / 后端
- 每次改动小步可运行

---

### 实现顺序

#### 步骤 1 — 数据模型：Word 加 createdAt 字段

在 `Word` 中新增 `createdAt: Long`（Unix 时间戳，毫秒），在 `addWord` 时自动写入 `System.currentTimeMillis()`。
这是词汇本日期分组的数据基础，必须最先完成。

影响文件：`data/Word.kt`、`data/WordRepository.kt`、`AppViewModel.kt`

---

#### 步骤 2 — 导航重构：BottomNavigation（3 Tab）

将现有 NavHost 改为带底部导航栏的结构，3 个 Tab：

| Tab | 路由 | 说明 |
|---|---|---|
| 首页 Home | `home` | 保留，重设计见步骤 3 |
| 词汇本 Wordbook | `wordbook` | 原 WordList，重设计见步骤 4 |
| 设置 Settings | `settings` | 新增，见步骤 5 |

- Camera 不是 Tab，从首页「拍照识词」按钮跳转，返回后回到首页
- 不要 Explore，不要分类网格

---

#### 步骤 3 — 首页重设计

布局（从上到下）：
- 顶部：今日日期（例如"2月25日，星期三"）+ "你好" + 一句引导文案
- 中间：插画/图标占位（使用 Material Icons，不引入图片资源）
- 主按钮：显眼的「拍照识词」
- 底部统计：「已收录 X 个单词」（从 `viewModel.words.size` 读取）

---

#### 步骤 4 — 词汇本重设计（日期分组）

列表层（Wordbook）：
- 按 `createdAt` 日期分组，每组显示：
  - 日期标题（例如"2月25日"）
  - 文字占位缩略图：取当天第一个识别的英文单词（`words.first().english`）
  - 当天新增词数（例如"3 个单词"）
- 点击某天 → 进入 DayDetail 页
- 空状态：文案 + 「去拍照识词」引导按钮

详情层（DayDetail）：
- 显示当天的单词列表（复用 WordCard）
- 支持返回词汇本
- 新增路由：`wordbook/{date}`（date 用 `yyyy-MM-dd` 格式）

> `imageUrl` 当前阶段仍为 null，缩略图用英文单词文字代替，不引入图片存储。

---

#### 步骤 5 — 设置页（占位）

卡片分组列表，两组：

**语言**
- 母语：中文
- 学习语言：English

**反馈**
- 意见反馈（占位，点击无动作）
- 给我们评分（占位，点击无动作）

不做订阅 / 付费模块。
