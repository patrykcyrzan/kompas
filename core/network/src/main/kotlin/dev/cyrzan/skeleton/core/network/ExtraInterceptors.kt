package dev.cyrzan.skeleton.core.network

import okhttp3.Interceptor

/**
 * Application interceptors contributed by `:app` per variant (Chucker and mocks in debug, none in release).
 * A list, not a Hilt `Set`: order matters – the first one is the outermost and sees what the later ones return.
 */
class ExtraInterceptors(val values: List<Interceptor>)
