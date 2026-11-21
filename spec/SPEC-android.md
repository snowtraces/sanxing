# 三省 (SanSheng) · 每日复盘与反省 Android App 综合规格说明书 (SPEC)

状态：待评审 (Draft for Review) · 遵循 Spec-Driven Development 规范

---

## 1. 目标与设计理念 (Objective & Vision)

### 1.1 背景与宗旨
「曾子曰：吾日三省吾身——为人谋而不忠乎？与朋友交而不信乎？传不习乎？」
本项目旨在打造一款具有**东方古雅水墨美学**与**现代AI整理能力**的 Android 每日反省复盘应用。
用户通过极简、无压力的文字或语音记录日常偏差，AI 智能提炼为结构化“事·省·悟”，并在主页与“坐禅画像”中持续内化反思，帮助用户破除执念、改进行为习惯。

### 1.2 品牌与命名决策
- **正式推荐命名**：**「三省」**（副标题：日省吾身，知行合一）。
- **心智定位**：非普通记事本，而是一座随身携带的“心性修持道场”。

### 1.3 水墨美学与视觉体系 (Ink Aesthetic Design System)
拒绝千篇一律的扁平化与 AI 模板感，采用极简水墨意境与克制的留白（Negative Space）：
1. **配色方案 (Paper & Ink Palette)**：
   - **宣纸底色 (Xuan Paper)**: Light `#F7F5F0` / Dark (夜间墨砚) `#1C1B1A`
   - **卡片底色 (Rice Card)**: Light `#FFFFFF` 带微妙暖灰底色与羽化软阴影 / Dark `#252422`
   - **墨分五色 (Ink Scale)**:
     - 焦墨 (浓黑·主标题): `#181818` / Dark `#EDEDED`
     - 浓墨 (正文内容): `#2C2C2C` / Dark `#D2D0CD`
     - 重墨 (次要文本/时间): `#5A5957` / Dark `#9E9C98`
     - 淡墨 (分割线/边框/气泡未点击态): `#E2DFD8` / Dark `#3A3835`
     - 清墨 (极淡背景与水痕): `#F0EDE6` / Dark `#2F2D2A`
   - **点睛印章 (Vermilion Seal Accent)**:
     - 朱砂红 `#B3392F`（用于核心确认按钮、重要标签、印章图标或高频警示点缀）。
2. **排版与留白 (Typography & Breathing Room)**：
   - 界面整体保留 24dp~32dp 的充裕页边距，大卡片之间间距 16dp，避免视觉拥挤。
   - 字体层次分明，字间距略微加宽（+0.5sp），营造沉静舒缓的阅读体验。
3. **动效节奏 (Motion)**：
   - 页面切换采用柔和的水墨淡入淡出（Fade Through 250ms）。
   - 气泡互动采用微浮动弹性动效与渐变褪色（消散感）。

---

## 2. 核心架构与功能规格 (System Architecture & Modules)

系统由 5 个核心业务模块组成：

```
                    ┌─────────────────────────┐
                    │    ui (Jetpack Compose) │
                    └────────────┬────────────┘
                                 │
         ┌───────────────┬───────┴───────┬──────────────┐
         ▼               ▼               ▼              ▼
   ┌───────────┐   ┌───────────┐   ┌───────────┐  ┌───────────┐
   │  journal  │◄──┤  review   │   │ portrait  │  │  backup   │
   │ (数据中心) │   │ (首页/历史)│   │ (坐禅画像) │  │ (导出导入) │
   └─────▲─────┘   └───────────┘   └─────┬─────┘  └─────┬─────┘
         │                               │              │
         └───────────────┬───────────────┴──────────────┘
                         ▼
                  ┌─────────────┐
                  │  ai-assist  │ (LLM 客户端与智能提示词引擎)
                  └─────────────┘
```

### 2.1 模块详细规格

#### M1: `journal` (核心复盘数据与录入)
- **数据结构 `Reflection`**:
  - `id`: UUID 字符串
  - `occurredAtMillis`: Long (发生时间戳，默认为当前时间，支持回溯)
  - `title`: String (主题，大白话，由 AI 生成或手动修改，< 20 字)
  - `categoryId`: String (关联分类，默认值域：学习、方法论、情绪、日常、工作、策略、交际；支持用户增删改)
  - `description`: String (事件真实起因及来龙去脉)
  - `reflection`: String (「省」：核心反思，明确指出**不应该**怎样、何种认知偏差或过失)
  - `action`: String (「悟」：改进法门，明确指出下一次**应该**怎样、行事准则)
  - `updatedAtMillis`: Long
- **录入交互流**:
  1. 支持纯文本键盘录入与系统语音识别 (Speech-to-Text)。
  2. 点击「AI 整理」：提取并结构化填入【主题、分类、问题描述、省、悟】输入框。
  3. AI 失败或离线时，用户可直接手动填写，无任何阻断。
  4. 严格校验：所有字段非纯空白时方可保存，保存后提供微震动反馈与纸质折叠入库动效。

