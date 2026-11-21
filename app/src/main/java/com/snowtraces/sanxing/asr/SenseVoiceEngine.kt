package com.snowtraces.sanxing.asr

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import java.io.File

class SenseVoiceEngine(private val context: Context) {
    private var recognizer: OfflineRecognizer? = null

    val isReady: Boolean get() = recognizer != null

    fun initModel(modelDir: File): Boolean {
        return try {
            val modelPath = File(modelDir, "model.int8.onnx").absolutePath
            val tokensPath = File(modelDir, "tokens.txt").absolutePath
            if (!File(modelPath).exists() || !File(tokensPath).exists()) {
                Log.w("SenseVoiceEngine", "Model files missing at $modelDir")
                return false
            }
            val senseVoiceConfig = OfflineSenseVoiceModelConfig(
                model = modelPath,
                language = "zh",
                useInverseTextNormalization = true
            )
            val modelConfig = OfflineModelConfig(
                senseVoice = senseVoiceConfig,
                tokens = tokensPath,
                numThreads = 4,
                debug = false
            )
            val recognizerConfig = OfflineRecognizerConfig(
                modelConfig = modelConfig
            )
            recognizer = OfflineRecognizer(
                assetManager = null,
                config = recognizerConfig
            )
            Log.i("SenseVoiceEngine", "SenseVoice local ASR model loaded successfully!")
            true
        } catch (e: Exception) {
            Log.e("SenseVoiceEngine", "Failed to init OfflineRecognizer", e)
            recognizer = null
            false
        }
    }

    fun decode(samples: FloatArray, sampleRate: Int = 16000): String {
        val rec = recognizer ?: return ""
        val stream: OfflineStream = rec.createStream()
        return try {
            stream.acceptWaveform(samples, sampleRate)
            rec.decode(stream)
            rec.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }
}
