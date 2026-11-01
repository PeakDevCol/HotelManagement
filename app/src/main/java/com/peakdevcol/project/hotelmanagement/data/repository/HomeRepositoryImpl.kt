package com.peakdevcol.project.hotelmanagement.data.repository

import com.peakdevcol.project.hotelmanagement.data.RemoteDataSource
import com.peakdevcol.project.hotelmanagement.domain.HomeRepository
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(private val remote: RemoteDataSource) :
    HomeRepository {
    override suspend fun logOut() {
        remote.logOut()
    }


}