package com.example.gymplan

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.io.File
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    // Applies the in-app language choice (Settings) before any resource is read.
    override fun attachBaseContext(base: Context) {
        val tag = Lang.get(base)
        val locale = if (tag.isEmpty()) Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(tag)
        Locale.setDefault(locale) // number and date formatting follow the chosen language too
        val cfg = Configuration(base.resources.configuration).apply { setLocale(locale) }
        super.attachBaseContext(base.createConfigurationContext(cfg))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clear = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = clear, navigationBarStyle = clear)
        val store = Store(File(filesDir, "gym.json"))
        setContent {
            AppTheme {
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Bg1, Bg2, Bg3)))
                        .statusBarsPadding()
                ) { CompositionLocalProvider(LocalContentColor provides Ink) { App(store) } }
            }
        }
    }
}

/** In-app language: "" = follow the system, otherwise a BCP-47 tag ("en", "pt"). */
object Lang {
    private fun prefs(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun get(c: Context) = prefs(c).getString("lang", "") ?: ""
    fun set(c: Context, tag: String) = prefs(c).edit().putString("lang", tag).apply()
}

// ---------- Theme ----------

val Accent = Color(0xFF7BC4FF)
val OnAccent = Color(0xFF04172A)
val Bg1 = Color(0xFF060A18)
val Bg2 = Color(0xFF0B1431)
val Bg3 = Color(0xFF172452)
val Glass = Color.White.copy(alpha = 0.06f)
val GlassBorder = Color.White.copy(alpha = 0.09f)
val Muted = Color(0xFF8D96B3)
val Ink = Color(0xFFE9EDF8)
val Danger = Color(0xFFFF6B7A)
val DialogBg = Color(0xFF10183A)

// Oxanium variable font (res/font/oxanium.ttf, OFL — docs/licenses/Oxanium-OFL.txt), one instance per weight.
@OptIn(ExperimentalTextApi::class)
val Oxanium = FontFamily(
    listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map {
        Font(R.font.oxanium, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
    }
)

private val AppTypography = Typography().run {
    fun TextStyle.ox() = copy(fontFamily = Oxanium)
    Typography(
        displayLarge.ox(), displayMedium.ox(), displaySmall.ox(),
        headlineLarge.ox(), headlineMedium.ox(), headlineSmall.ox(),
        titleLarge.ox(), titleMedium.ox(), titleSmall.ox(),
        bodyLarge.ox(), bodyMedium.ox(), bodySmall.ox(),
        labelLarge.ox(), labelMedium.ox(), labelSmall.ox(),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Accent, onPrimary = OnAccent,
        primaryContainer = Color(0xFF1B3A5C), onPrimaryContainer = Accent,
        secondary = Accent, background = Bg1,
        surface = Color(0xFF121A36), surfaceContainerHigh = DialogBg,
        onSurface = Ink, onSurfaceVariant = Muted, error = Danger,
        outline = Color.White.copy(alpha = 0.2f), outlineVariant = GlassBorder,
    ),
    typography = AppTypography,
    content = content,
)

fun fmt(d: Double) = if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(d)
fun fmtDate(t: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(t))
fun parseNum(s: String) = s.trim().replace(',', '.').toDoubleOrNull()

/** Distinct values of a per-set breakdown, in order: 10, 10, 8 reads "10 / 8". */
fun summary(values: List<String>) = values.distinct().joinToString(" / ")
fun Exercise.repsSummary() = summary(if (repsPerSet.isEmpty()) listOf(reps) else repsPerSet.map { it.toString() })
fun Exercise.weightsSummary() = summary(weightsPerSet.map(::fmt))

// ---------- Navigation ----------

@Composable
fun App(store: Store) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var planId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exId by rememberSaveable { mutableStateOf<Long?>(null) }
    val plans = store.data.plans
    val plan = plans.find { it.id == planId } ?: plans.firstOrNull()
    val ex = plan?.exercises?.find { it.id == exId }
    val now = rememberNow()
    // Persist the 24 h expiry of ticks; writes only when some plan's ticks actually expired.
    LaunchedEffect(now) { if (store.data.expireDone(now) !== store.data) store.update { it.expireDone(now) } }
    BackHandler(enabled = ex != null) { exId = null }
    if (plan != null && ex != null) ExerciseScreen(store, plan, ex, now, onBack = { exId = null })
    else HomeScreen(store, tab, plan, now, onTab = { tab = it }, onSelectPlan = { planId = it }, onOpen = { exId = it })
}

/** Wall-clock time that ticks every minute, so time-based state (tick expiry) recomposes. */
@Composable
fun rememberNow(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000)
            value = System.currentTimeMillis()
        }
    }
    return now
}

// ---------- Home ----------

@Composable
fun HomeScreen(store: Store, tab: Int, plan: Plan?, now: Long, onTab: (Int) -> Unit, onSelectPlan: (Long) -> Unit, onOpen: (Long) -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        // Material fade-through between top-level destinations.
        Crossfade(tab, animationSpec = tween(180), label = "tab") { t ->
            when (t) {
                0 -> PlansTab(store, plan, now, onSelectPlan, onOpen, onDialog = { dialog = it })
                1 -> BodyTab(store)
                else -> SettingsTab(store)
            }
        }
        BottomBar(tab, onTab, Modifier.align(Alignment.BottomCenter), onAdd = if (tab == 2) null else {
            { dialog = when { tab == 1 -> "body"; plan == null -> "plan"; else -> "exercise" } }
        })
    }
    val close = { dialog = null }
    when (dialog) {
        "plan" -> TextDialog(stringResource(R.string.new_plan), stringResource(R.string.plan_name_hint), "", close) { name ->
            val id = newId()
            store.update { it.upsertPlan(Plan(id, name)) }
            onSelectPlan(id)
            close()
        }
        "rename" -> if (plan != null) TextDialog(stringResource(R.string.rename_plan), stringResource(R.string.name), plan.name, close) { name ->
            store.update { it.upsertPlan(plan.copy(name = name)) }
            close()
        }
        "clonePlan" -> if (plan != null) TextDialog(stringResource(R.string.clone_plan), stringResource(R.string.name),
            stringResource(R.string.copy_name, plan.name), close) { name ->
            val before = store.data.plans.map { it.id }.toSet()
            store.update { it.clonePlan(plan.id, name) }
            store.data.plans.firstOrNull { it.id !in before }?.let { onSelectPlan(it.id) }
            close()
        }
        "sortPlans" -> SortPlansDialog(store.data.plans, close) { ids ->
            store.update { it.reorderPlans(ids) }
            close()
        }
        "deletePlan" -> if (plan != null) ConfirmDialog(stringResource(R.string.confirm_delete_plan, plan.name), close) {
            store.update { it.deletePlan(plan.id) }
            close()
        }
        "exercise" -> if (plan != null) ExerciseDialog(null, store.data.defaults, close) { e ->
            store.update { it.upsertExercise(plan.id, e) }
            close()
        }
        "body" -> NumberDialog(stringResource(R.string.log_body_weight), stringResource(R.string.weight_kg), close) { w ->
            store.update { it.copy(body = it.body + WeightEntry(System.currentTimeMillis(), w)) }
            close()
        }
    }
}

