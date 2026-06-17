package com.boatcontrol.sms

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class BoatWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val boatStorage = BoatStorage(context)
        val boats = boatStorage.loadBoats()

        provideContent {
            GlanceTheme {
                WidgetContent(boats)
            }
        }
    }

    @Composable
    private fun WidgetContent(boats: List<Boat>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(8.dp)
        ) {
            Text(
                text = "Boot Bediening",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.primary
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )

            if (boats.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Geen boten", style = TextStyle(color = GlanceTheme.colors.onSurface))
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(boats) { boat ->
                        BoatRow(boat)
                    }
                }
            }
        }
    }

    @Composable
    private fun BoatRow(boat: Boat) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(GlanceTheme.colors.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = boat.name,
                    style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                )
                Text(
                    text = boat.phoneNumber,
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                )
            }

            Row {
                Button(
                    text = "AAN",
                    onClick = actionRunCallback<SmsActionCallback>(
                        actionParametersOf(
                            SmsActionCallback.phoneKey to boat.phoneNumber,
                            SmsActionCallback.messageKey to "0000#ON#"
                        )
                    )
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Button(
                    text = "UIT",
                    onClick = actionRunCallback<SmsActionCallback>(
                        actionParametersOf(
                            SmsActionCallback.phoneKey to boat.phoneNumber,
                            SmsActionCallback.messageKey to "0000#OFF#"
                        )
                    )
                )
            }
        }
    }
}

class SmsActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val phoneNumber = parameters[phoneKey] ?: return
        val message = parameters[messageKey] ?: return
        sendSms(context, phoneNumber, message)
    }

    companion object {
        val phoneKey = ActionParameters.Key<String>("phone_number")
        val messageKey = ActionParameters.Key<String>("message")
    }
}

class BoatWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BoatWidget()
}
