package com.snowtraces.sanxing.asr

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {
    private const val TOKENS_URL =
        "https://www.modelscope.cn/api/v1/models/xiaowangge/sherpa-onnx-sense-voice-small/repo?Revision=master&FilePath=tokens.txt"
    private const val MODEL_URL =
        "https://www.modelscope.cn/api/v1/models/xiaowangge/sherpa-onnx-sense-voice-small/repo?Revision=master&FilePath=model_q8.onnx"

    fun getModelDir(context: Context): File = File(context.filesDir, "sense-voice").apply { mkdirs() }

    fun isModelReady(context: Context): Boolean {
        val dir = getModelDir(context)
        val model = File(dir, "model.int8.onnx")
        val tokens = File(dir, "tokens.txt")
        return model.exists() && model.length() > 10 * 1024 * 1024 && tokens.exists() && tokens.length() > 1000
    }

    fun deleteModel(context: Context): Boolean {
        val dir = getModelDir(context)
        return dir.deleteRecursively()
    }

    suspend fun download(
        context: Context,
        onProgress: (Float, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val dir = getModelDir(context)
        val tokensFile = File(dir, "tokens.txt")
        val modelFile = File(dir, "model.int8.onnx")

        try {
            onProgress(0.05f, "正在下载词表配置 (tokens.txt)...")
            downloadFile(TOKENS_URL, tokensFile) { _, _ -> }

            onProgress(0.1f, "正在下载 SenseVoice 端侧模型 (约 25MB)...")
            val ok = downloadFile(MODEL_URL, modelFile) { current, total ->
                val ratio = if (total > 0) current.toFloat() / total else 0.5f
                val progress = 0.1f + ratio * 0.88f
                val mbCurrent = String.format("%.1f", current / (1024.0 * 1024.0))
                val mbTotal = String.format("%.1f", total / (1024.0 * 1024.0))
                onProgress(progress, "正在下载 SenseVoice 模型: $mbCurrent MB / $mbTotal MB")
            }
            if (ok) {
                onProgress(1.0f, "端侧模型下载完成！")
            }
            ok
        } catch (e: Exception) {
            Log.e("ModelDownloader", "Download failed", e)
            onProgress(0f, "下载失败：${e.message}")
            false
        }
    }

    private fun downloadFile(urlStr: String, dest: File, onBytes: (Long, Long) -> Unit): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                error("HTTP error: $responseCode")
            }

            val totalBytes = connection.contentLength.toLong()
            val tempFile = File(dest.parentFile, "${dest.name}.tmp")
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var currentBytes = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        currentBytes += bytesRead
                        onBytes(currentBytes, totalBytes)
                    }
                }
            }
            if (dest.exists()) dest.delete()
            tempFile.renameTo(dest)
            true
        } finally {
            connection?.disconnect()
        }
    }
}
