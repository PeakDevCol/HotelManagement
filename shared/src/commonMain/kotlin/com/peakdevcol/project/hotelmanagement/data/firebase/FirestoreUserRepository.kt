package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.UserDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toUser
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class FirestoreUserRepository(
    private val firestore: Lazy<FirebaseFirestore>,
) : UserRepository {

    override fun observeUser(userId: String): Flow<User?> =
        firestore.value
            .collection(FirestorePaths.USERS)
            .document(userId)
            .snapshots
            .map { snapshot -> if (snapshot.exists) snapshot.data<UserDto>().toUser(userId) else null }
}
