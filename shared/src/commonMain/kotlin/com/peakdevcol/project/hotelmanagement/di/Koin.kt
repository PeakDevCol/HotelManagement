package com.peakdevcol.project.hotelmanagement.di

import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.data.catalog.StaticProductCatalog
import com.peakdevcol.project.hotelmanagement.data.firebase.FirebaseAuthRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreOrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductCatalogRepository
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateAccountUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoginUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveSessionUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import com.peakdevcol.project.hotelmanagement.ui.AppViewModel
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderViewModel
import com.peakdevcol.project.hotelmanagement.ui.home.HomeViewModel
import com.peakdevcol.project.hotelmanagement.ui.login.LoginViewModel
import com.peakdevcol.project.hotelmanagement.ui.signup.SignUpViewModel
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
    single<ProductCatalogRepository> { StaticProductCatalog() }
}

private val domainModule = module {
    factoryOf(::LoginUseCase)
    factoryOf(::CreateAccountUseCase)
    factoryOf(::LogOutUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::SaveOrderUseCase)
    factoryOf(::GetProductsUseCase)
}

private val uiModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::SignUpViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AddOrderViewModel)
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
