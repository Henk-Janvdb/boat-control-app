package com.boatcontrol.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : ComponentActivity() {
    private val requestSmsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoatControlApp(this) { permission ->
                requestSmsPermission.launch(permission)
            }
        }
    }
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF6600), // Rent a Boat Orange
    onPrimary = Color.White,
    secondary = Color(0xFF1A3A52), // Nautical Blue
    onSecondary = Color.White,
    surface = Color(0xFFFFF7F2), // Very light orange tint
    background = Color(0xFFFFF7F2),
    surfaceVariant = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6600),
    onPrimary = Color.Black,
    secondary = Color(0xFF4A90E2), // Lighter Blue for dark mode
    onSecondary = Color.White,
    surface = Color(0xFF121212),
    background = Color(0xFF121212),
    onSurface = Color.White,
    onBackground = Color.White,
    surfaceVariant = Color(0xFF1E1E1E)
)

@Composable
fun BoatControlApp(context: Context, requestPermission: (String) -> Unit) {
    val boatStorage = remember { BoatStorage(context) }
    var themeMode by remember { mutableStateOf(boatStorage.loadThemeMode()) }
    
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    
    var currentScreen by remember { mutableStateOf("main") }
    val boats = remember { mutableStateListOf<Boat>().apply { addAll(boatStorage.loadBoats()) } }

    // Handle system back button
    BackHandler(enabled = currentScreen != "main") {
        currentScreen = "main"
    }

    // Save boats whenever the list changes
    LaunchedEffect(boats.toList()) {
        boatStorage.saveBoats(boats.toList())
        BoatWidget().updateAll(context)
    }
    
    // Save theme whenever it changes
    LaunchedEffect(themeMode) {
        boatStorage.saveThemeMode(themeMode)
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (currentScreen == "main") {
                MainScreen(
                    context = context,
                    boats = boats,
                    onOpenSettings = { currentScreen = "settings" },
                    requestPermission = requestPermission
                )
            } else {
                SettingsScreen(
                    boats = boats,
                    themeMode = themeMode,
                    onThemeChange = { themeMode = it },
                    onBack = { currentScreen = "main" }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    context: Context,
    boats: List<Boat>,
    onOpenSettings: () -> Unit,
    requestPermission: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rent a Boat Flevoland") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (boats.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Geen boten toegevoegd. Ga naar instellingen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(boats) { boat ->
                        BoatControlCard(context, boat, requestPermission)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { openOnTrack(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("OnTrack Integratie", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BoatControlCard(context: Context, boat: Boat, requestPermission: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(boat.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(boat.phoneNumber, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        requestPermission(Manifest.permission.SEND_SMS)
                        sendSms(context, boat.phoneNumber, "0000#ON#")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Keeping Green for 'AAN'
                ) {
                    Text("AAN")
                }
                Button(
                    onClick = {
                        requestPermission(Manifest.permission.SEND_SMS)
                        sendSms(context, boat.phoneNumber, "0000#OFF#")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)) // Keeping Red for 'UIT'
                ) {
                    Text("UIT")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    boats: MutableList<Boat>,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit
) {
    var boatName by remember { mutableStateOf("") }
    var boatNumber by remember { mutableStateOf("") }
    var editingBoatId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Thema instellingen",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionButton(
                        text = "Systeem",
                        isSelected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeChange(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        text = "Licht",
                        isSelected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeChange(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        text = "Donker",
                        isSelected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeChange(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            item {
                Text(
                    if (editingBoatId == null) "Nieuwe boot toevoegen" else "Boot bewerken",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = boatName,
                    onValueChange = { boatName = it },
                    label = { Text("Naam boot") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                OutlinedTextField(
                    value = boatNumber,
                    onValueChange = { boatNumber = it },
                    label = { Text("Telefoonnummer") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (boatName.isNotBlank() && boatNumber.isNotBlank()) {
                                if (editingBoatId == null) {
                                    boats.add(Boat(name = boatName, phoneNumber = boatNumber))
                                } else {
                                    val index = boats.indexOfFirst { it.id == editingBoatId }
                                    if (index != -1) {
                                        boats[index] = boats[index].copy(name = boatName, phoneNumber = boatNumber)
                                    }
                                    editingBoatId = null
                                }
                                boatName = ""
                                boatNumber = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(if (editingBoatId == null) Icons.Default.Add else Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (editingBoatId == null) "Boot toevoegen" else "Opslaan")
                    }

                    if (editingBoatId != null) {
                        OutlinedButton(
                            onClick = {
                                editingBoatId = null
                                boatName = ""
                                boatNumber = ""
                            },
                            modifier = Modifier.weight(0.5f)
                        ) {
                            Text("Annuleren")
                        }
                    }
                }
            }
            
            item {
                Text("Uw boten", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            
            items(boats) { boat ->
                ListItem(
                    headlineContent = { Text(boat.name) },
                    supportingContent = { Text(boat.phoneNumber) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = {
                                boatName = boat.name
                                boatNumber = boat.phoneNumber
                                editingBoatId = boat.id
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { boats.remove(boat) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
fun ThemeOptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(text, fontSize = 12.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(text, fontSize = 12.sp)
        }
    }
}

fun openOnTrack(context: Context) {
    val packageName = "com.onntrackpro.gps"
    val webUrl = "https://www.onntrack.nl"
    
    val packageManager = context.packageManager
    
    // Probeer eerst de officiële Onntrack Pro app te vinden
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    
    if (intent != null) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return // Succesvol geopend
        } catch (e: Exception) {
            // Als het starten mislukt, gaan we door naar de Play Store/Web
        }
    }

    // Als de app niet direct gevonden wordt, probeer via Play Store
    try {
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(playStoreIntent)
        Toast.makeText(context, "Onntrack Pro app niet direct gestart, Play Store geopend", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        openWebFallback(context, webUrl)
    }
}

private fun openWebFallback(context: Context, url: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(browserIntent)
        Toast.makeText(context, "Onntrack app niet gevonden, website geopend", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Kan Onntrack niet openen", Toast.LENGTH_SHORT).show()
    }
}
