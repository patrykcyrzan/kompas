package dev.cyrzan.skeleton.core.startup.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import dev.cyrzan.skeleton.core.startup.AppInitializer

@Module
@InstallIn(SingletonComponent::class)
internal interface StartupModule {

    /** An app whose modules need no initialization still gets an (empty) set. */
    @Multibinds
    fun appInitializers(): Set<AppInitializer>
}
