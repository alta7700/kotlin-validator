@file:Suppress("UNUSED")
package org.goal2be.standard.auth.rbac

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import org.goal2be.standard.auth.RequestSubject
import org.goal2be.standard.auth.requestSubject
import org.goal2be.standard.auth.service.ServiceTokenData
import org.goal2be.standard.auth.service.getServiceTokenData
import org.goal2be.standard.auth.user.UserTokenData
import org.goal2be.standard.auth.user.getUserTokenData

val RBACPlugin = createRouteScopedPlugin("G2B_RBAC", ::RBACPluginConfig) {
    on(AuthenticationChecked) { call ->
        val mayBeCause: PermissionDeniedExceptionCause? = when (call.requestSubject) {
            RequestSubject.User -> {
                pluginConfig.userChecks.check(call.getUserTokenData(), call.principal()!!)
            }
            RequestSubject.Service -> {
                pluginConfig.serviceChecks.check(call.getServiceTokenData(), call.principal()!!)
            }
            RequestSubject.Anonymous -> null
        }

        if (mayBeCause != null) {
            throw PermissionDeniedException(mayBeCause)
        }
    }
}

class RBACPluginConfig {

    lateinit var userChecks: RBACCheck<UserTokenData, JWTPrincipal>
    lateinit var serviceChecks: RBACCheck<ServiceTokenData, JWTPrincipal>

}


fun Route.rbac(
    configure: RBACConfigHelper.() -> Unit,
    build: Route.() -> Unit
) : Route {

    val config = RBACConfigHelper().also { it.configure() }
    val configNames = config.getConfigNames()

    return if (config.rbacDisabled())
        authenticate(*configNames, strategy = config.strategy, build = build)
    else
        authenticate(*configNames, strategy = config.strategy) {
            createChild(RBACRouteSelector()).also { rbacRoute ->
                rbacRoute.install(RBACPlugin) {
                    userChecks = config.userChecks
                    serviceChecks = config.serviceChecks
                }
                rbacRoute.build()
            }
        }
}

class RBACConfigHelper {

    sealed interface Rules<D: Any, P: Principal> {
        class Check<D: Any, P: Principal>(val check: RBACCheck<D, P>) : Rules<D, P> {
            override fun toCheck(): RBACCheck<D, P> = check
        }

        class Allow<D: Any, P: Principal> : Rules<D, P> {
            override fun toCheck(): RBACCheck<D, P> = RBACSingleCheck(
                "This message will never be reached :)",
                boolCheck = { _, _ -> true }
            )
        }

        class Disallow<D: Any, P: Principal> : Rules<D, P> {
            override fun toCheck() : RBACCheck<D, P> = RBACSingleCheck(
                "This message will never be reached :)",
                boolCheck = { _, _ -> false }
            )
        }

        fun toCheck() : RBACCheck<D, P>
    }

    var strategy: AuthenticationStrategy = AuthenticationStrategy.FirstSuccessful
    fun optional() { strategy = AuthenticationStrategy.Optional }
    fun required() { strategy = AuthenticationStrategy.FirstSuccessful }

    private var userRules: Rules<UserTokenData, JWTPrincipal> = Rules.Disallow()
    val userChecks get() = userRules.toCheck()
    fun allowUser() { userRules = Rules.Allow() }
    fun user(configureChecks: AndChecksGroup<UserTokenData, JWTPrincipal>.() -> Unit) {
        userRules = Rules.Check(AndChecksGroup<UserTokenData, JWTPrincipal>().also { it.configureChecks() })
    }
    fun customUserCheck(check: (data: UserTokenData, principal: JWTPrincipal) -> PermissionDeniedExceptionCause?) {
        userRules = Rules.Check(RBACCustomCheck(check))
    }

    private var serviceRules: Rules<ServiceTokenData, JWTPrincipal> = Rules.Disallow()
    val serviceChecks get() = serviceRules.toCheck()
    fun allowService() { serviceRules = Rules.Allow() }
    fun service(configureChecks: AndChecksGroup<ServiceTokenData, JWTPrincipal>.() -> Unit) {
        serviceRules = Rules.Check(AndChecksGroup<ServiceTokenData, JWTPrincipal>().also { it.configureChecks() })
    }
    fun customServiceCheck(check: (data: ServiceTokenData, principal: JWTPrincipal) -> PermissionDeniedExceptionCause?) {
        serviceRules = Rules.Check(RBACCustomCheck(check))
    }


    fun rbacDisabled(): Boolean = !(userRules is Rules.Check || serviceRules is Rules.Check)

    fun getConfigNames() : Array<String> {
        return mutableListOf<String>().also {
            if (userRules !is Rules.Disallow) it.add("user-jwt")
            if (serviceRules !is Rules.Disallow) it.add("service-jwt")
        }.toTypedArray()
    }

}

class RBACRouteSelector : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Transparent
    override fun toString(): String = "(rbac)"
}
