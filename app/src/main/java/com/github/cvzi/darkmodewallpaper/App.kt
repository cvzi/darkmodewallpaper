package com.github.cvzi.darkmodewallpaper

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy


class App : Application() {
    init {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .detectResourceMismatches()
                    .detectUnbufferedIo()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }
    }

}