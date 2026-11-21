@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.snowtraces.sanxing.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowtraces.sanxing.R
import com.snowtraces.sanxing.aiassist.AiConfig
import com.snowtraces.sanxing.portrait.Theme

@Composable
fun PortraitPage(
    themes: List<Theme>,
    resolved: List<Theme> = emptyList(),
    analyzing: Boolean,
    retry: (() -> Unit)?,
    settings: () -> Unit,
    tap: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showResolvedDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("见我") {
            if (resolved.isNotEmpty()) {
                TextButton(onClick = { showResolvedDialog = true }) {
                    Text("破执 (${resolved.size})", color = Pine, fontSize = 13.sp)
                }
            }
            TextButton(onClick = settings) {
                Text("配置", color = ZhongMo, fontSize = 13.sp)
            }
        }

        // 东方水墨意境天地：保留原本的山水宣纸背景，仅安放水墨勾勒的正面坐禅小和尚
        Box(Modifier.fillMaxWidth().height(420.dp)) {
            // 1. 保留原本的水墨山峦、宣纸肌理与朱砂背景
            InkLandscape(Modifier.matchParentSize())

            // 2. 坐禅小和尚（用户指定正面水墨坐禅形象，透明背景融入山水）
            Image(
                painter = painterResource(id = R.drawable.zen_monk_figure),
                contentDescription = "水墨坐禅",
                modifier = Modifier
                    .size(210.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )

            // 悬浮禅思气泡 (轻盈、呼吸感)
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                themes.chunked(2).forEachIndexed { index, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(start = if (index == 1) 20.dp else 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
                    ) {
                        row.forEach { theme ->
                            val alpha by animateFloatAsState(if (theme.taps == 0) 0.95f else if (theme.taps == 1) 0.55f else 0.25f)
                            val isHighFreq = theme.sources.size > 1
                            Surface(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .heightIn(min = 44.dp)
                                    .graphicsLayer { this.alpha = alpha }
                                    .semantics { contentDescription = "${theme.label}，出现 ${theme.sources.size} 次" }
                                    .clickable(role = Role.Button, onClickLabel = "三省消障") {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        tap(theme.id)
                                    },
                                color = if (isHighFreq) Pine.copy(alpha = 0.85f) else Ivory,
                                contentColor = if (isHighFreq) Ivory else JiaoMo,
                                border = BorderStroke(1.dp, if (isHighFreq) Pine else DanMo),
                                shape = CircleShape
                            ) {
                                Box(
                                    Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = theme.label,
                                        style = if (isHighFreq) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 底部纯净留白与极轻提示
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (themes.isEmpty()) {
                Text("心无挂碍，万般澄明", color = ZhongMo, fontSize = 14.sp, fontFamily = FontFamily.Serif)
            } else {
                Text("轻触气泡三省吾身 · 三度消解化执", color = DanMo.copy(alpha = 0.9f).let { ZhongMo }, fontSize = 12.sp)
            }

            if (analyzing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 1.5.dp, color = Pine)
                    Text("体悟相似心结...", Modifier.padding(start = 8.dp), color = ZhongMo, fontSize = 12.sp)
                }
            }
            if (retry != null && !analyzing) {
                TextButton(onClick = retry) {
                    Text("重察心结", color = Pine, fontSize = 12.sp)
                }
            }
        }

        // 轻量素雅弹窗查看已破执记录 (不霸占主屏留白空间)
        if (showResolvedDialog) {
            AlertDialog(
                onDismissRequest = { showResolvedDialog = false },
                title = {
                    Text("心结渐解 · 破执澄明", fontFamily = FontFamily.Serif, fontSize = 18.sp, color = JiaoMo)
                },
                text = {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("以下过失已连续省思三次，化于无形：", fontSize = 13.sp, color = ZhongMo)
                        Spacer(Modifier.height(4.dp))
                        resolved.forEach { resTheme ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(resTheme.label, fontSize = 14.sp, color = NongMo)
                                Text("已破执 ✓", fontSize = 12.sp, color = Pine)
                            }
                            Divider(color = DanMo.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showResolvedDialog = false }) {
                        Text("归心", color = Pine)
                    }
                },
                containerColor = Ivory,
                shape = cardShape
            )
        }
    }
}

@Composable
fun SettingsPage(
    config: AiConfig, edit: (AiConfig) -> Unit, back: () -> Unit, category: () -> Unit,
    save: () -> Unit, test: () -> Unit, clear: () -> Unit,
    export: () -> Unit, restore: () -> Unit, busy: Boolean,
    asrReady: Boolean = false, asrDownloading: Boolean = false, asrProgress: Float = 0f,
    asrStatus: String = "", downloadAsr: () -> Unit = {}, deleteAsr: () -> Unit = {},
) {
    PageChrome("设置", back) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AI 服务", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(config.baseUrl, { edit(config.copy(baseUrl = it)) }, Modifier.fillMaxWidth(),
                label = { Text("HTTPS Base URL") }, placeholder = { Text("https://api.example.com/v1") }, singleLine = true)
            OutlinedTextField(config.model, { edit(config.copy(model = it)) }, Modifier.fillMaxWidth(),
                label = { Text("模型名称") }, singleLine = true)
            OutlinedTextField(config.apiKey, { edit(config.copy(apiKey = it)) }, Modifier.fillMaxWidth(),
                label = { Text("API 密钥") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("画像 AI 判断", style = MaterialTheme.typography.titleMedium)
                    Text("启用并保存后，相似候选的「省」会发送到此服务。", color = Muted)
                }
                Switch(config.portrait, { edit(config.copy(portrait = it)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Button(onClick = save, enabled = !busy) { Text("保存设置") }
                OutlinedButton(onClick = test, enabled = !busy) { Text("测试连接") }
            }
            TextButton(onClick = clear, enabled = !busy && config.apiKey.isNotEmpty()) { Text("清除密钥") }

            Divider(color = Line)
            Text("端侧离线语音 (Sherpa-ONNX)", style = MaterialTheme.typography.titleLarge)
            Text("内置阿里 SenseVoice-Small 端侧模型，纯手机本地 CPU 识别，完全脱离网络、零隐私外泄。", color = ZhongMo, fontSize = 13.sp)
            if (asrReady) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Pine.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Pine.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SenseVoice 端侧模型已就绪", fontWeight = FontWeight.Medium, color = Pine, fontSize = 14.sp)
                            Text("支持完全离线毫秒级高精度转写", color = ZhongMo, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = deleteAsr,
                            border = BorderStroke(1.dp, SealRed.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SealRed)
                        ) {
                            Text("删除", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (asrDownloading) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(progress = asrProgress, modifier = Modifier.fillMaxWidth(), color = Pine)
                            Text(asrStatus, color = ZhongMo, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = downloadAsr,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Pine)
                        ) {
                            Text("一键下载端侧离线模型 (约 25MB)", color = Pine, fontSize = 14.sp)
                        }
                    }
                }
            }

            Divider(color = Line)
            Text("复盘数据", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = category, modifier = Modifier.fillMaxWidth()) { Text("分类管理") }
            Text("备份文件包含你的复盘文字，请自行妥善保管。", color = Muted)
            OutlinedButton(onClick = export, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("导出备份") }
            OutlinedButton(onClick = restore, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("从备份恢复") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
