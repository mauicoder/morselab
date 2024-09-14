package net.maui.morselab.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@InstallIn(ViewModelComponent::class)
@Module
interface RepositoryModules {
    @Binds
    fun provideUserPreferencesRepositoryImpl(repository: UserPreferencesRepositoryImpl): UserPreferencesRepository

}