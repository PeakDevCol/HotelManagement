package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRoomTypesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveRoomTypeUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomTypeRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeTaxRepository
import com.peakdevcol.project.hotelmanagement.fakes.roomType
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomTypeForm
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomTypesEffect
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomTypesIntent
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomTypesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** RF-HAB-01, with decision 3A. */
class RoomTypesViewModelTest : MainDispatcherTest() {
    private val iva = Tax("iva", "IVA", 19_00)
    private val oldTax = Tax("old", "Impoconsumo", 8_00, active = false)
    private val double = roomType("double", "Doble", taxId = "old")
    private val types = FakeRoomTypeRepository(double)

    private fun viewModel() = RoomTypesViewModel(ObserveRoomTypesUseCase(types, FakeTaxRepository(iva, oldTax)), SaveRoomTypeUseCase(types))

    @Test
    fun aNewTypeIsSavedWithItsRateAndTax() {
        val viewModel = viewModel()

        viewModel.onIntent(RoomTypesIntent.AddClick)
        viewModel.onIntent(RoomTypesIntent.NameChange("Suite"))
        viewModel.onIntent(RoomTypesIntent.CapacityChange("4 personas"))
        viewModel.onIntent(RoomTypesIntent.RateChange("$600.000"))
        viewModel.onIntent(RoomTypesIntent.TaxSelect("iva"))
        viewModel.onIntent(RoomTypesIntent.Save)

        assertNull(viewModel.state.value.form)
        val suite = types.types.value.single { it.name == "Suite" }
        assertEquals(4, suite.capacity)
        assertEquals(600_000, suite.baseRate)
        assertEquals("iva", suite.taxId)
        assertEquals(listOf("Doble", "Suite"), viewModel.state.value.catalog.types.map { it.name })
    }

    @Test
    fun aWrongFieldKeepsTheFormOpenWithItsError() {
        val viewModel = viewModel()
        viewModel.onIntent(RoomTypesIntent.AddClick)
        viewModel.onIntent(RoomTypesIntent.NameChange("Suite"))
        viewModel.onIntent(RoomTypesIntent.CapacityChange("30"))
        viewModel.onIntent(RoomTypesIntent.RateChange("600000"))

        viewModel.onIntent(RoomTypesIntent.Save)

        assertEquals(RoomError.InvalidCapacity, viewModel.state.value.form?.error)
    }

    @Test
    fun editingATypeOffersItsInactiveTaxOnlyToIt() {
        val viewModel = viewModel()
        viewModel.onIntent(RoomTypesIntent.AddClick)
        assertEquals(listOf("iva"), viewModel.state.value.selectableTaxes.map { it.id })

        viewModel.onIntent(RoomTypesIntent.EditClick(double))

        assertEquals(RoomTypeForm(typeId = "double", name = "Doble", capacityText = "2", rateDigits = "250000", taxId = "old"), viewModel.state.value.form)
        assertEquals(listOf("old", "iva"), viewModel.state.value.selectableTaxes.map { it.id })
    }

    @Test
    fun aRejectionFromTheServerClosesTheFormAndSaysWhy() = runTest {
        types.saveFailure = RoomError.NotAllowed
        val viewModel = viewModel()
        viewModel.onIntent(RoomTypesIntent.EditClick(double))

        viewModel.onIntent(RoomTypesIntent.Save)

        assertEquals(RoomTypesEffect.Failed(RoomError.NotAllowed), viewModel.effects.first())
        assertNull(viewModel.state.value.form)
    }
}
