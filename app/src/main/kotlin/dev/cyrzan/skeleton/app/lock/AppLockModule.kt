package dev.cyrzan.skeleton.app.lock

import android.os.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Grace period in the background before the PIN is asked for again. A product decision: banking apps
 * often lock immediately (0), others after a minute or more. The grace also covers short trips out
 * of the app that are part of a flow (camera, file picker, share sheet, payment).
 */
private val LOCK_AFTER = 10.seconds

@Module
@InstallIn(SingletonComponent::class)
internal object AppLockModule {

    @Provides
    @Singleton
    fun provideAppLock(session: SessionRepository): AppLock = AppLock(
        session = session,
        lockAfterMillis = LOCK_AFTER.inWholeMilliseconds,
        // Monotonic and counts deep sleep – changing the device clock cannot skip the lock.
        now = SystemClock::elapsedRealtime,
    )
}
