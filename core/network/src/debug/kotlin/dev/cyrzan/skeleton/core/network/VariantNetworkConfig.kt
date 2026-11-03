package dev.cyrzan.skeleton.core.network

import kotlin.time.Duration.Companion.seconds

/** Generous: breakpoints on the client or the backend, slow dev environments. */
internal val networkConfig = NetworkConfig(
    baseUrl = API_BASE_URL,
    connectTimeout = 60.seconds,
    readTimeout = 60.seconds,
)
