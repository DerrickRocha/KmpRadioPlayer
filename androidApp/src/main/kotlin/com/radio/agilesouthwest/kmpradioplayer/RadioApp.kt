package com.radio.agilesouthwest.kmpradioplayer

import android.app.Application
import com.radio.agilesouthwest.kmpradioplayer.di.initKoin
import org.koin.android.ext.koin.androidContext

class RadioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@RadioApp)
        }
    }
}