@Composable
fun PlansTab(store: Store, plan: Plan?, now: Long, onSelectPlan: (Long) -> Unit, onOpen: (Long) -> Unit, onDialog: (String) -> Unit) {
    val plans = store.data.plans
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val gap = with(LocalDensity.current) { 12.dp.toPx() }
    var menu by remember { mutableStateOf<Long?>(null) }
    var toDelete by remember { mutableStateOf<Exercise?>(null) }
    var toClone by remember { mutableStateOf<Exercise?>(null) }
    var toEdit by remember { mutableStateOf<Exercise?>(null) }
    var toMove by remember { mutableStateOf<Exercise?>(null) }

    // Drag-to-reorder: a local copy of the order follows the finger; it is saved once on release.
    var order by remember(plan?.id, plan?.exercises) { mutableStateOf(plan?.exercises.orEmpty()) }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    fun heightOf(id: Long) = listState.layoutInfo.visibleItemsInfo.find { it.key == id }?.size?.toFloat()
    fun drag(dy: Float) {
        val id = dragId ?: return
        dragY += dy
        val i = order.indexOfFirst { it.id == id }
        val j = if (dragY > 0) i + 1 else i - 1
        if (j !in order.indices) return
        val h = (heightOf(order[j].id) ?: return) + gap
        if (kotlin.math.abs(dragY) > h / 2) {
            order = order.toMutableList().apply { add(j, removeAt(i)) }
            dragY -= if (j > i) h else -h
        }
    }

    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header(stringResource(R.string.title_training)) }
        item {
            PlanCards(plans, plan, { p -> p.exercises.count { store.data.isDone(it.id, now) } }, onSelectPlan, onDialog)
        }
        if (plan == null) {
            item { Empty(stringResource(R.string.empty_no_plans)) }
            return@LazyColumn
        }
        item {
            Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.exercises), style = MaterialTheme.typography.titleMedium)
                val doneCount = plan.exercises.count { store.data.isDone(it.id, now) }
                Text(
                    // An empty plan has nothing to count: "0/0 done" is noise.
                    if (plan.exercises.isEmpty()) "" else stringResource(R.string.done_count, doneCount, plan.exercises.size),
                    Modifier.weight(1f).padding(start = 8.dp),
                    color = if (doneCount > 0 && doneCount == plan.exercises.size) Accent else Muted,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (doneCount > 0) {
                    TextButton({ store.update { d -> d.clearDone(plan.exercises.map { it.id }) } }) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.clear), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        if (plan.exercises.isEmpty()) item { Empty(stringResource(R.string.empty_no_exercises)) }
        items(order, key = { it.id }) { e ->
            val dragging = e.id == dragId
            ExerciseCard(
                e,
                done = store.data.isDone(e.id, now),
                modifier = if (dragging) Modifier.zIndex(1f).graphicsLayer {
                    translationY = dragY
                    scaleX = 1.03f
                    scaleY = 1.03f
                } else Modifier.animateItem(),
                handle = Modifier.pointerInput(e.id) {
                    detectDragGestures(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragId = e.id
                            dragY = 0f
                        },
                        onDragEnd = {
                            store.update { it.reorderExercises(plan.id, order.map { x -> x.id }) }
                            dragId = null
                            dragY = 0f
                        },
                        onDragCancel = {
                            order = plan.exercises
                            dragId = null
                            dragY = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            drag(amount.y)
                        },
                    )
                },
                onToggle = { store.update { it.toggleDone(e.id) } },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menu = e.id
                },
                onClick = { onOpen(e.id) },
            ) {
                AppMenu(menu == e.id, { menu = null }, listOfNotNull(
                    MenuAction(Icons.Default.Edit, stringResource(R.string.edit)) { toEdit = e },
                    MenuAction(ImageVector.vectorResource(R.drawable.ic_copy), stringResource(R.string.clone_exercise)) { toClone = e },
                    if (plans.size > 1) MenuAction(Icons.Default.ArrowForward, stringResource(R.string.clone_to_plan)) { toMove = e } else null,
                    MenuAction(Icons.Default.Delete, stringResource(R.string.delete_exercise), destructive = true) { toDelete = e },
                ))
            }
        }
    }
    toDelete?.let { e ->
        ConfirmDialog(stringResource(R.string.confirm_delete_exercise, e.name), onDismiss = { toDelete = null }) {
            store.update { it.deleteExercise(plan!!.id, e.id) }
            toDelete = null
        }
    }
    toClone?.let { e ->
        TextDialog(stringResource(R.string.clone_exercise), stringResource(R.string.name),
            stringResource(R.string.copy_name, e.name), onDismiss = { toClone = null }) { name ->
            store.update { it.cloneExercise(plan!!.id, e.id, name) }
            toClone = null
        }
    }
    toEdit?.let { e ->
        ExerciseDialog(e, store.data.defaults, onDismiss = { toEdit = null }) { ex ->
            store.update { it.upsertExercise(plan!!.id, ex) }
            toEdit = null
        }
    }
    toMove?.let { e ->
        PlanPickerDialog(
            stringResource(R.string.clone_to_plan), plans.filter { it.id != plan!!.id },
            onDismiss = { toMove = null },
        ) { target ->
            store.update { it.cloneExerciseToPlan(plan!!.id, e.id, target.id) }
            toMove = null
        }
    }
}

