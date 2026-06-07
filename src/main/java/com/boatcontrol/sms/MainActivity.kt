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
            BoatControlApp(this, requestSmsPermission)
        }
    }
}

@Composable
fun BoatControlApp(context: Context, requestPermission: (String) -> Unit) {
    val phoneNumbers = remember { mutableStateListOf<String>() }
    val newPhoneNumber = remember { mutableStateOf("") }
    val boatName = remember { mutableStateOf("My Boat") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5DC)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    "Boat Control SMS",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A3A52),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Fleet Control",
                    fontSize = 18.sp,
                    color = Color(0xFF4A90E2),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Boat Name Input
                OutlinedTextField(
                    value = boatName.value,
                    onValueChange = { boatName.value = it },
                    label = { Text("Boat Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Phone Number Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newPhoneNumber.value,
                        onValueChange = { newPhoneNumber.value = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("+1234567890") }
                    )
                    Button(
                        onClick = {
                            if (newPhoneNumber.value.isNotEmpty()) {
                                phoneNumbers.add(newPhoneNumber.value)
                                newPhoneNumber.value = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Add")
                    }
                }

                // Phone Numbers List
                Text(
                    "SMS Relay Destinations (${phoneNumbers.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A3A52),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(phoneNumbers) { phone ->
                        PhoneNumberItem(phone) {
                            phoneNumbers.remove(phone)
                        }
                    }
                }

                // Control Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (phoneNumbers.isEmpty()) {
                                Toast.makeText(context, "Add phone numbers first", Toast.LENGTH_SHORT).show()
                            } else {
                                requestSmsPermission(Manifest.permission.SEND_SMS)
                                sendSmsToAll(context, phoneNumbers, "0000#ON#")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("ON", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (phoneNumbers.isEmpty()) {
                                Toast.makeText(context, "Add phone numbers first", Toast.LENGTH_SHORT).show()
                            } else {
                                requestSmsPermission(Manifest.permission.SEND_SMS)
                                sendSmsToAll(context, phoneNumbers, "0000#OFF#")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) {
                        Text("OFF", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            // Custom SMS option
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("CUSTOM", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneNumberItem(phoneNumber: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            phoneNumber,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A3A52)
        )
        Button(
            onClick = onRemove,
            modifier = Modifier.height(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
            Text("Remove", fontSize = 12.sp)
        }
    }
}

fun sendSmsToAll(context: Context, phoneNumbers: List<String>, message: String) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)
        phoneNumbers.forEach { phone ->
            smsManager.sendTextMessage(phone, null, message, null, null)
        }
        Toast.makeText(context, "SMS sent to ${phoneNumbers.size} number(s)", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "SMS permission required", Toast.LENGTH_SHORT).show()
    }
}