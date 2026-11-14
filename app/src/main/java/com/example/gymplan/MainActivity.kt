package com.example.gymplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
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

@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Accent, onPrimary = OnAccent,
        primaryContainer = Color(0xFF1B3A5C), onPrimaryContainer = Accent,
        secondary = Accent, background = Bg1,
        surface = Color(0xFF121A36), surfaceContainerHigh = Color(0xFF151E42),
        onSurface = Ink, onSurfaceVariant = Muted,
        outline = Color.White.copy(alpha = 0.2f), outlineVariant = GlassBorder,
    ),
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
        if (tab == 0) PlansTab(store, plan, onSelectPlan, onOpen, onDialog = { dialog = it })
        else BodyTab(store)
        BottomBar(tab, onTab, Modifier.align(Alignment.BottomCenter)) {
            dialog = when { tab == 1 -> "body"; plan == null -> "plan"; else -> "exercise" }
        }
    }
    val close = { dialog = null }
    when (dialog) {
        "plan" -> TextDialog("New plan", "Name (e.g. Day A – Legs)", "", close) { name ->
            val id = newId()
            store.update { it.upsertPlan(Plan(id, name)) }
            onSelectPlan(id)
            close()
        }
        "rename" -> if (plan != null) TextDialog("Rename plan", "Name", plan.name, close) { name ->
            store.update { it.upsertPlan(plan.copy(name = name)) }
            close()
        }
        "deletePlan" -> if (plan != null) ConfirmDialog("Delete \"${plan.name}\" and all its exercises and history?", close) {
            store.update { it.deletePlan(plan.id) }
            close()
        }
        "exercise" -> if (plan != null) ExerciseDialog(null, close) { e ->
            store.update { it.upsertExercise(plan.id, e) }
            close()
        }
        "body" -> NumberDialog("Log body weight", "Weight (kg)", close) { w ->
            store.update { it.copy(body = it.body + WeightEntry(System.currentTimeMillis(), w)) }
            close()
        }
    }
}

@Composable
fun PlansTab(store: Store, plan: Plan?, onSelectPlan: (Long) -> Unit, onOpen: (Long) -> Unit, onDialog: (String) -> Unit) {
    val plans = store.data.plans
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("Training") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(plans, key = { it.id }) { p ->
                    Chip(p.name, "${p.exercises.size} exercises", p.id == plan?.id) { onSelectPlan(p.id) }
                }
                item { Chip("+", "New plan", false) { onDialog("plan") } }
            }
        }
        if (plan == null) {
            item { Empty("Create your first plan to start adding exercises.") }
            return@LazyColumn
        }
        item {
            Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Exercises", style = MaterialTheme.typography.titleMedium)
                val doneCount = plan.exercises.count { store.data.isDone(it.id) }
                Text(
                    "  $doneCount/${plan.exercises.size} done today", Modifier.weight(1f),
                    color = if (doneCount > 0 && doneCount == plan.exercises.size) Accent else Muted,
                    style = MaterialTheme.typography.labelLarge,
                )
                IconButton({ onDialog("rename") }) { Icon(Icons.Default.Edit, "Rename plan", tint = Muted) }
                IconButton({ onDialog("deletePlan") }) { Icon(Icons.Default.Delete, "Delete plan", tint = Muted) }
            }
        }
        if (plan.exercises.isEmpty()) item { Empty("No exercises yet. Tap + to add one.") }
        items(plan.exercises, key = { it.id }) { e ->
            ExerciseCard(e, store.data.isDone(e.id), onToggle = { store.update { it.toggleDone(e.id) } }) { onOpen(e.id) }
        }
    }
}

