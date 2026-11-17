package com.openfy.music

import android.app.Application
import com.openfy.music.data.remote.NewPipeDownloader

class OpenfyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        NewPipeDownloader.init()
    }

    companion object {
        lateinit var instance: OpenfyApplication
            private set
    }
}
