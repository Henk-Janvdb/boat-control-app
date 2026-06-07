package com.boatcontrol.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import android.os.Bundle
import android.content.Intent
import android.net.Uri

data class Boat(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val phoneNumber: String
)

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

@Composable
fun BoatControlApp(context: Context, requestPermission: (String) -> Unit) {
    var currentScreen by remember { mutableStateOf("main") }
    val boats = remember { mutableStateListOf<Boat>() }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFFFF6600), // Rent a Boat Orange
            onPrimary = Color.White,
            secondary = Color(0xFF1A3A52), // Nautical Blue
            onSecondary = Color.White,
            surface = Color(0xFFFFF7F2), // Very light orange tint
            background = Color(0xFFFFF7F2)
        )
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
                    Text("Geen boten toegevoegd. Ga naar instellingen.", color = Color.Gray)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(boat.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Text(boat.phoneNumber, fontSize = 14.sp, color = Color.Gray)
            
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
    onBack: () -> Unit
) {
    var newBoatName by remember { mutableStateOf("") }
    var newBoatNumber by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen - Boten beheren") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Nieuwe boot toevoegen", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            
            OutlinedTextField(
                value = newBoatName,
                onValueChange = { newBoatName = it },
                label = { Text("Naam boot") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = newBoatNumber,
                onValueChange = { newBoatNumber = it },
                label = { Text("Telefoonnummer") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (newBoatName.isNotBlank() && newBoatNumber.isNotBlank()) {
                        boats.add(Boat(name = newBoatName, phoneNumber = newBoatNumber))
                        newBoatName = ""
                        newBoatNumber = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Boot toevoegen")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Uw boten", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(boats) { boat ->
                    ListItem(
                        headlineContent = { Text(boat.name) },
                        supportingContent = { Text(boat.phoneNumber) },
                        trailingContent = {
                            IconButton(onClick = { boats.remove(boat) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.White)
                    )
                }
            }
        }
    }
}

fun sendSms(context: Context, phoneNumber: String, message: String) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(context, "SMS sent to $phoneNumber", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error sending SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "SMS permission required", Toast.LENGTH_SHORT).show()
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
