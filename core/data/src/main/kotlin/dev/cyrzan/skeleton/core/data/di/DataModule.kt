package dev.cyrzan.skeleton.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.core.data.repository.FakeItemsRepository
import dev.cyrzan.skeleton.core.data.repository.FakeOrdersRepository
import dev.cyrzan.skeleton.core.data.repository.FakeSessionRepository
import dev.cyrzan.skeleton.core.data.repository.FakeSupportRepository
import dev.cyrzan.skeleton.core.data.repository.ItemsRepository
import dev.cyrzan.skeleton.core.data.repository.OrdersRepository
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import dev.cyrzan.skeleton.core.data.repository.SupportRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindItemsRepository(impl: FakeItemsRepository): ItemsRepository

    @Binds
    @Singleton
    abstract fun bindOrdersRepository(impl: FakeOrdersRepository): OrdersRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: FakeSessionRepository): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSupportRepository(impl: FakeSupportRepository): SupportRepository
}
