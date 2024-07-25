package org.goal2be.standard.validator.types.extended

import org.goal2be.standard.utils.uuidOrNull
import org.goal2be.standard.validator.ValidatorMetaCollection
import org.goal2be.standard.validator.exceptions.IncorrectTypeException
import org.goal2be.standard.validator.ExtendedValidator
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

class UuidValidator<T: UUID?>(meta: ValidatorMetaCollection<T>, thisType: KType) : ExtendedValidator<T, String>(meta, thisType) {

    override val internalType: KType = typeOf<String>()
    private val uuidIncorrectTypeException = IncorrectTypeException(message = "Value must be a valid uuid")

    @Suppress("UNCHECKED_CAST")
    override fun convertFromInternal(value: String): T {
        return (value.uuidOrNull() as T?) ?: throw uuidIncorrectTypeException
    }

    init {
        applyMeta()
    }
}

fun ValidatorTypesRegistry.initUuid() {
    addMatcher<UUID?> { type, meta ->
        if (type.isSupertypeOf(typeOf<UUID>())) {
            UuidValidator(meta, type)
        }
        else null
    }
}
