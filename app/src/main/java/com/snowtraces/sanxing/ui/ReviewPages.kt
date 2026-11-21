@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class
)

package com.snowtraces.sanxing.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowtraces.sanxing.R
import com.snowtraces.sanxing.journal.Reflection

@Composable
fun HomePage(
    item: Reflection?,
    category: String,
    hasReflectedToday: Boolean = false,
    add: () -> Unit,
    open: (String) -> Unit,
    next: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 统一顶栏：固定高度与对齐，切 Tab 绝对不抖动
        ScreenHeader("当下") {
            if (item != null) {
                TextButton(onClick = next) {
                    Text("易一悟", color = ZhongMo, fontSize = 13.sp)
                }
            }
            TextButton(onClick = add) {
                Text("＋ 录念", color = Pine, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // 水墨山远景与充裕留白
        InkLandscape(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
        )

        Spacer(Modifier.height(12.dp))

        // 核心宣纸大卡片 (简约水墨基调，书法质感与印章点缀)
        val accent = if (item != null) getCategoryAccent(item.categoryId, category) else Pine
        val watermarkRes = if (item != null) getCategoryWatermarkRes(item.categoryId, category) else R.drawable.ink_cat_default
        Column(Modifier.padding(horizontal = 24.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Ivory)
                    .border(BorderStroke(1.dp, DanMo), RoundedCornerShape(8.dp))
                    .then(if (item == null) Modifier else Modifier.clickable(onClickLabel = "查看详情") { open(item.id) })
            ) {
                if (item != null) {
                    // 右下角写意水墨风格的大号分类背景水印 (原生透明彩色，保留四周装饰，清浅通透)
                    Image(
                        painter = painterResource(watermarkRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(136.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 8.dp)
                            .alpha(0.22f),
                        contentScale = ContentScale.Fit
                    )
                }

                Column(
                    Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (item == null) {
                        // 空态：如白绢素笺
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "日省其过 · 自见其明",
                                fontFamily = FontFamily.Serif,
                                fontSize = 22.sp,
                                color = JiaoMo,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "虚怀若谷，静听己心。\n写下当下的困惑与反省，心法良方将于此处显现。",
                                color = ZhongMo,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = add,
                                border = BorderStroke(1.dp, DanMo),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NongMo)
                            ) {
                                Text("开始第一次省察", fontSize = 13.sp)
                            }
                        }
                    } else {
                        // 悟：核心字句，大字号、衬线体、呼吸感
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (hasReflectedToday) "今日已省" else "今日未省",
                                    color = if (hasReflectedToday) Pine else SealRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("·", color = DanMo)
                                Text(
                                    text = dateLabel(item.occurredAtMillis, "M月d日"),
                                    color = ZhongMo,
                                    fontSize = 13.sp
                                )
                            }
                            // 极简朱青点缀
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(accent.copy(alpha = 0.7f), CircleShape)
                            )
                        }

                        Text(
                            text = item.action,
                            color = JiaoMo,
                            fontFamily = FontFamily.Serif,
                            fontSize = 24.sp,
                            lineHeight = 38.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Normal
                        )

                        // 极淡落款信息
                        Divider(color = DanMo.copy(alpha = 0.6f), thickness = 0.5.dp)

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.title,
                                color = ZhongMo,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text("品读原委 ‹", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun HistoryPage(
    items: List<Reflection>,
    total: Int,
    categories: Map<String, String>,
    query: String,
    listState: LazyListState,
    search: (String) -> Unit,
    add: () -> Unit,
    open: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("过往") {
            TextButton(onClick = add) {
                Text("＋ 录念", color = Pine, fontSize = 14.sp)
            }
        }

        // 极简素雅水墨搜索栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Paper
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = search,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                placeholder = { Text("回首向来处 · 搜寻往日省察...", color = ZhongMo, fontSize = 14.sp) },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Ivory,
                    unfocusedBorderColor = DanMo,
                    focusedBorderColor = Pine
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(onClick = { search("") }) {
                            Text("重置", color = ZhongMo, fontSize = 12.sp)
                        }
                    }
                }
            )
        }

        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (total == 0) "前尘过往，澄澈无染" else "未寻得契合之省察",
                    color = ZhongMo,
                    fontSize = 14.sp
                )
                if (total == 0) {
                    TextButton(onClick = add) { Text("录下第一念", color = Pine) }
                } else {
                    TextButton(onClick = { search("") }) { Text("清空搜索", color = Pine) }
                }
            }
        } else {
            // 简约水墨手札列表：宣纸素白、淡墨细线，分类以右下角写意水墨水印呈现
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val catName = categories[item.categoryId].orEmpty().ifBlank { "日常" }
                    val accent = getCategoryAccent(item.categoryId, catName)
                    val watermarkRes = getCategoryWatermarkRes(item.categoryId, catName)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Ivory)
                            .border(BorderStroke(1.dp, DanMo), RoundedCornerShape(8.dp))
                            .clickable(onClickLabel = "查看复盘") { open(item.id) }
                    ) {
                        // 1. 右下角写意水墨风格的大号分类背景水印 (原生透明彩色，完整保留四周装饰，清浅通透)
                        Image(
                            painter = painterResource(watermarkRes),
                            contentDescription = null,
                            modifier = Modifier
                                .size(128.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = 8.dp)
                                .alpha(0.22f),
                            contentScale = ContentScale.Fit
                        )

                        // 2. 前景手札正文
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 顶行：发生时间 (清晰黑体) + 分类印色微点
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateLabel(item.occurredAtMillis, "M月d日 · HH:mm"),
                                    color = ZhongMo,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(accent.copy(alpha = 0.65f), CircleShape)
                                )
                            }

                            // 标题：东方大宋体、焦墨沉着、金石风骨
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = JiaoMo,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.5.sp,
                                letterSpacing = 0.5.sp
                            )

                            // 悟 / 对治良方：前缀微印色点缀，正文清晰黑体，字字分明
                            if (item.action.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "悟",
                                        color = accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                    Text(
                                        text = item.action,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = NongMo,
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            // 底部原委微摘：纯色无透明度衰减，清晰易读
                            if (item.description.isNotBlank()) {
                                Text(
                                    text = item.description,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = ZhongMo,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailPage(
    items: List<Reflection>,
    currentId: String,
    categories: Map<String, String>,
    back: () -> Unit,
    edit: (Reflection) -> Unit,
    delete: (Reflection) -> Unit,
    onSelect: ((String) -> Unit)? = null
) {
    val initialIdx = remember(items, currentId) {
        items.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    }
    var pageIndex by remember(currentId) { mutableStateOf(initialIdx) }
    var slideDirection by remember { mutableStateOf(1) }
    var totalDrag by remember { mutableStateOf(0f) }

    val currentItem = items.getOrNull(pageIndex) ?: items.firstOrNull()

    PageChrome("省悟详记", back) {
        if (currentItem == null || items.isEmpty()) {
            Text("暂无复盘记录", Modifier.padding(24.dp), color = ZhongMo)
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 卡片区域：支持左右手势滑动翻阅
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(pageIndex, items.size) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (totalDrag < -40f && pageIndex < items.size - 1) {
                                        slideDirection = 1
                                        pageIndex++
                                        items.getOrNull(pageIndex)?.let { onSelect?.invoke(it.id) }
                                    } else if (totalDrag > 40f && pageIndex > 0) {
                                        slideDirection = -1
                                        pageIndex--
                                        items.getOrNull(pageIndex)?.let { onSelect?.invoke(it.id) }
                                    }
                                    totalDrag = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                }
                            )
                        }
                ) {
                    AnimatedContent(
                        targetState = pageIndex,
                        transitionSpec = {
                            if (slideDirection > 0) {
                                (slideInHorizontally(animationSpec = tween(240)) { width -> width / 3 } + fadeIn(tween(240)))
                                    .with(slideOutHorizontally(animationSpec = tween(240)) { width -> -width / 3 } + fadeOut(tween(240)))
                            } else {
                                (slideInHorizontally(animationSpec = tween(240)) { width -> -width / 3 } + fadeIn(tween(240)))
                                    .with(slideOutHorizontally(animationSpec = tween(240)) { width -> width / 3 } + fadeOut(tween(240)))
                            }
                        }
                    ) { targetIdx ->
                        val item = items.getOrNull(targetIdx) ?: return@AnimatedContent
                        val category = categories[item.categoryId].orEmpty()
                        val accent = getCategoryAccent(item.categoryId, category)
                        val watermarkRes = getCategoryWatermarkRes(item.categoryId, category)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Ivory)
                                .border(BorderStroke(1.dp, DanMo), RoundedCornerShape(8.dp))
                        ) {
                            // 右下角写意水墨大号分类背景水印 (原生透明彩色，完整保留四周装饰，清浅通透)
                            Image(
                                painter = painterResource(watermarkRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(142.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 8.dp, y = 10.dp)
                                    .alpha(0.20f),
                                contentScale = ContentScale.Fit
                            )

                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                // 顶行：时间与篇数进度 + 印色微点
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(accent.copy(alpha = 0.7f), CircleShape)
                                        )
                                        Text(dateLabel(item.occurredAtMillis), color = ZhongMo, fontSize = 13.sp)
                                    }
                                    if (items.size > 1) {
                                        Text(
                                            text = "${targetIdx + 1}/${items.size}",
                                            color = ZhongMo,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Heading(item.title)
                                for ((title, value) in listOf(
                                    "原委始末" to item.description,
                                    "省察 · 偏差执念" to item.reflection,
                                    "心法 · 对治良方" to item.action
                                )) {
                                    Divider(color = DanMo.copy(alpha = 0.6f), thickness = 0.5.dp)
                                    Text(title, color = accent, fontSize = 14.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
                                    Text(value, style = MaterialTheme.typography.bodyLarge, color = NongMo, lineHeight = 24.sp)
                                }
                            }
                        }
                    }
                }

                // 底部操作与轻量滑动提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { edit(currentItem) },
                            colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = Ivory)
                        ) {
                            Text("润色")
                        }
                        TextButton(onClick = { delete(currentItem) }) {
                            Text("舍去此篇", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (items.size > 1) {
                        Text(
                            text = "‹ 左右滑动翻阅 ›",
                            color = ZhongMo.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
