package com.buypilot.core.common.di

import com.buypilot.core.common.coroutine.DefaultDispatcherProvider
import com.buypilot.core.common.coroutine.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}
