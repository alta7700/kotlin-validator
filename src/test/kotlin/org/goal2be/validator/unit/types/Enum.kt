package org.goal2be.validator.unit.types

import org.goal2be.standard.validator.Validated
import org.goal2be.standard.validator.exceptions.ValidationException
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import org.goal2be.standard.validator.types.extended.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals

class EnumTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun initTypes() {
            ValidatorTypesRegistry()
        }
    }

    @Validated(EnumValidator::class)
    enum class TestEnumClass(
        @DefaultEnumMapper
        val value: Short,
        val otherValue: String
    ) {
        Test1(1, "test1"),
        Test2(2, "test2"),
        Test3(3, "test3");
    }

    @Test
    fun testEnum() {
        val shortValidator = ValidatorTypesRegistry.match<TestEnumClass>()
        val stringValidator = ValidatorTypesRegistry.match<TestEnumClass> { mapBy(TestEnumClass::otherValue) }
        val nameValidator = ValidatorTypesRegistry.match<TestEnumClass> { mapByName() }
        assertEquals(shortValidator.convertFrom(typeOf<String>(), "1"), TestEnumClass.Test1)
        assertThrows<ValidationException> { shortValidator.convertFrom(typeOf<String>(), "4") }
        assertEquals(stringValidator.convertFrom(typeOf<String>(), "test1"), TestEnumClass.Test1)
        assertThrows<ValidationException> { stringValidator.convertFrom(typeOf<String>(), "Test1") }
        assertEquals(nameValidator.convertFrom(typeOf<String>(), "Test1"), TestEnumClass.Test1)
        assertThrows<ValidationException> { nameValidator.convertFrom(typeOf<String>(), "test1") }
    }
}