@Composable
fun ExerciseCard(e: Exercise, done: Boolean, onToggle: () -> Unit, onClick: () -> Unit) = GlassCard(
    Modifier.padding(horizontal = 20.dp).fillMaxWidth().alpha(if (done) 0.6f else 1f), onClick,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Avatar(e.name)
        Column(Modifier.weight(1f)) {
            Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(
                    if (e.cardio) e.intensity.ifBlank { "Cardio" } else "${e.sets} × ${e.reps}",
                    e.seat.takeIf { it.isNotBlank() }?.let { "Seat $it" },
                )
                    .filterNotNull().joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(fmt(e.tracked), style = MaterialTheme.typography.titleLarge, color = Accent, fontWeight = FontWeight.Bold)
            Text(e.unit, style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        CheckCircle(done, onToggle)
    }
}

@Composable
fun BodyTab(store: Store) {
    val entries = store.data.body.sortedBy { it.date }
    var toDelete by remember { mutableStateOf<WeightEntry?>(null) }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("Body") }
        if (entries.isEmpty()) {
            item { Empty("No weigh-ins yet. Tap + to log your body weight.") }
            return@LazyColumn
        }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Text("Current weight", color = Muted, style = MaterialTheme.typography.labelLarge)
                BigWeight(entries.last().weight)
                Text("Last weigh-in ${fmtDate(entries.last().date)}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { ChartCard(entries) }
        items(entries.reversed(), key = { it.date }) { e ->
            HistoryRow(e, trailing = { IconButton({ toDelete = e }) { Icon(Icons.Default.Delete, "Delete", tint = Muted) } })
        }
    }
    toDelete?.let { e ->
        ConfirmDialog("Delete ${fmt(e.weight)} kg from ${fmtDate(e.date)}?", onDismiss = { toDelete = null }) {
            store.update { d -> d.copy(body = d.body - e) }
            toDelete = null
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
                CircleButton(Icons.Default.Edit, "Edit") { editing = true }
                CircleButton(Icons.Default.Delete, "Delete exercise") { deleting = true }
            }
        }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Text(if (ex.cardio) "Duration" else "Working weight", color = Muted, style = MaterialTheme.typography.labelLarge)
                BigWeight(ex.tracked, ex.unit)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ex.cardio) Pill(ex.intensity.ifBlank { "Cardio" })
                    else {
                        Pill("${ex.sets} sets")
                        Pill("${ex.reps} reps")
                    }
                }
            }
        }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                InfoRow("Seat", ex.seat)
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = GlassBorder)
                InfoRow("Adjustments", ex.notes)
            }
        }
        item {
            val mod = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp)
            val shape = RoundedCornerShape(16.dp)
            val toggle = { store.update { it.toggleDone(ex.id) } }
            if (done) OutlinedButton(toggle, mod, shape = shape) { Text("Completed today ✓ (undo)", fontWeight = FontWeight.SemiBold) }
            else Button(toggle, mod, shape = shape) { Text("Mark complete", fontWeight = FontWeight.SemiBold) }
        }
        item {
            OutlinedButton(
                { editing = true },
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text(if (ex.cardio) "Update duration / settings" else "Update weight / settings") }
        }
        item { ChartCard(history, ex.unit) }
        items(history.reversed(), key = { it.date }) { h -> HistoryRow(h, ex.unit) }
    }
    if (editing) ExerciseDialog(ex, onDismiss = { editing = false }) { e ->
        store.update { it.upsertExercise(plan.id, e) }
        editing = false
    }
    if (deleting) ConfirmDialog("Delete \"${ex.name}\" and its weight history?", onDismiss = { deleting = false }) {
        store.update { it.deleteExercise(plan.id, ex.id) }
        onBack()
    }
}

// ---------- Building blocks ----------

