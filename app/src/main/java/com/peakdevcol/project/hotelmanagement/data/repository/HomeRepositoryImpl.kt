package com.peakdevcol.project.hotelmanagement.data.repository

import com.peakdevcol.project.hotelmanagement.data.RemoteDataSource
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(private val remote: RemoteDataSource) :
    HomeRepository {
    override suspend fun logOut(): Boolean {
         return remote.logOut()
    }


}