@Composable
fun ExerciseCard(
    e: Exercise, done: Boolean, modifier: Modifier, handle: Modifier,
    onToggle: () -> Unit, onLongClick: () -> Unit, onClick: () -> Unit,
    menu: @Composable () -> Unit = {},
) = GlassCard(
    modifier.padding(horizontal = 20.dp).fillMaxWidth().alpha(if (done) 0.6f else 1f),
    // End padding is 8dp: the check's 48dp touch area puts its visible ring 16dp from the edge.
    onClick = onClick, onLongClick = onLongClick, padding = PaddingValues(start = 4.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(handle.size(width = 36.dp, height = 48.dp), contentAlignment = Alignment.Center) {
            Icon(ImageVector.vectorResource(R.drawable.ic_drag), stringResource(R.string.drag_to_reorder), tint = Muted)
        }
        Column(Modifier.weight(1f)) {
            Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val subtitle = listOfNotNull(
                if (e.cardio) e.intensity.ifBlank { stringResource(R.string.cardio) } else null,
                e.seat.takeIf { !e.cardio && it.isNotBlank() }?.let { stringResource(R.string.seat_value, it) },
            ).joinToString("  ·  ")
            if (subtitle.isNotEmpty()) Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        // Strength: sets × reps sit beside the weight. A bodyweight exercise has no load to show,
        // so its 0 kg column is omitted; cardio shows its duration.
        val showWeight = e.cardio || e.tracked > 0.0
        if (!e.cardio || showWeight) Column(horizontalAlignment = Alignment.Start) {
            if (showWeight) Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val varied = if (e.cardio) null else e.cardWeights()
                if (varied != null) Text(varied, Modifier.widthIn(max = 150.dp), style = MaterialTheme.typography.titleMedium,
                    color = Accent, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                else Text(fmt(e.tracked), style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.85f),
                    color = Accent, fontWeight = FontWeight.Bold)
                Text(e.unit, style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            if (!e.cardio) Text(e.cardReps(), Modifier.widthIn(max = 150.dp), style = MaterialTheme.typography.bodyMedium,
                color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CheckCircle(done, e.name, onToggle)
    }
    menu() // popup anchored to the card
}

@Composable
fun BodyTab(store: Store) {
    val entries = store.data.body.sortedBy { it.date }
    var toDelete by remember { mutableStateOf<WeightEntry?>(null) }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header(stringResource(R.string.title_body)) }
        if (entries.isEmpty()) {
            item { Empty(stringResource(R.string.empty_no_weighins)) }
            return@LazyColumn
        }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Text(stringResource(R.string.current_weight), color = Muted, style = MaterialTheme.typography.labelLarge)
                BigWeight(entries.last().weight)
                Text(stringResource(R.string.last_weighin, fmtDate(entries.last().date)), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { ChartCard(entries) }
        items(entries.reversed(), key = { it.date }) { e ->
            HistoryRow(e, trailing = {
                IconButton({ toDelete = e }) { Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = Muted) }
            })
        }
    }
    toDelete?.let { e ->
        ConfirmDialog(stringResource(R.string.confirm_delete_entry, fmt(e.weight), fmtDate(e.date)), onDismiss = { toDelete = null }) {
            store.update { d -> d.copy(body = d.body - e) }
            toDelete = null
        }
    }
}

@Composable
fun SettingsTab(store: Store) {
    val ctx = LocalContext.current
    val current = remember { Lang.get(ctx) }
    val version = remember { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName.orEmpty() }
    var importPreview by remember { mutableStateOf<Data?>(null) }
    var notice by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // Plain JSON, same bytes as files/gym.json; the system picker grants access to one chosen file.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = try {
                ctx.contentResolver.openOutputStream(uri)
                    ?.use { it.write(store.data.toJson().toByteArray(Charsets.UTF_8)) } != null
            } catch (e: Exception) {
                false
            }
            notice = R.string.export_title to if (ok) R.string.export_done else R.string.export_failed
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val backup = try {
                ctx.contentResolver.openInputStream(uri)?.use { it.readTextAtMost() }?.let(::parseBackup)
            } catch (e: Exception) {
                null
            }
            if (backup == null) notice = R.string.import_title to R.string.import_invalid else importPreview = backup
        }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header(stringResource(R.string.title_settings)) }
        item { SectionLabel(stringResource(R.string.language)) }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), padding = PaddingValues(vertical = 4.dp)) {
                listOf(
                    "" to stringResource(R.string.lang_system),
                    "en" to stringResource(R.string.lang_en),
                    "pt" to stringResource(R.string.lang_pt),
                ).forEachIndexed { i, (tag, label) ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = GlassBorder)
                    Row(
                        Modifier.fillMaxWidth().selectable(tag == current, role = Role.RadioButton) {
                            if (tag != current) {
                                Lang.set(ctx, tag)
                                (ctx as? Activity)?.recreate()
                            }
                        }.padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                            color = if (tag == current) Accent else Ink)
                        if (tag == current) Icon(Icons.Default.Check, null, tint = Accent)
                    }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.data)) }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), padding = PaddingValues(vertical = 4.dp)) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .clickable { exportLauncher.launch("gymplan-${LocalDate.now()}.json") }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.export_data), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, color = Ink)
                    Icon(painterResource(R.drawable.ic_export), null, tint = Muted)
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = GlassBorder)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .clickable { importLauncher.launch(arrayOf("*/*")) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.import_data), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, color = Ink)
                    Icon(painterResource(R.drawable.ic_import), null, tint = Muted)
                }
            }
        }
        item { SectionLabel(stringResource(R.string.about)) }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.version, version), color = Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.about_text), color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    importPreview?.let { d ->
        AppDialog(
            stringResource(R.string.import_question),
            stringResource(R.string.import_action),
            onDismiss = { importPreview = null },
            onConfirm = {
                store.update { d }
                importPreview = null
                notice = R.string.import_title to R.string.import_done
            },
            destructive = true,
        ) {
            Column {
                Text(
                    stringResource(R.string.import_summary, d.plans.size, d.plans.sumOf { it.exercises.size }, d.body.size),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.import_warning), color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    notice?.let { (title, text) ->
        NoticeDialog(stringResource(title), stringResource(text)) { notice = null }
    }
}

// ---------- Exercise ----------

@Composable
fun ExerciseScreen(store: Store, plan: Plan, ex: Exercise, now: Long, onBack: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val history = store.data.history[ex.id].orEmpty()
    val done = store.data.isDone(ex.id, now)
    LazyColumn(
        Modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Header(ex.name, onBack) {
                CircleButton(Icons.Default.Edit, stringResource(R.string.edit)) { editing = true }
                CircleButton(Icons.Default.Delete, stringResource(R.string.delete_exercise)) { deleting = true }
            }
        }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                // A bodyweight exercise has no load to show: omit the 0 kg line instead.
                if (ex.cardio || ex.tracked > 0.0) {
                    Text(stringResource(if (ex.cardio) R.string.duration else R.string.working_weight), color = Muted,
                        style = MaterialTheme.typography.labelLarge)
                    BigWeight(ex.tracked, ex.unit)
                    Spacer(Modifier.height(12.dp))
                }
                // Pills wrap onto a new line instead of squeezing the long weight summary into a column.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ex.cardio) Pill(ex.intensity.ifBlank { stringResource(R.string.cardio) })
                    else {
                        Pill(pluralStringResource(R.plurals.sets_count, ex.sets, ex.sets))
                        Pill(stringResource(R.string.reps_value, ex.repsSummary()))
                        if (ex.tracked > 0.0 && ex.weightsPerSet.distinct().size > 1) {
                            Pill(stringResource(R.string.weight_value, ex.weightsSummary()))
                        }
                    }
                }
            }
        }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                if (!ex.cardio) {
                    InfoRow(stringResource(R.string.seat), ex.seat)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = GlassBorder)
                }
                InfoRow(stringResource(R.string.adjustments), ex.notes)
            }
        }
        item {
            val mod = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp)
            val shape = RoundedCornerShape(16.dp)
            val toggle = { store.update { it.toggleDone(ex.id) } }
            if (done) OutlinedButton(toggle, mod, shape = shape) {
                Icon(Icons.Default.Check, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.completed_undo), fontWeight = FontWeight.SemiBold)
            }
            else Button(toggle, mod, shape = shape) { Text(stringResource(R.string.mark_complete), fontWeight = FontWeight.SemiBold) }
        }
        item {
            OutlinedButton(
                { editing = true },
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text(stringResource(if (ex.cardio) R.string.update_duration else R.string.update_weight)) }
        }
        item { ChartCard(history, ex.unit) }
        items(history.reversed(), key = { it.date }) { h -> HistoryRow(h, ex.unit) }
    }
    if (editing) ExerciseDialog(ex, store.data.defaults, onDismiss = { editing = false }) { e ->
        store.update { it.upsertExercise(plan.id, e) }
        editing = false
    }
    if (deleting) ConfirmDialog(stringResource(R.string.confirm_delete_exercise, ex.name), onDismiss = { deleting = false }) {
        store.update { it.deleteExercise(plan.id, ex.id) }
        onBack()
    }
}

