package com.peakdevcol.project.hotelmanagement.di

import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.data.backup.backupRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirebaseAccountCreationRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirebaseAuthRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreCategoryRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreOrderRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreProductRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreSettingsRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreTaxRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreUserRepository
import com.peakdevcol.project.hotelmanagement.data.local.LocalPendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.data.local.localTextStore
import com.peakdevcol.project.hotelmanagement.domain.repository.AccountCreationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.BackupRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.PendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TaxRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import com.peakdevcol.project.hotelmanagement.domain.usecase.AddProductsToOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CloseOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateEmployeeUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.DeleteCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.DismissRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ExportBackupUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoginUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveHotelSettingsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveInventoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenOrdersUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReconcilePendingAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingOrderCountUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RegisterPaymentUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RemoveProductFromOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReopenOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOperationalDayStartUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveProductUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveTaxUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SendPasswordEmailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.UpdateEmployeeUseCase
import com.peakdevcol.project.hotelmanagement.ui.AppViewModel
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderViewModel
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersViewModel
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailViewModel
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormViewModel
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesViewModel
import com.peakdevcol.project.hotelmanagement.ui.home.HomeViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoriesViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.InventoryViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.ProductFormViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.TaxesViewModel
import com.peakdevcol.project.hotelmanagement.ui.login.LoginViewModel
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private const val PENDING_ADDITIONS_FILE = "pending-additions"
private const val ACCOUNT_CREATION_APP = "account-creation"

private val dataModule = module {
    single<AuthRepository> { FirebaseAuthRepository(auth = lazy { Firebase.auth }) }
    single<AccountCreationRepository> {
        val config = get<AppConfig>()
        FirebaseAccountCreationRepository(
            secondaryAuth = lazy {
                Firebase.auth(Firebase.initialize(config.platformContext, Firebase.app.options, ACCOUNT_CREATION_APP))
            },
        )
    }
    single<UserRepository> { FirestoreUserRepository(firestore = lazy { Firebase.firestore }) }
    single<OrderRepository> { FirestoreOrderRepository(firestore = lazy { Firebase.firestore }) }
    single<ProductRepository> { FirestoreProductRepository(firestore = lazy { Firebase.firestore }) }
    single<CategoryRepository> { FirestoreCategoryRepository(firestore = lazy { Firebase.firestore }) }
    single<TaxRepository> { FirestoreTaxRepository(firestore = lazy { Firebase.firestore }) }
    single<SettingsRepository> { FirestoreSettingsRepository(firestore = lazy { Firebase.firestore }) }
    single<BackupRepository> { backupRepository(firestore = lazy { Firebase.firestore }) }
    single<PendingAdditionRepository> {
        val config = get<AppConfig>()
        LocalPendingAdditionRepository(store = lazy { localTextStore(config.localDataDirectory, PENDING_ADDITIONS_FILE) })
    }
}

private val domainModule = module {
    factoryOf(::LoginUseCase)
    factoryOf(::SendPasswordEmailUseCase)
    factoryOf(::ObserveEmployeesUseCase)
    factoryOf(::CreateEmployeeUseCase)
    factoryOf(::UpdateEmployeeUseCase)
    factoryOf(::ObserveHotelSettingsUseCase)
    factoryOf(::SaveOperationalDayStartUseCase)
    factoryOf(::ExportBackupUseCase)
    factoryOf(::LogOutUseCase)
    factoryOf(::ObserveAccessUseCase)
    factoryOf(::ObserveOpenOrdersUseCase)
    factoryOf(::ObserveOrderDetailUseCase)
    factoryOf(::CloseOrderUseCase)
    factoryOf(::ReopenOrderUseCase)
    factoryOf(::RegisterPaymentUseCase)
    factoryOf(::CancelOrderUseCase)
    factoryOf(::AddProductsToOrderUseCase)
    factoryOf(::RemoveProductFromOrderUseCase)
    factoryOf(::ReconcilePendingAdditionsUseCase)
    factoryOf(::ObserveRejectedAdditionsUseCase)
    factoryOf(::DismissRejectedAdditionsUseCase)
    factoryOf(::ObservePendingOrderCountUseCase)
    factoryOf(::SaveOrderUseCase)
    factoryOf(::GetProductsUseCase)
    factoryOf(::ObserveInventoryUseCase)
    factoryOf(::SaveProductUseCase)
    factoryOf(::SaveCategoryUseCase)
    factoryOf(::SaveTaxUseCase)
    factoryOf(::MoveCategoryUseCase)
    factoryOf(::DeleteCategoryUseCase)
}

private val uiModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AddOrderViewModel)
    viewModelOf(::CurrentOrdersViewModel)
    viewModelOf(::InventoryViewModel)
    viewModelOf(::EmployeesViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { parameters ->
        EmployeeFormViewModel(
            employeeId = parameters.getOrNull(),
            observeEmployees = get(),
            observeAccess = get(),
            createEmployee = get(),
            updateEmployee = get(),
            sendPasswordEmail = get(),
        )
    }
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::TaxesViewModel)
    viewModel { parameters ->
        ProductFormViewModel(productId = parameters.getOrNull(), observeInventory = get(), saveProduct = get())
    }
    viewModel { parameters ->
        OrderDetailViewModel(
            orderId = parameters.get(),
            observeOrderDetail = get(),
            closeOrder = get(),
            reopenOrder = get(),
            registerPayment = get(),
            cancelOrder = get(),
            getProducts = get(),
            addProductsToOrder = get(),
            removeProductFromOrder = get(),
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
