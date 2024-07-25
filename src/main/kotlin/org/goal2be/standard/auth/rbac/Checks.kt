package org.goal2be.standard.auth.rbac

import io.ktor.server.auth.Principal
interface RBACCheck<D: Any, P: Principal> {
    suspend fun check (data: D, principal: P) : PermissionDeniedExceptionCause?
    fun invert() : RBACCheck<D, P>
}

class RBACSingleCheck<D: Any, P: Principal>(
    val cause: PermissionDeniedExceptionCause.Cause,
    val invertedCause: PermissionDeniedExceptionCause.Cause,
    val boolCheck: (data: D, principal: P) -> Boolean,
    val invertedBoolCheck: (data: D, principal: P) -> Boolean = { data, principal -> !boolCheck(data, principal) }
) : RBACCheck<D, P> {
    constructor(
        message: String,
        invertMessage: String? = null,
        boolCheck: (data: D, principal: P) -> Boolean,
        invertBoolCheck: (data: D, principal: P) -> Boolean = { data, principal -> !boolCheck(data, principal) },
    ) : this(
        cause = PermissionDeniedExceptionCause.Cause(message),
        invertedCause = PermissionDeniedExceptionCause.Cause(invertMessage ?: "Invert: $message"),
        boolCheck = boolCheck,
        invertedBoolCheck = invertBoolCheck
    )

    override suspend fun check(data: D, principal: P) : PermissionDeniedExceptionCause? {
        return if (boolCheck(data, principal)) null else cause
    }

    override fun invert() = RBACSingleCheck(
        cause = invertedCause,
        invertedCause = cause,
        boolCheck = invertedBoolCheck,
        invertedBoolCheck = boolCheck,
    )
}

class RBACCustomCheck<D: Any, P: Principal>(
    val checkFunction: (data: D, principal: P) -> PermissionDeniedExceptionCause?
) : RBACCheck<D, P> {
    override suspend fun check(data: D, principal: P): PermissionDeniedExceptionCause? = checkFunction(data, principal)

    override fun invert(): RBACCheck<D, P> {
        throw NotImplementedError("This check can't be inverted successfully.")
    }
}