// ---------- Plan selector ----------

/** Plan cards that carry today's progress, so picking a plan doubles as a status read. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlanCards(plans: List<Plan>, selected: Plan?, doneOf: (Plan) -> Int, onSelect: (Long) -> Unit, onDialog: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var menu by remember { mutableStateOf<Long?>(null) }
    val shape = RoundedCornerShape(20.dp)
    val card = Modifier.width(148.dp).heightIn(min = 96.dp)
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(plans, key = { it.id }) { p ->
            val on = p.id == selected?.id
            val n = p.exercises.size
            val done = doneOf(p)
            val frac by animateFloatAsState(if (n == 0) 0f else done.toFloat() / n, tween(300), label = "progress")
            Column(
                card.clip(shape).background(if (on) Accent.copy(alpha = 0.12f) else Glass)
                    .border(if (on) 1.5.dp else 1.dp, if (on) Accent else GlassBorder, shape)
                    // Long-press selects the plan too, so the menu's actions apply to the selected plan.
                    .combinedClickable(role = Role.Tab, onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(p.id)
                        menu = p.id
                    }) { onSelect(p.id) }
                    .semantics { this.selected = on }.padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = if (on) Ink else Ink.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Column {
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(Accent))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (n == 0) stringResource(R.string.no_exercises) else stringResource(R.string.today_progress, done, n),
                        color = if (n > 0 && done == n) Accent else Muted, style = MaterialTheme.typography.labelSmall,
                    )
                }
                AppMenu(menu == p.id, { menu = null }, listOfNotNull(
                    if (plans.size > 1) MenuAction(ImageVector.vectorResource(R.drawable.ic_sort), stringResource(R.string.sort_plans)) { onDialog("sortPlans") } else null,
                    MenuAction(ImageVector.vectorResource(R.drawable.ic_copy), stringResource(R.string.clone_plan)) { onDialog("clonePlan") },
                    MenuAction(Icons.Default.Edit, stringResource(R.string.rename_plan)) { onDialog("rename") },
                    MenuAction(Icons.Default.Delete, stringResource(R.string.delete_plan), destructive = true) { onDialog("deletePlan") },
                ))
            }
        }
        item {
            val dash = Muted.copy(alpha = 0.45f)
            Column(
                card.clip(shape).drawBehind {
                    drawRoundRect(dash, cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))))
                }.clickable { onDialog("plan") }.padding(14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Add, null, tint = Accent)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.new_plan), color = Muted, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ---------- Building blocks ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(Glass).border(1.dp, GlassBorder, shape)
            .then(if (onClick != null) Modifier.combinedClickable(onLongClick = onLongClick, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun Header(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) = Row(
    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
) {
    onBack?.let { CircleButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), it) }
    Text(
        title, Modifier.weight(1f),
        style = if (onBack == null) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
    actions()
}

/** Lists the other plans; tapping one hands it to [onPick]. */
@Composable
fun PlanPickerDialog(title: String, plans: List<Plan>, onDismiss: () -> Unit, onPick: (Plan) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, GlassBorder, shape),
        shape = shape,
        containerColor = DialogBg,
        titleContentColor = Ink,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                plans.forEach { p ->
                    Text(
                        p.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(p) }.padding(horizontal = 12.dp, vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel), color = Muted) } },
    )
}

