package com.example.fruitylicious.di

import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.BranchConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    fun provideBranchConfig(branchConfigManager: BranchConfigManager): BranchConfig {
        return branchConfigManager.getBranchConfig()
    }
}
