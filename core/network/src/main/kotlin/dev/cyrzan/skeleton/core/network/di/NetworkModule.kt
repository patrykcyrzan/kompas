package dev.cyrzan.skeleton.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.core.network.ExtraInterceptors
import dev.cyrzan.skeleton.core.network.NetworkConfig
import dev.cyrzan.skeleton.core.network.networkConfig
import dev.cyrzan.skeleton.core.network.variantInterceptors
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

/** Needs [ExtraInterceptors] bound by the app. */
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    /** From `src/debug` or `src/release` of this module, depending on the variant. */
    @Provides
    fun provideNetworkConfig(): NetworkConfig = networkConfig

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // New fields on the backend must not break older app versions.
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(config: NetworkConfig, extra: ExtraInterceptors): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(config.connectTimeout)
            .readTimeout(config.readTimeout)
            // Outermost: logs what the app's interceptors (e.g. mocks) answer, too.
            .apply { variantInterceptors.forEach(::addInterceptor) }
            .apply { extra.values.forEach(::addInterceptor) }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(config: NetworkConfig, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