@Composable
fun SectionLabel(text: String) = Text(
    text.uppercase(), Modifier.padding(start = 24.dp, top = 8.dp),
    color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
)

@Composable
fun CircleButton(icon: ImageVector, desc: String, onClick: () -> Unit) = Box(
    Modifier.size(48.dp).clip(CircleShape).background(Glass).border(1.dp, GlassBorder, CircleShape).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) { Icon(icon, desc, Modifier.size(20.dp), tint = Ink) }

@Composable
fun Pill(text: String) = Text(
    text,
    Modifier.clip(CircleShape).background(Glass).border(1.dp, GlassBorder, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
    style = MaterialTheme.typography.labelLarge,
)

@Composable
fun CheckCircle(done: Boolean, label: String, onToggle: () -> Unit) = Box(
    // 48dp touch target around a 32dp ring; read as a checkbox named after the exercise.
    Modifier.size(48.dp).clip(CircleShape)
        .toggleable(done, role = Role.Checkbox, onValueChange = { onToggle() })
        .semantics { contentDescription = label },
    contentAlignment = Alignment.Center,
) {
    val fill by animateColorAsState(if (done) Accent else Color.Transparent, tween(150), label = "check")
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(fill)
            .border(2.dp, if (done) Accent else Muted.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { if (done) Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = OnAccent) }
}

@Composable
fun BigWeight(w: Double, unit: String = "kg") = Row(verticalAlignment = Alignment.Bottom) {
    Text(fmt(w), style = MaterialTheme.typography.displayMedium, color = Accent, fontWeight = FontWeight.SemiBold)
    Text(" $unit", style = MaterialTheme.typography.titleMedium, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun InfoRow(label: String, value: String) = Column {
    Text(label, color = Muted, style = MaterialTheme.typography.labelLarge)
    Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun HistoryRow(e: WeightEntry, unit: String = "kg", trailing: @Composable () -> Unit = {}) =
    GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${fmt(e.weight)} $unit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(fmtDate(e.date), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            trailing()
        }
    }

@Composable
fun Empty(text: String) = Text(
    text, Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
    style = MaterialTheme.typography.bodyLarge, color = Muted,
)

@Composable
fun BottomBar(tab: Int, onTab: (Int) -> Unit, modifier: Modifier, onAdd: (() -> Unit)?) = Row(
    modifier.navigationBarsPadding().padding(bottom = 16.dp)
        .shadow(20.dp, CircleShape).clip(CircleShape).background(Color(0xFF131B3A))
        .border(1.dp, GlassBorder, CircleShape)
        .animateContentSize(tween(200)) // the bar resizes as labels and the add button come and go
        .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    NavItem(ImageVector.vectorResource(R.drawable.ic_fitness), stringResource(R.string.nav_plans), tab == 0) { onTab(0) }
    NavItem(Icons.Default.Person, stringResource(R.string.nav_body), tab == 1) { onTab(1) }
    NavItem(Icons.Default.Settings, stringResource(R.string.nav_settings), tab == 2) { onTab(2) }
    if (onAdd != null) {
        Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(28.dp).background(GlassBorder))
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Accent).clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Add, stringResource(R.string.add), Modifier.size(24.dp), tint = OnAccent) }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) = Row(
    Modifier.clip(CircleShape).background(if (selected) Accent.copy(alpha = 0.15f) else Color.Transparent)
        .selectable(selected, role = Role.Tab, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    // Selected tab shows its label, so the icon needs no description of its own there.
    Icon(icon, if (selected) null else label, tint = if (selected) Accent else Muted)
    if (selected) Text(label, color = Accent, style = MaterialTheme.typography.labelLarge, maxLines = 1)
}

@Composable
fun ChartCard(entries: List<WeightEntry>, unit: String = "kg") = GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
    Text(stringResource(R.string.evolution), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    if (entries.size < 2) {
        Text(stringResource(R.string.need_two_values), color = Muted, style = MaterialTheme.typography.bodyMedium)
        return@GlassCard
    }
    val sorted = entries.sortedBy { it.date }
    val min = sorted.minOf { it.weight }
    val max = sorted.maxOf { it.weight }
    val delta = sorted.last().weight - sorted.first().weight
    Text(
        // Typographic minus (U+2212), not a hyphen, to pair with the "+".
        stringResource(R.string.delta_since, (if (delta >= 0) "+" else "−") + fmt(kotlin.math.abs(delta)), unit, fmtDate(sorted.first().date)),
        color = Muted, style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(12.dp))
    Text("${fmt(max)} $unit", style = MaterialTheme.typography.labelSmall, color = Muted)
    Canvas(Modifier.fillMaxWidth().height(160.dp).padding(6.dp)) {
        val vSpan = (max - min).takeIf { it > 0 } ?: 1.0
        val t0 = sorted.first().date
        val tSpan = (sorted.last().date - t0).takeIf { it > 0 } ?: 1L
        val pts = sorted.map {
            Offset(
                (it.date - t0).toFloat() / tSpan * size.width,
                size.height - ((it.weight - min) / vSpan).toFloat() * size.height,
            )
        }
        for (i in 0..3) {
            val y = size.height * i / 3
            drawLine(GlassBorder, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val line = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        val fill = Path().apply {
            addPath(line)
            lineTo(pts.last().x, size.height)
            lineTo(pts.first().x, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(Accent.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, Accent, style = Stroke(3.dp.toPx()))
        pts.forEach {
            drawCircle(Bg2, 6.dp.toPx(), it)
            drawCircle(Accent, 4.dp.toPx(), it)
        }
    }
    Text("${fmt(min)} $unit", style = MaterialTheme.typography.labelSmall, color = Muted)
}

class MenuAction(val icon: ImageVector, val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** Long-press menu in the dialog palette; destructive actions are red. */
@Composable
fun AppMenu(expanded: Boolean, onDismiss: () -> Unit, actions: List<MenuAction>) = DropdownMenu(
    expanded, onDismiss,
    shape = RoundedCornerShape(16.dp), containerColor = DialogBg, border = BorderStroke(1.dp, GlassBorder),
) {
    actions.forEach { a ->
        val c = if (a.destructive) Danger else Ink
        DropdownMenuItem(
            text = { Text(a.label, color = c, style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = { Icon(a.icon, null, tint = c) },
            onClick = {
                onDismiss()
                a.onClick()
            },
        )
    }
}

// ---------- Dialogs ----------

/** Plain confirmation with a single OK button, used for export/import results. */
@Composable
fun NoticeDialog(title: String, text: String, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, GlassBorder, shape),
        shape = shape,
        containerColor = DialogBg,
        titleContentColor = Ink,
        textContentColor = Ink.copy(alpha = 0.85f),
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) },
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(onDismiss, shape = RoundedCornerShape(14.dp)) {
                Text(stringResource(R.string.ok), fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

/** Shared dialog chrome: glass border, rounded corners, filled confirm button (red when destructive). */
@Composable
fun AppDialog(
    title: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, GlassBorder, shape),
        shape = shape,
        containerColor = DialogBg,
        titleContentColor = Ink,
        textContentColor = Ink.copy(alpha = 0.85f),
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) },
        text = content,
        confirmButton = {
            Button(
                onConfirm, enabled = confirmEnabled, shape = RoundedCornerShape(14.dp),
                colors = if (destructive) ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White)
                else ButtonDefaults.buttonColors(),
            ) { Text(confirmText, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel), color = Muted) } },
    )
}

@Composable
fun Field(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, minLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null,
) = OutlinedTextField(
    value, onChange, modifier.fillMaxWidth(),
    label = { Text(label) },
    singleLine = singleLine,
    minLines = minLines,
    trailingIcon = trailing,
    shape = RoundedCornerShape(14.dp),
    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Accent, unfocusedBorderColor = GlassBorder,
        focusedContainerColor = Glass, unfocusedContainerColor = Glass,
        focusedLabelColor = Accent, unfocusedLabelColor = Muted, cursorColor = Accent,
    ),
)

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) = Row(
    Modifier.fillMaxWidth().clip(CircleShape).background(Glass).border(1.dp, GlassBorder, CircleShape).padding(4.dp),
) {
    options.forEachIndexed { i, label ->
        Box(
            Modifier.weight(1f).clip(CircleShape).background(if (i == selected) Accent else Color.Transparent)
                .selectable(i == selected, role = Role.RadioButton) { onSelect(i) }.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = if (i == selected) OnAccent else Muted, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Vertical wheel picker: the number in the middle is selected, neighbours fade out. Scroll or
 * fling the wheel; tapping a row scrolls it to the middle.
 */
@Composable
fun NumberWheel(
    value: Int, range: IntRange, onChange: (Int) -> Unit,
    modifier: Modifier = Modifier, rows: Int = 5, itemHeight: Dp = 36.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
) {
    val values = range.toList()
    val initial = (value - range.first).coerceIn(values.indices)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val scope = rememberCoroutineScope()
    var current by remember { mutableIntStateOf(initial) }
    var syncing by remember { mutableStateOf(false) }

    // The item closest to the middle of the wheel is the selection.
    LaunchedEffect(state) {
        snapshotFlow {
            val info = state.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - middle) }?.index
        }.distinctUntilChanged().collect { i ->
            if (!syncing && i != null && i != current && i in values.indices) {
                current = i
                onChange(range.first + i)
            }
        }
    }
    // Follow values changed from outside (e.g. the common Reps wheel resetting every set).
    LaunchedEffect(value) {
        val target = (value - range.first).coerceIn(values.indices)
        if (target != current) {
            syncing = true
            current = target
            state.animateScrollToItem(target)
            syncing = false
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        modifier = modifier.height(itemHeight * rows),
        contentPadding = PaddingValues(vertical = itemHeight * (rows / 2)),
    ) {
        items(values.size, key = { values[it] }) { i ->
            val distance = abs(i - current)
            val alpha by animateFloatAsState(
                when (distance) { 0 -> 1f; 1 -> 0.5f; else -> 0.2f }, tween(140), label = "wheelAlpha",
            )
            val scale by animateFloatAsState(if (distance == 0) 1.2f else 1f, tween(140), label = "wheelScale")
            Box(
                Modifier.fillMaxWidth().height(itemHeight)
                    .clickable { scope.launch { state.animateScrollToItem(i) } }
                    .semantics { selected = distance == 0 },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    values[i].toString(),
                    style = textStyle,
                    fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = Ink,
                    modifier = Modifier.graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
        }
    }
}

// Compact numeric row: label outside the box, three-row wheel inside it.
private val wheelRows = 3
private val wheelItemHeight = 26.dp
private val numericFieldHeight = wheelItemHeight * wheelRows

/** Label above the field; the split action, when present, ends the same line. */
@Composable
fun FieldHeader(
    label: String, hint: String?, active: Boolean, onToggle: (() -> Unit)?, description: String,
    modifier: Modifier = Modifier,
) = Row(
    modifier.fillMaxWidth().height(40.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        label, Modifier.weight(1f).padding(start = 4.dp),
        color = Muted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
    if (active && hint != null) Text(
        hint, Modifier.padding(end = 2.dp),
        color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1,
    )
    if (onToggle != null) SplitToggle(active, description, onToggle)
}

/** Open / close the per-set breakdown; the arrow points down to open and up to close. */
@Composable
fun SplitToggle(active: Boolean, description: String, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val icon = ImageVector.vectorResource(if (active) R.drawable.ic_more_up else R.drawable.ic_more_down)
    Box(
        modifier.size(40.dp).clip(CircleShape).toggleable(active, role = Role.Switch) { onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape)
                .background(if (active) Accent else Glass)
                .border(1.dp, if (active) Accent else GlassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, description, Modifier.size(18.dp), tint = if (active) OnAccent else Muted)
        }
    }
}

/** Compact vertical wheel in a card, sized to line up with [WeightCard]. */
@Composable
fun WheelCard(value: Int, range: IntRange, onChange: (Int) -> Unit, modifier: Modifier = Modifier) = Box(
    modifier.fillMaxWidth().height(numericFieldHeight).clip(RoundedCornerShape(14.dp))
        .background(Glass).border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
) {
    NumberWheel(
        value, range, onChange, Modifier.fillMaxWidth(), rows = wheelRows, itemHeight = wheelItemHeight,
        textStyle = MaterialTheme.typography.titleMedium,
    )
}

/** Same size as the centred wheel item (titleMedium, SemiBold, scaled 1.2x in [NumberWheel]). */
@Composable
private fun selectedWheelStyle(color: Color) = MaterialTheme.typography.titleMedium.copy(
    color = color, fontWeight = FontWeight.SemiBold,
    fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.2f,
)

/** Weight input drawn like [WheelCard] so the three numeric fields share one height. */
@Composable
fun WeightCard(value: String, onChange: (String) -> Unit, unit: String, modifier: Modifier = Modifier) = Box(
    modifier.fillMaxWidth().height(numericFieldHeight).clip(RoundedCornerShape(14.dp))
        .background(Glass).border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value, onChange, Modifier.weight(1f),
            singleLine = true,
            textStyle = selectedWheelStyle(color = Ink),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(Accent),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) Text("0", color = Muted, style = selectedWheelStyle(color = Muted))
                    inner()
                }
            },
        )
        Text(unit, color = Muted, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Label above a text field, so the label never floats inside the box. */
@Composable
fun LabeledField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, minLines: Int = 1,
) = Column(modifier) {
    Text(
        label, Modifier.padding(start = 4.dp, bottom = 6.dp),
        color = Muted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(),
        singleLine = singleLine, minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent, unfocusedBorderColor = GlassBorder,
            focusedContainerColor = Glass, unfocusedContainerColor = Glass, cursorColor = Accent,
        ),
    )
}

