package com.peakdevcol.project.hotelmanagement.di

import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.data.firebase.FirebaseAuthRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreOrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateAccountUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoginUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveSessionUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import com.peakdevcol.project.hotelmanagement.ui.start.StartViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val dataModule = module {
    single<AuthRepository> {
        FirebaseAuthRepository(auth = lazy { Firebase.auth }, firestore = lazy { Firebase.firestore })
    }
    single<OrderRepository> { FirestoreOrderRepository(firestore = lazy { Firebase.firestore }) }
}

private val domainModule = module {
    factoryOf(::LoginUseCase)
    factoryOf(::CreateAccountUseCase)
    factoryOf(::LogOutUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::SaveOrderUseCase)
}

private val uiModule = module {
    viewModelOf(::StartViewModel)
}

fun initKoin(config: AppConfig, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            module { single { config } },
            dataModule,
            domainModule,
            uiModule,
        )
    }
}
