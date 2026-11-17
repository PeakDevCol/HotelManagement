package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.TaxDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toTax
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.repository.TaxRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

internal class FirestoreTaxRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = INVENTORY_SERVER_WAIT,
) : TaxRepository {

    private val taxes get() = firestore.value.collection(FirestorePaths.TAXES)

    override fun observeTaxes(): Flow<List<Tax>> =
        taxes.snapshots.map { snapshot -> snapshot.documents.map { it.data<TaxDto>().toTax(it.id) } }

    override suspend fun save(tax: Tax): Result<Unit> = inventoryWrite(serverWait) {
        taxes.document(tax.id).set(tax.toDto())
    }
}
