package com.snowtraces.sanxing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// 水墨意境东方美学调色板 (Ink Aesthetic Palette)
val XuanPaper = Color(0xFFF7F5F0) // 宣纸微暖白底
val Paper = XuanPaper
val Ivory = Color(0xFFFFFCF5)     // 仿古宣纸卡片白
val JiaoMo = Color(0xFF181818)    // 焦墨 (高对比度标题)
val NongMo = Color(0xFF2C2C2C)    // 浓墨 (正文沉静)
val ZhongMo = Color(0xFF5A5957)   // 重墨 (副标题/日期)
val DanMo = Color(0xFFE2DFD8)     // 淡墨 (分割边框)
val QingMo = Color(0xFFF0EDE6)    // 清墨 (极淡底色)
val SealRed = Color(0xFFB3392F)   // 朱砂红印章色

// 兼容别名
val Ink = NongMo
val Pine = Color(0xFF324A40)
val Muted = ZhongMo
val Line = DanMo
val Seal = SealRed

@Composable
fun SealBadge(text: String, modifier: Modifier = Modifier, color: Color = SealRed) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.2.dp, color.copy(alpha = 0.85f)),
        color = color.copy(alpha = 0.08f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            letterSpacing = 1.sp
        )
    }
}

// 分类水墨点缀印色 (古雅、克制、画龙点睛)
fun getCategoryAccent(catId: String, catName: String = ""): Color {
    val key = (catId + catName).lowercase()
    return when {
        key.contains("emotion") || key.contains("情") || key.contains("心") -> SealRed            // 朱砂红印
        key.contains("learn") || key.contains("学") -> Color(0xFF28536B)                          // 黛蓝印
        key.contains("work") || key.contains("工") || key.contains("业") || key.contains("事") -> Pine      // 松青印
        key.contains("method") || key.contains("法") || key.contains("道") -> Color(0xFF2D5A46)      // 苍竹印
        key.contains("strat") || key.contains("策") || key.contains("谋") -> Color(0xFF8A5A28)      // 赭褐印
        key.contains("social") || key.contains("交") || key.contains("友") -> Color(0xFF8B435A)      // 胭脂印
        else -> Color(0xFF5C5248)                                                                  // 茶褐印
    }
}

// 分类写意水墨文字水印资源
fun getCategoryWatermarkRes(catId: String, catName: String = ""): Int {
    val key = (catId + catName).lowercase()
    return when {
        key.contains("emotion") || key.contains("情") || key.contains("心") -> com.snowtraces.sanxing.R.drawable.ink_cat_emotion
        key.contains("learn") || key.contains("学") -> com.snowtraces.sanxing.R.drawable.ink_cat_learning
        key.contains("work") || key.contains("工") || key.contains("业") || key.contains("事") -> com.snowtraces.sanxing.R.drawable.ink_cat_work
        key.contains("method") || key.contains("法") || key.contains("道") -> com.snowtraces.sanxing.R.drawable.ink_cat_method
        key.contains("strat") || key.contains("策") || key.contains("谋") -> com.snowtraces.sanxing.R.drawable.ink_cat_strategy
        key.contains("social") || key.contains("交") || key.contains("友") || key.contains("人") -> com.snowtraces.sanxing.R.drawable.ink_cat_social
        key.contains("xiushen") || key.contains("修") || key.contains("身") -> com.snowtraces.sanxing.R.drawable.ink_cat_xiushen
        key.contains("zhizhi") || key.contains("知") || key.contains("理") -> com.snowtraces.sanxing.R.drawable.ink_cat_zhizhi
        key.contains("duxing") || key.contains("行") || key.contains("笃") -> com.snowtraces.sanxing.R.drawable.ink_cat_duxing
        key.contains("daily") || key.contains("日") || key.contains("常") -> com.snowtraces.sanxing.R.drawable.ink_cat_daily
        else -> com.snowtraces.sanxing.R.drawable.ink_cat_default
    }
}

@Composable
fun InkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Pine, onPrimary = Ivory, background = Paper, onBackground = Ink,
        surface = Ivory, onSurface = Ink, onSurfaceVariant = Muted,
        secondaryContainer = Color(0xFFDDE6DC), onSecondaryContainer = Ink,
        outline = Muted, error = Color(0xFF9E302D),
    ), content = content)
}