@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(Glass).border(1.dp, GlassBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun Header(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) = Row(
    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
) {
    onBack?.let { CircleButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", it) }
    Text(
        title, Modifier.weight(1f),
        style = if (onBack == null) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
    actions()
}

@Composable
fun CircleButton(icon: ImageVector, desc: String, onClick: () -> Unit) = Box(
    Modifier.size(44.dp).clip(CircleShape).background(Glass).border(1.dp, GlassBorder, CircleShape).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) { Icon(icon, desc, Modifier.size(20.dp), tint = Ink) }

@Composable
fun Chip(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.widthIn(min = 72.dp).clip(shape)
            .background(if (selected) Brush.linearGradient(listOf(Accent, AccentDeep)) else SolidColor(Glass))
            .border(1.dp, if (selected) Color.Transparent else GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = if (selected) OnAccent else Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
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
fun CheckCircle(done: Boolean, onToggle: () -> Unit) = Box(
    Modifier.size(32.dp).clip(CircleShape)
        .background(if (done) Accent else Color.Transparent)
        .border(2.dp, if (done) Accent else Muted.copy(alpha = 0.6f), CircleShape)
        .clickable(onClickLabel = if (done) "Mark not done" else "Mark done", onClick = onToggle),
    contentAlignment = Alignment.Center,
) { if (done) Icon(Icons.Default.Check, "Done", Modifier.size(20.dp), tint = OnAccent) }

@Composable
fun Avatar(name: String) = Box(
    Modifier.size(48.dp).clip(CircleShape)
        .background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.35f), AccentDeep.copy(alpha = 0.1f))))
        .border(1.dp, Accent.copy(alpha = 0.3f), CircleShape),
    contentAlignment = Alignment.Center,
) { Text(name.take(1).uppercase(), color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }

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
fun HistoryRow(e: WeightEntry, unit: String = "kg", trailing: @Composable () -> Unit = {}) = GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
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
fun BottomBar(tab: Int, onTab: (Int) -> Unit, modifier: Modifier, onAdd: () -> Unit) = Row(
    modifier.navigationBarsPadding().padding(bottom = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
) {
    Row(
        Modifier.shadow(20.dp, CircleShape).clip(CircleShape).background(Color(0xFF131B3A))
            .border(1.dp, GlassBorder, CircleShape).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NavItem(Icons.Default.Home, "Plans", tab == 0) { onTab(0) }
        NavItem(Icons.Default.Person, "Body", tab == 1) { onTab(1) }
    }
    Box(
        Modifier.size(64.dp).shadow(18.dp, CircleShape, ambientColor = Accent, spotColor = Accent).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFC2E4FF), Accent, AccentDeep)))
            .clickable(onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Default.Add, "Add", Modifier.size(28.dp), tint = OnAccent) }
}

@Composable
fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) = Row(
    Modifier.clip(CircleShape).background(if (selected) Accent.copy(alpha = 0.15f) else Color.Transparent)
        .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    Icon(icon, label, tint = if (selected) Accent else Muted)
    if (selected) Text(label, color = Accent, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun ChartCard(entries: List<WeightEntry>, unit: String = "kg") = GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
    Text("Evolution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    if (entries.size < 2) {
        Text("Log at least 2 values to see the chart.", color = Muted, style = MaterialTheme.typography.bodyMedium)
        return@GlassCard
    }
    val sorted = entries.sortedBy { it.date }
    val min = sorted.minOf { it.weight }
    val max = sorted.maxOf { it.weight }
    val delta = sorted.last().weight - sorted.first().weight
    Text(
        (if (delta >= 0) "+" else "") + fmt(delta) + " $unit since " + fmtDate(sorted.first().date),
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

@Composable
fun ConfirmDialog(text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    text = { Text(text) },
    confirmButton = { TextButton(onConfirm) { Text("Delete") } },
    dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
)

@Composable
fun TextDialog(title: String, label: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(v, { v = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton({ onSave(v.trim()) }, enabled = v.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun NumberDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var v by remember { mutableStateOf("") }
    val n = parseNum(v)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(v, { v = it }, label = { Text(label) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        },
        confirmButton = { TextButton({ n?.let(onSave) }, enabled = n != null && n > 0) { Text("Save") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New exercise" else "Edit exercise") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!cardio, { cardio = false }, { Text("Strength") })
                    FilterChip(cardio, { cardio = true }, { Text("Cardio") })
                }
                OutlinedTextField(name, { name = it }, label = { Text(if (cardio) "Exercise (e.g. Treadmill)" else "Exercise") }, singleLine = true)
                if (cardio) {
                    OutlinedTextField(minutes, { minutes = it }, label = { Text("Duration (min)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(intensity, { intensity = it }, label = { Text("Speed / incline / level") }, singleLine = true)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(sets, { sets = it }, Modifier.weight(1f), label = { Text("Sets") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(reps, { reps = it }, Modifier.weight(1f), label = { Text("Reps") }, singleLine = true)
                    }
                    OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                OutlinedTextField(seat, { seat = it }, label = { Text("Seat adjustment") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Other adjustments / notes") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton({
                onSave(
                    Exercise(
                        initial?.id ?: newId(), name.trim(), setsN ?: 1, reps.trim(), weightN ?: 0.0, seat.trim(), notes.trim(),
                        cardio, minutesN ?: 0, intensity.trim(),
                    )
                )
            }, enabled = valid) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
