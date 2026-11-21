package com.snowtraces.sanxing.asr

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.util.Collections

class PcmRecorder(private val sampleRate: Int = 16000) {
    private var audioRecord: AudioRecord? = null
    @Volatile private var isRecording = false
    private var readJob: Job? = null
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(2048)

    private val samples = Collections.synchronizedList(mutableListOf<Float>())

    val recording: Boolean get() = isRecording

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope): Boolean {
        if (isRecording) return true
        samples.clear()
        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("PcmRecorder", "AudioRecord initialization failed")
                record.release()
                return false
            }
            record.startRecording()
            audioRecord = record
            isRecording = true

            readJob = scope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(bufferSize / 2)
                while (isRecording && isActive) {
                    val read = record.read(shortBuffer, 0, shortBuffer.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            samples.add(shortBuffer[i] / 32768.0f)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("PcmRecorder", "Failed to start AudioRecord", e)
            isRecording = false
            audioRecord?.release()
            audioRecord = null
            false
        }
    }

    suspend fun stop(): FloatArray = withContext(Dispatchers.IO) {
        if (!isRecording) return@withContext FloatArray(0)
        isRecording = false
        try {
            readJob?.cancelAndJoin()
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e("PcmRecorder", "Error while stopping recording", e)
        } finally {
            audioRecord?.release()
            audioRecord = null
            readJob = null
        }
        val result = synchronized(samples) { samples.toFloatArray() }
        samples.clear()
        result
    }
}
