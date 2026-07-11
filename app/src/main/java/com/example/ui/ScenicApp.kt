package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.domain.CelestialCalculator
import com.example.domain.ScenicPin
import org.json.JSONObject
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.location.LocationServices

private val DarkMapStyleJson = """
[
  {
    "elementType": "geometry",
    "stylers": [
      { "color": "#121214" }
    ]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#e0e0e6" }
    ]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [
      { "color": "#121214" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [
      { "color": "#44444a" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#5a5a62" },
      { "weight": 1.2 }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [
      { "color": "#6e6e78" }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#9b9ba4" },
      { "weight": 1.5 }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      { "color": "#1a3a5f" }
    ]
  },
  {
    "featureType": "poi.park",
    "elementType": "geometry",
    "stylers": [
      { "color": "#1b331b" }
    ]
  }
]
""".trimIndent()

private val DarkMapStyle = com.google.android.gms.maps.model.MapStyleOptions(DarkMapStyleJson)

enum class ScenicTab {
    HOME,
    MAP,
    SETTINGS,
    USER
}

enum class FoldPosture {
    FLAT_BOOK,   // Vertical hinge split (Left Map, Right Dashboard)
    TABLETOP     // Horizontal hinge split (Top Map, Bottom Dashboard)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenicApp(viewModel: ScenicViewModel) {
    val context = LocalContext.current
    val allPins by viewModel.allPins.collectAsStateWithLifecycle()
    val selectedPin by viewModel.selectedPin.collectAsStateWithLifecycle()

    val useFahrenheit by viewModel.settingsManager.useFahrenheit.collectAsStateWithLifecycle()
    val useDmsCoordinates by viewModel.settingsManager.useDmsCoordinates.collectAsStateWithLifecycle()
    val use24HourFormat by viewModel.settingsManager.use24HourFormat.collectAsStateWithLifecycle()
    val defaultFilmStock = "Portra 400"
    val defaultIso = 400
    val defaultAperture = "f/8"
    val enableHaptic by viewModel.settingsManager.enableHaptic.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(ScenicTab.HOME) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showUnsavedWarning by remember { mutableStateOf(false) }

