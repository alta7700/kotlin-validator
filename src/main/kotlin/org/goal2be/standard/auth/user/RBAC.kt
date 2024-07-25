@file:Suppress("UNUSED")
package org.goal2be.standard.auth.user

import io.ktor.server.auth.*
import org.goal2be.standard.auth.rbac.RBACChecksGroup
import org.goal2be.standard.auth.rbac.RBACSingleCheck

fun <P: Principal> RBACChecksGroup<UserTokenData, P>.verified() = addCheck(userVerifiedCheck())
fun <P: Principal> userVerifiedCheck() = RBACSingleCheck<UserTokenData, P>(
    "User must be verified",
    "User must be not verified",
    { data, _ -> data.isVerified() }
)

fun <P: Principal> RBACChecksGroup<UserTokenData, P>.completed() = addCheck(userCompletedCheck())
fun <P: Principal> userCompletedCheck() = RBACSingleCheck<UserTokenData, P>(
    "User must be completed",
    "User must be not completed",
    { data, _ -> data.isCompleted() }
)

fun <P: Principal> RBACChecksGroup<UserTokenData, P>.person() = addCheck(userPersonCheck())
fun <P: Principal> userPersonCheck() = RBACSingleCheck<UserTokenData, P>(
    "User must be a person",
    "User must be not a person",
    { data, _ -> data.isPerson() }
)

fun <P: Principal> RBACChecksGroup<UserTokenData, P>.organization() = addCheck(userOrganizationCheck())
fun <P: Principal> userOrganizationCheck() = RBACSingleCheck<UserTokenData, P>(
    "User must be an organization",
    "User must be not a organization",
    { data, _ -> data.isOrganization() }
)

fun <P: Principal> RBACChecksGroup<UserTokenData, P>.nullType() = addCheck(userTypeCheck(null))
fun <P: Principal> RBACChecksGroup<UserTokenData, P>.onlyType(type: UserType) = addCheck(userTypeCheck(type))
fun <P: Principal> userTypeCheck(type: UserType?) = RBACSingleCheck<UserTokenData, P>(
    "User type must be $type",
    "User must be not $type",
    { data, _ -> data.type == type }
)

fun <P: Principal> RBACChecksGroup<UserTokenData, P>.oneOfType(vararg types: UserType) = addCheck(userOneOfTypesCheck(types.toList()))
fun <P: Principal> userOneOfTypesCheck(types: List<UserType>) = RBACSingleCheck<UserTokenData, P>(
    "User type must be one of $types",
    "User type must be not one of $types",
    { data, _ -> data.type in types }
)
