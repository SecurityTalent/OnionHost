package com.onionhost.app

import android.app.Application
import com.onionhost.app.http.AnonymousChatStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OnionHostApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AnonymousChatStore.initialize(this)
    }
}
