@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.snowtraces.sanxing

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowtraces.sanxing.aiassist.AiClient
import com.snowtraces.sanxing.aiassist.AiConfig
import com.snowtraces.sanxing.aiassist.AiSettings
import com.snowtraces.sanxing.asr.*
import com.snowtraces.sanxing.backup.*
import com.snowtraces.sanxing.journal.*
import com.snowtraces.sanxing.portrait.*
import com.snowtraces.sanxing.review.*
import com.snowtraces.sanxing.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val store by lazy { JournalStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = AiSettings(applicationContext)
        setContent { InkTheme { SanshengApp(store, settings, this) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) store.close()
    }
}

private val draftSaver = listSaver<Draft, Any>(
    save = { listOf(it.occurredAtMillis, it.title, it.categoryId, it.description, it.reflection, it.action) },
    restore = { Draft(it[0] as Long, it[1] as String, it[2] as String,
        it[3] as String, it[4] as String, it[5] as String) },
)

@Composable
private fun SanshengApp(store: JournalStore, settings: AiSettings, activity: MainActivity) {
    val scope = rememberCoroutineScope()
    val lock = remember { Mutex() }
    val notices = remember { SnackbarHostState() }
    val ai = remember { AiClient() }
    var tab by rememberSaveable { mutableStateOf(0) }
    var route by rememberSaveable { mutableStateOf("main") }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val historyState = rememberLazyListState()
    var data by remember { mutableStateOf<JournalSnapshot?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf(AiConfig()) }
    var configEdit by remember { mutableStateOf(AiConfig()) }
    var draft by rememberSaveable(stateSaver = draftSaver) { mutableStateOf(Draft()) }
    var baseline by rememberSaveable(stateSaver = draftSaver) { mutableStateOf(Draft()) }
    var source by rememberSaveable { mutableStateOf("") }
    var sourceBaseline by rememberSaveable { mutableStateOf("") }
    var editorId by rememberSaveable { mutableStateOf("") }
    var original by remember { mutableStateOf<Draft?>(null) }
    var missing by remember { mutableStateOf(emptySet<String>()) }
    var saving by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var organizeJob by remember { mutableStateOf<Job?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var consentedBase by rememberSaveable { mutableStateOf("") }
    var prompt by remember { mutableStateOf<String?>(null) }
    var pendingBackup by remember { mutableStateOf<Backup?>(null) }
    var retry by remember { mutableStateOf(0) }
    val judged = remember { mutableSetOf<Pair<String, String>>() }
    val asrEngine = remember { SenseVoiceEngine(activity.applicationContext) }
    val pcmRecorder = remember { PcmRecorder(16000) }
    var asrReady by remember { mutableStateOf(false) }
    var asrDownloading by remember { mutableStateOf(false) }
    var asrProgress by remember { mutableStateOf(0f) }
    var asrStatus by remember { mutableStateOf("") }
    var isOfflineRecording by remember { mutableStateOf(false) }
    var isOfflineDecoding by remember { mutableStateOf(false) }
    val snapshot = data
    val records = snapshot?.records.orEmpty()
    val categories = snapshot?.categories.orEmpty()
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    val themes = remember(snapshot?.portrait) { decodeThemes(snapshot?.portrait ?: "[]") }
    val item = records.find { it.id == openId }

    fun notify(message: String) { scope.launch { notices.showSnackbar(message, duration = SnackbarDuration.Short) } }

    suspend fun <T> io(block: () -> T): T = lock.withLock { withContext(Dispatchers.IO) { block() } }

    fun refresh(): JournalSnapshot {
        val current = store.snapshot()
        val old = decodeThemes(current.portrait)
        val next = reconcileThemes(current.records, old)
        if (old == next) return current
        val encoded = encodeThemes(next)
        store.writePortrait(encoded)
        return current.copy(portrait = encoded)
    }

    fun load() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                data = io {
                    store.ensureSampleData()
                    refresh()
                }
                config = io { settings.load() }
                configEdit = config
                loadFailed = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadFailed = true
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        load()
        withContext(Dispatchers.IO) {
            if (ModelDownloader.isModelReady(activity.applicationContext)) {
                asrReady = asrEngine.initModel(ModelDownloader.getModelDir(activity.applicationContext))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (pcmRecorder.recording) {
                scope.launch { pcmRecorder.stop() }
            }
        }
    }

    LaunchedEffect(records, selectedId, snapshot != null) {
        if (records.none { it.id == selectedId }) selectedId = chooseReflection(records)?.id
    }

    fun editRecord(record: Reflection?) {
        editorId = record?.id ?: newId()
        draft = record?.draft() ?: Draft(categoryId = categories.firstOrNull { !it.archived }?.id.orEmpty())
        baseline = draft
        source = if (record != null) {
            buildString {
                append(record.description)
                if (record.reflection.isNotBlank()) {
                    append("\n\n【省思】\n${record.reflection}")
                }
                if (record.action.isNotBlank()) {
                    append("\n\n【心法】\n${record.action}")
                }
            }
        } else ""
        sourceBaseline = source
        original = null
        missing = emptySet()
        route = "editor"
    }

    fun closeEditor() {
        if (isOfflineRecording) {
            isOfflineRecording = false
            scope.launch { pcmRecorder.stop() }
        }
        organizeJob?.cancel()
        organizeJob = null
        route = if (openId == null) "main" else "detail"
    }

    fun back() {
        when (route) {
            "editor" -> if (!saving) {
                if (source != sourceBaseline) prompt = "discard" else closeEditor()
            }
            "detail" -> { route = "main"; openId = null }
            "settings" -> route = "main"
            "categories" -> route = "settings"
            else -> if (tab != 0) tab = 0
        }
    }

    BackHandler(route != "main" || tab != 0) { back() }

    var toggleOfflineRecordAction: (() -> Unit)? = null

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("SanSheng_Voice", "[DEBUG_LOG] RECORD_AUDIO permission result: granted=$granted")
        if (granted) {
            toggleOfflineRecordAction?.invoke()
        } else {
            notify("未授予录音权限，无法启用语音录入")
        }
    }

    fun downloadAsrModel() {
        if (asrDownloading) return
        asrDownloading = true
        asrProgress = 0f
        asrStatus = "正在连接 ModelScope 模型库..."
        scope.launch {
            val ok = ModelDownloader.download(activity.applicationContext) { progress, status ->
                asrProgress = progress
                asrStatus = status
            }
            asrDownloading = false
            if (ok) {
                withContext(Dispatchers.IO) {
                    asrReady = asrEngine.initModel(ModelDownloader.getModelDir(activity.applicationContext))
                }
                notify("端侧离线语音模型已就绪！")
            } else {
                notify("模型下载未完成，可检查网络后重试")
            }
        }
    }

    fun deleteAsrModel() {
        ModelDownloader.deleteModel(activity.applicationContext)
        asrReady = false
        notify("已清理端侧模型，释放空间")
    }

    fun toggleOfflineRecord() {
        toggleOfflineRecordAction = ::toggleOfflineRecord
        Log.d("SanSheng_ASR", "[DEBUG_LOG] toggleOfflineRecord invoked. isRecording=$isOfflineRecording, asrReady=$asrReady")
        val hasPermission = ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.d("SanSheng_ASR", "[DEBUG_LOG] Requesting RECORD_AUDIO permission...")
            recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }
        if (!isOfflineRecording) {
            val started = pcmRecorder.start(scope)
            Log.d("SanSheng_ASR", "[DEBUG_LOG] pcmRecorder.start() result: $started")
            if (started) {
                isOfflineRecording = true
            } else {
                notify("无法开启麦克风录音")
            }
        } else {
            isOfflineRecording = false
            isOfflineDecoding = true
            scope.launch {
                val startTime = System.currentTimeMillis()
                val samples = pcmRecorder.stop()
                Log.d("SanSheng_ASR", "[DEBUG_LOG] pcmRecorder.stop() captured ${samples.size} samples")
                if (samples.isNotEmpty()) {
                    val text = withContext(Dispatchers.IO) { asrEngine.decode(samples) }
                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d("SanSheng_ASR", "[DEBUG_LOG] asrEngine.decode finished in ${elapsed}ms: '$text'")
                    if (text.isNotBlank()) {
                        source = listOf(source, text).filter { it.isNotBlank() }.joinToString("\n")
                    } else {
                        notify("未能识别到清晰语音")
                    }
                }
                isOfflineDecoding = false
            }
        }
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                try {
                    val count = io {
                        val current = refresh()
                        val body = encodeBackup(current, System.currentTimeMillis())
                        val output = activity.contentResolver.openOutputStream(uri, "wt") ?: error("无法写入文件")
                        output.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
                        current.records.size
                    }
                    val filename = uri.lastPathSegment?.substringAfterLast('/') ?: "三省备份"
                    notify("已导出 $filename，共 $count 条复盘")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    notify("导出失败，请重新选择可写入的位置")
                } finally { busy = false }
            }
        }
    }

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                try {
                    pendingBackup = withContext(Dispatchers.IO) {
                        val stream = activity.contentResolver.openInputStream(uri) ?: error("无法读取备份")
                        stream.use { readBackup(it) }
                    }
                    prompt = "restore"
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    notify("无法读取备份，请选择完整的三省 V1 JSON 文件")
                } finally { busy = false }
            }
        }
    }

    fun organize() {
        if (organizeJob != null) return
        if (source.isBlank() && draft.description.isBlank() && draft.reflection.isBlank() && draft.action.isBlank()) {
            notify("请先输入复盘内容")
            return
        }
        val endpoint = runCatching { config.endpoint() }.getOrElse {
            notify("请先在画像的设置中配置 AI 服务")
            return
        }
        if (consentedBase != endpoint.toString()) {
            prompt = "consent"
            return
        }
        val before = draft
        val originalText = source
        val savedConfig = config
        organizeJob = scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { ai.organize(savedConfig, originalText, before, categories) }
                if (route == "editor" && draft == before && source == originalText) {
                    original = before
                    draft = result
                    missing = emptySet()
                } else notify("草稿已有修改，已保留当前内容")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                notify("AI 整理失败，请检查连接或继续手动编辑")
            } finally {
                if (organizeJob == coroutineContext[Job]) organizeJob = null
            }
        }
    }

    fun saveDraft() {
        if (saving) return
        val rawText = source.trim()
        if (rawText.isBlank()) {
            notify("请先写下当下的心事或经历")
            return
        }
        saving = true
        scope.launch {
            try {
                // AI 自动提取字段；若未配置或失败则本地智能兜底，无需用户手动拆分
                val structuredDraft = if (config.apiKey.isNotBlank() && runCatching { config.endpoint() }.isSuccess) {
                    try {
                        withContext(Dispatchers.IO) {
                            ai.organize(config, rawText, Draft(), categories)
                        }
                    } catch (_: Exception) {
                        fallbackExtract(rawText, categories)
                    }
                } else {
                    fallbackExtract(rawText, categories)
                }

                val now = System.currentTimeMillis()
                val currentItem = item
                val isEditingExisting = currentItem != null && currentItem.id == editorId
                val candidate = structuredDraft.copy(
                    occurredAtMillis = if (isEditingExisting) currentItem!!.occurredAtMillis else structuredDraft.occurredAtMillis
                ).toReflection(
                    id = editorId,
                    created = if (isEditingExisting) currentItem!!.createdAtMillis else now,
                    updated = now
                )
                data = io {
                    store.atomically {
                        store.save(candidate)
                        refresh()
                    }
                }
                openId = candidate.id
                closeEditor()
                notify(if (isEditingExisting) "润色定案，前尘更明" else "此念已记，心安神宁")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                notify("存盘未能完成：${e.message ?: "请检查输入"}")
            } finally { saving = false }
        }
    }

    fun saveSettings(value: AiConfig) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val saved = io { settings.save(value); settings.load() }
                config = saved
                configEdit = saved
                judged.clear()
                notify(if (saved.apiKey.isBlank()) "设置已保存，未保留 API 密钥" else "设置已保存")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IllegalArgumentException) {
                notify(failure.message ?: "请检查 AI 设置")
            } catch (_: Exception) {
                notify("无法保存设置")
            } finally { busy = false }
        }
    }

    LaunchedEffect(records, config, retry, tab, route) {
        if (tab != 2 || route != "main" || !config.portrait || runCatching { config.endpoint() }.isFailure) return@LaunchedEffect
        if (records.size < 2) return@LaunchedEffect
        val candidates = withContext(Dispatchers.Default) { similarCandidates(themes) }
        val remaining = candidates.filter { (it.leftText to it.rightText) !in judged }
        if (remaining.isEmpty()) return@LaunchedEffect
        analyzing = true
        try {
            for (candidate in remaining) {
                val label = withContext(Dispatchers.IO) {
                    ai.sameBehavior(config, candidate.leftText, candidate.rightText)
                }
                judged.add(candidate.leftText to candidate.rightText)
                if (label != null) data = io {
                    val current = refresh()
                    val fresh = decodeThemes(current.portrait)
                    val merged = mergeThemes(fresh, candidate, label)
                    val encoded = encodeThemes(merged)
                    if (merged != fresh) store.writePortrait(encoded)
                    current.copy(portrait = encoded)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            notify("画像整理暂时失败，可以稍后重试")
        } finally { analyzing = false }
    }

    val pages = listOf("当下", "过往", "见我")
    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(notices) },
        bottomBar = {
            if (route == "main" && snapshot != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Ivory,
                    border = BorderStroke(0.8.dp, DanMo.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.forEachIndexed { index, page ->
                            val selected = tab == index
                            TextButton(
                                onClick = { tab = index },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (selected) Pine else ZhongMo
                                )
                            ) {
                                Text(
                                    text = page,
                                    fontSize = if (selected) 16.sp else 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                snapshot == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (loadFailed) {
                        Text("暂时无法读取本机复盘")
                        TextButton(onClick = ::load) { Text("重试") }
                    } else CircularProgressIndicator()
                }
                route == "editor" -> EditorPage(
                    source = source,
                    onSource = { source = it },
                    onBack = ::back,
                    save = ::saveDraft,
                    saving = saving,
                    pageTitle = if (item != null && item.id == editorId) "润色心法" else "省思",
                    asrReady = asrReady,
                    isRecording = isOfflineRecording,
                    isDecoding = isOfflineDecoding,
                    toggleOfflineRecord = ::toggleOfflineRecord,
                    asrDownloading = asrDownloading,
                    asrProgress = asrProgress,
                    downloadAsr = ::downloadAsrModel
                )
                route == "detail" -> {
                    val sortedRecords = remember(records) {
                        records.sortedWith(compareByDescending<Reflection> { it.occurredAtMillis }.thenByDescending { it.createdAtMillis })
                    }
                    if (item != null) DetailPage(
                        items = sortedRecords,
                        currentId = openId ?: item.id,
                        categories = categoryNames,
                        back = ::back,
                        edit = { target -> editRecord(target) },
                        delete = { target -> openId = target.id; prompt = "delete" },
                        onSelect = { selectedId -> openId = selectedId }
                    )
                    else PageChrome("省悟详记", ::back) { Text("这条复盘已不存在", Modifier.padding(22.dp)) }
                }
                route == "settings" -> SettingsPage(configEdit, { configEdit = it }, ::back, { route = "categories" },
                    { saveSettings(configEdit) }, {
                        if (!busy) {
                            busy = true
                            val testConfig = configEdit
                            scope.launch {
                                try {
                                    testConfig.endpoint()
                                    withContext(Dispatchers.IO) { ai.ping(testConfig) }
                                    notify("AI 服务连接成功")
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    notify("连接失败，请检查地址、模型和密钥")
                                } finally { busy = false }
                            }
                        }
                    }, { saveSettings(configEdit.copy(apiKey = "", portrait = false)) },
                    { export.launch("三省-${dateLabel(System.currentTimeMillis(), "yyyyMMdd-HHmmss")}.json") },
                    { restorePicker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, busy,
                    asrReady = asrReady, asrDownloading = asrDownloading, asrProgress = asrProgress,
                    asrStatus = asrStatus, downloadAsr = ::downloadAsrModel, deleteAsr = ::deleteAsrModel)
                route == "categories" -> CategoriesPage(categories, ::back) { id, name, archived ->
                    scope.launch {
                        try {
                            data = io { store.updateCategory(id, name, archived); refresh() }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: IllegalArgumentException) {
                            notify(failure.message ?: "分类无法更新")
                        } catch (_: Exception) {
                            notify("分类无法更新")
                        }
                    }
                }
                tab == 0 -> {
                    val featured = records.find { it.id == selectedId }
                    val hasReflectedToday = remember(records) {
                        val cal = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        val todayStart = cal.timeInMillis
                        records.any { it.occurredAtMillis >= todayStart }
                    }
                    HomePage(featured, categoryNames[featured?.categoryId].orEmpty(), hasReflectedToday, { editRecord(null) },
                        { openId = it; route = "detail" }, { selectedId = chooseReflection(records, selectedId)?.id })
                }
                tab == 1 -> {
                    val history = remember(records, categoryNames, query) {
                        searchHistory(records, categoryNames, query)
                    }
                    HistoryPage(history, records.size, categoryNames, query, historyState, { query = it },
                        { editRecord(null) }, { openId = it; route = "detail" })
                }
                else -> PortraitPage(visibleThemes(themes, records), resolvedThemes(themes), analyzing,
                    if (config.portrait && config.apiKey.isNotBlank()) ({ judged.clear(); retry++ }) else null,
                    { configEdit = config; route = "settings" },
                    { id -> scope.launch {
                        try {
                            data = io {
                                val current = refresh()
                                val tapped = encodeThemes(tapTheme(decodeThemes(current.portrait), id))
                                store.writePortrait(tapped)
                                current.copy(portrait = tapped)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) { notify("无法保存气泡状态") }
                    } })
            }
        }
    }

    val activePrompt = prompt
    if (activePrompt != null) {
        val backup = pendingBackup
        val (title, body) = when (activePrompt) {
            "discard" -> "放弃修改？" to "当前修改尚未保存。"
            "delete" -> "删除复盘？" to "这条复盘会从历史、首页和画像中移除。"
            "consent" -> "使用 AI 整理？" to "当前草稿和可选分类将发送到 ${config.baseUrl}。"
            else -> "替换本机数据？" to (backup?.let {
                "备份于 ${dateLabel(it.exportedAt)} 导出，含 ${it.snapshot.records.size} 条复盘和 ${it.snapshot.categories.size} 个分类。导入将替换本机复盘、分类和画像状态。"
            }.orEmpty())
        }
        AlertDialog(onDismissRequest = { prompt = null; pendingBackup = null },
            title = { Text(title) }, text = { Text(body) },
            confirmButton = { TextButton(onClick = {
                prompt = null
                when (activePrompt) {
                    "discard" -> closeEditor()
                    "delete" -> item?.let { deleted ->
                        scope.launch {
                            try {
                                data = io {
                                    store.atomically {
                                        store.delete(deleted.id)
                                        refresh()
                                    }
                                }
                                openId = null
                                route = "main"
                                val result = notices.showSnackbar(
                                    message = "此篇已舍去",
                                    actionLabel = "撤还",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    data = io {
                                        store.atomically {
                                            store.save(deleted)
                                            refresh()
                                        }
                                    }
                                    openId = deleted.id
                                    route = "detail"
                                    notices.showSnackbar("已收回此篇，前尘复存", duration = SnackbarDuration.Short)
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) { notify("无法舍去复盘") }
                        }
                    }
                    "consent" -> {
                        consentedBase = config.endpoint().toString()
                        organize()
                    }
                    "restore" -> if (backup != null && !busy) {
                        pendingBackup = null
                        busy = true
                        scope.launch {
                            try {
                                data = io {
                                    store.atomically {
                                        store.restore(backup.snapshot)
                                        refresh()
                                    }
                                }
                                selectedId = null
                                judged.clear()
                                notify("备份恢复完成")
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                notify("恢复失败，原有复盘已保留")
                            } finally { busy = false }
                        }
                    }
                }
            }) { Text(when (activePrompt) { "delete" -> "删除"; "restore" -> "确认替换"; "consent" -> "发送并整理"; else -> "放弃" }) } },
            dismissButton = { TextButton(onClick = { prompt = null; pendingBackup = null }) { Text("取消") } },
        )
    }
}

private fun fallbackExtract(rawText: String, categories: List<Category>): Draft {
    val clean = rawText.trim()
    val lines = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val defaultCatId = categories.firstOrNull { !it.archived }?.id ?: "daily"

    val reflectionMarker = lines.indexOfFirst {
        it.startsWith("【省思】") || it.startsWith("【省思偏差】") || it.startsWith("省思：") || it.startsWith("省：")
    }
    val actionMarker = lines.indexOfFirst {
        it.startsWith("【心法】") || it.startsWith("【对治心法】") || it.startsWith("心法：") || it.startsWith("悟：")
    }

    val desc = if (reflectionMarker > 0) {
        lines.take(reflectionMarker).joinToString("\n")
    } else if (reflectionMarker == 0 && lines.size > 1) {
        lines[1]
    } else clean

    val refl = if (reflectionMarker >= 0) {
        val end = if (actionMarker > reflectionMarker) actionMarker else lines.size
        lines.subList(reflectionMarker, end).joinToString("\n")
            .replace(Regex("^(【省思】|【省思偏差】|省思：|省：)"), "").trim()
    } else clean

    val act = if (actionMarker >= 0) {
        lines.drop(actionMarker).joinToString("\n")
            .replace(Regex("^(【心法】|【对治心法】|心法：|悟：)"), "").trim()
    } else "知非即舍，行有所止"

    val firstLine = desc.lines().firstOrNull { it.isNotBlank() } ?: clean
    val title = if (firstLine.length <= 15) firstLine else firstLine.take(14) + "…"
    return Draft(
        occurredAtMillis = System.currentTimeMillis(),
        title = title.ifBlank { "今日复盘" },
        categoryId = defaultCatId,
        description = desc.ifBlank { clean },
        reflection = refl.ifBlank { clean },
        action = act.ifBlank { "知非即舍，行有所止" }
    )
}
