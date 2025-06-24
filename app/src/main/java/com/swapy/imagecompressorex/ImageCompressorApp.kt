package com.swapy.imagecompressorex

import android.app.Application
import timber.log.Timber

class ImageCompressorApp : Application(){

    companion object {
        lateinit var instance: ImageCompressorApp
            private set
        lateinit var PACKAGE_NAME : String
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        PACKAGE_NAME = BuildConfig.APPLICATION_ID
    }

}