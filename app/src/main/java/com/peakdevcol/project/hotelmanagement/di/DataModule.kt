package com.peakdevcol.project.hotelmanagement.di

import com.peakdevcol.project.hotelmanagement.data.repository.LoginRepository
import com.peakdevcol.project.hotelmanagement.data.repository.LoginRepositoryImpl
import com.peakdevcol.project.hotelmanagement.data.repository.SignInRepository
import com.peakdevcol.project.hotelmanagement.data.repository.SignInRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {

    @Binds
    fun bindsLoginRepository(loginRepositoryImpl: LoginRepositoryImpl): LoginRepository

    @Binds
    fun bindsSignInRepository(signInRepositoryImpl: SignInRepositoryImpl): SignInRepository

}