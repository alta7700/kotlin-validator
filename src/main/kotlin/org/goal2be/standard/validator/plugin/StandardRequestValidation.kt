package org.goal2be.standard.validator.plugin

import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import io.ktor.server.application.createApplicationPlugin

class StandardRequestValidationConfig(
    val initCustomTypes: (() -> Unit)? = null
)

val StandardRequestValidation = createApplicationPlugin("RequestValidation", ::StandardRequestValidationConfig) {

    ValidatorTypesRegistry()
    pluginConfig.initCustomTypes?.invoke()

}