/** Per-set wheel in a card: set label above three visible numbers. */
@Composable
fun SetWheel(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit, modifier: Modifier = Modifier) = Column(
    modifier.clip(RoundedCornerShape(14.dp)).background(Glass)
        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(top = 6.dp, bottom = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
    NumberWheel(value, range, onChange, Modifier.fillMaxWidth(), rows = 3, itemHeight = 32.dp)
}

/** One equal-width cell per set; past six sets it wraps into fixed rows of four. */
@Composable
fun PerSetGrid(count: Int, modifier: Modifier = Modifier, cell: @Composable (Modifier, Int) -> Unit) =
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (count <= 6) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(count) { i -> cell(Modifier.weight(1f), i) }
        }
    } else {
        (0 until count).chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { c ->
                    val i = row.getOrNull(c)
                    if (i != null) cell(Modifier.weight(1f), i) else Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) = AppDialog(
    stringResource(R.string.delete_question), stringResource(R.string.delete), onDismiss, onConfirm, destructive = true,
) { Text(text, style = MaterialTheme.typography.bodyLarge) }

@Composable
fun TextDialog(title: String, label: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AppDialog(title, stringResource(R.string.save), onDismiss, { onSave(v.trim()) }, confirmEnabled = v.isNotBlank()) {
        Field(v, { v = it }, label)
    }
}

@Composable
fun NumberDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var v by remember { mutableStateOf("") }
    val n = parseNum(v)
    AppDialog(title, stringResource(R.string.save), onDismiss, { n?.let(onSave) }, confirmEnabled = n != null && n > 0) {
        Field(v, { v = it }, label, keyboard = KeyboardType.Decimal)
    }
}

