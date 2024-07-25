package org.goal2be.validator.unit.types

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.goal2be.standard.validator.*
import org.goal2be.standard.validator.exceptions.ValidationExceptionList
import org.goal2be.standard.validator.meta.default
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import org.goal2be.standard.validator.types.base.lte
import org.goal2be.standard.validator.types.extended.Optional
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals


class DataClassTest {
    companion object{
        @JvmStatic
        @BeforeAll
        fun initTypes() {
            ValidatorTypesRegistry()
        }
    }
    data class DCWithGenerics<T: Any, T2: Number>(
        val type: String,
        val data: T,
        val data2: T2,
    ) {
        companion object {
            @DCValidator("StringInt")
            val validator1 = DataClassValidator.create<DCWithGenerics<String, Int>>()
            @DCValidator("IntInt")
            val validator2 = DataClassValidator.create<DCWithGenerics<Int, Int>> {
                field(DCWithGenerics<Int, Int>::data) { lte(10) }
            }
        }
    }

    @Test
    fun testDCWithGenerics() {
        val validator = ValidatorTypesRegistry.match<DCWithGenerics<String, Int>> { useValidatorName("StringInt") }
        assertEquals(DCWithGenerics("asd", "asd", 1), validator.convertFrom<Map<String, String>, DCWithGenerics<String, Int>>(
            mapOf("type" to "asd", "data" to "asd", "data2" to "1")
        ))
        val validator2 = ValidatorTypesRegistry.match<DCWithGenerics<Int, Int>> { useValidatorName("IntInt") }
        assertEquals(DCWithGenerics("asd", 10, 1), validator2.convertFrom<Map<String, String>, DCWithGenerics<Int, Int>>(
            mapOf("type" to "asd", "data" to "10", "data2" to "1")
        ))
        assertThrows<ValidationExceptionList> { validator2.convertFrom<Map<String, String>, DCWithGenerics<Int, Int>>(
            mapOf("type" to "asd", "data" to "11", "data2" to "1")
        ) }
    }

    data class DC(
        @DefaultedOnly val type: String,
        val data: Optional<String>,
    ) {
        companion object {
            @DCValidator
            val validator = DataClassValidator.create<DC> {
                field(DC::type) { default("10") }
            }
        }
    }

    @Test
    fun testDC() {
        assertEquals(
            DC(type = "10", data = Optional.Value("qwe")),
            DC.validator.convertFrom<JsonElement, DC>(Json.parseToJsonElement("""{"type": "abracadabra", "data": "qwe"}"""))
        )
    }

}