package com.peakdevcol.project.hotelmanagement.di

import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.data.backup.backupRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirebaseAccountCreationRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirebaseAuthRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirebaseReceptionMarkingRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreAttendanceCorrectionRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreAttendanceRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreCashClosingRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreCategoryRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreDeviceRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreGuestRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreOrderRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestorePaymentRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreHousekeepingRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreProductRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreReservationRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreRoomNightRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreRoomRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreRoomTypeRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreSettingsRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreShiftRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreTaxRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreTeamAttendanceRepository
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestoreUserRepository
import com.peakdevcol.project.hotelmanagement.data.local.LocalPendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.data.local.LocalThisDevice
import com.peakdevcol.project.hotelmanagement.data.local.localTextStore
import com.peakdevcol.project.hotelmanagement.data.location.canMarkForOthersOnThisPlatform
import com.peakdevcol.project.hotelmanagement.data.location.locationProvider
import com.peakdevcol.project.hotelmanagement.domain.repository.AccountCreationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceCorrectionRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.BackupRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.CashClosingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.DeviceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.GuestRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.HousekeepingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationProvider
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.PaymentRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.PendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReceptionMarkingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomNightRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ShiftRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TaxRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TeamAttendanceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ThisDevice
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import com.peakdevcol.project.hotelmanagement.domain.usecase.AddProductsToOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.AuthorizeThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ChargeOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CloseOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CopyPreviousWeekUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CorrectMarkUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateReservationUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateEmployeeUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.DeleteCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.DeleteShiftUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.DismissRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ExportBackupUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.FindAvailableRoomsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetReservationContextUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.IdentifyThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadAttendanceReportUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadBalanceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadCashClosingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocateThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocationAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoginUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkAtReceptionUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkHousekeepingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkWithPhoneUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveDevicesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveHotelSettingsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveHotelZoneUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveInventoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveMyScheduleUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenEntriesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenEntryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenOrdersUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingCollectionUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingOrderCountUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveReceptionMarkingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRoomBoardUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRoomTypesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveScheduleWeekUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveUpcomingReservationsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveWhoIsWorkingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReconcilePendingAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RegisterMissingMarksUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RemoveProductFromOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReopenOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RevokeDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveAttendanceRulesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveCashClosingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveGuestUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveHotelZoneUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOperationalDayStartUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveProductUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveRoomTypeUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveRoomUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveShiftUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveTaxUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SearchGuestsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SendPasswordEmailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.UpdateEmployeeUseCase
import com.peakdevcol.project.hotelmanagement.ui.AppViewModel
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderViewModel
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddProductsViewModel
import com.peakdevcol.project.hotelmanagement.ui.attendance.AttendanceViewModel
import com.peakdevcol.project.hotelmanagement.ui.attendance.ReceptionMarkingViewModel
import com.peakdevcol.project.hotelmanagement.ui.attendance.WhoIsWorkingViewModel
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.AttendanceReviewViewModel
import com.peakdevcol.project.hotelmanagement.ui.balance.BalanceViewModel
import com.peakdevcol.project.hotelmanagement.ui.balance.CashClosingViewModel
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersViewModel
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailViewModel
import com.peakdevcol.project.hotelmanagement.ui.devices.DevicesViewModel
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormViewModel
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesViewModel
import com.peakdevcol.project.hotelmanagement.ui.hotelzone.HotelZoneViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoriesViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.InventoryViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.ProductFormViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.TaxesViewModel
import com.peakdevcol.project.hotelmanagement.ui.login.LoginViewModel
import com.peakdevcol.project.hotelmanagement.ui.main.MainViewModel
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomTypesViewModel
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomsViewModel
import com.peakdevcol.project.hotelmanagement.ui.schedules.SchedulesViewModel
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.initialize
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private const val PENDING_ADDITIONS_FILE = "pending-additions"
private const val ACCOUNT_CREATION_APP = "account-creation"
private const val RECEPTION_MARKING_APP = "reception-marking"
private const val THIS_DEVICE_FILE = "this-device"

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
    single<PaymentRepository> { FirestorePaymentRepository(firestore = lazy { Firebase.firestore }) }
    single<CashClosingRepository> { FirestoreCashClosingRepository(firestore = lazy { Firebase.firestore }) }
    single<ProductRepository> { FirestoreProductRepository(firestore = lazy { Firebase.firestore }) }
    single<CategoryRepository> { FirestoreCategoryRepository(firestore = lazy { Firebase.firestore }) }
    single<TaxRepository> { FirestoreTaxRepository(firestore = lazy { Firebase.firestore }) }
    single<SettingsRepository> { FirestoreSettingsRepository(firestore = lazy { Firebase.firestore }) }
    single<ShiftRepository> { FirestoreShiftRepository(firestore = lazy { Firebase.firestore }) }
    single<AttendanceRepository> { FirestoreAttendanceRepository(firestore = lazy { Firebase.firestore }) }
    single<TeamAttendanceRepository> { FirestoreTeamAttendanceRepository(firestore = lazy { Firebase.firestore }) }
    single<AttendanceCorrectionRepository> { FirestoreAttendanceCorrectionRepository(firestore = lazy { Firebase.firestore }) }
    single<DeviceRepository> { FirestoreDeviceRepository(firestore = lazy { Firebase.firestore }) }
    single<RoomTypeRepository> { FirestoreRoomTypeRepository(firestore = lazy { Firebase.firestore }) }
    single<RoomRepository> { FirestoreRoomRepository(firestore = lazy { Firebase.firestore }) }
    single<HousekeepingRepository> { FirestoreHousekeepingRepository(firestore = lazy { Firebase.firestore }) }
    single<GuestRepository> { FirestoreGuestRepository(firestore = lazy { Firebase.firestore }) }
    single<ReservationRepository> { FirestoreReservationRepository(firestore = lazy { Firebase.firestore }) }
    single<RoomNightRepository> { FirestoreRoomNightRepository(firestore = lazy { Firebase.firestore }) }
    single<ReceptionMarkingRepository> {
        val config = get<AppConfig>()
        FirebaseReceptionMarkingRepository(
            receptionApp = lazy { Firebase.initialize(config.platformContext, Firebase.app.options, RECEPTION_MARKING_APP) },
        )
    }
    single<ThisDevice> {
        val config = get<AppConfig>()
        LocalThisDevice(
            store = lazy { localTextStore(config.localDataDirectory, THIS_DEVICE_FILE) },
            canMarkForOthers = canMarkForOthersOnThisPlatform(),
        )
    }
    single<LocationProvider> { locationProvider(get()) }
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
    factoryOf(::ChargeOrderUseCase)
    factoryOf(::ObserveOrderUseCase)
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
    factory { LoadBalanceUseCase(paymentRepository = get(), settingsRepository = get(), cashClosingRepository = get()) }
    factoryOf(::LoadCashClosingUseCase)
    factoryOf(::SaveCashClosingUseCase)
    factoryOf(::ObservePendingCollectionUseCase)
    factoryOf(::ObserveScheduleWeekUseCase)
    factory { ObserveMyScheduleUseCase(authRepository = get(), shiftRepository = get()) }
    factoryOf(::SaveShiftUseCase)
    factoryOf(::DeleteShiftUseCase)
    factoryOf(::CopyPreviousWeekUseCase)
    factoryOf(::SaveAttendanceRulesUseCase)
    factoryOf(::ObserveOpenEntryUseCase)
    factory { MarkWithPhoneUseCase(authRepository = get(), attendanceRepository = get(), settingsRepository = get(), locationProvider = get()) }
    factory {
        MarkAtReceptionUseCase(receptionMarkingRepository = get(), deviceRepository = get(), settingsRepository = get(), thisDevice = get())
    }
    factoryOf(::ObserveReceptionMarkingUseCase)
    factoryOf(::LocationAccessUseCase)
    factoryOf(::LocateThisDeviceUseCase)
    factoryOf(::ObserveHotelZoneUseCase)
    factoryOf(::SaveHotelZoneUseCase)
    factoryOf(::ObserveDevicesUseCase)
    factoryOf(::AuthorizeThisDeviceUseCase)
    factoryOf(::RevokeDeviceUseCase)
    factoryOf(::IdentifyThisDeviceUseCase)
    factoryOf(::ObserveWhoIsWorkingUseCase)
    factoryOf(::ObserveOpenEntriesUseCase)
    factoryOf(::LoadAttendanceReportUseCase)
    factory { CorrectMarkUseCase(authRepository = get(), correctionRepository = get()) }
    factory { RegisterMissingMarksUseCase(authRepository = get(), correctionRepository = get()) }
    factoryOf(::ObserveRoomBoardUseCase)
    factoryOf(::ObserveRoomTypesUseCase)
    factoryOf(::SaveRoomTypeUseCase)
    factoryOf(::SaveRoomUseCase)
    factoryOf(::MarkHousekeepingUseCase)
    factory { GetReservationContextUseCase(authRepository = get(), userRepository = get(), settingsRepository = get()) }
    factory {
        FindAvailableRoomsUseCase(
            roomRepository = get(),
            roomTypeRepository = get(),
            taxRepository = get(),
            roomNightRepository = get(),
            settingsRepository = get(),
        )
    }
    factoryOf(::SearchGuestsUseCase)
    factoryOf(::SaveGuestUseCase)
    factory {
        CreateReservationUseCase(
            authRepository = get(),
            userRepository = get(),
            roomRepository = get(),
            roomTypeRepository = get(),
            taxRepository = get(),
            roomNightRepository = get(),
            reservationRepository = get(),
            settingsRepository = get(),
        )
    }
    factory { ObserveUpcomingReservationsUseCase(reservationRepository = get(), settingsRepository = get()) }
}

