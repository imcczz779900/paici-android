# 拍词 (Paici) – MVP Specification

## 1. 项目目标

"拍词"是一个面向中文学习者的 Android App，用于：
通过 **拍照识别现实物体**，快速获得对应的 **英文单词**，
并将其转化为可复习的单词卡片。

当前阶段目标：
- 搭建清晰、可扩展的 App 架构
- 完成从「首页 → 拍照 → 显示单词 → 加入词本」的最小闭环
- 重点放在 **学习体验与 UI**，而非识别算法本身

---

## 2. 当前技术栈（固定约束）

- Android
- Kotlin
- Jetpack Compose
- Material 3（支持暗色模式）
- Navigation Compose
- Minimum SDK: 26
- 本地数据存储（Room / DataStore，后续接入）
- CameraX（`androidx.camera:camera-camera2`、`camera-lifecycle`、`camera-view`）
- ML Kit Image Labeling（`com.google.mlkit:image-labeling`）

> ⚠️ 禁止在未明确要求的情况下：
> - 引入后端服务
> - 引入账号系统
> - 引入复杂第三方架构（如 Hilt / MVI）

---

## 3. 功能范围（MVP）

### 3.1 首页（Home）

- App 标题：拍词
- 主操作按钮：
    - 「拍照识词」
    - 「我的单词」
- 不显示任何识别结果或列表

---

### 3.2 拍照识词（Camera / Recognize）

权限：需在 `AndroidManifest.xml` 声明 `android.permission.CAMERA`，并在运行时动态申请。

第一阶段：
- 使用 CameraX 打开相机
- 拍照并获得一张 Bitmap
- 使用成熟方案（如 ML Kit Image Labeling）获取 1–3 个英文标签

中文释义策略：
- ML Kit 只返回英文标签，**不提供中文**
- 第一阶段使用硬编码映射表（`Map<String, String>`），覆盖常见物体（50–100 词）
- 映射表中找不到的词，`chinese` 字段显示空字符串或「—」占位
- 第二阶段再考虑接入词典 API

显示内容：
- 英文单词
- 中文释义
- 一个「加入我的单词」按钮

---

### 3.3 我的单词（Word List）

- 显示已保存的单词列表
- 每个单词展示：
    - 英文
    - 中文
- 支持空状态展示
- 暂不实现复杂搜索 / 分组 / 复习算法

---

## 4. 数据模型（初版）

```kotlin
Word(
    id: Int,
    english: String,        // 英文单词
    chinese: String,        // 中文释义
    imageUrl: String? = null  // 拍照来源的图片路径（可选，第一阶段可为 null）
)
```

- 第一阶段允许使用内存或假数据
- 第二阶段再替换为 Room

---

## 5. 页面与导航

- `HomeScreen`
- `CameraScreen`（识别结果在此页面内以状态展示，**不单独建 ResultScreen**）
- `WordListScreen`

- 使用 Navigation Compose 管理页面跳转
- Screen 定义使用 sealed class

---

## 6. 非目标（明确不做）

以下功能明确不在当前阶段实现：

- 登录 / 注册
- 云同步
- 多语言 UI
- 自定义模型 / 训练
- 广告 / 订阅

---

## 7. 代码风格与原则

- UI 与逻辑分离（即使是简单版本）
- 尽量使用 stateless composable
- 使用 lambda 传递事件，不在 UI 中直接操作 NavController
- 每次改动尽量小而可运行

---

## 8. 未来可扩展方向（仅作为参考）

- 单词复习（间隔重复）
- OCR + 场景文字识别
- 多词分类（物品 / 食物 / 动物）
- 接入在线词典 API
