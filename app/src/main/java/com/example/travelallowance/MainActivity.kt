package com.example.travelallowance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.travelallowance.data.*
import com.example.travelallowance.location.LocationHelper
import com.example.travelallowance.location.RoutesClient
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import com.example.travelallowance.util.StatementExporter

private val Navy = Color(0xFF102A56)
private val Blue = Color(0xFF1769E0)
private val Violet = Color(0xFF7048E8)
private val Ink = Color(0xFF17243A)
private val Muted = Color(0xFF718096)
private val Canvas = Color(0xFFF4F7FC)
private val Green = Color(0xFF168653)
private val Amber = Color(0xFFB76A12)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TravelAllowanceTheme { TravelAllowanceApp() } }
    }
}

@Composable
private fun TravelAllowanceTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Blue, onPrimary = Color.White, secondary = Violet,
        background = Canvas, onBackground = Ink, surface = Color.White,
        onSurface = Ink, surfaceVariant = Color(0xFFEAF0FA),
        outline = Color(0xFFDCE4F0), error = Color(0xFFB42318)
    )
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelAllowanceApp() {
    val context = LocalContext.current
    val repo = remember { TaRepository() }
    val location = remember { LocationHelper(context) }
    val routes = remember { RoutesClient(BuildConfigField.MAPS_KEY) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("Home") }
    var profile by remember { mutableStateOf(EmployeeProfile()) }
    var settings by remember { mutableStateOf(TaSettings()) }
    var journeys by remember { mutableStateOf(listOf<Journey>()) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var start by remember { mutableStateOf<GeoPointRecord?>(null) }
    var destination by remember { mutableStateOf<GeoPointRecord?>(null) }
    var returnPoint by remember { mutableStateOf<GeoPointRecord?>(null) }
    var remarks by remember { mutableStateOf("") }
    var journeyDate by remember { mutableStateOf(LocalDate.now().toString()) }
    val snackbar = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        message = if (result.values.any { it }) "Location permission granted. Capture the location again." else "Location permission is required to capture GPS coordinates."
    }
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun load() {
        scope.launch {
            busy = true
            try {
                repo.ensureUser()
                profile = repo.loadProfile() ?: profile
                settings = repo.loadSettings()
                journeys = repo.loadMonth(selectedMonth)
            } catch (e: Exception) { message = e.message ?: "Cloud loading failed" }
            finally { busy = false }
        }
    }
    LaunchedEffect(Unit) { load() }
    LaunchedEffect(selectedMonth) {
        scope.launch { try { journeys = repo.loadMonth(selectedMonth) } catch (e: Exception) { message = e.message ?: "Could not load month" } }
    }
    LaunchedEffect(message) { if (message.isNotBlank()) snackbar.showSnackbar(message) }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Travel Allowance", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("MANAGEMENT PORTAL", fontSize = 9.sp, letterSpacing = 1.5.sp, color = Color.White.copy(alpha = .75f))
                    }
                },
                actions = {
                    IconButton(onClick = { load() }, enabled = !busy) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val nav = listOf(
                    Triple("Home", Icons.Default.Home, "Home"),
                    Triple("Journey", Icons.Default.AddLocationAlt, "New"),
                    Triple("History", Icons.Default.ReceiptLong, "Records"),
                    Triple("Profile", Icons.Default.Person, "Profile"),
                    Triple("Settings", Icons.Default.Settings, "Settings")
                )
                nav.forEach { (key, icon, label) ->
                    NavigationBarItem(
                        selected = tab == key,
                        onClick = { tab = key },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Blue, selectedTextColor = Blue, indicatorColor = Color(0xFFE8F0FF))
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                "Home" -> DashboardScreen(profile, journeys, selectedMonth, { selectedMonth = it }, onNew = { tab = "Journey" }, onProfile = { tab = "Profile" }, onHistory = { tab = "History" })
                "Profile" -> ProfileScreen(profile, { profile = it }, {
                    scope.launch { busy = true; try { repo.saveProfile(profile); message = "Employee profile saved to cloud." } catch (e: Exception) { message = e.message ?: "Save failed" }; busy = false }
                }, busy,
                    onCreateAccount = { email, password -> scope.launch { busy = true; try { repo.linkEmailPassword(email, password); profile = profile.copy(email = email); repo.saveProfile(profile); message = "Account secured. Use this email and password to sign in on another device." } catch (e: Exception) { message = e.message ?: "Account linking failed" }; busy = false } },
                    onSignIn = { email, password -> scope.launch { busy = true; try { repo.signInEmailPassword(email, password); load(); message = "Signed in and cloud data synchronised." } catch (e: Exception) { message = e.message ?: "Sign-in failed" }; busy = false } }
                )
                "Settings" -> SettingsScreen(settings, { settings = it }, {
                    scope.launch { busy = true; try { repo.saveSettings(settings); message = "TA settings saved to cloud." } catch (e: Exception) { message = e.message ?: "Save failed" }; busy = false }
                }, busy)
                "Journey" -> NewJourneyScreen(
                    start, destination, returnPoint, remarks, journeyDate, busy,
                    onRemarks = { remarks = it }, onDate = { journeyDate = it },
                    onCapture = { label, set ->
                        if (!hasLocationPermission()) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        else scope.launch { try { set(location.current(label)); message = "$label captured successfully." } catch (e: Exception) { message = e.message ?: "GPS capture failed" } }
                    },
                    onSave = {
                        scope.launch {
                            busy = true
                            try {
                                val a = start ?: error("Capture the starting point")
                                val b = destination ?: error("Capture the destination")
                                val c = returnPoint ?: error("Capture the final return point")
                                val parsedDate = runCatching { LocalDate.parse(journeyDate) }.getOrElse { error("Enter journey date as YYYY-MM-DD") }
                                val outward = routes.distanceKm(a, b)
                                val returning = routes.distanceKm(b, c)
                                val total = outward + returning
                                val full = total >= settings.thresholdKm
                                val record = Journey(
                                    UUID.randomUUID().toString(), parsedDate.toString(), a, b, c,
                                    outward, returning, total, if (full) "FULL" else "HALF",
                                    if (full) settings.fullRate else settings.halfRate, remarks
                                )
                                repo.saveJourney(record)
                                selectedMonth = YearMonth.from(parsedDate)
                                journeys = repo.loadMonth(selectedMonth)
                                message = "Journey saved · ${total.fmt()} km · ${if (full) "Full" else "Half"} TA · ${record.taAmount.inr()}"
                                start = null; destination = null; returnPoint = null; remarks = ""; journeyDate = LocalDate.now().toString(); tab = "Home"
                            } catch (e: Exception) { message = e.message ?: "Journey could not be saved" }
                            finally { busy = false }
                        }
                    },
                    setStart = { start = it }, setDestination = { destination = it }, setReturn = { returnPoint = it }
                )
                "History" -> HistoryScreen(
                    selectedMonth, { selectedMonth = it }, journeys, profile,
                    onDelete = { item -> scope.launch { try { repo.deleteJourney(item.date.take(7), item.id); journeys = repo.loadMonth(selectedMonth); message = "Journey deleted." } catch (e: Exception) { message = e.message ?: "Delete failed" } } },
                    onUpdate = { item, newRemarks -> scope.launch { try { repo.saveJourney(item.copy(remarks = newRemarks)); journeys = repo.loadMonth(selectedMonth); message = "Journey updated." } catch (e: Exception) { message = e.message ?: "Update failed" } } }
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    profile: EmployeeProfile, journeys: List<Journey>, month: YearMonth,
    onMonth: (YearMonth) -> Unit, onNew: () -> Unit, onProfile: () -> Unit, onHistory: () -> Unit
) {
    val totalKm = journeys.sumOf { it.totalKm }
    val totalTa = journeys.sumOf { it.taAmount }
    val fullCount = journeys.count { it.taType == "FULL" }
    val halfCount = journeys.count { it.taType == "HALF" }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Navy, Blue, Violet))).padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .16f), modifier = Modifier.size(46.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Badge, null, tint = Color.White) } }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Welcome back", color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
                            Text(profile.name.ifBlank { "Complete your profile" }, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(profile.employeeId.ifBlank { "Employee ID not added" }, color = Color.White.copy(alpha = .78f), fontSize = 11.sp)
                        }
                        IconButton(onClick = onProfile) { Icon(Icons.Default.Edit, null, tint = Color.White) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("MONTHLY OVERVIEW", color = Color.White.copy(alpha = .72f), fontSize = 10.sp, letterSpacing = 1.4.sp)
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
            }
        }
        item { MonthSelectorModern(month, onMonth) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Total journeys", journeys.size.toString(), Icons.Default.DirectionsCar, Color(0xFFE8F1FF), Blue, Modifier.weight(1f))
                MetricCard("Distance", "${totalKm.fmt()} km", Icons.Default.Speed, Color(0xFFE6F7EF), Green, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Full TA", fullCount.toString(), Icons.Default.Assessment, Color(0xFFF0EAFE), Violet, Modifier.weight(1f))
                MetricCard("Half TA", halfCount.toString(), Icons.Default.ReceiptLong, Color(0xFFFFF1E4), Amber, Modifier.weight(1f))
            }
        }
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF633BE0), Color(0xFF276FE6)))).padding(20.dp)) {
                Column {
                    Text("TOTAL TA PAYABLE", color = Color.White.copy(alpha = .78f), fontSize = 11.sp, letterSpacing = 1.2.sp)
                    Text(totalTa.inr(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
                    Text("Based on ${journeys.size} saved journeys", color = Color.White.copy(alpha = .78f), fontSize = 11.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("New journey", Icons.Default.AddLocationAlt, onNew, Modifier.weight(1f))
                ActionTile("Monthly report", Icons.Default.Assessment, onHistory, Modifier.weight(1f))
                ActionTile("Profile", Icons.Default.Person, onProfile, Modifier.weight(1f))
            }
        }
        item { SectionHeading("Recent journeys", "This month") }
        if (journeys.isEmpty()) item { EmptyState("No journeys recorded", "Your saved journeys will appear here.") }
        items(journeys.sortedByDescending { it.date }.take(4)) { JourneyCard(it) }
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: ImageVector, tint: Color, iconColor: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = tint, modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(21.dp)) } }
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun ActionTile(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = Blue, modifier = Modifier.size(25.dp))
            Text(title, fontSize = 10.sp, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun MonthSelectorModern(month: YearMonth, onChange: (YearMonth) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { onChange(month.minusMonths(1)) }) { Text("‹", color = Blue, fontSize = 26.sp) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CalendarMonth, null, tint = Blue, modifier = Modifier.size(18.dp))
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            IconButton(onClick = { onChange(month.plusMonths(1)) }) { Text("›", color = Blue, fontSize = 26.sp) }
        }
    }
}

