package org.goal2be.validator.unit.types

import kotlinx.datetime.LocalDate
import org.goal2be.standard.utils.utcNow
import org.goal2be.standard.validator.DCValidator
import org.goal2be.standard.validator.DataClassValidator
import org.goal2be.standard.validator.convertFrom
import org.goal2be.standard.validator.exceptions.ValidationExceptionList
import org.goal2be.standard.validator.meta.defaultFactory
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import org.goal2be.standard.validator.types.extended.Optional
import org.goal2be.standard.validator.types.extended.maxPeriod
import org.goal2be.standard.validator.types.extended.minPeriod
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun initTypes() {
            ValidatorTypesRegistry()
        }
    }

    data class TestDataclass(
        val date: LocalDate,
        val optionalDate: Optional<LocalDate?>,
        val defaultDate: LocalDate,
    ) {
        companion object {
            @DCValidator
            val validator = DataClassValidator.create<TestDataclass> {
                field(TestDataclass::date) { minPeriod(years = 13).maxPeriod(years = 15) }
                field(TestDataclass::defaultDate) { defaultFactory { LocalDate.utcNow() } }
            }
        }
    }

    @Test
    fun opt() {
        val result = ValidatorTypesRegistry.match<TestDataclass>().convertFrom<Map<String, Any>, TestDataclass>(mapOf(
            "date" to "2010-07-07"
        ))
        assertEquals(result.date, LocalDate(2010, 7, 7))
        assertTrue(!result.optionalDate.isSet())
        assertEquals(result.defaultDate, LocalDate.utcNow())
    }

    @Test
    fun more() {
        assertThrows<ValidationExceptionList> {
            ValidatorTypesRegistry.match<TestDataclass>().convertFrom(mapOf(
                "date" to "2000-07-07"
            ))
        }
    }

    @Test
    fun less() {
        assertThrows<ValidationExceptionList> {
            ValidatorTypesRegistry.match<TestDataclass>().convertFrom(
                mapOf("date" to "2020-07-07")
            )
        }
    }
}