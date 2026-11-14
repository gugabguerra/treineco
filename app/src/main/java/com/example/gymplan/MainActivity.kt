package com.example.gymplan

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.io.File
import java.text.DateFormat
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
val AccentDeep = Color(0xFF4A86F0)
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

// ---------- Navigation ----------

@Composable
fun App(store: Store) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var planId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exId by rememberSaveable { mutableStateOf<Long?>(null) }
    val plans = store.data.plans
    val plan = plans.find { it.id == planId } ?: plans.firstOrNull()
    val ex = plan?.exercises?.find { it.id == exId }
    BackHandler(enabled = ex != null) { exId = null }
    if (plan != null && ex != null) ExerciseScreen(store, plan, ex, onBack = { exId = null })
    else HomeScreen(store, tab, plan, onTab = { tab = it }, onSelectPlan = { planId = it }, onOpen = { exId = it })
}

// ---------- Home ----------

@Composable
fun HomeScreen(store: Store, tab: Int, plan: Plan?, onTab: (Int) -> Unit, onSelectPlan: (Long) -> Unit, onOpen: (Long) -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        // Material fade-through between top-level destinations.
        Crossfade(tab, animationSpec = tween(180), label = "tab") { t ->
            when (t) {
                0 -> PlansTab(store, plan, onSelectPlan, onOpen, onDialog = { dialog = it })
                1 -> BodyTab(store)
                else -> SettingsTab()
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
        "deletePlan" -> if (plan != null) ConfirmDialog(stringResource(R.string.confirm_delete_plan, plan.name), close) {
            store.update { it.deletePlan(plan.id) }
            close()
        }
        "exercise" -> if (plan != null) ExerciseDialog(null, close) { e ->
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
fun PlansTab(store: Store, plan: Plan?, onSelectPlan: (Long) -> Unit, onOpen: (Long) -> Unit, onDialog: (String) -> Unit) {
    val plans = store.data.plans
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val gap = with(LocalDensity.current) { 12.dp.toPx() }
    var toDelete by remember { mutableStateOf<Exercise?>(null) }

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
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(plans, key = { it.id }) { p ->
                    val n = p.exercises.size
                    val sub = if (n == 0) stringResource(R.string.no_exercises)
                        else pluralStringResource(R.plurals.exercises_count, n, n)
                    Chip(p.name, sub, p.id == plan?.id) { onSelectPlan(p.id) }
                }
                item { Chip(null, stringResource(R.string.new_plan), false, icon = Icons.Default.Add) { onDialog("plan") } }
            }
        }
        if (plan == null) {
            item { Empty(stringResource(R.string.empty_no_plans)) }
            return@LazyColumn
        }
        item {
            Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.exercises), style = MaterialTheme.typography.titleMedium)
                val doneCount = plan.exercises.count { store.data.isDone(it.id) }
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
                IconButton({ onDialog("rename") }) { Icon(Icons.Default.Edit, stringResource(R.string.rename_plan), tint = Muted) }
                IconButton({ onDialog("deletePlan") }) { Icon(Icons.Default.Delete, stringResource(R.string.delete_plan), tint = Muted) }
            }
        }
        if (plan.exercises.isEmpty()) item { Empty(stringResource(R.string.empty_no_exercises)) }
        items(order, key = { it.id }) { e ->
            val dragging = e.id == dragId
            ExerciseCard(
                e,
                done = store.data.isDone(e.id),
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
                    toDelete = e
                },
                onClick = { onOpen(e.id) },
            )
        }
    }
    toDelete?.let { e ->
        ConfirmDialog(stringResource(R.string.confirm_delete_exercise, e.name), onDismiss = { toDelete = null }) {
            store.update { it.deleteExercise(plan!!.id, e.id) }
            toDelete = null
        }
    }
}

