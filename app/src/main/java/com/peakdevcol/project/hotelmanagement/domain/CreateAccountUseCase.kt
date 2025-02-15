package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.ui.signin.model.BaseUserSignIn
import javax.inject.Inject

// UserSaveService is a class that contain the call to save data in fireStore and get a response

class CreateAccountUseCase @Inject constructor(private val signInRepository: SignInRepository) {
    suspend operator fun invoke(userSignIn: BaseUserSignIn): Boolean {
        return signInRepository.createAccount(userSignIn)
    }
}