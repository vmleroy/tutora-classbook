package com.vmleroy.tutora.app

import android.app.Application

class TutoraApplication : Application() {
    /** The app's wiring; screens reach it through their ViewModels. */
    val container: AppContainer by lazy { AppContainer(this) }
}
