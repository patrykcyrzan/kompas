package dev.cyrzan.skeleton.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import androidx.lifecycle.lifecycleScope
import dev.cyrzan.skeleton.app.analytics.AnalyticsIdentity
import dev.cyrzan.skeleton.app.analytics.ScreenTracker
import dev.cyrzan.skeleton.app.lock.AppLock
import dev.cyrzan.skeleton.core.analytics.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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

    @Inject
    internal lateinit var screenTracker: ScreenTracker

    private val inForeground = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        // The process lifecycle is never destroyed, so its scope lives as long as the app.
        analyticsIdentity.start(ProcessLifecycleOwner.get().lifecycleScope)
        // The user sees a screen only in the foreground and not behind the lock.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            combine(inForeground, appLock.isLocked) { foreground, locked -> foreground && !locked }
                .collect(screenTracker::setVisible)
        }
        // The whole process, not an Activity: moving between our own Activities is not "leaving the app",
        // and rotation does not count either (ProcessLifecycleOwner debounces it).
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    // Lock first: a screen that comes back behind the lock must not count as seen.
                    appLock.onAppForegrounded()
                    inForeground.value = true
                }

                override fun onStop(owner: LifecycleOwner) {
                    appLock.onAppBackgrounded()
                    inForeground.value = false
                }
            },
        )
    }
}
