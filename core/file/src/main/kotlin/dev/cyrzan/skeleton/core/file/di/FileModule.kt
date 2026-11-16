package dev.cyrzan.skeleton.core.file.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.core.file.AndroidFileStorage
import dev.cyrzan.skeleton.core.file.FileStorage

@Module
@InstallIn(SingletonComponent::class)
internal interface FileModule {

    @Binds
    fun bindFileStorage(impl: AndroidFileStorage): FileStorage
}
