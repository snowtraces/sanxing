@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.snowtraces.sanxing.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowtraces.sanxing.journal.Category

@Composable
fun AudioWaveIndicator(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition()
    val h1 by transition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val h2 by transition.animateFloat(
        initialValue = 13f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val h3 by transition.animateFloat(
        initialValue = 6f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.height(18.dp).padding(end = 6.dp)
    ) {
        Box(Modifier.width(2.5.dp).height(h1.dp).background(color, RoundedCornerShape(1.dp)))
        Box(Modifier.width(2.5.dp).height(h3.dp).background(color, RoundedCornerShape(1.dp)))
        Box(Modifier.width(2.5.dp).height(h2.dp).background(color, RoundedCornerShape(1.dp)))
    }
}

@Composable
fun EditorPage(
    source: String,
    onSource: (String) -> Unit,
    onBack: () -> Unit,
    save: () -> Unit,
    saving: Boolean,
    pageTitle: String = "省思",
    asrReady: Boolean = false,
    isRecording: Boolean = false,
    isDecoding: Boolean = false,
    toggleOfflineRecord: () -> Unit = {},
    asrDownloading: Boolean = false,
    asrProgress: Float = 0f,
    downloadAsr: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    PageChrome(pageTitle, onBack) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 唯此一框：宽敞、留白、纯粹
            OutlinedTextField(
                value = source,
                onValueChange = onSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        if (pageTitle == "省思") {
                            "在此写下此刻所见、所历、所思，心中所起之微澜...\n\n归心定案后，AI 将自动凝练出简短主题、体察分类，并梳理出「省察偏差」与「对治心法」。"
                        } else {
                            "在此润色修改原委、省思或心法...\n\n归心定案后，将重新为您梳理定案。"
                        },
                        color = ZhongMo.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        lineHeight = 25.sp
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Ivory,
                    unfocusedBorderColor = DanMo,
                    focusedBorderColor = Pine
                )
            )

            if (saving) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Pine)
                    Spacer(Modifier.width(8.dp))
                    Text("正在体悟凝练心法...", color = ZhongMo, fontSize = 13.sp)
                }
            }

            if (asrDownloading) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = asrProgress,
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = Pine,
                        trackColor = DanMo.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "正在下载端侧离线语音模型 (${(asrProgress * 100).toInt()}%)...",
                        color = ZhongMo,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (asrReady && !isDecoding) {
                            toggleOfflineRecord()
                        } else if (!asrDownloading && !isDecoding) {
                            downloadAsr()
                        }
                    },
                    enabled = !saving && !asrDownloading && !isDecoding,
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(
                        if (isRecording) 1.2.dp else 1.dp,
                        when {
                            isRecording -> SealRed
                            isDecoding || asrReady -> Pine
                            else -> DanMo
                        }
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isRecording) SealRed.copy(alpha = 0.08f) else Ivory,
                        contentColor = when {
                            isRecording -> SealRed
                            isDecoding || asrReady -> Pine
                            else -> NongMo
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        when {
                            isRecording -> {
                                AudioWaveIndicator(SealRed)
                                Text(
                                    text = "倾听中",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            isDecoding -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = Pine
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "识别中…",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            asrDownloading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = Pine
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${(asrProgress * 100).toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            asrReady -> {
                                Text(
                                    text = "🎙 语音",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            else -> {
                                Text(
                                    text = "下载语音",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = save,
                    enabled = !saving && source.isNotBlank() && !isRecording && !isDecoding && !asrDownloading,
                    modifier = Modifier.weight(1.3f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = Ivory)
                ) {
                    Text(
                        text = if (saving) "凝思中…" else "归心定案",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, invalid: Boolean, singleLine: Boolean = false, update: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = update,
            modifier = Modifier.fillMaxWidth().heightIn(min = if (singleLine) 0.dp else 84.dp),
            label = { Text(label, color = ZhongMo) },
            isError = invalid,
            singleLine = singleLine,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Ivory.copy(alpha = 0.5f),
                unfocusedBorderColor = DanMo,
                focusedBorderColor = Pine
            )
        )
        if (invalid) Text("请填写此项", Modifier.padding(start = 12.dp, top = 4.dp),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DateField(time: Long, edit: () -> Unit) {
    OutlinedButton(
        onClick = edit,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, DanMo),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NongMo)
    ) {
        Text("时间  ${dateLabel(time)}")
    }
}

@Composable
private fun CategoryPicker(id: String, categories: List<Category>, invalid: Boolean, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = categories.firstOrNull { it.id == id }
    Column {
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, DanMo),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NongMo)
            ) {
                Text(current?.let { "分类  ${it.name}" } ?: "选择分类")
            }
            DropdownMenu(expanded, { expanded = false }) {
                categories.filter { !it.archived || it.id == id }.forEach {
                    DropdownMenuItem(text = { Text(it.name) }, onClick = { expanded = false; select(it.id) })
                }
            }
        }
        if (invalid) Text("请选择分类", Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun CategoriesPage(categories: List<Category>, back: () -> Unit, save: (String?, String, Boolean?) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    PageChrome("分类管理", back) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            OutlinedButton(
                onClick = { selected = null; name = ""; editing = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                border = BorderStroke(1.dp, DanMo),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NongMo)
            ) {
                Text("＋ 新增分类", fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            categories.forEach { item ->
                val accent = getCategoryAccent(item.id, item.name)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SealBadge(item.name, color = if (item.archived) ZhongMo else accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (item.archived) ZhongMo else JiaoMo,
                            fontFamily = FontFamily.Serif
                        )
                        if (item.archived) Text("已归档", color = Muted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { selected = item.id; name = item.name; editing = true }) {
                        Text("重命名", color = ZhongMo, fontSize = 13.sp)
                    }
                    TextButton(onClick = { save(item.id, item.name, !item.archived) }) {
                        Text(if (item.archived) "恢复" else "归档", color = if (item.archived) Pine else ZhongMo, fontSize = 13.sp)
                    }
                }
                Divider(color = DanMo.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false },
        title = { Text(if (selected == null) "新增分类" else "重命名") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("分类名称") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            save(selected, name, null)
            editing = false
        }) { Text("保存") } },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("取消") } },
    )
}
