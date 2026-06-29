package com.example.warehouse

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClient.init()
    }
}