@Composable
private fun SectionHeading(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
        Text(trailing, fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun JourneyCard(j: Journey, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null, onMap: (() -> Unit)? = null) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(j.date, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Muted)
                StatusPill(if (j.taType == "FULL") "FULL TA" else "HALF TA", j.taType == "FULL")
            }
            Text("${j.start.label}  →  ${j.destination.label}  →  ${j.returnPoint.label}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (j.remarks.isNotBlank()) Text(j.remarks, color = Muted, fontSize = 11.sp)
            HorizontalDivider(color = Color(0xFFEDF1F7))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${j.totalKm.fmt()} km combined", color = Muted, fontSize = 11.sp)
                Text(j.taAmount.inr(), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (onEdit != null || onDelete != null || onMap != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onMap != null) TextButton(onClick = onMap) { Icon(Icons.Default.Map, null); Spacer(Modifier.width(4.dp)); Text("Route") }
                    if (onEdit != null) TextButton(onClick = onEdit) { Icon(Icons.Default.EditNote, null); Spacer(Modifier.width(4.dp)); Text("Edit") }
                    if (onDelete != null) TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB42318))) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, full: Boolean) {
    val color = if (full) Green else Amber
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = .10f)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Default.ReceiptLong, null, tint = Blue, modifier = Modifier.size(34.dp))
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NewJourneyScreen(
    start: GeoPointRecord?, destination: GeoPointRecord?, returnPoint: GeoPointRecord?, remarks: String, journeyDate: String, busy: Boolean,
    onRemarks: (String) -> Unit, onDate: (String) -> Unit, onCapture: (String, (GeoPointRecord) -> Unit) -> Unit, onSave: () -> Unit,
    setStart: (GeoPointRecord) -> Unit, setDestination: (GeoPointRecord) -> Unit, setReturn: (GeoPointRecord) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { ScreenIntro("Record a journey", "Capture all three GPS points to calculate your eligible travel allowance.", Icons.Default.AddLocationAlt) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Journey details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    OutlinedTextField(journeyDate, onDate, modifier = Modifier.fillMaxWidth(), label = { Text("Journey date (YYYY-MM-DD)") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(remarks, onRemarks, modifier = Modifier.fillMaxWidth(), label = { Text("Purpose / remarks") }, placeholder = { Text("e.g. Substation inspection") }, minLines = 2, shape = RoundedCornerShape(14.dp))
                }
            }
        }
        item { LocationPointCard("1", "Starting point", start, "Capture current GPS location", Blue) { onCapture("Starting point", setStart) } }
        item { LocationPointCard("2", "Destination", destination, "Capture GPS at destination", Color(0xFFE34E5C)) { onCapture("Destination", setDestination) } }
        item { LocationPointCard("3", "Final return point", returnPoint, "Capture GPS after return", Green) { onCapture("Final return point", setReturn) } }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Map, null, tint = Blue)
                    Column {
                        Text("How distance is calculated", fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 13.sp)
                        Text("Road distance (Start → Destination) + road distance (Destination → Return). The combined distance determines full or half TA.", color = Color(0xFF43536C), fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Button(onClick = onSave, enabled = !busy && start != null && destination != null && returnPoint != null, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Default.CloudDone, null)
                Spacer(Modifier.width(9.dp)); Text(if (busy) "Calculating route…" else "Calculate & save journey", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocationPointCard(number: String, title: String, point: GeoPointRecord?, subtitle: String, accent: Color, onCapture: () -> Unit) {
    Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .12f), modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Text(number, color = accent, fontWeight = FontWeight.Bold) } }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(if (point == null) subtitle else "${point.latitude}, ${point.longitude}", color = Muted, fontSize = 10.sp)
                if (point != null) Text("GPS captured", color = Green, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            FilledTonalButton(onClick = onCapture, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Icon(Icons.Default.AddLocationAlt, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(if (point == null) "Capture" else "Update", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ProfileScreen(p: EmployeeProfile, onChange: (EmployeeProfile) -> Unit, onSave: () -> Unit, busy: Boolean,
    onCreateAccount: (String, String) -> Unit, onSignIn: (String, String) -> Unit) {
    var accountPassword by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { ScreenIntro("Employee profile", "Your details are attached to each monthly TA statement.", Icons.Default.Person) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Personal & employment details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Field("Employee name", p.name) { onChange(p.copy(name = it)) }
                    Field("Employee ID", p.employeeId) { onChange(p.copy(employeeId = it)) }
                    Field("Designation", p.designation) { onChange(p.copy(designation = it)) }
                    Field("Department", p.department) { onChange(p.copy(department = it)) }
                    Field("Office / Division", p.officeName) { onChange(p.copy(officeName = it)) }
                    Field("Office address", p.officeAddress, singleLine = false) { onChange(p.copy(officeAddress = it)) }
                    Field("Mobile number", p.mobile) { onChange(p.copy(mobile = it)) }
                    Field("Email (optional)", p.email) { onChange(p.copy(email = it)) }
                }
            }
        }
        item { PrimaryButton("Save employee profile", busy, onSave) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Secure cloud account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Link this installation to an email and password so you can recover your records on another phone.", color = Muted, fontSize = 11.sp)
                    Field("Account email", p.email) { onChange(p.copy(email = it)) }
                    OutlinedTextField(accountPassword, { accountPassword = it }, label = { Text("Password (minimum 6 characters)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp))
                    Button(onClick = { if (p.email.contains("@") && accountPassword.length >= 6) onCreateAccount(p.email, accountPassword) }, enabled = !busy && p.email.contains("@") && accountPassword.length >= 6, modifier = Modifier.fillMaxWidth()) { Text("Secure current cloud account") }
                    OutlinedButton(onClick = { if (p.email.contains("@") && accountPassword.length >= 6) onSignIn(p.email, accountPassword) }, enabled = !busy && p.email.contains("@") && accountPassword.length >= 6, modifier = Modifier.fillMaxWidth()) { Text("Sign in to existing account") }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(s: TaSettings, onChange: (TaSettings) -> Unit, onSave: () -> Unit, busy: Boolean) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenIntro("TA rates & settings", "Configure the allowance rates used for all new journeys.", Icons.Default.Settings) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Allowance configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    NumberField("Full TA rate (₹) · 80 km or above", s.fullRate) { onChange(s.copy(fullRate = it)) }
                    NumberField("Half TA rate (₹) · below threshold", s.halfRate) { onChange(s.copy(halfRate = it)) }
                    NumberField("Distance eligibility threshold (km)", s.thresholdKm) { onChange(s.copy(thresholdKm = it)) }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF))) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TA calculation rule", color = Navy, fontWeight = FontWeight.Bold)
                    Text("Combined distance = Start → Destination + Destination → Return", fontSize = 12.sp, color = Ink)
                    Text("• Combined distance ≥ ${s.thresholdKm.fmt()} km: Full TA (${s.fullRate.inr()})", fontSize = 12.sp, color = Green, fontWeight = FontWeight.SemiBold)
                    Text("• Combined distance < ${s.thresholdKm.fmt()} km: Half TA (${s.halfRate.inr()})", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { PrimaryButton("Save TA settings", busy, onSave) }
    }
}

@Composable
private fun HistoryScreen(
    month: YearMonth, onMonth: (YearMonth) -> Unit, journeys: List<Journey>, profile: EmployeeProfile,
    onDelete: (Journey) -> Unit, onUpdate: (Journey, String) -> Unit
) {
    val context = LocalContext.current
    val km = journeys.sumOf { it.totalKm }
    val ta = journeys.sumOf { it.taAmount }
    var editing by remember { mutableStateOf<Journey?>(null) }
    var editRemarks by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Journey?>(null) }
    if (editing != null) AlertDialog(
        onDismissRequest = { editing = null }, title = { Text("Edit journey remarks") },
        text = { OutlinedTextField(editRemarks, { editRemarks = it }, label = { Text("Purpose / remarks") }, minLines = 2) },
        confirmButton = { TextButton(onClick = { editing?.let { onUpdate(it, editRemarks) }; editing = null }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
    )
    if (deleting != null) AlertDialog(
        onDismissRequest = { deleting = null }, title = { Text("Delete journey?") },
        text = { Text("This record will be permanently deleted from the cloud month register.") },
        confirmButton = { TextButton(onClick = { deleting?.let(onDelete); deleting = null }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB42318))) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
    )
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenIntro("Monthly TA statement", "Review, edit, share and export your journey register.", Icons.Default.Assessment) }
        item { MonthSelectorModern(month, onMonth) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))} summary", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { SummaryValue("Journeys", journeys.size.toString()); SummaryValue("Distance", "${km.fmt()} km"); SummaryValue("TA payable", ta.inr()) }
                    HorizontalDivider(color = Color(0xFFEDF1F7))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Full TA · ${journeys.count { it.taType == "FULL" }}", color = Green, fontSize = 12.sp)
                        Text("Half TA · ${journeys.count { it.taType == "HALF" }}", color = Amber, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { runCatching { val f = StatementExporter.exportPdf(context, profile, month, journeys); StatementExporter.share(context, f, "application/pdf") } .onFailure { it.printStackTrace() } }, modifier = Modifier.weight(1f), enabled = journeys.isNotEmpty()) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(5.dp)); Text("PDF") }
                OutlinedButton(onClick = { runCatching { val f = StatementExporter.exportExcel(context, profile, month, journeys); StatementExporter.share(context, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }.onFailure { it.printStackTrace() } }, modifier = Modifier.weight(1f), enabled = journeys.isNotEmpty()) { Text("Excel") }
            }
        }
        item { SectionHeading("Journey register", "${journeys.size} entries") }
        if (journeys.isEmpty()) item { EmptyState("No records for this month", "Choose another month or add a new journey.") }
        items(journeys.sortedBy { it.date }, key = { it.id }) { item ->
            JourneyCard(item,
                onEdit = { editing = item; editRemarks = item.remarks },
                onDelete = { deleting = item },
                onMap = { openJourneyInMaps(context, item) }
            )
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, null, tint = Blue)
                    Spacer(Modifier.width(10.dp))
                    Text("Cloud-synchronised records · ${month}", fontSize = 11.sp, color = Navy)
                }
            }
        }
    }
}

private fun openJourneyInMaps(context: android.content.Context, j: Journey) {
    val origin = "${j.start.latitude},${j.start.longitude}"
    val destination = "${j.destination.latitude},${j.destination.longitude}"
    val waypoint = "${j.returnPoint.latitude},${j.returnPoint.longitude}"
    val url = "https://www.google.com/maps/dir/?api=1&origin=$origin&destination=$waypoint&waypoints=$destination&travelmode=driving"
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 9.sp, color = Muted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun ScreenIntro(title: String, subtitle: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFFE7F0FF), modifier = Modifier.size(46.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Blue, modifier = Modifier.size(23.dp)) } }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(subtitle, fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun PrimaryButton(text: String, busy: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Icon(Icons.Default.CloudDone, null)
        Spacer(Modifier.width(8.dp)); Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Field(label: String, value: String, singleLine: Boolean = true, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else 2, shape = RoundedCornerShape(14.dp))
}

@Composable
private fun NumberField(label: String, value: Double, onValue: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(text, { text = it; it.toDoubleOrNull()?.let(onValue) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
}

private fun Double.fmt(): String = String.format(Locale.US, "%.2f", this)
private fun Double.inr(): String = "₹" + String.format(Locale("en", "IN"), "%,.2f", this)
