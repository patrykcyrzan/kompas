package dev.cyrzan.skeleton.core.network

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/** Purely network debugging aids; ones that need the app (Chucker, mocks) come in via [ExtraInterceptors]. */
internal val variantInterceptors: List<Interceptor> = listOf(
    // Logcat tag "okhttp.OkHttpClient".
    HttpLoggingInterceptor()
        .setLevel(HttpLoggingInterceptor.Level.BODY)
        .apply {
            redactHeader("Authorization")
            redactHeader("Cookie")
        },
)
