package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.UserDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toUser
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserDtoTest {
    private val dto = UserDto(
        fullName = "Ana Gómez",
        email = "ana@hotel.co",
        type = "reception",
        area = "housekeeping",
        active = true,
    )

    @Test
    fun readsTheRoleAndTheAreaFromTheLowercaseCodesOfThePrd() {
        val expected = User(
            id = "uid-1",
            fullName = "Ana Gómez",
            email = "ana@hotel.co",
            type = UserType.RECEPTION,
            area = UserArea.HOUSEKEEPING,
            active = true,
        )

        assertEquals(expected, dto.toUser("uid-1"))
    }

    @Test
    fun aProfileWithAnUnknownRoleOrAreaIsNotValid() {
        assertNull(dto.copy(type = "owner").toUser("uid-1"))
        assertNull(dto.copy(area = "").toUser("uid-1"))
    }

    @Test
    fun aProfileWithoutTheActiveFieldIsInactive() {
        val incomplete = UserDto(fullName = "Ana Gómez", email = "ana@hotel.co", type = "basic", area = "service")

        assertEquals(false, incomplete.toUser("uid-1")?.active)
    }
}
