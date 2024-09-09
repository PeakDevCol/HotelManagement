package com.peakdevcol.project.hotelmanagement.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.peakdevcol.project.hotelmanagement.data.RemoteDataSource
import com.peakdevcol.project.hotelmanagement.data.network.AuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object FireBaseModule {

    @Singleton
    @Provides
    fun provideFirebaseAuth() = FirebaseAuth.getInstance()

    @Singleton
    @Provides
    fun providerFireBaseFireStore() = Firebase.firestore

    @Provides
    @Singleton
    fun providerRemoteSources(
        firebaseAuth: FirebaseAuth,
        fireStore: FirebaseFirestore
    ): RemoteDataSource = AuthService(firebaseAuth, fireStore)


}