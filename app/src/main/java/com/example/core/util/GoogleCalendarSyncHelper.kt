package com.example.core.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.example.core.model.CalendarEvent
import java.time.ZoneId

/**
 * Helper utility to sync planned study sessions and calendar events
 * directly with Google Calendar via system calendar intents.
 */
object GoogleCalendarSyncHelper {

    fun syncEventToGoogleCalendar(context: Context, event: CalendarEvent) {
        try {
            val startMillis = event.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "📚 [NEXORA LEARN] ${event.title}")
                putExtra(CalendarContract.Events.DESCRIPTION, event.description.ifBlank { "Planned study session in NEXORA LEARN" })
                putExtra(CalendarContract.Events.ALL_DAY, event.isAllDay)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + (60 * 60 * 1000L)) // 1 hour duration
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Google Calendar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