    val navigateWithWarning = { action: () -> Unit ->
        if (hasUnsavedChanges) {
            pendingNavAction = action
            showUnsavedWarning = true
        } else {
            action()
        }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    val triggerHaptic = {
        if (enableHaptic) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        }
    }
    val triggerHapticLight = {
        if (enableHaptic) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Responsive layout sizing based on standard foldables / screen dimensions
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isExpanded = screenWidthDp >= 600

    LaunchedEffect(isExpanded) {
        if (isExpanded && activeTab == ScenicTab.HOME) {
            activeTab = ScenicTab.MAP
        }
    }

    val isBackEnabled = if (isExpanded) {
        selectedPin != null || activeTab != ScenicTab.MAP
    } else {
        selectedPin != null || activeTab != ScenicTab.HOME
    }
    BackHandler(enabled = isBackEnabled) {
        if (selectedPin != null) {
            viewModel.selectPin(null)
        } else if (isExpanded) {
            if (activeTab != ScenicTab.MAP) {
                activeTab = ScenicTab.MAP
            }
        } else if (activeTab != ScenicTab.HOME) {
            activeTab = ScenicTab.HOME
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.quickScoutTrigger.collect {
            triggerQuickScout(context, viewModel)
        }
    }

    // Draggable Split Pane properties
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    var splitRatio by remember { mutableStateOf(0.5f) }
    var foldPosture by remember { mutableStateOf(FoldPosture.FLAT_BOOK) }
    var isSidebarExpanded by remember { mutableStateOf(false) }
    
    // UI dialog triggers
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedLocationForAdd by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var cameraActivePinId by remember { mutableStateOf<Long?>(null) }
    var selectedPinForEdit by remember { mutableStateOf<ScenicPin?>(null) }

    // Request permissions launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            Log.d("ScenicApp", "Location permissions granted")
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            locationPermissionLauncher.launch(needed.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            if (!isExpanded) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Explore",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scenic Scout",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.OfflinePin, contentDescription = "Offline Cache", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isExpanded) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth().testTag("scenic_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == ScenicTab.HOME,
                        onClick = {
                            triggerHapticLight()
                            navigateWithWarning {
                                activeTab = ScenicTab.HOME
                                viewModel.selectPin(null)
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_home_btn")
                    )
                    NavigationBarItem(
                        selected = activeTab == ScenicTab.MAP,
                        onClick = {
                            triggerHapticLight()
                            navigateWithWarning {
                                activeTab = ScenicTab.MAP
                            }
                        },
                        icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                        label = { Text("Map") },
                        modifier = Modifier.testTag("nav_map_btn")
                    )
                    NavigationBarItem(
                        selected = activeTab == ScenicTab.SETTINGS,
                        onClick = {
                            triggerHapticLight()
                            navigateWithWarning {
                                activeTab = ScenicTab.SETTINGS
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings_btn")
                    )
                    NavigationBarItem(
                        selected = activeTab == ScenicTab.USER,
                        onClick = {
                            triggerHapticLight()
                            navigateWithWarning {
                                activeTab = ScenicTab.USER
                            }
                        },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "User") },
                        label = { Text("User") },
                        modifier = Modifier.testTag("nav_user_btn")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isExpanded) {
                val sidebarWidth = if (isSidebarExpanded) 256.dp else 76.dp
                val animatedSidebarWidth by animateDpAsState(
                    targetValue = sidebarWidth,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "sidebarWidth"
                )
                Column(
                    modifier = Modifier
                        .width(animatedSidebarWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 24.dp, horizontal = if (isSidebarExpanded) 12.dp else 8.dp)
                ) {
                    // Brand logo / Toggle
                    if (isSidebarExpanded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "Explore",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scenic Scout",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 18.sp
                                    )
                                )
                            }
                            IconButton(
                                onClick = { isSidebarExpanded = false },
                                modifier = Modifier.testTag("sidebar_collapse_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Collapse Sidebar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Explore",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            IconButton(
                                onClick = { isSidebarExpanded = true },
                                modifier = Modifier.testTag("sidebar_expand_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Expand Sidebar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Navigation Items
                    val menuItems = listOf(
                        Triple(ScenicTab.MAP, "Field Map", Icons.Default.Map),
                        Triple(ScenicTab.SETTINGS, "Settings", Icons.Default.Settings),
                        Triple(ScenicTab.USER, "User Profile", Icons.Default.AccountCircle)
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = if (isSidebarExpanded) Alignment.Start else Alignment.CenterHorizontally
                    ) {
                        menuItems.forEach { (tab, label, icon) ->
                            val isSelected = activeTab == tab
                            val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            val textWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable {
                                        triggerHapticLight()
                                        navigateWithWarning {
                                            activeTab = tab
                                            if (tab == ScenicTab.HOME) {
                                                viewModel.selectPin(null)
                                            }
                                        }
                                    }
                                    .padding(horizontal = if (isSidebarExpanded) 16.dp else 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (isSidebarExpanded) Arrangement.Start else Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isSidebarExpanded) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = textWeight,
                                            color = contentColor
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (!isExpanded || activeTab != ScenicTab.MAP) {
                    SyncStatusIndicator(viewModel)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
            when (activeTab) {
                ScenicTab.HOME -> {
                    if (isExpanded) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                if (foldPosture == FoldPosture.FLAT_BOOK) {
                                    // Row split layout (Book Mode)
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        // Left Pane: Map
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(splitRatio)
                                        ) {
                                            InteractiveMapView(
                                                pins = allPins,
                                                selectedPin = selectedPin,
                                                onPinSelected = { viewModel.selectPin(it) },
                                                onMapClicked = { lat, lng ->
                                                    selectedLocationForAdd = Pair(lat, lng)
                                                    showAddDialog = true
                                                }
                                            )
                                        }

                                        // Interactive Draggable Divider
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(8.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .pointerInput(Unit) {
                                                    detectHorizontalDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        val dragDelta = dragAmount / (screenWidthDp * density)
                                                        splitRatio = (splitRatio + dragDelta).coerceIn(0.25f, 0.75f)
                                                    }
                                                }
                                                .clickable(enabled = false) {},
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight(0.15f)
                                                    .width(3.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }

                                        // Right Pane: Dashboard
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(1f - splitRatio)
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            if (selectedPin != null) {
                                                EnvironmentalDashboard(
                                                    pin = selectedPin!!,
                                                    useFahrenheit = useFahrenheit,
                                                    useDmsCoordinates = useDmsCoordinates,
                                                    use24HourFormat = use24HourFormat,
                                                    viewModel = viewModel,
                                                    onUpdatePin = { viewModel.updatePin(it) },
                                                    onDeletePin = { viewModel.deletePin(it) },
                                                    onCapturePhoto = { pinId ->
                                                        cameraActivePinId = pinId
                                                        showCameraDialog = true
                                                    },
                                                    onClearSelection = {
                                                        navigateWithWarning {
                                                            viewModel.selectPin(null)
                                                        }
                                                    }
                                                )
                                            } else {
                                                EmptyDashboardState(allPins, viewModel) { viewModel.selectPin(it) }
                                            }
                                        }
                                    }
                                } else {
                                    // Column split layout (Tabletop Mode)
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Top Pane: Map
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(splitRatio)
                                        ) {
                                            InteractiveMapView(
                                                pins = allPins,
                                                selectedPin = selectedPin,
                                                onPinSelected = { viewModel.selectPin(it) },
                                                onMapClicked = { lat, lng ->
                                                    selectedLocationForAdd = Pair(lat, lng)
                                                    showAddDialog = true
                                                }
                                            )
                                        }

                                        // Horizontal Draggable Divider
                                        val screenHeightDp = configuration.screenHeightDp
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .pointerInput(Unit) {
                                                    detectVerticalDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        val dragDelta = dragAmount / (screenHeightDp * density)
                                                        splitRatio = (splitRatio + dragDelta).coerceIn(0.25f, 0.75f)
                                                    }
                                                }
                                                .clickable(enabled = false) {},
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.15f)
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }

                                        // Bottom Pane: Dashboard
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f - splitRatio)
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            if (selectedPin != null) {
                                                EnvironmentalDashboard(
                                                    pin = selectedPin!!,
                                                    useFahrenheit = useFahrenheit,
                                                    useDmsCoordinates = useDmsCoordinates,
                                                    use24HourFormat = use24HourFormat,
                                                    viewModel = viewModel,
                                                    onUpdatePin = { viewModel.updatePin(it) },
                                                    onDeletePin = { viewModel.deletePin(it) },
                                                    onCapturePhoto = { pinId ->
                                                        cameraActivePinId = pinId
                                                        showCameraDialog = true
                                                    },
                                                    onClearSelection = {
                                                        navigateWithWarning {
                                                            viewModel.selectPin(null)
                                                        }
                                                    }
                                                )
                                            } else {
                                                EmptyDashboardState(allPins, viewModel) { viewModel.selectPin(it) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Compact (Folded) Single View Layout with sliding sheets or bottom bars
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                if (selectedPin == null) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            QuickScoutHeroButton {
                                                triggerQuickScout(context, viewModel)
                                            }
                                        }
                                        item {
                                            Text(
                                                text = "Recent Pins",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        if (allPins.isEmpty()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(24.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOff,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(48.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text("No pins logged yet", fontWeight = FontWeight.Bold)
                                                        Text("Tap the quick scout trigger or tap anywhere on the map in unfolded view.", textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        } else {
                                            items(allPins, key = { it.id }) { pin ->
                                                val dismissState = rememberSwipeToDismissBoxState(
                                                    confirmValueChange = { value ->
                                                        when (value) {
                                                            SwipeToDismissBoxValue.EndToStart -> {
                                                                viewModel.deletePin(pin)
                                                                true
                                                            }
                                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                                selectedPinForEdit = pin
                                                                false
                                                            }
                                                            else -> false
                                                        }
                                                    }
                                                )

                                                SwipeToDismissBox(
                                                    state = dismissState,
                                                    backgroundContent = {
                                                        val targetColor = when (dismissState.dismissDirection) {
                                                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                                            else -> Color.Transparent
                                                        }
                                                        val animatedColor by animateColorAsState(
                                                            targetValue = targetColor,
                                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                            label = "dismiss_color_anim"
                                                        )
                                                        val targetScale = if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) 1.1f else 0.8f
                                                        val animatedScale by animateFloatAsState(
                                                            targetValue = targetScale,
                                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                            label = "dismiss_scale_anim"
                                                        )
                                                        val alignment = when (dismissState.dismissDirection) {
                                                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                                            else -> Alignment.Center
                                                        }
                                                        val icon = when (dismissState.dismissDirection) {
                                                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                                            SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                                            else -> Icons.Default.Circle
                                                        }
                                                        val label = when (dismissState.dismissDirection) {
                                                            SwipeToDismissBoxValue.StartToEnd -> "Edit"
                                                            SwipeToDismissBoxValue.EndToStart -> "Delete"
                                                            else -> ""
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(animatedColor)
                                                                .padding(horizontal = 16.dp),
                                                            contentAlignment = alignment
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.graphicsLayer(
                                                                    scaleX = animatedScale,
                                                                    scaleY = animatedScale
                                                                )
                                                            ) {
                                                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                                                    Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                                } else if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                                                    Text(label, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onErrorContainer)
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("pin_swipe_box_${pin.id}")
                                                ) {
                                                    CompactPinItem(
                                                        pin = pin,
                                                        useFahrenheit = useFahrenheit,
                                                        useDmsCoordinates = useDmsCoordinates
                                                    ) {
                                                        viewModel.selectPin(pin)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Display the selected pin details
                                    EnvironmentalDashboard(
                                        pin = selectedPin!!,
                                        useFahrenheit = useFahrenheit,
                                        useDmsCoordinates = useDmsCoordinates,
                                        use24HourFormat = use24HourFormat,
                                        viewModel = viewModel,
                                        onUpdatePin = { viewModel.updatePin(it) },
                                        onDeletePin = { viewModel.deletePin(it) },
                                        onCapturePhoto = { pinId ->
                                            cameraActivePinId = pinId
                                            showCameraDialog = true
                                        },
                                        onClearSelection = {
                                            navigateWithWarning {
                                                viewModel.selectPin(null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                ScenicTab.MAP -> {
                    if (!isExpanded) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            InteractiveMapView(
                                pins = allPins,
                                selectedPin = selectedPin,
                                onPinSelected = { pin ->
                                    viewModel.selectPin(pin)
                                    activeTab = ScenicTab.HOME
                                },
                                onMapClicked = { lat, lng ->
                                    selectedLocationForAdd = Pair(lat, lng)
                                    showAddDialog = true
                                }
                            )
                        }
                    } else {
                        val mapWeight by animateFloatAsState(
                            targetValue = if (selectedPin != null) 0.5f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "mapWeight"
                        )
                        
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left pane: Full Screen Immersive Map (shrinks dynamically)
                            Box(
                                modifier = Modifier
                                    .weight(mapWeight)
                                    .fillMaxHeight()
                            ) {
                                InteractiveMapView(
                                    pins = allPins,
                                    selectedPin = selectedPin,
                                    onPinSelected = { pin ->
                                        viewModel.selectPin(pin)
                                    },
                                    onMapClicked = { lat, lng ->
                                        selectedLocationForAdd = Pair(lat, lng)
                                        showAddDialog = true
                                    }
                                )

                                val coroutineScope = rememberCoroutineScope()
                                val context = LocalContext.current

                                // Floating Bento Panel: Weather & Recent Pins
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = selectedPin == null,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically(),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .width(360.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // 1. Cloud & Cache Status Card inside the floating bento
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1.2f)
                                                    ) {
                                                        val lastSyncTime by viewModel.settingsManager.lastSyncTime.collectAsStateWithLifecycle()
                                                        val lastLocalChangeTime by viewModel.settingsManager.lastLocalChangeTime.collectAsStateWithLifecycle()
                                                        val isSyncing by viewModel.firebaseBackupManager.isSyncing.collectAsStateWithLifecycle()
                                                        
                                                        if (isSyncing) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(16.dp),
                                                                strokeWidth = 2.dp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "Syncing...",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        } else {
                                                            val isUpToDate = lastLocalChangeTime <= lastSyncTime || (lastSyncTime > 0 && lastLocalChangeTime == 0L)
                                                            val icon = if (lastSyncTime == 0L) Icons.Default.CloudOff else if (isUpToDate) Icons.Default.CloudDone else Icons.Default.CloudSync
                                                            val text = if (lastSyncTime == 0L) "Local-Only" else if (isUpToDate) "Cloud-Synced" else "Unsynced Edits"
                                                            val iconColor = if (lastSyncTime == 0L) MaterialTheme.colorScheme.onSurfaceVariant else if (isUpToDate) MaterialTheme.colorScheme.primary else Color(0xFFE65100)
                                                            
                                                            Icon(
                                                                imageVector = icon,
                                                                contentDescription = null,
                                                                tint = iconColor,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column {
                                                                Text(
                                                                    text = text,
                                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                val syncText = if (lastSyncTime > 0L) {
                                                                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                                                    sdf.format(Date(lastSyncTime))
                                                                } else {
                                                                    "Offline mode"
                                                                }
                                                                Text(
                                                                    text = syncText,
                                                                    fontSize = 10.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .height(24.dp)
                                                            .width(1.dp)
                                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                    )

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.End,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(start = 8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.OfflinePin,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                text = "Map Cache",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Offline-Ready",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // 2. Weather Widget inside the floating bento
                                            FloatingWeatherWidget(viewModel = viewModel, useFahrenheit = useFahrenheit)

                                            // 3. Recent Pins List inside the floating bento
                                            FloatingRecentPinsWidget(allPins = allPins, onPinClick = { viewModel.selectPin(it) })
                                        }
                                    }
                                }

                                // Center/bottom Quick Scout FAB
                                FloatingActionButton(
                                    onClick = {
                                        triggerQuickScout(context, viewModel)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 32.dp)
                                        .height(56.dp)
                                        .widthIn(min = 180.dp),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddLocationAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "QUICK SCOUT",
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }

                            // Right pane: Half-Screen slider / Details panel
                            if (mapWeight < 0.99f) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f - mapWeight)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    if (selectedPin != null) {
                                        EnvironmentalDashboard(
                                            pin = selectedPin!!,
                                            useFahrenheit = useFahrenheit,
                                            useDmsCoordinates = useDmsCoordinates,
                                            use24HourFormat = use24HourFormat,
                                            viewModel = viewModel,
                                            onUpdatePin = { viewModel.updatePin(it) },
                                            onDeletePin = { viewModel.deletePin(it) },
                                            onCapturePhoto = { pinId ->
                                                cameraActivePinId = pinId
                                                showCameraDialog = true
                                            },
                                            onClearSelection = {
                                                navigateWithWarning {
                                                    viewModel.selectPin(null)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                ScenicTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        foldPosture = foldPosture,
                        onFoldPostureChange = { foldPosture = it }
                    )
                }
                ScenicTab.USER -> {
                    AccountScreen(viewModel = viewModel)
                }
            }
        }
    }
}

        // Add Pin Dialog
        if (showAddDialog && selectedLocationForAdd != null) {
            AddPinDialog(
                lat = selectedLocationForAdd!!.first,
                lng = selectedLocationForAdd!!.second,
                onDismiss = {
                    showAddDialog = false
                    selectedLocationForAdd = null
                },
                onSave = { name, type, timeCategory ->
                    viewModel.addPin(
                        name = name,
                        latitude = selectedLocationForAdd!!.first,
                        longitude = selectedLocationForAdd!!.second,
                        landscapeType = type,
                        timeOfDayCategory = timeCategory,
                        filmStock = defaultFilmStock,
                        iso = defaultIso,
                        aperture = defaultAperture,
                        context = context
                    )
                    showAddDialog = false
                    selectedLocationForAdd = null
                }
            )
        }

        // CameraX Capture Dialog
        if (showCameraDialog && cameraActivePinId != null) {
            CameraCaptureDialog(
                onDismiss = {
                    showCameraDialog = false
                    cameraActivePinId = null
                },
                onPhotoCaptured = { uriPath ->
                    val currentSelected = selectedPin
                    if (currentSelected != null && currentSelected.id == cameraActivePinId) {
                        val updated = currentSelected.copy(photoUri = uriPath)
                        viewModel.updatePin(updated)
                    }
                    showCameraDialog = false
                    cameraActivePinId = null
                }
            )
        }

        if (showUnsavedWarning) {
            AlertDialog(
                onDismissRequest = {
                    showUnsavedWarning = false
                    pendingNavAction = null
                },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved edits in your pin notes. Are you sure you want to discard them?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUnsavedWarning = false
                            viewModel.setHasUnsavedChanges(false) // Reset warning flag so we can navigate
                            pendingNavAction?.invoke()
                            pendingNavAction = null
                        }
                    ) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showUnsavedWarning = false
                            pendingNavAction = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }


        // Edit Pin Dialog
        if (selectedPinForEdit != null) {
            EditPinDialog(
                pin = selectedPinForEdit!!,
                onDismiss = { selectedPinForEdit = null },
                onSave = { updatedPin ->
                    viewModel.updatePin(updatedPin)
                    selectedPinForEdit = null
                }
            )
        }
    }
}

@Composable
fun QuickScoutHeroButton(onClick: () -> Unit) {
    val view = androidx.compose.ui.platform.LocalView.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onClick()
            }
            .testTag("quick_scout_hero_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddLocationAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "QUICK SCOUT PIN",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun CompactPinItem(
    pin: ScenicPin,
    useFahrenheit: Boolean,
    useDmsCoordinates: Boolean,
    onClick: () -> Unit
) {
    val view = androidx.compose.ui.platform.LocalView.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .testTag("compact_pin_item_${pin.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant), // Soft green background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getLandscapeIcon(pin.landscapeType),
                    contentDescription = null,
                    tint = getLandscapeTypeColor(pin.landscapeType),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pin.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                val tempStr = formatTemperatureCompact(pin.temperature, useFahrenheit)
                val coordStr = formatCoordinates(pin.latitude, pin.longitude, useDmsCoordinates)
                Text(
                    text = "$coordStr$tempStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val weatherIcon = when {
                    pin.weatherStatus?.lowercase()?.contains("clear") == true -> Icons.Outlined.WbSunny
                    pin.weatherStatus?.lowercase()?.contains("cloud") == true -> Icons.Outlined.Cloud
                    pin.weatherStatus?.lowercase()?.contains("rain") == true -> Icons.Outlined.Umbrella
                    pin.weatherStatus?.lowercase()?.contains("snow") == true -> Icons.Outlined.AcUnit
                    else -> Icons.Outlined.CloudQueue
                }
                val weatherLabel = pin.weatherStatus ?: "Pending"
                val weatherColor = when {
                    pin.weatherStatus?.lowercase()?.contains("clear") == true -> Color(0xFFFFB300) // Sunny Amber
                    pin.weatherStatus?.lowercase()?.contains("cloud") == true -> MaterialTheme.colorScheme.primary
                    pin.weatherStatus?.lowercase()?.contains("rain") == true -> Color(0xFF2196F3) // Rainy Blue
                    pin.weatherStatus?.lowercase()?.contains("snow") == true -> Color(0xFF00BCD4) // Cyan / Snow
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = "Weather: $weatherLabel",
                    tint = weatherColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = weatherLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun WeatherWidget(
    style: String,
    content: String,
    temp: Double?,
    desc: String?,
    wind: Double?,
    humidity: Int?,
    city: String?,
    useFahrenheit: Boolean,
    isLoading: Boolean,
    error: String?
) {
    if (isLoading) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
        return
    }

    if (error != null) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Weather Widget Offline", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text("Verify OpenWeather API key in secrets", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
        return
    }

    if (temp == null) return

    val displayTemp = if (useFahrenheit) {
        "${((temp * 9 / 5) + 32).toInt()}°F"
    } else {
        "${temp.toInt()}°C"
    }

    val displayWind = if (useFahrenheit) {
        val mph = (wind ?: 0.0) * 2.23694
        String.format("%.1f mph", mph)
    } else {
        String.format("%.1f m/s", wind ?: 0.0)
    }

    val weatherIcon = when {
        desc?.lowercase()?.contains("clear") == true -> Icons.Outlined.WbSunny
        desc?.lowercase()?.contains("cloud") == true -> Icons.Outlined.Cloud
        desc?.lowercase()?.contains("rain") == true -> Icons.Outlined.Umbrella
        desc?.lowercase()?.contains("snow") == true -> Icons.Outlined.AcUnit
        else -> Icons.Outlined.CloudQueue
    }

    val iconColor = when {
        desc?.lowercase()?.contains("clear") == true -> Color(0xFFFFB300)
        desc?.lowercase()?.contains("cloud") == true -> MaterialTheme.colorScheme.primary
        desc?.lowercase()?.contains("rain") == true -> Color(0xFF2196F3)
        desc?.lowercase()?.contains("snow") == true -> Color(0xFF00BCD4)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    when (style) {
        "Compact" -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("weather_widget_compact"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(weatherIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayTemp, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(desc ?: "Clear", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (city != null) {
                        Text(city, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }
        "Minimalist" -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("weather_widget_minimalist")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(weatherIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(displayTemp, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light)
                            Text(desc ?: "Clear", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (city != null) {
                        Text(city, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
                
                if (content != "Temp Only" && (wind != null || humidity != null)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (content == "Full Details" || content == "Temp + Wind") {
                            Text("Wind: $displayWind", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        if (content == "Full Details" && humidity != null) {
                            Text("Humidity: $humidity%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
        else -> { // "Glassmorphic" / Default
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("weather_widget_glassmorphic"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            if (city != null) {
                                Text(
                                    text = city,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayTemp,
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Icon(
                                imageVector = weatherIcon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc ?: "Clear",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (content != "Temp Only" && (wind != null || humidity != null)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (content == "Full Details" || content == "Temp + Wind") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Air,
                                        contentDescription = "Wind Speed",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = displayWind,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (content == "Full Details" && humidity != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = "Humidity",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Humidity: $humidity%",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDashboardState(
    allPins: List<ScenicPin>,
    viewModel: ScenicViewModel,
    onSelectPin: (ScenicPin) -> Unit
) {
    val context = LocalContext.current
    val settingsManager = viewModel.settingsManager
    
    val showWeatherWidget by settingsManager.showWeatherWidget.collectAsStateWithLifecycle()
    val weatherWidgetStyle by settingsManager.weatherWidgetStyle.collectAsStateWithLifecycle()
    val weatherWidgetContent by settingsManager.weatherWidgetContent.collectAsStateWithLifecycle()
    val useFahrenheit by settingsManager.useFahrenheit.collectAsStateWithLifecycle()
    val useDmsCoordinates by settingsManager.useDmsCoordinates.collectAsStateWithLifecycle()

    var weatherTemp by remember { mutableStateOf<Double?>(null) }
    var weatherDesc by remember { mutableStateOf<String?>(null) }
    var weatherWind by remember { mutableStateOf<Double?>(null) }
    var weatherHumidity by remember { mutableStateOf<Int?>(null) }
    var weatherIconCode by remember { mutableStateOf<String?>(null) }
    var isLoadingWeather by remember { mutableStateOf(false) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    var weatherCity by remember { mutableStateOf<String?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(showWeatherWidget, allPins) {
        if (!showWeatherWidget) return@LaunchedEffect
        
        isLoadingWeather = true
        weatherError = null
        
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            
        val triggerFetch: (Double, Double) -> Unit = { lat, lon ->
            coroutineScope.launch {
                val result = viewModel.fetchCurrentWeather(lat, lon)
                result.onSuccess { response ->
                    weatherTemp = response.main.temp
                    weatherDesc = response.weather.firstOrNull()?.description?.capitalize(Locale.getDefault()) ?: "Clear"
                    weatherWind = response.wind?.speed
                    weatherHumidity = response.main.humidity
                    weatherIconCode = response.weather.firstOrNull()?.main ?: "Clear"
                    isLoadingWeather = false
                }.onFailure { err ->
                    weatherError = err.localizedMessage ?: "Failed to fetch weather"
                    isLoadingWeather = false
                }
            }
        }

        if (hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        triggerFetch(location.latitude, location.longitude)
                        weatherCity = "Current Location"
                    } else {
                        val recentPin = allPins.firstOrNull()
                        if (recentPin != null) {
                            triggerFetch(recentPin.latitude, recentPin.longitude)
                            weatherCity = "Near ${recentPin.name}"
                        } else {
                            triggerFetch(40.7128, -74.0060)
                            weatherCity = "New York (Default)"
                        }
                    }
                }.addOnFailureListener {
                    val recentPin = allPins.firstOrNull()
                    if (recentPin != null) {
                        triggerFetch(recentPin.latitude, recentPin.longitude)
                        weatherCity = "Near ${recentPin.name}"
                    } else {
                        triggerFetch(40.7128, -74.0060)
                        weatherCity = "New York (Default)"
                    }
                }
            } catch (e: SecurityException) {
                val recentPin = allPins.firstOrNull()
                if (recentPin != null) {
                    triggerFetch(recentPin.latitude, recentPin.longitude)
                    weatherCity = "Near ${recentPin.name}"
                } else {
                    triggerFetch(40.7128, -74.0060)
                    weatherCity = "New York (Default)"
                }
            }
        } else {
            val recentPin = allPins.firstOrNull()
            if (recentPin != null) {
                triggerFetch(recentPin.latitude, recentPin.longitude)
                weatherCity = "Near ${recentPin.name}"
            } else {
                triggerFetch(40.7128, -74.0060)
                weatherCity = "New York (Default)"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showWeatherWidget) {
            WeatherWidget(
                style = weatherWidgetStyle,
                content = weatherWidgetContent,
                temp = weatherTemp,
                desc = weatherDesc,
                wind = weatherWind,
                humidity = weatherHumidity,
                city = weatherCity,
                useFahrenheit = useFahrenheit,
                isLoading = isLoadingWeather,
                error = weatherError
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scenic Scout Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Explore solar altitudes, golden hours, and weather details.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap any map marker or tap-hold to log a new spot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Pins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (allPins.isNotEmpty()) {
                    Text(
                        text = "${allPins.size} spots logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (allPins.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Pins Logged Yet",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Pins logged on the map will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                allPins.take(5).forEach { pin ->
                    CompactPinItem(
                        pin = pin,
                        useFahrenheit = useFahrenheit,
                        useDmsCoordinates = useDmsCoordinates
                    ) {
                        onSelectPin(pin)
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveMapView(
    pins: List<ScenicPin>,
    selectedPin: ScenicPin?,
    onPinSelected: (ScenicPin) -> Unit,
    onMapClicked: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        val initialLatLng = if (selectedPin != null) {
            LatLng(selectedPin.latitude, selectedPin.longitude)
        } else if (pins.isNotEmpty()) {
            LatLng(pins[0].latitude, pins[0].longitude)
        } else {
            LatLng(37.7749, -122.4194) // Default to San Francisco
        }
        position = CameraPosition.fromLatLngZoom(initialLatLng, 12f)
    }

    var locationFetched by remember { mutableStateOf(false) }

    LaunchedEffect(hasPermission, selectedPin) {
        if (hasPermission && !locationFetched && selectedPin == null) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        locationFetched = true
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(location.latitude, location.longitude),
                            14f
                        )
                    } else {
                        try {
                            fusedLocationClient.getCurrentLocation(
                                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                null
                            ).addOnSuccessListener { currLoc ->
                                if (currLoc != null) {
                                    locationFetched = true
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                        LatLng(currLoc.latitude, currLoc.longitude),
                                        14f
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ScenicApp", "Error getting current location", e)
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("ScenicApp", "SecurityException getting last location", e)
            }
        }
    }

    LaunchedEffect(selectedPin) {
        if (selectedPin != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(selectedPin.latitude, selectedPin.longitude),
                14f
            )
        }
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cachingTileProvider = remember(isDark) { CachingTileProvider(context, isDark) }
    val mapStyleOptions = remember(isDark) { if (isDark) DarkMapStyle else null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("interactive_canvas_map")
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = com.google.maps.android.compose.MapProperties(
                isMyLocationEnabled = hasPermission,
                mapStyleOptions = mapStyleOptions
            ),
            uiSettings = com.google.maps.android.compose.MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            onMapClick = { latLng ->
                onMapClicked(latLng.latitude, latLng.longitude)
            }
        ) {
            if (!isDark) {
                com.google.maps.android.compose.TileOverlay(
                    tileProvider = cachingTileProvider
                )
            }
            pins.forEach { pin ->
                val isSelected = selectedPin?.id == pin.id
                Marker(
                    state = com.google.maps.android.compose.MarkerState(position = LatLng(pin.latitude, pin.longitude)),
                    title = pin.name,
                    snippet = pin.landscapeType,
                    onClick = {
                        onPinSelected(pin)
                        true
                    }
                )
            }
        }

        // Overlay Info Card: helper text about tapping anywhere to add a pin
        // (Remove 'Topographical canvas active' header as requested)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Tap anywhere on the map to log a new Scout pin.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Quick Zoom and Location Controls column in Top-End corner
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Zoom Buttons
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                IconButton(onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            com.google.android.gms.maps.CameraUpdateFactory.zoomOut()
                        )
                    }
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            com.google.android.gms.maps.CameraUpdateFactory.zoomIn()
                        )
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Current Location FAB
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    if (hasPermission) {
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                                LatLng(location.latitude, location.longitude),
                                                15f
                                            )
                                        )
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Location not found. Ensure GPS is enabled.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.e("ScenicApp", "Location permission exception", e)
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Location permission not granted. Please allow location access in settings.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("my_location_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My Location")
            }
        }
    }
}

@Composable
fun FoldedPinMapView(
    pin: ScenicPin,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cachingTileProvider = remember(isDark) { CachingTileProvider(context, isDark) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(pin.latitude, pin.longitude), 14f)
    }

    LaunchedEffect(pin) {
        cameraPositionState.move(
            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                LatLng(pin.latitude, pin.longitude),
                14f
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val mapStyleOptions = remember(isDark) { if (isDark) DarkMapStyle else null }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = com.google.maps.android.compose.MapProperties(
                mapStyleOptions = mapStyleOptions
            ),
            uiSettings = com.google.maps.android.compose.MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = true
            )
        ) {
            if (!isDark) {
                com.google.maps.android.compose.TileOverlay(
                    tileProvider = cachingTileProvider
                )
            }
            Marker(
                state = MarkerState(position = LatLng(pin.latitude, pin.longitude)),
                title = pin.name,
                snippet = pin.landscapeType
            )
        }

        // Zoom Indicator label overlay in the bottom-left corner
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(14.dp)
            )
        }

        // Compass / Orientation indicator in top right corner
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = "North",
                tint = primaryColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "N",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun WeatherMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun EnvironmentalDashboard(
    pin: ScenicPin,
    useFahrenheit: Boolean,
    useDmsCoordinates: Boolean,
    use24HourFormat: Boolean,
    viewModel: ScenicViewModel,
    onUpdatePin: (ScenicPin) -> Unit,
    onDeletePin: (ScenicPin) -> Unit,
    onCapturePhoto: (Long) -> Unit,
    onClearSelection: () -> Unit
) {
    var isEditingNotes by remember { mutableStateOf(false) }
    var editFilm by remember { mutableStateOf(pin.filmStock) }
    var editIso by remember { mutableStateOf(pin.iso.toString()) }
    var editAperture by remember { mutableStateOf(pin.aperture) }
    var editShutterSpeed by remember { mutableStateOf(pin.shutterSpeed) }
    var editNotes by remember { mutableStateOf(pin.notes) }
    var editName by remember { mutableStateOf(pin.name) }

    val hasUnsavedChanges = isEditingNotes && (
        editName != pin.name || 
        editFilm != pin.filmStock || 
        editIso != pin.iso.toString() || 
        editAperture != pin.aperture || 
        editShutterSpeed != pin.shutterSpeed || 
        editNotes != pin.notes
    )

    LaunchedEffect(hasUnsavedChanges) {
        viewModel.setHasUnsavedChanges(hasUnsavedChanges)
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.setHasUnsavedChanges(false)
        }
    }

    LaunchedEffect(pin) {
        editFilm = pin.filmStock
        editIso = pin.iso.toString()
        editAperture = pin.aperture
        editShutterSpeed = pin.shutterSpeed
        editNotes = pin.notes
        editName = pin.name
    }

    val context = LocalContext.current
    val isFolded = LocalConfiguration.current.screenWidthDp < 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("environmental_dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Dashboard Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                if (isEditingNotes) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Pin Name") }
                    )
                } else {
                    Text(
                        text = pin.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(onClick = {
                    if (isEditingNotes) {
                        val finalPin = pin.copy(
                            name = editName,
                            filmStock = editFilm,
                            iso = editIso.toIntOrNull() ?: 100,
                            aperture = editAperture,
                            shutterSpeed = editShutterSpeed,
                            notes = editNotes
                        )
                        onUpdatePin(finalPin)
                        isEditingNotes = false
                    } else {
                        isEditingNotes = true
                    }
                }) {
                    Icon(
                        imageVector = if (isEditingNotes) Icons.Default.Save else Icons.Default.Edit,
                        contentDescription = "Edit Details"
                    )
                }
                IconButton(onClick = { onDeletePin(pin) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Location", tint = MaterialTheme.colorScheme.error)
                }
            }

        if (isFolded) {
            FoldedPinMapView(pin = pin)
        }

            // Coordinate Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Coordinates: " + formatCoordinates(pin.latitude, pin.longitude, useDmsCoordinates),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        val timePattern = if (use24HourFormat) "MMM dd, yyyy HH:mm:ss" else "MMM dd, yyyy h:mm:ss a"
                        val loggedText = remember(pin.timestamp, use24HourFormat) {
                            SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(pin.timestamp))
                        }
                        Text(
                            text = "Logged: $loggedText",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = {
                            // Direct Tap-to-Navigate explicitly using geo URI intent
                            val uri = Uri.parse("geo:0,0?q=${pin.latitude},${pin.longitude}(${Uri.encode(pin.name)})")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("navigate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Go", fontSize = 12.sp)
                    }
                }
            }

            // Photo Reference Block
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reference Photo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pin.photoUri != null) {
                        AsyncImage(
                            model = File(pin.photoUri),
                            contentDescription = "Scout Photo Reference",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = { onCapturePhoto(pin.id) },
                        modifier = Modifier.fillMaxWidth().testTag("snap_photo_btn")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (pin.photoUri != null) "Retake Photo Reference" else "Attach Photo Reference")
                    }
                }
            }

            // Weather Data Dashboard Card (Detailed)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("weather_section_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var isCurrentWeatherSelected by remember { mutableStateOf(false) }

                    var currentTemp by remember { mutableStateOf<Double?>(null) }
                    var currentStatus by remember { mutableStateOf<String?>(null) }
                    var currentClouds by remember { mutableStateOf<Int?>(null) }
                    var currentHumidity by remember { mutableStateOf<Int?>(null) }
                    var currentWindSpeed by remember { mutableStateOf<Double?>(null) }
                    var isLoadingCurrentWeather by remember { mutableStateOf(false) }
                    var currentWeatherError by remember { mutableStateOf<String?>(null) }
                    val coroutineScope = rememberCoroutineScope()

                    LaunchedEffect(isCurrentWeatherSelected, pin.id) {
                        if (isCurrentWeatherSelected && currentTemp == null) {
                            isLoadingCurrentWeather = true
                            currentWeatherError = null
                            coroutineScope.launch {
                                val result = viewModel.fetchCurrentWeather(pin.latitude, pin.longitude)
                                result.onSuccess { response ->
                                    currentTemp = response.main.temp
                                    currentStatus = response.weather.firstOrNull()?.description?.capitalize(Locale.getDefault()) ?: "Clear"
                                    currentClouds = response.clouds.all
                                    currentHumidity = response.main.humidity
                                    currentWindSpeed = response.wind?.speed
                                    isLoadingCurrentWeather = false
                                }.onFailure { err ->
                                    currentWeatherError = err.localizedMessage ?: "Failed to fetch weather"
                                    isLoadingCurrentWeather = false
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.WbSunny,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Weather",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isCurrentWeatherSelected) {
                                        currentTemp = null // re-trigger fetch
                                    } else {
                                        viewModel.syncWeatherForPin(pin.id)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = "Sync Weather",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isCurrentWeatherSelected) {
                                    if (currentTemp != null) Icons.Default.CloudQueue else Icons.Default.CloudOff
                                } else {
                                    if (pin.isWeatherSynced) Icons.Default.CloudQueue else Icons.Default.CloudOff
                                },
                                contentDescription = null,
                                tint = if (isCurrentWeatherSelected) {
                                    if (currentTemp != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                } else {
                                    if (pin.isWeatherSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Toggle Switch Control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "At Scout Time",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (!isCurrentWeatherSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isCurrentWeatherSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = isCurrentWeatherSelected,
                            onCheckedChange = { isCurrentWeatherSelected = it },
                            modifier = Modifier.testTag("weather_mode_toggle_switch")
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Current Weather",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isCurrentWeatherSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentWeatherSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isCurrentWeatherSelected && isLoadingCurrentWeather) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        }
                    } else if (isCurrentWeatherSelected && currentWeatherError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Offline: $currentWeatherError",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val temp = if (isCurrentWeatherSelected) currentTemp else pin.temperature
                        val status = if (isCurrentWeatherSelected) currentStatus else (pin.weatherStatus ?: "Pending")
                        val clouds = if (isCurrentWeatherSelected) currentClouds else pin.cloudCoverage
                        val humidity = if (isCurrentWeatherSelected) currentHumidity else pin.humidity
                        val windSpeed = if (isCurrentWeatherSelected) currentWindSpeed else pin.windSpeed

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WeatherMetricCard(
                                    label = "Temperature",
                                    value = formatTemperature(temp, useFahrenheit),
                                    icon = Icons.Outlined.Thermostat,
                                    modifier = Modifier.weight(1f)
                                )
                                WeatherMetricCard(
                                    label = "Condition",
                                    value = status ?: "Pending",
                                    icon = when {
                                        status?.lowercase()?.contains("clear") == true -> Icons.Outlined.WbSunny
                                        status?.lowercase()?.contains("cloud") == true -> Icons.Outlined.Cloud
                                        status?.lowercase()?.contains("rain") == true -> Icons.Outlined.Umbrella
                                        status?.lowercase()?.contains("snow") == true -> Icons.Outlined.AcUnit
                                        else -> Icons.Outlined.CloudQueue
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WeatherMetricCard(
                                    label = "Clouds",
                                    value = if (clouds != null) "$clouds%" else "N/A",
                                    icon = Icons.Outlined.FilterDrama,
                                    modifier = Modifier.weight(1f)
                                )
                                WeatherMetricCard(
                                    label = "Humidity",
                                    value = if (humidity != null) "$humidity%" else "N/A",
                                    icon = Icons.Outlined.WaterDrop,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WeatherMetricCard(
                                    label = "Wind Speed",
                                    value = if (windSpeed != null) "$windSpeed m/s" else "N/A",
                                    icon = Icons.Outlined.Air,
                                    modifier = Modifier.weight(1f)
                                )
                                WeatherMetricCard(
                                    label = "Sync Status",
                                    value = if (isCurrentWeatherSelected) {
                                        if (currentTemp != null) "Connected" else "Offline"
                                    } else {
                                        if (pin.isWeatherSynced) "Connected" else "Offline"
                                    },
                                    icon = if (isCurrentWeatherSelected) {
                                        if (currentTemp != null) Icons.Outlined.Sync else Icons.Outlined.CloudOff
                                    } else {
                                        if (pin.isWeatherSynced) Icons.Outlined.Sync else Icons.Outlined.CloudOff
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Celestial Math Trackers (Fully Offline Calculations)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Sun and Moon Position",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Circular Gauge for Sun positions
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Sun Altitude & Azimuth", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            CelestialGauge(
                                altitude = pin.sunAltitude,
                                azimuth = pin.sunAzimuth,
                                label = "SUN"
                            )
                        }

                        // Circular Gauge for Moon Positions & Phase
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Moon Phase & Angles", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            CelestialGauge(
                                altitude = pin.moonAltitude,
                                azimuth = pin.moonAzimuth,
                                label = "MOON"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Twilight and Golden Hour Timelines
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("Light Windows", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Golden Hour", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatTimeStr(pin.goldenHourStart, use24HourFormat), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Twilight", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatTimeStr(pin.twilightEnd, use24HourFormat), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
    }
}

@Composable
fun CelestialGauge(altitude: Double, azimuth: Double, label: String) {
    // Beautiful circular Gauge representing astronomical coordinate dials
    Box(
        modifier = Modifier
            .size(90.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 8.dp.toPx()

            // Draw Azimuth pointer line
            val angleRad = Math.toRadians(azimuth - 90.0) // 0 deg is North (Up)
            val pointerEndX = center.x + radius * Math.cos(angleRad).toFloat()
            val pointerEndY = center.y + radius * Math.sin(angleRad).toFloat()

            drawLine(
                color = Color.Red.copy(alpha = 0.7f),
                start = center,
                end = Offset(pointerEndX, pointerEndY),
                strokeWidth = 2.dp.toPx()
            )

            // Draw circular dial markers
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = radius * (altitude.coerceIn(0.0, 90.0).toFloat() / 90f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(String.format("Alt: %.1f°", altitude), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format("Az: %.0f°", azimuth), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPinDialog(
    pin: ScenicPin,
    onDismiss: () -> Unit,
    onSave: (ScenicPin) -> Unit
) {
    var name by remember { mutableStateOf(pin.name) }
    var type by remember { mutableStateOf(pin.landscapeType) }
    var timeCategory by remember { mutableStateOf(pin.timeOfDayCategory) }

    var showDiscardWarning by remember { mutableStateOf(false) }

    val hasChanges = name != pin.name ||
                     type != pin.landscapeType ||
                     timeCategory != pin.timeOfDayCategory

    val handleExit = {
        if (hasChanges) {
            showDiscardWarning = true
        } else {
            onDismiss()
        }
    }

    if (showDiscardWarning) {
        AlertDialog(
            onDismissRequest = { showDiscardWarning = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have made changes to this location. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardWarning = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardWarning = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    val types = listOf("Mountain", "Beach", "Forest", "Desert", "Urban", "Lake", "Park", "Pin", "Sun")
    val times = listOf("Sunrise", "Day", "GoldenHour", "Sunset", "BlueHour", "Night")

    AlertDialog(
        onDismissRequest = handleExit,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        title = { Text("Edit Scenic Scout Location") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Scenic Location Name") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_pin_name_input")
                )

                Text("Pin Icon", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { t ->
                                FilterChip(
                                    selected = type == t,
                                    onClick = { type = t },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = getLandscapeIcon(t),
                                            contentDescription = t,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (type == t) MaterialTheme.colorScheme.onPrimaryContainer else getLandscapeTypeColor(t)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = t,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Text("Intended Shoot Time", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    times.take(3).forEach { tm ->
                        FilterChip(
                            selected = timeCategory == tm,
                            onClick = { timeCategory = tm },
                            label = {
                                Text(
                                    text = tm,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    times.drop(3).forEach { tm ->
                        FilterChip(
                            selected = timeCategory == tm,
                            onClick = { timeCategory = tm },
                            label = {
                                Text(
                                    text = tm,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        pin.copy(
                            name = name,
                            landscapeType = type,
                            timeOfDayCategory = timeCategory
                        )
                    )
                },
                modifier = Modifier.testTag("edit_pin_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = handleExit,
                modifier = Modifier.testTag("edit_pin_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddPinDialog(
    lat: Double,
    lng: Double,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("New Scout Spot") }
    var type by remember { mutableStateOf("Pin") }
    var timeCategory by remember { mutableStateOf(getSuggestedTimeCategory(System.currentTimeMillis())) }

    val types = listOf("Mountain", "Beach", "Forest", "Desert", "Urban", "Lake", "Park", "Pin", "Sun")
    val times = listOf("Sunrise", "Day", "GoldenHour", "Sunset", "BlueHour", "Night")

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        title = { Text("Log New Scenic Scout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Scenic Location Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_pin_name_input")
                )

                Text("Pin Icon", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { t ->
                                FilterChip(
                                    selected = type == t,
                                    onClick = { type = t },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = getLandscapeIcon(t),
                                            contentDescription = t,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (type == t) MaterialTheme.colorScheme.onPrimaryContainer else getLandscapeTypeColor(t)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = t,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Text("Intended Shoot Time", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    times.take(3).forEach { tm ->
                        FilterChip(
                            selected = timeCategory == tm,
                            onClick = { timeCategory = tm },
                            label = {
                                Text(
                                    text = tm,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    times.drop(3).forEach { tm ->
                        FilterChip(
                            selected = timeCategory == tm,
                            onClick = { timeCategory = tm },
                            label = {
                                Text(
                                    text = tm,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, type, timeCategory) },
                modifier = Modifier.testTag("save_pin_dialog_btn")
            ) {
                Text("Confirm Scout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CameraCaptureDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Bind camera provider
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("ScenicApp", "Camera binding failed: ${e.localizedMessage}")
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("ScenicApp", "Camera binding failed: ${e.localizedMessage}")
                }
            }, ContextCompat.getMainExecutor(context))
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Capture Photo Reference") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val photoFile = File(
                        context.cacheDir,
                        "scout_photo_${System.currentTimeMillis()}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onPhotoCaptured(photoFile.absolutePath)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("ScenicApp", "Photo capture failed: ${exception.localizedMessage}")
                            }
                        }
                    )
                },
                modifier = Modifier.testTag("camera_shutter_btn")
            ) {
                Text("Capture Shutter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatTemperature(tempCelsius: Double?, useFahrenheit: Boolean): String {
    if (tempCelsius == null) return "N/A"
    return if (useFahrenheit) {
        val tempF = tempCelsius * 9.0 / 5.0 + 32.0
        String.format(Locale.US, "%.1f°F", tempF)
    } else {
        String.format(Locale.US, "%.1f°C", tempCelsius)
    }
}

fun formatTemperatureCompact(tempCelsius: Double?, useFahrenheit: Boolean): String {
    if (tempCelsius == null) return ""
    return if (useFahrenheit) {
        val tempF = tempCelsius * 9.0 / 5.0 + 32.0
        " • ${tempF.toInt()}°F"
    } else {
        " • ${tempCelsius.toInt()}°C"
    }
}

fun formatCoordinates(lat: Double, lng: Double, useDms: Boolean): String {
    if (!useDms) {
        return String.format(Locale.US, "%.4f, %.4f", lat, lng)
    }
    
    fun toDms(value: Double, positiveSuffix: String, negativeSuffix: String): String {
        val suffix = if (value >= 0) positiveSuffix else negativeSuffix
        val absValue = Math.abs(value)
        val degrees = absValue.toInt()
        val minutesAndSeconds = (absValue - degrees) * 60.0
        val minutes = minutesAndSeconds.toInt()
        val seconds = (minutesAndSeconds - minutes) * 60.0
        return String.format(Locale.US, "%d°%d'%.1f\"%s", degrees, minutes, seconds, suffix)
    }
    
    val latStr = toDms(lat, "N", "S")
    val lngStr = toDms(lng, "E", "W")
    return "$latStr, $lngStr"
}

fun getSuggestedTimeCategory(timestamp: Long): String {
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..6 -> "Sunrise"
        7, 8 -> "GoldenHour"
        in 9..16 -> "Day"
        17, 18 -> "Sunset"
        19, 20 -> "BlueHour"
        else -> "Night"
    }
}

// Utility Helpers to map categories to appropriate system symbols
fun getLandscapeIcon(type: String): ImageVector {
    return when (type) {
        "Mountain" -> Icons.Default.Terrain
        "Beach" -> Icons.Default.BeachAccess
        "Forest" -> Icons.Default.Forest
        "Desert" -> Icons.Default.Landscape
        "Urban" -> Icons.Default.LocationCity
        "Lake" -> Icons.Default.Water
        "Park" -> Icons.Default.Eco
        "Pin" -> Icons.Default.Place
        "Camera" -> Icons.Default.CameraAlt
        "Sun" -> Icons.Default.WbSunny
        else -> Icons.Default.FilterHdr
    }
}

fun getLandscapeTypeColor(type: String): Color {
    return when (type) {
        "Mountain" -> Color(0xFF8C9B82) // Soft sage
        "Beach" -> Color(0xFFD4A373) // Sandy warm amber
        "Forest" -> Color(0xFF386B1D) // Forest green
        "Desert" -> Color(0xFFC48C6B) // Warm clay
        "Urban" -> Color(0xFF6F776B) // Slate green
        "Lake" -> Color(0xFF537A71) // Muted teal
        "Park" -> Color(0xFF2E7D32) // Emerald Green
        "Pin" -> Color(0xFFE53935) // Red pin
        "Camera" -> Color(0xFF1976D2) // Soft Blue
        "Sun" -> Color(0xFFE65100) // Deep Orange
        else -> Color(0xFF6F776B) // Soft gray/green
    }
}

private fun triggerQuickScout(context: Context, viewModel: ScenicViewModel) {
    val timestamp = System.currentTimeMillis()
    val use24Hour = viewModel.settingsManager.use24HourFormat.value
    val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
    val timeString = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(timestamp))

    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPermission) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            // Always request an active, high-accuracy current location check first
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    addQuickPin(context, viewModel, location.latitude, location.longitude, timeString)
                } else {
                    // Fallback to lastLocation if getCurrentLocation succeeded but returned null
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        val lat = lastLoc?.latitude ?: (37.7749 + (Math.random() - 0.5) * 0.04)
                        val lng = lastLoc?.longitude ?: (-122.4194 + (Math.random() - 0.5) * 0.04)
                        addQuickPin(context, viewModel, lat, lng, timeString)
                    }.addOnFailureListener {
                        val lat = 37.7749 + (Math.random() - 0.5) * 0.04
                        val lng = -122.4194 + (Math.random() - 0.5) * 0.04
                        addQuickPin(context, viewModel, lat, lng, timeString)
                    }
                }
            }.addOnFailureListener {
                // Fallback to lastLocation if getCurrentLocation fails
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    val lat = lastLoc?.latitude ?: (37.7749 + (Math.random() - 0.5) * 0.04)
                    val lng = lastLoc?.longitude ?: (-122.4194 + (Math.random() - 0.5) * 0.04)
                    addQuickPin(context, viewModel, lat, lng, timeString)
                }.addOnFailureListener {
                    val lat = 37.7749 + (Math.random() - 0.5) * 0.04
                    val lng = -122.4194 + (Math.random() - 0.5) * 0.04
                    addQuickPin(context, viewModel, lat, lng, timeString)
                }
            }
        } catch (e: SecurityException) {
            val lat = 37.7749 + (Math.random() - 0.5) * 0.04
            val lng = -122.4194 + (Math.random() - 0.5) * 0.04
            addQuickPin(context, viewModel, lat, lng, timeString)
        }
    } else {
        val lat = 37.7749 + (Math.random() - 0.5) * 0.04
        val lng = -122.4194 + (Math.random() - 0.5) * 0.04
        addQuickPin(context, viewModel, lat, lng, timeString)
        android.widget.Toast.makeText(context, "Location permission not granted. Quick Scout used simulated location.", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun addQuickPin(context: Context, viewModel: ScenicViewModel, lat: Double, lng: Double, timeString: String) {
    viewModel.addPin(
        name = "Instant Scout @ $timeString",
        latitude = lat,
        longitude = lng,
        landscapeType = "Pin",
        timeOfDayCategory = getSuggestedTimeCategory(System.currentTimeMillis()),
        filmStock = "Portra 400",
        iso = 400,
        aperture = "f/8",
        notes = "Auto-scouted via Quick Action Trigger",
        shutterSpeed = "1/125s",
        context = context
    )

    // Trigger immediate fine haptic vibration feedback if enabled
    val enableHaptic = viewModel.settingsManager.enableHaptic.value
    if (enableHaptic) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScenicViewModel,
    foldPosture: FoldPosture,
    onFoldPostureChange: (FoldPosture) -> Unit
) {
    val settingsManager = viewModel.settingsManager

    val useFahrenheit by settingsManager.useFahrenheit.collectAsStateWithLifecycle()
    val useDmsCoordinates by settingsManager.useDmsCoordinates.collectAsStateWithLifecycle()
    val use24HourFormat by settingsManager.use24HourFormat.collectAsStateWithLifecycle()
    val enableHaptic by settingsManager.enableHaptic.collectAsStateWithLifecycle()
    val showWeatherWidget by settingsManager.showWeatherWidget.collectAsStateWithLifecycle()
    val weatherWidgetStyle by settingsManager.weatherWidgetStyle.collectAsStateWithLifecycle()
    val weatherWidgetContent by settingsManager.weatherWidgetContent.collectAsStateWithLifecycle()

    val view = androidx.compose.ui.platform.LocalView.current
    val triggerHapticLight = {
        if (enableHaptic) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "Scout Preferences",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Personalize coordinate readouts, display scales, and system options",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // CARD 1: Units Configurations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Regional & Display Formats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                // Temperature Scale Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Temperature Scale",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Format temperature readouts in Celsius or Fahrenheit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !useFahrenheit,
                            onClick = {
                                triggerHapticLight()
                                settingsManager.setUseFahrenheit(false)
                            },
                            label = { Text("Celsius (°C)", fontSize = 11.sp) },
                            leadingIcon = if (!useFahrenheit) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = useFahrenheit,
                            onClick = {
                                triggerHapticLight()
                                settingsManager.setUseFahrenheit(true)
                            },
                            label = { Text("Fahrenheit (°F)", fontSize = 11.sp) },
                            leadingIcon = if (useFahrenheit) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                // Coordinates format Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Coordinate Format",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Decimal format for maps or Degrees/Minutes/Seconds format.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !useDmsCoordinates,
                            onClick = {
                                triggerHapticLight()
                                settingsManager.setUseDmsCoordinates(false)
                            },
                            label = { Text("Decimal Degrees", fontSize = 11.sp) },
                            leadingIcon = if (!useDmsCoordinates) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = useDmsCoordinates,
                            onClick = {
                                triggerHapticLight()
                                settingsManager.setUseDmsCoordinates(true)
                            },
                            label = { Text("DMS Format", fontSize = 11.sp) },
                            leadingIcon = if (useDmsCoordinates) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                // Time Format Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Time Format",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Choose system display layout for sunrise, sunset, and scouted logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !use24HourFormat,
                            onClick = {
                                triggerHapticLight()
                                settingsManager.setUse24HourFormat(false)
                            },
                            label = { Text("12-hour (AM/PM)", fontSize = 11.sp) },
                            leadingIcon = if (!use24HourFormat) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f).testTag("time_format_12h_chip")
                        )
                        FilterChip(
                            selected = use24HourFormat,
                            onClick = {
                                triggerHapticLight()
                                settingsManager.setUse24HourFormat(true)
                            },
                            label = { Text("24-hour", fontSize = 11.sp) },
                            leadingIcon = if (use24HourFormat) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f).testTag("time_format_24h_chip")
                        )
                    }
                }
            }
        }

        // CARD 3: Tactile Feedback
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Haptic Feedback",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Vibrate device on quick scouting actions",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = enableHaptic,
                    onCheckedChange = { 
                        settingsManager.setEnableHaptic(it)
                        if (it) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        } else {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    },
                    modifier = Modifier.testTag("settings_haptic_switch")
                )
            }
        }

        // CARD: Weather Widget Configuration
        Card(
            modifier = Modifier.fillMaxWidth().testTag("weather_widget_settings_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Current Weather Widget",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Show real-time local weather at the top of empty state",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Switch(
                        checked = showWeatherWidget,
                        onCheckedChange = {
                            triggerHapticLight()
                            settingsManager.setShowWeatherWidget(it)
                        },
                        modifier = Modifier.testTag("settings_weather_widget_switch")
                    )
                }

                if (showWeatherWidget) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Widget Style Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Widget Style",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "Choose the visual presentation style of the weather card.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val styles = listOf("Glassmorphic", "Minimalist", "Compact")
                            styles.forEach { style ->
                                FilterChip(
                                    selected = weatherWidgetStyle == style,
                                    onClick = {
                                        triggerHapticLight()
                                        settingsManager.setWeatherWidgetStyle(style)
                                    },
                                    label = { Text(style, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("weather_style_${style.lowercase()}_chip")
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Widget Content Density Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Widget Content",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "Configure how much weather telemetry to display.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val contents = listOf("Full Details", "Temp Only", "Temp + Wind")
                            contents.forEach { content ->
                                FilterChip(
                                    selected = weatherWidgetContent == content,
                                    onClick = {
                                        triggerHapticLight()
                                        settingsManager.setWeatherWidgetContent(content)
                                    },
                                    label = { Text(content, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("weather_content_${content.replace(" ", "_").lowercase()}_chip")
                                )
                            }
                        }
                    }
                }
            }
        }

        // CARD: Foldable Posture Settings
        Card(
            modifier = Modifier.fillMaxWidth().testTag("foldable_optimization_settings_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pixel Fold Optimization",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Configure custom dual-pane or single-pane tabletop layouts for folded postures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = foldPosture == FoldPosture.FLAT_BOOK,
                        onClick = {
                            triggerHapticLight()
                            onFoldPostureChange(FoldPosture.FLAT_BOOK)
                        },
                        label = { Text("📖 Book Split", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("settings_posture_book_chip")
                    )
                    FilterChip(
                        selected = foldPosture == FoldPosture.TABLETOP,
                        onClick = {
                            triggerHapticLight()
                            onFoldPostureChange(FoldPosture.TABLETOP)
                        },
                        label = { Text("🛋️ Tabletop", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("settings_posture_tabletop_chip")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(viewModel: ScenicViewModel) {
    val firebaseBackupManager = viewModel.firebaseBackupManager

    val userEmail by firebaseBackupManager.currentUserEmail.collectAsStateWithLifecycle()
    val isSyncing by firebaseBackupManager.isSyncing.collectAsStateWithLifecycle()

    // Authentication States
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    var googleStatusText by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("599327496836-9c9o14p32utdag2hismh6brgek6f2bup.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    coroutineScope.launch {
                        authError = null
                        viewModel.signInWithGoogle(
                            idToken = idToken,
                            onSuccess = {
                                backupStatus = "Successfully signed in with Google!"
                            },
                            onFailure = { error ->
                                authError = error.localizedMessage ?: "Google Sign-In failed"
                            }
                        )
                    }
                } else {
                    authError = "Google Sign-In failed: Null ID Token"
                }
            } catch (e: ApiException) {
                authError = "Google Sign-In failed: ${e.statusCode} - ${e.localizedMessage}"
            }
        } else {
            authError = "Google Sign-In cancelled"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "User Account",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Backup and sync your scouted spots across your devices",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cloud Backup & Sync",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (userEmail != null) {
                    // Logged-in view
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Account Status",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        userEmail!!,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            backupStatus = "Backing up..."
                                            viewModel.backupPinsToCloud(
                                                onSuccess = { backupStatus = "Backup completed successfully!" },
                                                onFailure = { backupStatus = "Backup failed: ${it.localizedMessage}" }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("backup_cloud_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Backup", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            backupStatus = "Restoring pins..."
                                            viewModel.restorePinsFromBackup(
                                                onSuccess = { count -> backupStatus = "Successfully restored $count pins!" },
                                                onFailure = { backupStatus = "Restore failed: ${it.localizedMessage}" }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("restore_cloud_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    firebaseBackupManager.logout()
                                    backupStatus = null
                                },
                                modifier = Modifier.fillMaxWidth().testTag("logout_btn"),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                )
                            ) {
                                Text("Log Out", fontSize = 11.sp)
                            }
                        }
                    }

                    if (backupStatus != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (backupStatus!!.contains("failed")) Icons.Default.ErrorOutline else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (backupStatus!!.contains("failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = backupStatus!!,
                                color = if (backupStatus!!.contains("failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Modernized Sign In & Registration Suite
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Visual Welcome & Header Card
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isRegisterMode) "Create Your Cloud Vault" else "Welcome Back, Explorer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRegisterMode) "Set up an account to safeguard your photography spots securely." else "Sign in to restore and sync your custom analog setups instantly.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Modern sliding capsule tab row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isRegisterMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        isRegisterMode = false
                                        authError = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign In",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isRegisterMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isRegisterMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        isRegisterMode = true
                                        authError = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Register",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isRegisterMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Input fields with modern outline styling
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible }
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        if (authError != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = authError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Premium Action Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    authError = null
                                    if (isRegisterMode) {
                                        val result = firebaseBackupManager.signUp(emailInput, passwordInput)
                                        result.onFailure { authError = it.localizedMessage ?: "Failed to sign up" }
                                    } else {
                                        val result = firebaseBackupManager.login(emailInput, passwordInput)
                                        result.onFailure { authError = it.localizedMessage ?: "Failed to log in" }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("auth_action_btn"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting Security Vault...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isRegisterMode) "Register Vault Account" else "Secure Sign In",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Visual Divider
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                            Text(
                                text = "OR CONTINUE WITH",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 12.dp),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        }

                        // Modern Styled Google Sign In Button (Disabled / Greyed Out)
                        OutlinedButton(
                            onClick = {
                                if (googleStatusText == null) {
                                    googleStatusText = "Coming Soon..."
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        googleStatusText = null
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_auth_btn")
                                .alpha(0.5f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            androidx.compose.animation.Crossfade(
                                targetState = googleStatusText ?: (if (isRegisterMode) "Google Quick Register" else "Google Secure Login"),
                                label = "GoogleButtonTextTransition"
                            ) { textToDisplay ->
                                Text(
                                    text = textToDisplay,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}

fun formatTimeStr(timeStr: String, use24Hour: Boolean): String {
    if (use24Hour) return timeStr
    val regex = Regex("""\b(\d{1,2}):(\d{2})\b""")
    return regex.replace(timeStr) { matchResult ->
        val hour = matchResult.groupValues[1].toIntOrNull()
        val minute = matchResult.groupValues[2]
        if (hour != null) {
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            "$displayHour:$minute $amPm"
        } else {
            matchResult.value
        }
    }
}

@Composable
fun SyncStatusIndicator(viewModel: ScenicViewModel) {
    val lastSyncTime by viewModel.settingsManager.lastSyncTime.collectAsStateWithLifecycle()
    val lastLocalChangeTime by viewModel.settingsManager.lastLocalChangeTime.collectAsStateWithLifecycle()
    val isSyncing by viewModel.firebaseBackupManager.isSyncing.collectAsStateWithLifecycle()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag("sync_status_indicator_bar"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Database Sync Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.5f)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .testTag("sync_indicator_loading"),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Syncing with cloud...",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    val isUpToDate = lastLocalChangeTime <= lastSyncTime || (lastSyncTime > 0 && lastLocalChangeTime == 0L)
                    val icon: ImageVector
                    val text: String
                    val iconColor: Color
                    
                    if (lastSyncTime == 0L) {
                        icon = Icons.Default.CloudOff
                        text = "Local-Only Database"
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    } else if (isUpToDate) {
                        icon = Icons.Default.CloudDone
                        text = "Database: Cloud-Synced"
                        iconColor = Color(0xFF2E7D32) // Emerald Green
                    } else {
                        icon = Icons.Default.CloudSync
                        text = "Unsynced Local Edits"
                        iconColor = Color(0xFFE65100) // Deep Orange
                    }
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = "Sync Status",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (lastSyncTime > 0L) {
                            val syncedText = remember(lastSyncTime) {
                                val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                "Synced: ${sdf.format(Date(lastSyncTime))}"
                            }
                            Text(
                                text = syncedText,
                                style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Sign in to backup pins",
                                style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            )
            
            // Right Side: Offline Map Cache Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OfflinePin,
                    contentDescription = "Offline Cache",
                    tint = Color(0xFF2E7D32), // Emerald Green
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Map Cache",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Offline-Ready",
                        style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingWeatherWidget(viewModel: ScenicViewModel, useFahrenheit: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    var weatherTemp by remember { mutableStateOf<Double?>(null) }
    var weatherDesc by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val allPins by viewModel.allPins.collectAsStateWithLifecycle()
    
    LaunchedEffect(allPins) {
        val triggerFetch: (Double, Double) -> Unit = { lat, lon ->
            coroutineScope.launch {
                val result = viewModel.fetchCurrentWeather(lat, lon)
                result.onSuccess { response ->
                    weatherTemp = response.main.temp
                    weatherDesc = response.weather.firstOrNull()?.description?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Clear"
                }
            }
        }
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        triggerFetch(location.latitude, location.longitude)
                    } else {
                        val recentPin = allPins.firstOrNull()
                        if (recentPin != null) {
                            triggerFetch(recentPin.latitude, recentPin.longitude)
                        } else {
                            triggerFetch(40.7128, -74.0060)
                        }
                    }
                }
            } catch (e: SecurityException) {
                triggerFetch(40.7128, -74.0060)
            }
        } else {
            val recentPin = allPins.firstOrNull()
            if (recentPin != null) {
                triggerFetch(recentPin.latitude, recentPin.longitude)
            } else {
                triggerFetch(40.7128, -74.0060)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Weather info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val displayTemp = if (useFahrenheit) {
                        "${weatherTemp?.let { (it * 9 / 5 + 32).toInt() } ?: 77}°F"
                    } else {
                        "${weatherTemp?.toInt() ?: 25}°C"
                    }
                    Text(
                        text = displayTemp,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = weatherDesc ?: "Loading weather...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "LIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FloatingRecentPinsWidget(allPins: List<com.example.domain.ScenicPin>, onPinClick: (com.example.domain.ScenicPin) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Recent Pins",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (allPins.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = "No pins logged yet",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Pins Logged Yet",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Log scenic locations to see them here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            allPins.take(5).forEach { pin ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPinClick(pin) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getLandscapeIcon(pin.landscapeType),
                                contentDescription = pin.landscapeType,
                                tint = getLandscapeTypeColor(pin.landscapeType),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pin.name,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pin.landscapeType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}




