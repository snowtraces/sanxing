# Sanxing (三省) ProGuard / R8 混淆配置

# 1. 保护 JNI 原生库与 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. Sherpa-ONNX 与 ONNX Runtime 规则
-keep class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn com.k2fsa.sherpa.onnx.**

-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# 3. 保护数据模型（用于本地 SQLite、JSON 备份与序列化）
-keepclassmembers class com.snowtraces.sanxing.journal.Reflection { *; }
-keepclassmembers class com.snowtraces.sanxing.journal.Category { *; }
-keepclassmembers class com.snowtraces.sanxing.journal.Draft { *; }
-keepclassmembers class com.snowtraces.sanxing.portrait.Theme { *; }
-keepclassmembers class com.snowtraces.sanxing.aiassist.AiConfig { *; }

# 4. Compose 运行时与动画保护
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
