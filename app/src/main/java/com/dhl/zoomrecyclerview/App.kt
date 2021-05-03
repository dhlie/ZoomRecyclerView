package com.dhl.zoomrecyclerview

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 *
 * Author: duanhl
 * Create: 5/3/21 9:45 PM
 * Description:
 *
 */
class App : Application() {

    companion object {

        @SuppressLint("StaticFieldLeak")
        lateinit var instance: Context

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}