#### M2: `review` (首页禅心悟道与历史回顾)
- **主屏 (Home Tab)**:
  - 核心展示：全屏水墨大卡片，随机抽取历史一条**「悟」**（字体优雅、配朱砂印章）。若无历史记录，展示引导式预置金句。
  - 交互：点击主屏卡片可原地展开为完整复盘详情大卡片；悬浮快捷「笔墨录入」FAB 按钮。
- **历史列表 (History Tab)**:
  - 顶部搜索栏：支持按文本模糊检索（标题、问题、省、悟）。
  - 分类横向滚轴：快速过滤（全部分类、工作、情绪等）。
  - 小卡片瀑布流/列表：突出展示时间、分类标签、一句话「省」与「悟」。
  - 点击小卡片平滑过渡进入大卡片详情页，支持重新编辑或确认删除。

#### M3: `portrait` (水墨坐禅与消障画像)
- **视觉主体**:
  - 画面居中绘制极简抽象水墨小和尚打坐形象（矢量 Canvas 或水墨灰度图形）。
  - 周围悬浮 3~6 个大小不一的气泡，代表当前最具代表性或高频的「省」。
- **气泡规则与算法**:
  - 气泡文本：精炼为 2~5 个字（如“急躁打断”、“熬夜内耗”、“盲目应承”）。
  - 权重算法：本地基于 Jaccard 相似度初筛 + （可选）LLM 聚类合并同义反思。高频重复的问题气泡尺寸更大、墨色更重。
  - **点击消障（三省破执）交互**:
    - 第 1 次点击：气泡墨色变淡 40%（初省其过）；
    - 第 2 次点击：气泡墨色变淡 70% 并带虚线墨迹（再省知过）；
    - 第 3 次点击：气泡化作轻烟动画消散，标记为“已破除”，不再在画像中常驻。状态持久化存储。

#### M4: `ai-assist` (LLM 客户端与语义提炼)
- **接口兼容**: 标准 OpenAI 兼容协议 (`POST /chat/completions`)，支持各类大模型（OpenAI, DeepSeek, Claude 代理等）。
- **配置项**:
  - `baseUrl`: 默认 `https://api.openai.com/v1`
  - `apiKey`: 密文存储于 Android Keystore，界面显示掩码
  - `modelName`: 用户自定义，如 `deepseek-chat`, `gpt-4o-mini`
- **提炼 Prompt 规范**:
  - 输出严格 JSON 格式：`{"title": "...", "categoryId": "...", "description": "...", "reflection": "不应该...", "action": "应该..."}`
  - 强制遵循用户真实语义，杜绝虚构，语言简练通俗。

#### M5: `backup` (隐私与数据自持)
- **本地优先 (Local-First)**: 数据仅存储在设备本地 SQLite，不主动外传。
- **备份导出/恢复**:
  - 通过 Android SAF (Storage Access Framework) 导出为加密/纯文本 JSON 备份文件。
  - 支持全量覆盖或合并恢复。

---

## 3. 技术栈与命令基线 (Tech Stack & Commands)

| 领域 | 选型 | 版本/规范 |
|---|---|---|
| OS / Platform | Android | `minSdk 24`, `targetSdk 34`, `compileSdk 34` |
| 语言与运行时 | Kotlin / JVM | Kotlin 1.9.24 / JDK 21 |
| 构建系统 | Gradle / AGP | Gradle 8.7 / AGP 8.4.2 |
| UI 框架 | Jetpack Compose | Compose BOM 2023.01.00 / Compiler 1.5.14 |
| 依赖注入 | 纯函数式单例 / 依赖注入 | 避免过度工程，采用干净的 ServiceLocator 架构 |
| 本地存储 | 本机 SQLite / Room 兼容规范 | 事务保证与外键关联 |
| 密钥安全 | Android Keystore | MasterKey + EncryptedSharedPreferences |
| 网络请求 | OkHttp 4.x / Kotlinx Serialization | 纯净 JSON 解析，零多余膨胀 |

### 核心构建命令
- 构建 Debug APK: `.\gradlew.bat :app:assembleDebug`
- 执行单元测试: `.\gradlew.bat :app:testDebugUnitTest`
- 执行代码检查: `.\gradlew.bat :app:lintDebug`
- 安装到设备: `.\gradlew.bat :app:installDebug`

---

## 4. 项目结构与代码规范 (Project Structure & Style)

```
app/src/main/java/com/snowtraces/sanxing/
├── MainActivity.kt                // 宿主 Activity 与根导航
├── journal/                       // 复盘数据模型、存储与 DAO
│   ├── Journal.kt
│   └── JournalStore.kt
├── review/                        // 历史检索与随机悟逻辑
│   └── History.kt
├── portrait/                      // 坐禅与气泡消障算法
│   ├── Themes.kt
│   └── BubbleClustering.kt
├── aiassist/                      // LLM 客户端与结构化提示词
│   ├── AiClient.kt
│   └── PromptTemplates.kt
├── backup/                        // JSON 备份与 SAF 文件管理
│   └── Backup.kt
└── ui/                            // 水墨风格 Compose 界面组件
    ├── Ink.kt                     // 水墨调色板、纸张质感与排版规范
    ├── JournalPages.kt            // 录入与大卡片详情
    ├── ReviewPages.kt             // 首页悟道卡片与历史列表
    ├── PortraitPages.kt           // 坐禅小和尚 Canvas 绘制与悬浮气泡
    └── SettingsPages.kt           // LLM 与数据配置
```

