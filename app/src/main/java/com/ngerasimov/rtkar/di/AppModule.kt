package com.ngerasimov.rtkar.di

import android.content.Context
import com.ngerasimov.rtkar.data.StorageManager
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import com.ngerasimov.rtkar.data.repository.DeviceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-wide dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideStorageManager(@ApplicationContext context: Context): StorageManager {
        return StorageManager(context)
    }
    
    @Provides
    @Singleton
    fun provideRtkApiWrapper(@ApplicationContext context: Context): RtkApiWrapper {
        return RtkApiWrapper(context)
    }
    
    @Provides
    @Singleton
    fun provideDeviceRepository(
        apiWrapper: RtkApiWrapper,
        storageManager: StorageManager
    ): DeviceRepository {
        return DeviceRepository(apiWrapper, storageManager)
    }
}
