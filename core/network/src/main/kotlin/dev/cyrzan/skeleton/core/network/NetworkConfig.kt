package dev.cyrzan.skeleton.core.network

import kotlin.time.Duration

internal const val API_BASE_URL = "https://api.kompas.example.com/"

/**
 * Per-variant network settings. Each build type has its own [networkConfig] in `src/debug` / `src/release`
 * of this module; the app's debug build picks this module's debug variant, release picks release.
 */
internal data class NetworkConfig(
    val baseUrl: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
)
