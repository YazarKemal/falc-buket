package com.prompthavenai.falcibuket.ui.screens.flows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BirthDateValidationTest {

    @Test
    fun acceptsValidPastDates() {
        assertNull(validateBirthDate("01.01.2000"))
        assertNull(validateBirthDate("15.06.1985"))
        assertNull(validateBirthDate(" 03.09.1999 "))
    }

    @Test
    fun acceptsLeapDayOnlyInLeapYears() {
        assertNull(validateBirthDate("29.02.2020"))
        assertNotNull(validateBirthDate("29.02.2021"))
    }

    @Test
    fun rejectsImpossibleCalendarDates() {
        assertNotNull(validateBirthDate("31.02.2000"))
        assertNotNull(validateBirthDate("32.01.2000"))
        assertNotNull(validateBirthDate("01.13.2000"))
        assertNotNull(validateBirthDate("00.00.2000"))
    }

    @Test
    fun rejectsWrongFormatAndBlank() {
        assertNotNull(validateBirthDate(""))
        assertNotNull(validateBirthDate("   "))
        assertNotNull(validateBirthDate("2000-01-01"))
        assertNotNull(validateBirthDate("1 Ocak 2000"))
        assertNotNull(validateBirthDate("01/01/2000"))
    }

    @Test
    fun rejectsFutureAndOutOfRangeYears() {
        assertNotNull(validateBirthDate("01.01.2999"))
        assertNotNull(validateBirthDate("01.01.1800"))
    }

    @Test
    fun returnsTurkishMessages() {
        assertEquals("Doğum tarihi gerekli", validateBirthDate(""))
        assertNotNull(validateBirthDate("01.13.2000"))
    }
}
