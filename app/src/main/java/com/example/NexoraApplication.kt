package com.example

import android.app.Application
import com.example.core.supabase.SupabaseClientManager

class NexoraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        // Initialize Supabase with application context
        SupabaseClientManager.initialize(this)
    }

    companion object {
        lateinit var appContext: NexoraApplication
            private set
    }
}