@Composable
fun ExerciseCard(
    e: Exercise, done: Boolean, modifier: Modifier, handle: Modifier,
    onToggle: () -> Unit, onLongClick: () -> Unit, onClick: () -> Unit,
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
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    if (e.cardio) e.intensity.ifBlank { stringResource(R.string.cardio) } else "${e.sets} × ${e.reps}",
                    e.seat.takeIf { it.isNotBlank() }?.let { stringResource(R.string.seat_value, it) },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(fmt(e.tracked), style = MaterialTheme.typography.titleLarge, color = Accent, fontWeight = FontWeight.Bold)
            Text(e.unit, style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        CheckCircle(done, e.name, onToggle)
    }
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
fun SettingsTab() {
    val ctx = LocalContext.current
    val current = remember { Lang.get(ctx) }
    val version = remember { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName.orEmpty() }
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
}

// ---------- Exercise ----------

@Composable
fun ExerciseScreen(store: Store, plan: Plan, ex: Exercise, onBack: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val history = store.data.history[ex.id].orEmpty()
    val done = store.data.isDone(ex.id)
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
                Text(stringResource(if (ex.cardio) R.string.duration else R.string.working_weight), color = Muted,
                    style = MaterialTheme.typography.labelLarge)
                BigWeight(ex.tracked, ex.unit)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ex.cardio) Pill(ex.intensity.ifBlank { stringResource(R.string.cardio) })
                    else {
                        Pill(pluralStringResource(R.plurals.sets_count, ex.sets, ex.sets))
                        Pill(stringResource(R.string.reps_value, ex.reps))
                    }
                }
            }
        }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                InfoRow(stringResource(R.string.seat), ex.seat)
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = GlassBorder)
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
    if (editing) ExerciseDialog(ex, onDismiss = { editing = false }) { e ->
        store.update { it.upsertExercise(plan.id, e) }
        editing = false
    }
    if (deleting) ConfirmDialog(stringResource(R.string.confirm_delete_exercise, ex.name), onDismiss = { deleting = false }) {
        store.update { it.deleteExercise(plan.id, ex.id) }
        onBack()
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
fun Chip(title: String?, sub: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.widthIn(min = 72.dp).clip(shape)
            .background(if (selected) Brush.linearGradient(listOf(Accent, AccentDeep)) else SolidColor(Glass))
            .border(1.dp, if (selected) Color.Transparent else GlassBorder, shape)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val ink = if (selected) OnAccent else Ink
        val titleStyle = MaterialTheme.typography.titleMedium
        // An icon takes the title's line height so icon and text chips stay the same height.
        if (icon != null) Box(Modifier.height(with(LocalDensity.current) { titleStyle.lineHeight.toDp() }), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = ink)
        }
        if (title != null) Text(title, color = ink, fontWeight = FontWeight.SemiBold, style = titleStyle)
        Text(sub, color = if (selected) OnAccent.copy(alpha = 0.7f) else Muted, style = MaterialTheme.typography.labelSmall)
    }
}

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

// ---------- Dialogs ----------

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
) = OutlinedTextField(
    value, onChange, modifier.fillMaxWidth(),
    label = { Text(label) },
    singleLine = singleLine,
    minLines = minLines,
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
fun ExerciseDialog(initial: Exercise?, onDismiss: () -> Unit, onSave: (Exercise) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sets by remember { mutableStateOf(initial?.sets?.toString() ?: "3") }
    var reps by remember { mutableStateOf(initial?.reps ?: "10") }
    var weight by remember { mutableStateOf(initial?.weight?.let(::fmt) ?: "") }
    var seat by remember { mutableStateOf(initial?.seat ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var cardio by remember { mutableStateOf(initial?.cardio ?: false) }
    var minutes by remember { mutableStateOf(initial?.minutes?.takeIf { it > 0 }?.toString() ?: "") }
    var intensity by remember { mutableStateOf(initial?.intensity ?: "") }
    val setsN = sets.trim().toIntOrNull()
    val weightN = if (weight.isBlank()) 0.0 else parseNum(weight)
    val minutesN = minutes.trim().toIntOrNull()
    val valid = name.isNotBlank() && if (cardio) minutesN != null && minutesN > 0
        else setsN != null && setsN > 0 && weightN != null && weightN >= 0
    AppDialog(
        stringResource(if (initial == null) R.string.new_exercise else R.string.edit_exercise),
        stringResource(R.string.save),
        onDismiss,
        onConfirm = {
            onSave(
                Exercise(
                    initial?.id ?: newId(), name.trim(), setsN ?: 1, reps.trim(), weightN ?: 0.0, seat.trim(), notes.trim(),
                    cardio, minutesN ?: 0, intensity.trim(),
                )
            )
        },
        confirmEnabled = valid,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Segmented(listOf(stringResource(R.string.strength), stringResource(R.string.cardio)), if (cardio) 1 else 0) { cardio = it == 1 }
            Field(name, { name = it }, stringResource(if (cardio) R.string.exercise_cardio_hint else R.string.exercise))
            if (cardio) {
                Field(minutes, { minutes = it }, stringResource(R.string.duration_min), keyboard = KeyboardType.Number)
                Field(intensity, { intensity = it }, stringResource(R.string.intensity))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(sets, { sets = it }, stringResource(R.string.sets), Modifier.weight(1f), keyboard = KeyboardType.Number)
                    Field(reps, { reps = it }, stringResource(R.string.reps), Modifier.weight(1f))
                }
                Field(weight, { weight = it }, stringResource(R.string.weight_kg), keyboard = KeyboardType.Decimal)
            }
            Field(seat, { seat = it }, stringResource(R.string.seat_adjustment))
            Field(notes, { notes = it }, stringResource(R.string.other_adjustments), singleLine = false, minLines = 2)
        }
    }
}
