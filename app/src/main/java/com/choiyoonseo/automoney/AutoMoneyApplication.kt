package com.choiyoonseo.automoney

import android.app.Application
import com.choiyoonseo.automoney.di.AppContainer

class AutoMoneyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

