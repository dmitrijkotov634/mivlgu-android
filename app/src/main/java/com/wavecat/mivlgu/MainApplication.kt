package com.wavecat.mivlgu

import android.app.Application
import ru.rustore.sdk.remoteconfig.AppId
import ru.rustore.sdk.remoteconfig.DeviceId
import ru.rustore.sdk.remoteconfig.RemoteConfigClientBuilder

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        RemoteConfigClientBuilder(
            appId = AppId("9f4e56a4-00ed-41ba-b428-fbf28c4f650a"),
            context = applicationContext
        )
            .setDeviceId(DeviceId("main"))
            .build()
            .init()
    }
}