private val uiModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::AddOrderViewModel)
    viewModel { CurrentOrdersViewModel(observeOpenOrders = get()) }
    viewModelOf(::InventoryViewModel)
    viewModelOf(::EmployeesViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::BalanceViewModel)
    viewModelOf(::CashClosingViewModel)
    viewModel { AttendanceViewModel(observeMySchedule = get(), observeOpenEntry = get(), markWithPhone = get(), locationAccess = get()) }
    viewModelOf(::ReceptionMarkingViewModel)
    viewModel { WhoIsWorkingViewModel(observeWhoIsWorking = get()) }
    viewModel {
        AttendanceReviewViewModel(
            loadAttendanceReport = get(),
            observeOpenEntries = get(),
            observeEmployees = get(),
            correctMark = get(),
            registerMissingMarks = get(),
        )
    }
    viewModelOf(::HotelZoneViewModel)
    viewModelOf(::DevicesViewModel)
    viewModelOf(::RoomsViewModel)
    viewModelOf(::RoomTypesViewModel)
    viewModel {
        SchedulesViewModel(
            observeScheduleWeek = get(),
            observeEmployees = get(),
            saveShift = get(),
            deleteShift = get(),
            copyPreviousWeek = get(),
        )
    }
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
            chargeOrder = get(),
            cancelOrder = get(),
            removeProductFromOrder = get(),
        )
    }
    viewModel { parameters ->
        AddProductsViewModel(orderId = parameters.get(), observeOrder = get(), getProducts = get(), addProductsToOrder = get())
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