@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = JiaoMo,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun ScreenHeader(title: String, action: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Heading(title)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = action
        )
    }
}

@Composable
fun PageChrome(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("‹ 返回", color = Pine, fontSize = 15.sp)
            }
            Spacer(Modifier.width(8.dp))
            Heading(title)
        }
        content()
    }
}

fun dateLabel(time: Long, pattern: String = "yyyy年M月d日 HH:mm"): String =
    SimpleDateFormat(pattern, Locale.SIMPLIFIED_CHINESE).format(Date(time))

@Composable
fun InkLandscape(modifier: Modifier = Modifier, monk: Boolean = false) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // Layered silhouettes share the same paper ground.
        fun point(x: Float, y: Float) = Offset(x * w, y * h)
        fun mountain(base: Float, opacity: Float, ridges: List<Pair<Float, Float>>) {
            val path = Path().apply {
                moveTo(0f, base * h)
                ridges.forEach { (x, y) -> lineTo(x * w, y * h) }
                lineTo(w, base * h)
                close()
            }
            drawPath(path, Pine.copy(alpha = opacity))
        }
        drawCircle(Seal.copy(alpha = 0.76f), w * 0.049f, point(0.76f, 0.30f))
        mountain(0.84f, 0.11f, listOf(0f to 0.66f, 0.14f to 0.45f, 0.21f to 0.41f,
            0.34f to 0.62f, 0.47f to 0.37f, 0.51f to 0.34f, 0.68f to 0.64f,
            0.82f to 0.48f, 0.90f to 0.52f, 1f to 0.66f))
        mountain(0.87f, 0.15f, listOf(0f to 0.73f, 0.12f to 0.60f, 0.24f to 0.54f,
            0.39f to 0.77f, 0.63f to 0.82f, 0.80f to 0.63f, 0.93f to 0.57f, 1f to 0.64f))
        val grain = Random(37)
        repeat(180) {
            val x = grain.nextFloat()
            val y = 0.57f + grain.nextFloat() * 0.33f
            drawCircle(Pine.copy(alpha = grain.nextFloat() * 0.09f), w * 0.0019f, point(x, y))
        }

        if (monk) {
            drawOval(Pine.copy(alpha = 0.22f), point(0.24f, 0.89f), Size(w * 0.52f, h * 0.045f))
            drawPath(Path().apply {
                moveTo(w * 0.50f, h * 0.51f)
                cubicTo(w * 0.38f, h * 0.50f, w * 0.36f, h * 0.65f, w * 0.38f, h * 0.73f)
                cubicTo(w * 0.31f, h * 0.73f, w * 0.27f, h * 0.83f, w * 0.34f, h * 0.88f)
                quadraticBezierTo(w * 0.50f, h * 0.93f, w * 0.67f, h * 0.88f)
                cubicTo(w * 0.73f, h * 0.82f, w * 0.68f, h * 0.74f, w * 0.62f, h * 0.73f)
                cubicTo(w * 0.64f, h * 0.64f, w * 0.60f, h * 0.51f, w * 0.50f, h * 0.51f)
                close()
            }, Pine)
            val headRadius = w * 0.081f
            drawCircle(Ivory, headRadius, point(0.50f, 0.44f))
            drawCircle(Ink, headRadius, point(0.50f, 0.44f), style = Stroke(w * 0.007f))
            drawPath(Path().apply {
                moveTo(w * 0.45f, h * 0.56f)
                quadraticBezierTo(w * 0.50f, h * 0.64f, w * 0.56f, h * 0.69f)
                moveTo(w * 0.43f, h * 0.73f)
                quadraticBezierTo(w * 0.50f, h * 0.81f, w * 0.57f, h * 0.73f)
            }, Ivory.copy(alpha = 0.62f), style = Stroke(w * 0.005f))
            for (x in listOf(0.47f, 0.525f)) {
                drawLine(Ink, point(x - 0.012f, 0.445f), point(x + 0.012f, 0.447f), w * 0.005f)
            }
        }
    }
}

val cardShape = RoundedCornerShape(22.dp)
