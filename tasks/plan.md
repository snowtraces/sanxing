# 三省 (SanSheng) · V1 技术实施计划 (Technical Implementation Plan)

遵循 `spec-driven-development` 与 `planning-and-task-breakdown` 规范。

---

## 1. 组件与依赖关系 (Components & Dependency Graph)

```
[ui/Ink.kt] (水墨设计体系与基础图元)
    │
    ├──► [ui/ReviewPages.kt] (首页禅心大卡片、历史分类过滤与搜索)
    │        ▲
    │        │ 依赖 [journal/JournalStore.kt]
    │
    ├──► [ui/JournalPages.kt] (录入与大卡片复盘详情)
    │        ▲
    │        │ 依赖 [aiassist/AiClient.kt]
    │
    └──► [ui/SettingsPages.kt & PortraitPages] (坐禅小和尚、气泡消障动效与设置)
             ▲
             │ 依赖 [portrait/Themes.kt]
```

## 2. 实施顺序 (Implementation Order)

1. **第 1 阶段：设计系统 (Design System)**：升级 `ui/Ink.kt`，确立宣纸底色、墨分五色、朱砂印章、字距呼吸感与卡片质感。
2. **第 2 阶段：首页与卡片 (Home & Cards)**：重构首页大卡片（空态禅意金句、点击全貌详情）与历史页分类 Filter Chips。
3. **第 3 阶段：坐禅与气泡画像 (Portrait & Bubbles)**：升级 Canvas 坐禅意境小和尚，完善气泡根据频次的视觉权重与三省破执逐级消散动效。
4. **第 4 阶段：AI 结构化引擎 (AI Assist)**：精炼“事·省·悟”Prompt 契约与中文异常处理。
5. **第 5 阶段：端到端验证与构建 (E2E Verification)**：执行单元测试、Lint 与 APK 打包。

## 3. 风险与缓解对策 (Risks & Mitigations)

| 风险项 | 潜在影响 | 缓解对策 |
|---|---|---|
| Compose Canvas 绘制在不同分辨率下拉伸变形 | 小和尚与山水比例失真 | 使用比例坐标归一化计算 (`size.width * ratio`)，锁定宽高比 |
| 气泡点击消散状态丢失 | 用户三省破执后重启应用恢复 | 立即同步写入 SQLite `theme.taps` 字段，事务提交 |
| AI 返回 JSON 包含 markdown 标记代码块 | 解析失败报错 | 预处理剥离 ````json ... ```` 标记，严格容错 |

## 4. 阶段验证检查点 (Checkpoints)

- **Checkpoint 1 (UI基线)**: `./gradlew.bat :app:compileDebugKotlin` 编译无告警。
- **Checkpoint 2 (逻辑与单元测试)**: `./gradlew.bat :app:testDebugUnitTest` 全部通过。
- **Checkpoint 3 (完整交付包)**: `./gradlew.bat :app:assembleDebug` 生成有效 APK。
