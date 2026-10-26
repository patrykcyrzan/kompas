package dev.cyrzan.skeleton.core.network

import kotlin.time.Duration.Companion.seconds

/** Fail fast and let the user retry instead of staring at a spinner. */
internal val networkConfig = NetworkConfig(
    baseUrl = API_BASE_URL,
    connectTimeout = 10.seconds,
    readTimeout = 15.seconds,
)
