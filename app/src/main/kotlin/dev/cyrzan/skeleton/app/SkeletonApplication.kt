package dev.cyrzan.skeleton.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import dev.cyrzan.skeleton.app.lock.AppLock
import javax.inject.Inject

@HiltAndroidApp
class SkeletonApplication : Application() {

    @Inject
    internal lateinit var appLock: AppLock

    override fun onCreate() {
        super.onCreate()
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
