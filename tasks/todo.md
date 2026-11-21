# 三省 (SanSheng) · V1 开发任务清单 (Task Breakdown)

- [x] Task 1: 升级水墨东方美学设计体系 (Ink Aesthetic Tokens & Primitives)
  - Acceptance: 引入宣纸底色 (`#F7F5F0`)、墨分五色（焦墨、浓墨、重墨、淡墨、清墨）、朱砂红印章 (`#B3392F`)，提供专属的印章 Composable 和微宽字间距排版，支持大卡片纸张软阴影。
  - Verify: `.\gradlew.bat :app:compileDebugKotlin` 编译通过无错误。
  - Files: `app/src/main/java/com/snowtraces/sanxing/ui/Ink.kt`

- [x] Task 2: 首页禅心悟道大卡片与详情交互重构 (Home Zen Card & Details Interaction)
  - Acceptance: 首页大卡片具备充裕留白与纸质质感，冷启动时显示意境空态金句；有数据时突出「悟」与朱砂印章；点击卡片支持原地进入复盘详情并平滑返回；保留换一条便捷操作；支持「今日尚未内省」/「今日已省」印章状态提醒。
  - Verify: `.\gradlew.bat :app:testDebugUnitTest` 运行通过，页面预览无崩溃。
  - Files: `app/src/main/java/com/snowtraces/sanxing/ui/ReviewPages.kt`, `app/src/main/java/com/snowtraces/sanxing/MainActivity.kt`

- [x] Task 3: 历史复盘分类过滤与搜索体验升级 (History Category Filter Chips & Search)
  - Acceptance: 历史页顶部增加横向分类筛选滚轴（全部、学习、情绪、工作等 Chip）；支持实时模糊搜索与清空；历史小卡片呼吸感增强。
  - Verify: `.\gradlew.bat :app:testDebugUnitTest` 运行通过。
  - Files: `app/src/main/java/com/snowtraces/sanxing/ui/ReviewPages.kt`, `app/src/main/java/com/snowtraces/sanxing/MainActivity.kt`, `app/src/main/java/com/snowtraces/sanxing/review/History.kt`

- [x] Task 4: 水墨坐禅小和尚与三省破执气泡交互增强 (Portrait Zen Monk & Bubble Dissolve)
  - Acceptance: 丰富 Canvas 坐禅小和尚的水墨意境细节；气泡尺寸和墨色按频次差异化呈现；支持点击 3 次渐变褪色至消散（初省淡40%、再省淡70%、三省消散）；提供「已破执 · 心结渐解」历史归档查看抽屉。
  - Verify: `.\gradlew.bat :app:testDebugUnitTest` 针对画像分组及消障逻辑测试通过。
  - Files: `app/src/main/java/com/snowtraces/sanxing/ui/SettingsPages.kt`, `app/src/main/java/com/snowtraces/sanxing/portrait/Themes.kt`

- [x] Task 5: 优化 AI 结构化整理 Prompt 与异常友好兜底 (AI Prompt Tuning & Robust UX)
  - Acceptance: 优化 Prompt 确保从日常草稿精准提炼出“事·省（不应该...）·悟（应该...）”；增强 Markdown 代码块剥离容错；提供清晰的中文超时/网络错误提示。
  - Verify: `.\gradlew.bat :app:testDebugUnitTest` 验证 AI 契约测试通过。
  - Files: `app/src/main/java/com/snowtraces/sanxing/aiassist/AiClient.kt`, `app/src/test/java/com/snowtraces/sanxing/AiClientTest.kt`

- [x] Task 6: 全链路自动化测试与交付 APK 构建 (Full E2E Test & APK Build)
  - Acceptance: 全套单元测试与 Lint 检查 100% 绿色通过，成功构建出可正常安装的 debug APK。
  - Verify: 执行 `.\gradlew.bat :app:testDebugUnitTest` 和 `.\gradlew.bat :app:assembleDebug`。
  - Files: 产出 `app/build/outputs/apk/debug/app-debug.apk`。

- [x] Task 7: 极简心流与水墨意境深度提纯 (Zen Minimalism Refinements)
  - Acceptance:
    1. 主导航彻底改为单排水墨文字条（高度 54dp），去除双层臃肿结构。
    2. 新增复盘彻底精简为“单一开阔大文本框”，用户自由倾诉，点击存盘后台 AI 自动提炼与归类，无需手动填写多重表单。
    3. 画像页保留原本原汁原味的水墨山水与宣纸背景（`InkLandscape`），坐禅主体采用正面端坐、双手结禅定印的水墨线条勾勒纯透明图元（`zen_monk_figure.png`），虚实相生。
  - Verify: 单元测试全部通过，APK 重新打包并验证产出。
  - Files: `ui/SettingsPages.kt`, `ui/JournalPages.kt`, `MainActivity.kt`, `res/drawable/zen_monk_figure.png`