@Composable
fun SortPlansDialog(plans: List<Plan>, onDismiss: () -> Unit, onSave: (List<Long>) -> Unit) {
    var order by remember { mutableStateOf(plans) }
    fun move(i: Int, j: Int) { order = order.toMutableList().apply { add(j, removeAt(i)) } }
    AppDialog(stringResource(R.string.sort_plans), stringResource(R.string.save), onDismiss, { onSave(order.map { it.id }) }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            order.forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton({ move(i, i - 1) }, enabled = i > 0) {
                        Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up, p.name),
                            tint = if (i > 0) Ink else Muted.copy(alpha = 0.3f))
                    }
                    IconButton({ move(i, i + 1) }, enabled = i < order.lastIndex) {
                        Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down, p.name),
                            tint = if (i < order.lastIndex) Ink else Muted.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

/** Compact per-set input: the set name sits above the field, so the field never has to float a label. */
/** Compact per-set input: the set name sits above the box, so narrow cells never clip the number. */
@Composable
fun SetValueField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) = Column(
    modifier, horizontalAlignment = Alignment.Start,
) {
    Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
            .background(Glass).border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value, onChange, Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            singleLine = true,
            textStyle = selectedWheelStyle(color = Ink).copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(Accent),
        )
    }
}

/** New exercises start from [defaults]: the sets, reps and weight of the last one added. */
@Composable
fun ExerciseDialog(initial: Exercise?, defaults: ExerciseDefaults, onDismiss: () -> Unit, onSave: (Exercise) -> Unit) {
    val setRange = 1..20
    val repRange = 1..50
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sets by remember { mutableStateOf((initial?.sets ?: defaults.sets).coerceIn(setRange)) }
    var reps by remember {
        mutableStateOf(((initial?.reps ?: defaults.reps).toIntOrNull() ?: 10).coerceIn(repRange))
    }
    var weight by remember { mutableStateOf((initial?.weight ?: defaults.weight)?.let(::fmt) ?: "") }
    var seat by remember { mutableStateOf(initial?.seat ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var cardio by remember { mutableStateOf(initial?.cardio ?: false) }
    var minutes by remember { mutableStateOf(initial?.minutes?.takeIf { it > 0 }?.toString() ?: "") }
    var intensity by remember { mutableStateOf(initial?.intensity ?: "") }

    // Per-set breakdown, opened by the icon inside the Reps / Weight field.
    var repsSplit by remember { mutableStateOf(initial != null && initial.sets > 1 && initial.repsPerSet.isNotEmpty()) }
    var repsPerSet by remember { mutableStateOf(initial?.repsPerSet.orEmpty()) }
    var weightsSplit by remember { mutableStateOf(initial != null && initial.sets > 1 && initial.weightsPerSet.isNotEmpty()) }
    var weightsPerSet by remember { mutableStateOf(initial?.weightsPerSet.orEmpty().map(::fmt)) }

    fun repAt(i: Int) = repsPerSet.getOrElse(i) { reps }
    fun weightAt(i: Int) = weightsPerSet.getOrElse(i) { weight }
    fun weightValueAt(i: Int) = weightAt(i).let { if (it.isBlank()) 0.0 else parseNum(it) }
    fun resizeSets(n: Int) {
        if (repsSplit) repsPerSet = List(n) { repAt(it) }
        if (weightsSplit) weightsPerSet = List(n) { weightAt(it) }
        if (n == 1) { // a single set has nothing to break down
            repsSplit = false
            weightsSplit = false
        }
        sets = n
    }

    val weightN = if (weight.isBlank()) 0.0 else parseNum(weight)
    val minutesN = minutes.trim().toIntOrNull()
    val valid = name.isNotBlank() && if (cardio) minutesN != null && minutesN > 0
        else weightN != null && weightN >= 0 &&
            (!weightsSplit || List(sets) { weightValueAt(it) }.all { it != null && it >= 0 })

    val canSplit = sets > 1

    fun toggleRepsSplit() {
        if (!repsSplit) repsPerSet = List(sets) { reps }
        repsSplit = !repsSplit
    }
    fun toggleWeightsSplit() {
        if (!weightsSplit) weightsPerSet = List(sets) { weight }
        weightsSplit = !weightsSplit
    }

    val perSetHint = stringResource(R.string.select_for_each_set)
    val setsHeader: @Composable (Modifier) -> Unit = { modifier ->
        FieldHeader(stringResource(R.string.sets), null, false, null, "", modifier)
    }
    val repsHeader: @Composable (Boolean, Modifier) -> Unit = { active, modifier ->
        FieldHeader(
            stringResource(R.string.reps), perSetHint, active,
            if (canSplit) ::toggleRepsSplit else null, stringResource(R.string.reps_per_set), modifier,
        )
    }
    val weightHeader: @Composable (Boolean, Modifier) -> Unit = { active, modifier ->
        FieldHeader(
            stringResource(if (active) R.string.weight_kg else R.string.weight), perSetHint, active,
            if (canSplit) ::toggleWeightsSplit else null, stringResource(R.string.weights_per_set), modifier,
        )
    }
    val weightCells: @Composable (Modifier) -> Unit = { modifier ->
        PerSetGrid(sets, modifier) { m, i ->
            SetValueField(
                stringResource(R.string.set_number, i + 1), weightAt(i),
                { v -> weightsPerSet = List(sets) { j -> if (j == i) v else weightAt(j) } }, m,
            )
        }
    }
    val repsCells: @Composable (Modifier) -> Unit = { modifier ->
        PerSetGrid(sets, modifier) { m, i ->
            SetWheel(
                stringResource(R.string.set_number, i + 1), repAt(i), repRange,
                onChange = { v -> repsPerSet = List(sets) { j -> if (j == i) v else repAt(j) } },
                modifier = m,
            )
        }
    }

    AppDialog(
        stringResource(if (initial == null) R.string.new_exercise else R.string.edit_exercise),
        stringResource(R.string.save),
        onDismiss,
        onConfirm = {
            val repValues = if (repsSplit) List(sets) { repAt(it) } else emptyList()
            val weightValues = if (weightsSplit) List(sets) { weightValueAt(it) ?: 0.0 } else emptyList()
            onSave(
                Exercise(
                    initial?.id ?: newId(), name.trim(), sets,
                    (repValues.firstOrNull() ?: reps).toString(),
                    if (weightValues.isNotEmpty()) weightValues.max() else weightN ?: 0.0,
                    if (cardio) "" else seat.trim(), notes.trim(),
                    cardio, minutesN ?: 0, intensity.trim(),
                    repsPerSet = repValues,
                    weightsPerSet = weightValues,
                )
            )
        },
        confirmEnabled = valid,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Segmented(listOf(stringResource(R.string.strength), stringResource(R.string.cardio)), if (cardio) 1 else 0) { cardio = it == 1 }
            LabeledField(
                name, { name = it },
                stringResource(if (cardio) R.string.exercise_cardio_hint else R.string.name),
            )
            if (cardio) {
                LabeledField(minutes, { minutes = it }, stringResource(R.string.duration_min), keyboard = KeyboardType.Number)
                LabeledField(intensity, { intensity = it }, stringResource(R.string.intensity))
            } else if (!repsSplit && !weightsSplit) {
                // Sets, reps and weight share one row; labels sit above their boxes.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(0.85f)) {
                        setsHeader(Modifier)
                        WheelCard(sets, setRange, ::resizeSets)
                    }
                    Column(Modifier.weight(1f)) {
                        repsHeader(false, Modifier)
                        WheelCard(reps, repRange, { v ->
                            reps = v
                            if (repsSplit) repsPerSet = List(sets) { v }
                        })
                    }
                    Column(Modifier.weight(1.15f)) {
                        weightHeader(false, Modifier)
                        WeightCard(weight, { v ->
                            weight = v
                            if (weightsSplit) weightsPerSet = List(sets) { v }
                        }, "kg")
                    }
                }
            } else {
                // A split field takes the full width, with the other numeric fields above it.
                if (weightsSplit && !repsSplit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            setsHeader(Modifier)
                            WheelCard(sets, setRange, ::resizeSets)
                        }
                        Column(Modifier.weight(1f)) {
                            repsHeader(false, Modifier)
                            WheelCard(reps, repRange, { v ->
                                reps = v
                                if (repsSplit) repsPerSet = List(sets) { v }
                            })
                        }
                    }
                } else {
                    Column(Modifier.width(104.dp)) {
                        setsHeader(Modifier)
                        WheelCard(sets, setRange, ::resizeSets)
                    }
                }
                if (repsSplit) {
                    Column {
                        repsHeader(true, Modifier)
                        repsCells(Modifier)
                    }
                }
                Column {
                    weightHeader(weightsSplit, Modifier)
                    if (weightsSplit) weightCells(Modifier)
                    else WeightCard(weight, { v ->
                        weight = v
                        if (weightsSplit) weightsPerSet = List(sets) { v }
                    }, "kg")
                }
            }
            if (!cardio) LabeledField(seat, { seat = it }, stringResource(R.string.seat_adjustment))
            LabeledField(notes, { notes = it }, stringResource(R.string.other_adjustments),
                singleLine = false, minLines = 2)
        }
    }
}
