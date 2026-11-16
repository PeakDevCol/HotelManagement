package com.peakdevcol.project.hotelmanagement.di

import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.data.catalog.StaticProductCatalog
import com.peakdevcol.project.hotelmanagement.data.firebase.FirebaseAuthRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreOrderRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreUserRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductCatalogRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CloseOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoginUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenOrdersUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingOrderCountUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RegisterPaymentUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReopenOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import com.peakdevcol.project.hotelmanagement.ui.AppViewModel
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderViewModel
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersViewModel
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailViewModel
import com.peakdevcol.project.hotelmanagement.ui.home.HomeViewModel
import com.peakdevcol.project.hotelmanagement.ui.login.LoginViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val dataModule = module {
    single<AuthRepository> { FirebaseAuthRepository(auth = lazy { Firebase.auth }) }
    single<UserRepository> { FirestoreUserRepository(firestore = lazy { Firebase.firestore }) }
    single<OrderRepository> { FirestoreOrderRepository(firestore = lazy { Firebase.firestore }) }
    single<ProductCatalogRepository> { StaticProductCatalog() }
}

private val domainModule = module {
    factoryOf(::LoginUseCase)
    factoryOf(::LogOutUseCase)
    factoryOf(::ObserveAccessUseCase)
    factoryOf(::ObserveOpenOrdersUseCase)
    factoryOf(::ObserveOrderDetailUseCase)
    factoryOf(::CloseOrderUseCase)
    factoryOf(::ReopenOrderUseCase)
    factoryOf(::RegisterPaymentUseCase)
    factoryOf(::CancelOrderUseCase)
    factoryOf(::ObservePendingOrderCountUseCase)
    factoryOf(::SaveOrderUseCase)
    factoryOf(::GetProductsUseCase)
}

private val uiModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AddOrderViewModel)
    viewModelOf(::CurrentOrdersViewModel)
    viewModel { parameters ->
        OrderDetailViewModel(
            orderId = parameters.get(),
            observeOrderDetail = get(),
            closeOrder = get(),
            reopenOrder = get(),
            registerPayment = get(),
            cancelOrder = get(),
        )
    }
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