- [x] Task 8: 主导航「当下 · 过往 · 见我」与全站禅意文案重构 (Poetic Zen Nomenclature)
  - Acceptance:
    1. 主导航改名：首页->「当下」、历史->「过往」、画像->「见我」。
    2. 按钮文案：换一条->「易一悟」、记省->「录念」、保存->「归心定案」、编辑->「润色」、删除->「舍去此篇」。
    3. 搜索与空态文案：「回首向来处 · 搜寻往日省察」、「前尘过往，澄澈无染」。
    4. 见我画像文案：「轻触气泡三省吾身 · 三度消解化执」、「心结渐解 · 破执澄明」。
  - Verify: 编译通过，单元测试通过，打包生成最新 APK。
  - Files: `MainActivity.kt`, `ui/ReviewPages.kt`, `ui/SettingsPages.kt`, `ui/JournalPages.kt`

- [x] Task 9: 四字短标气泡与生产假数据剥除 (Sample Reflections Stripping & Bubble Refinement)
  - Acceptance:
    1. 画像气泡算法适配标点截断，精准提炼出四字短标（如「急躁打断」、「熬夜内耗」、「盲目应承」）。
    2. 正式发布环境彻底剥离测试假数据与硬编码密钥，保持首装纯净素笺空态；测试数据剥离至独立单元测试中。
  - Verify: `.\gradlew.bat :app:testDebugUnitTest` 验证通过，APK 构建完成。
  - Files: `journal/Journal.kt`, `journal/JournalStore.kt`, `portrait/Themes.kt`, `MainActivity.kt`, `JournalTest.kt`

- [x] Task 10: 修正润色交互回填与新增删除撤还恢复机制 (Refinement Auto-fill & Delete Undo Recovery)
  - Acceptance:
    1. 点击「润色」已有复盘时，自动将原复盘的“原委事件、省思偏差、对治心法”格式化回填到输入框，支持自由修改与无缝重定案。
    2. 润色定案后完好保留原始发生时间 `occurredAtMillis` 与创建时间，只更新修改时间与内容。
    3. 点击「舍去此篇」确认删除后，底部弹出「此篇已舍去 · 【撤还】」操作条，点击即可一键无损复原并重新打开详情页。
  - Verify: 单元测试全部通过，APK 构建完成。
  - Files: `MainActivity.kt`, `ui/JournalPages.kt`

- [x] Task 11: Android 11+ 语音识别包可见性适配与输入法协同 (Speech Package Visibility Queries)
  - Acceptance:
    1. 在 `AndroidManifest.xml` 中配置 `<queries>` 声明 `RecognitionService` 与 `ACTION_RECOGNIZE_SPEECH`，解决高版本系统的软件包嗅探隔离问题。
    2. 说明国内 ROM 语音生态特性与输入法键盘语音协同方案。
  - Verify: Lint 与编译通过，APK 构建成功。
  - Files: `AndroidManifest.xml`

- [x] Task 12: 麦克风录音权限声明与动态申请集成 (Record Audio Permission & Runtime Grant)
  - Acceptance:
    1. 清单中加入 `android.permission.RECORD_AUDIO` 声明。
    2. 点击语音录入时，先检查并弹出系统麦克风权限请求弹窗，授权后自动调起语音引擎，解决“系统语音引擎需要您的授权”拦截问题。
  - Verify: 单元测试全部通过，APK 构建完成。
  - Files: `AndroidManifest.xml`, `MainActivity.kt`

- [x] Task 13: 集成 Sherpa-ONNX 端侧离线 ASR 与 SenseVoice 语音引擎 (On-device Offline ASR)
  - Acceptance:
    1. 引入 `sherpa-onnx:v2.0.1` 官方端侧 ONNX 动态推理库，限制 `arm64-v8a` 架构（APK 控制在 36MB 极致轻量）。
    2. 封装 `SenseVoiceEngine` 与 `PcmRecorder` 底层 16kHz PCM 麦克风采集与毫秒级本地解码。
    3. 支持在【设置】中一键从 ModelScope 国内高速 CDN 下载阿里 SenseVoice-Small 端侧离线模型（约 25MB），就绪后永久离线使用。
    4. 模型就绪时，录入页升级为「🎙 离线语音录入」/「⏹ 正在聆听·点此结束」，松手本地即时出字，彻底摆脱系统权限与云端依赖。
  - Verify: 单元测试全部通过，APK 构建完成并输出。
  - Files: `asr/SenseVoiceEngine.kt`, `asr/PcmRecorder.kt`, `asr/ModelDownloader.kt`, `MainActivity.kt`, `ui/SettingsPages.kt`, `ui/JournalPages.kt`
