package dev.cyrzan.skeleton.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import androidx.lifecycle.lifecycleScope
import dev.cyrzan.skeleton.app.analytics.AnalyticsIdentity
import dev.cyrzan.skeleton.app.lock.AppLock
import dev.cyrzan.skeleton.core.analytics.Analytics
import javax.inject.Inject

@HiltAndroidApp
class SkeletonApplication : Application() {

    @Inject
    internal lateinit var appLock: AppLock

    // Injected only to be created at startup: it starts following consent changes right away.
    @Inject
    lateinit var analytics: Analytics

    @Inject
    internal lateinit var analyticsIdentity: AnalyticsIdentity

    override fun onCreate() {
        super.onCreate()
        // The process lifecycle is never destroyed, so its scope lives as long as the app.
        analyticsIdentity.start(ProcessLifecycleOwner.get().lifecycleScope)
        // The whole process, not an Activity: moving between our own Activities is not "leaving the app",
        // and rotation does not count either (ProcessLifecycleOwner debounces it).
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = appLock.onAppForegrounded()
                override fun onStop(owner: LifecycleOwner) = appLock.onAppBackgrounded()
            },
        )
    }
}
