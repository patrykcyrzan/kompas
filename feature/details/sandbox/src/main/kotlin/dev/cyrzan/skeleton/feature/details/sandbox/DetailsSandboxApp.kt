package dev.cyrzan.skeleton.feature.details.sandbox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt's root has to live in the app module itself – the rest comes from `:sandbox`. */
@HiltAndroidApp
class DetailsSandboxApp : Application()
