package dev.cyrzan.skeleton.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.core.data.repository.FakeItemsRepository
import dev.cyrzan.skeleton.core.data.repository.ItemsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindItemsRepository(impl: FakeItemsRepository): ItemsRepository
}
