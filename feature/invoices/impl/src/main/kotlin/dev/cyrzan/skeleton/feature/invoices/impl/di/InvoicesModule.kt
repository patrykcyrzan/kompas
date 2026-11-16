package dev.cyrzan.skeleton.feature.invoices.impl.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.feature.invoices.impl.data.DefaultInvoicesRepository
import dev.cyrzan.skeleton.feature.invoices.impl.data.remote.InvoicesService
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoicesRepository
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface InvoicesModule {

    /** Stateless – no scope needed; a cache in the repository would make it `@Singleton`. */
    @Binds
    fun bindInvoicesRepository(impl: DefaultInvoicesRepository): InvoicesRepository

    companion object {
        /** The shared Retrofit from :core:network – base URL, timeouts and the app's interceptors (mocks, Chucker). */
        @Provides
        @Singleton
        fun provideInvoicesService(retrofit: Retrofit): InvoicesService = retrofit.create(InvoicesService::class.java)
    }
}
