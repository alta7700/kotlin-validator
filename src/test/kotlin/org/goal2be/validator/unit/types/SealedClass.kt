package org.goal2be.validator.unit.types

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.goal2be.standard.validator.*
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals

class SealedClassTest {

    companion object{
        @JvmStatic
        @BeforeAll
        fun initTypes() {
            ValidatorTypesRegistry()
        }
    }

    @Validated(SealedDiscriminatorValidator::class)
    sealed interface Root {
        val name: String

        @DiscriminatorTag("first")
        data class First(override val name: String, val cool: Int) : Root {
            companion object { @DCValidator val validator = DataClassValidator.create<First>() }
        }
        @DiscriminatorTag("second")
        data class Second(override val name: String, val veryCool: String) : Root {
            companion object { @DCValidator val validator = DataClassValidator.create<Second> {
                alias(Second::veryCool, "very_cool")
            } }
        }
        @DiscriminatorTag("third")
        data class Third(override val name: String, val veryVeryCool: Short) : Root {
            companion object { @DCValidator val validator = DataClassValidator.create<Third> {
                alias(Third::veryVeryCool, "very_very_cool")
            } }
        }
    }

    @Test
    fun testRoot() {
        with(ValidatorTypesRegistry.match<Root>()) {
            assertEquals(Root.First("HEHE", 123123123), convertFrom<JsonElement, Root>(Json.parseToJsonElement(
                """{ "type": "first", "data": {"name": "HEHE", "cool": "123123123"}}""".trimIndent()
            )))
            assertEquals(Root.Second("HEHE", "HEHE"), convertFrom<JsonElement, Root>(Json.parseToJsonElement(
                """{ "type": "second", "data": {"name": "HEHE", "very_cool": "HEHE"}}""".trimIndent()
            )))
            assertEquals(Root.Third("HEHE", 12312),convertFrom<JsonElement, Root>(Json.parseToJsonElement(
                """{ "type": "third", "data": {"name": "HEHE", "very_very_cool": "12312"}}""".trimIndent()
            )))
        }
    }
}