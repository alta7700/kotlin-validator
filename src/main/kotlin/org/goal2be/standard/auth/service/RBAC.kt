@file:Suppress("UNUSED")
package org.goal2be.standard.auth.service

import io.ktor.server.auth.*
import org.goal2be.standard.auth.rbac.RBACChecksGroup
import org.goal2be.standard.auth.rbac.RBACSingleCheck

fun <P: Principal> RBACChecksGroup<ServiceTokenData, P>.onlyNames(vararg names: String) = addCheck(serviceOnlyNames(names.toList()))
private fun <P: Principal> serviceOnlyNames(names: List<String>) = RBACSingleCheck<ServiceTokenData, P>(
    "Service with only this names are allowed ($names)",
    "Service with this name is not allowed",
    boolCheck = { data, _ -> data.serviceName in names }
)

fun <P: Principal> RBACChecksGroup<ServiceTokenData, P>.withGrants(vararg grantNames: String) = addCheck(serviceWithGrantsCheck(grantNames.toList()))
private fun <P: Principal> serviceWithGrantsCheck(grantNames: List<String>) = RBACSingleCheck<ServiceTokenData, P>(
    "Token must has all of grants ($grantNames)",
    boolCheck = { data, _ -> grantNames.all { it in data.grants } }
)

fun <P: Principal> RBACChecksGroup<ServiceTokenData, P>.oneOfGrant(vararg grantNames: String) = addCheck(serviceWithOneOfGrantCheck(grantNames.toList()))
private fun <P: Principal> serviceWithOneOfGrantCheck(grantNames: List<String>) = RBACSingleCheck<ServiceTokenData, P>(
    "Token must has one of grants ($grantNames)",
    boolCheck = { data, _ -> grantNames.any { it in data.grants } }
)
