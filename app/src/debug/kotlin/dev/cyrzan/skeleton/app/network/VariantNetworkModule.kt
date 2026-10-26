package dev.cyrzan.skeleton.app.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.core.network.ExtraInterceptors
import java.io.FileNotFoundException
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object VariantNetworkModule {

    @Provides
    @Singleton
    fun provideExtraInterceptors(@ApplicationContext context: Context, mockSettings: MockSettings): ExtraInterceptors =
        ExtraInterceptors(
            listOf(
                // Outermost, so mocked responses show up in Chucker too.
                ChuckerInterceptor.Builder(context)
                    .redactHeaders("Authorization", "Cookie")
                    .build(),
                MockInterceptor(
                    isEnabled = mockSettings::isEnabled,
                    readAsset = { path ->
                        try {
                            context.assets.open(path).bufferedReader().use { it.readText() }
                        } catch (_: FileNotFoundException) {
                            null
                        }
                    },
                ),
            ),
        )
}
