package com.openfy.music

import android.app.Application

class OpenfyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OpenfyApplication
            private set
    }
}
