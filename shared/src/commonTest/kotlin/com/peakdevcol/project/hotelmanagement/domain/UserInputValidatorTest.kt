package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.validation.UserInputValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserInputValidatorTest {

    @Test
    fun acceptsWellFormedEmails() {
        assertTrue(UserInputValidator.isValidEmail("juan@hotel.com"))
        assertTrue(UserInputValidator.isValidEmail("  maria.perez+bar@hotel.com.co "))
    }

    @Test
    fun rejectsMalformedEmails() {
        assertFalse(UserInputValidator.isValidEmail(""))
        assertFalse(UserInputValidator.isValidEmail("juan"))
        assertFalse(UserInputValidator.isValidEmail("juan@hotel"))
        assertFalse(UserInputValidator.isValidEmail("juan @hotel.com"))
    }

    @Test
    fun passwordNeedsAtLeastSixCharacters() {
        assertFalse(UserInputValidator.isValidPassword("12345"))
        assertTrue(UserInputValidator.isValidPassword("123456"))
    }

    @Test
    fun fullNameNeedsAtLeastSixCharactersIgnoringSpaces() {
        assertFalse(UserInputValidator.isValidFullName("  Ana  "))
        assertTrue(UserInputValidator.isValidFullName("Ana Ruiz"))
    }
}
