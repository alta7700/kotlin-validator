package org.goal2be.standard.validator.plugin

import io.ktor.server.application.*
import io.ktor.server.routing.*

val CallRequestValidation = createRouteScopedPlugin("RouteRequestValidation", { CallRequestValidationConfig() }) {

    on(RequestValidationHook) { call -> pluginConfig.data.parseCall(call) }
}

class CallRequestValidationConfig {
    private var dataWrapper: RequestValidationData<*>? = null
    var data
        get() = dataWrapper ?: throw Exception("RequestValidationData instance must be initialized")
        set(value) { dataWrapper = value }

}

fun <T: Any> Route.validate(data: RequestValidationData<T>, build: Route.() -> Unit): Route {
    val validatedRoute = createChild(ValidationRouteSelector())
    validatedRoute.install(CallRequestValidation) { this.data = data }
    validatedRoute.build()
    return validatedRoute
}

class ValidationRouteSelector : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation = RouteSelectorEvaluation.Transparent
    override fun toString(): String = "(validate)"
}
