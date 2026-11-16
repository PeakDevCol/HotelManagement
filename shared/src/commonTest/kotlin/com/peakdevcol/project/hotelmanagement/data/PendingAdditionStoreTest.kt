package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.local.LocalPendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.data.local.LocalTextStore
import com.peakdevcol.project.hotelmanagement.data.local.PendingAdditionCodec
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PendingAdditionStoreTest {
    private class MemoryTextStore(var text: String? = null) : LocalTextStore {
        override fun read(): String? = text
        override fun write(text: String) {
            this.text = text
        }
    }

    private val beer = Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500, quantity = 2, lineId = "line-a")
    private val soda = Product(id = "3", name = "Coca-Cola", unitPrice = 3_000, quantity = 1, lineId = "line-b")
    private val toTable = PendingAddition("addition-1", "order-1", OrderDestination.Table(3), listOf(beer, soda))
    private val toPool = PendingAddition("addition-2", "order-2", OrderDestination.Pool(), listOf(soda), rejected = true)

    @Test
    fun readsBackWhatItWrote() {
        val additions = listOf(toTable, toPool, toTable.copy(id = "addition-3", destination = OrderDestination.Other))

        assertEquals(additions, PendingAdditionCodec.decode(PendingAdditionCodec.encode(additions)))
    }

    @Test
    fun anEmptyListIsAnEmptyText() {
        assertEquals("", PendingAdditionCodec.encode(emptyList()))
        assertTrue(PendingAdditionCodec.decode("").isEmpty())
    }

    @Test
    fun aProductNameWithStrangeCharactersDoesNotBreakTheFormat() {
        val strange = beer.copy(name = "Cerveza\u001EAguila\u001F 2x1\u001D\u001C")

        val read = PendingAdditionCodec.decode(PendingAdditionCodec.encode(listOf(toTable.copy(products = listOf(strange)))))

        assertEquals("CervezaAguila 2x1", read.single().products.single().name)
    }

    @Test
    fun aDamagedRecordIsSkippedAndTheRestIsKept() {
        val text = PendingAdditionCodec.encode(listOf(toTable)) + "\u001E" + "garbage" + "\u001E" + PendingAdditionCodec.encode(listOf(toPool))

        assertEquals(listOf(toTable, toPool), PendingAdditionCodec.decode(text))
    }

    @Test
    fun theAdditionsSurviveClosingTheApp() = runTest {
        val file = MemoryTextStore()
        val beforeClosing = LocalPendingAdditionRepository(lazy { file })
        beforeClosing.save(toTable)
        beforeClosing.save(toPool)

        val afterOpening = LocalPendingAdditionRepository(lazy { file })

        assertEquals(listOf(toTable, toPool), afterOpening.observePendingAdditions().first())
    }

    @Test
    fun savingAgainReplacesTheAdditionAndRemovingForgetsIt() = runTest {
        val file = MemoryTextStore()
        val repository = LocalPendingAdditionRepository(lazy { file })
        repository.save(toTable)

        repository.save(toTable.copy(rejected = true))
        assertEquals(listOf(toTable.copy(rejected = true)), repository.observePendingAdditions().first())

        repository.remove(setOf("addition-1"))
        assertTrue(LocalPendingAdditionRepository(lazy { file }).observePendingAdditions().first().isEmpty())
    }
}
