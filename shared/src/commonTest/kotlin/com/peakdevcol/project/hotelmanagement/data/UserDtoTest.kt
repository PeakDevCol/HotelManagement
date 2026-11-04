package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals

class UserDtoTest {
    @Test
    fun storesTheTypeWithTheLowercaseCodeOfThePrd() {
        val dto = NewUser(fullName = "Ana Gómez", email = "ana@hotel.co", type = UserType.ADMIN).toDto()

        assertEquals("Ana Gómez", dto.fullName)
        assertEquals("ana@hotel.co", dto.email)
        assertEquals("admin", dto.type)
    }

    @Test
    fun letsTheServerSetTheCreationDate() {
        val dto = NewUser(fullName = "Ana Gómez", email = "ana@hotel.co").toDto()

        assertEquals(Timestamp.ServerTimestamp, dto.createdAt)
    }
}
