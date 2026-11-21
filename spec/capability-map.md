# 三省 Android App 能力地图

状态：已确认。V1 模块规格待评审。

面向希望坚持每日复盘的个人用户。用户把当天的一件事记为「省」和「悟」，日后可以找回行动计划，也能从画像里看到反复出现的问题。

| 模块 | 职责 | 依赖 |
| --- | --- | --- |
| `journal` | 文字与语音转文字录入，编辑、保存和查看复盘，维护分类。复盘包含时间、主题、分类、问题描述、省、悟 | — |
| `review` | 首页随机展示历史「悟」，历史列表支持搜索，两个入口都能打开详情并返回原页面 | `journal` |
| `ai-assist` | 在画像页进入设置，配置 LLM API；将草稿整理成结构化复盘并推荐现有分类，用户确认后保存 | `journal` |
| `portrait` | 水墨坐禅画像，使用文本相似度筛选候选、由 LLM 判断重复问题，优先呈现高频「省」；气泡文字为 2～5 字，点击逐次淡出，第三次隐藏并记住状态 | `journal`、`ai-assist` |
| `backup` | 从设置导出本机复盘、分类和画像状态，支持从备份恢复 | `journal`、`portrait` |

实现顺序：`journal` → `review` 和 `ai-assist` → `portrait` → `backup`。

## 工具链基线

与本机 `note_all/android_client` 对齐。Gradle 8.7、Android Gradle Plugin 8.4.2、Kotlin 1.9.24、Java/JVM 21、Compose Compiler 1.5.14、Compose BOM 2023.01.00、`compileSdk 34`、`minSdk 24`。参考工程未声明 `targetSdk`，其 debug 合并清单实际为 24，首版按该值对齐。

## 默认方案

- 暂用「三省」作为 App 名称。默认分类为学习、方法论、情绪、日常、工作、策略、交际。
- 复盘保存在本机。LLM 使用用户配置的 OpenAI 兼容接口。AI 未配置或请求失败时可继续手动编辑并保存。语音使用设备提供的语音识别服务。
- 备份通过 Android 文件选择器导入和导出 JSON。备份包含复盘数据、分类及气泡状态，API 密钥只保存在设备上。

## V1 规格

- [通用规格](SPEC-android.md)
- [复盘录入与分类](SPEC-journal.md)
- [首页与历史](SPEC-review.md)
- [AI 整理与设置](SPEC-ai-assist.md)
- [画像](SPEC-portrait.md)
- [备份与恢复](SPEC-backup.md)
