package com.boatcontrol.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Boat(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val phoneNumber: String
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class BoatStorage(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("boat_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveBoats(boats: List<Boat>) {
        val json = gson.toJson(boats)
        sharedPreferences.edit().putString("boats", json).apply()
    }

    fun loadBoats(): List<Boat> {
        val json = sharedPreferences.getString("boats", null) ?: return emptyList()
        val type = object : TypeToken<List<Boat>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveThemeMode(mode: ThemeMode) {
        sharedPreferences.edit().putString("theme_mode", mode.name).apply()
    }

    fun loadThemeMode(): ThemeMode {
        val name = sharedPreferences.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try { ThemeMode.valueOf(name!!) } catch (e: Exception) { ThemeMode.SYSTEM }
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
