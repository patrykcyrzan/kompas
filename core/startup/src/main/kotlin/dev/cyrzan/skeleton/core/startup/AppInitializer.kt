package dev.cyrzan.skeleton.core.startup

import android.app.Application

/**
 * Work a module needs done once, when the process starts (an SDK's `initialize(context)`...).
 * Each module binds its own `@IntoSet`; the app runs the whole `Set<AppInitializer>` in `Application.onCreate`,
 * so it never imports a single SDK:
 *
 * ```
 * @Binds @IntoSet
 * fun bindChatInitializer(impl: ChatInitializer): AppInitializer
 * ```
 *
 * Runs on the main thread before the first screen – keep it short, and prefer initializing lazily
 * on first use when nothing needs the SDK earlier.
 */
interface AppInitializer {

    /** Higher runs first. Most initializers do not care. */
    val priority: Int get() = 0

    fun init(application: Application)
}

/** Runs [initializers] in [AppInitializer.priority] order. */
fun Set<AppInitializer>.initAll(application: Application) {
    sortedByDescending { it.priority }.forEach { it.init(application) }
}
