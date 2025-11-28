package com.loom.bedrock

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class LoomApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("Bedrock Loom initialized")
    }
}