### 代码规范
1. **单一数据流 (UDF)**: ViewModel/Store 发出不可变 State，UI 发出 Intent/Action。
2. **离线优先**: 任何网络失败不应造成输入内容丢失或 UI 崩溃。
3. **中文友好的异常日志**: 错误均转化为用户可理解的中文提示（如“网络连接超时，已保留本地草稿”）。

---

## 5. 测试策略 (Testing Strategy)

1. **单元测试 (`app/src/test`)**:
   - `JournalTest`: 校验六大字段合法性、分类增删改、非法输入防御。
   - `AiClientTest`: 模拟 OpenAI 返回合规 JSON、格式畸变、网络断开等各种边缘情况。
   - `PortraitTest`: 校验气泡聚类阈值、消障点击 3 次状态转移与持久化。
   - `BackupTest`: 校验 JSON 导出字段完备性、跨版本备份反序列化及损坏文件拦截。
2. **设备测试 (`app/src/androidTest`)**:
   - 验证 SQLite 事务一致性。
   - 验证 Compose 导航流：首页 -> 新增 -> 历史 -> 画像 -> 设置。

---

## 6. 行为边界 (Boundaries)

- **Always (始终遵守)**:
  - 保持水墨极简意境，遵循纸墨配色规范与合理留白。
  - 用户未保存草稿在退出时必须弹窗防丢提醒。
  - API Key 必须使用安全存储，日志中坚决禁止打印明文 Key。
- **Ask First (提前确认)**:
  - 调整 Android SDK 版本基线或引入大型外部依赖。
  - 更改核心数据表结构或破坏向后兼容的备份文件格式。
- **Never (绝对禁止)**:
  - 在未配置 AI 时阻断用户正常手动录入。
  - 将用户的反思私密内容未经确认自动静默上传第三方。
  - 出现花哨刺眼的高饱和配色或密集嘈杂的布局。

---

## 7. 验收成功标准 (Success Criteria)

1. **冷启动与主页**: 新装 App 首次打开，主屏优雅呈现预置禅意金句与预置 5 篇高质量模拟数据；随时可自由抽取「悟」。
2. **AI 结构化整理**: 输入一段日常流水账，一键提炼出精准的主题、分类、问题、省（不应该...）、悟（应该...），字段可自由二次润色。
3. **水墨坐禅气泡**: 画像页面正面端坐水墨小僧神态静谧，气泡动态漂浮，点击 3 次变淡至消解，重新进入 App 状态依然保持。
4. **测试与构建通过**: `.\gradlew.bat testDebugUnitTest` 与 `assembleDebug` 均 100% 成功。

---

## 8. 当前落地状态与交付清单 (Current Implementation & Delivery)

已于 2026-11-21 完成全套 V1.0 功能交付，经过全面单元测试与 Lint 静态分析检验：

1. **水墨视觉系统**：
   - 宣纸底色 (`#F7F5F0`)、墨分五色、朱砂印章、微宽字距；统一 64dp 防抖顶栏。
   - 底部单排导航：【当下】（观照今日心法）、【过往】（回首往日书简）、【见我】（坐禅消障破执）。
2. **正面水墨小僧与气泡消障**：
   - 画面主体为双手结禅定印的端坐正面水墨小和尚透明图元，背景保留宣纸山水微颗粒。
   - 气泡采用四字短标算法（「急躁打断」、「熬夜内耗」、「盲目应承」等），三度轻触淡化消障，右上角归档明细抽屉。
3. **极简倾诉与无缝润色**：
   - 新增页面精简为开阔单文本框，后台 AI 异步梳理（通义千问 `qwen3.8-flash` 默认注入）。
   - 润色交互自动回填原复盘全文，时间轴与创建时间保持守恒。
4. **安全删除与撤还**：
   - 二次弹窗确认删除，底部附带即时【撤还】Snackbar，防误触秒级复原。
5. **本地存储与模拟数据**：
   - 纯净 SQLite 本地存储，预置 5 篇涵盖工作、情绪、策略、学习、交际维度的生活复盘数据。
6. **设备兼容与包可见性**：
   - 补齐 Android 11+ `<queries>` 声明与 `RECORD_AUDIO` 麦克风动态授权。
   - 增加针对小米（MIUI / HyperOS）小爱语音拦截的智能指引弹窗与直达设置意图。
7. **Sherpa-ONNX 端侧离线 ASR 与 SenseVoice 模型**：
   - 集成 `sherpa-onnx:v2.0.1`，适配 `arm64-v8a`，安装包控制在 36.7MB；
   - 支持设置页一键下载阿里 SenseVoice-Small 端侧模型（约 25MB），就绪后永久离线使用；
   - 录入页提供端侧录音识别，彻底摆脱手机厂商私有语音限制，毫秒级本地解码。
