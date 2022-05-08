package com.shamlou.fallingwords

import androidx.multidex.MultiDexApplication
import com.shamlou.fallingwords.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class Application : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        // Init Koin
        startKoin {
            androidContext(this@Application)
            modules(
                listOf(
                    appModule
                )
            )
        }
    }
}
