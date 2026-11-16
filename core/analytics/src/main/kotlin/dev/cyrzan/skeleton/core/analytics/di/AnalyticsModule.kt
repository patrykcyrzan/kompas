package dev.cyrzan.skeleton.core.analytics.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.analytics.Analytics
import dev.cyrzan.skeleton.core.analytics.ConsentAwareAnalytics
import dev.cyrzan.skeleton.core.analytics.consent.ConsentRepository
import dev.cyrzan.skeleton.core.analytics.consent.PrefsConsentRepository
import dev.cyrzan.skeleton.core.analytics.provider.AnalyticsProvider
import dev.cyrzan.skeleton.core.analytics.provider.AnonymousMetricsProvider
import dev.cyrzan.skeleton.core.analytics.provider.FirebaseAnalyticsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindConsentRepository(impl: PrefsConsentRepository): ConsentRepository

    // Adding PostHog = one more class and one more @IntoSet binding; nothing else changes.
    @Binds
    @IntoSet
    abstract fun bindFirebase(impl: FirebaseAnalyticsProvider): AnalyticsProvider

    @Binds
    @IntoSet
    abstract fun bindAnonymous(impl: AnonymousMetricsProvider): AnalyticsProvider

    companion object {
        @Provides
        @Singleton
        fun provideAnalytics(
            providers: Set<@JvmSuppressWildcards AnalyticsProvider>,
            consentRepository: ConsentRepository,
        ): Analytics = ConsentAwareAnalytics(
            providers = providers,
            consentRepository = consentRepository,
            // Lives as long as the process, like the singleton itself.
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
}
