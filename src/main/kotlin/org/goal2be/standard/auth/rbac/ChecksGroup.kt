package org.goal2be.standard.auth.rbac

import io.ktor.server.auth.*

@Suppress("UNUSED")
abstract class RBACChecksGroup<D: Any, P: Principal>(
    internal val checks: MutableList<RBACCheck<D, P>>,
    internal val inverted: Boolean,
) : RBACCheck<D, P> {

    fun customCheck(
        message: String,
        boolCheck: (data: D, principal: P) -> Boolean
    ) = addCheck(RBACSingleCheck(message, boolCheck = boolCheck), invert = false)

    fun addCheck(check: RBACCheck<D, P>) = addCheck(check, inverted)
    fun addCheck(check: RBACCheck<D, P>, invert: Boolean) {
        checks.add(if (invert) check.invert() else check)
    }

    fun and(build: RBACChecksGroup<D, P>.() -> Unit) = addCheck(AndChecksGroup<D, P>(inverted = inverted).also(build), invert = false)
    fun or(build: RBACChecksGroup<D, P>.() -> Unit) = addCheck(OrChecksGroup<D, P>(inverted = inverted).also(build), invert = false)
    fun not(build: RBACChecksGroup<D, P>.() -> Unit) = addCheck(AndChecksGroup<D, P>(inverted = !inverted).also(build), invert = false)

}

class AndChecksGroup<D: Any, P: Principal>(
    checks: MutableList<RBACCheck<D, P>> = mutableListOf(),
    inverted: Boolean = false
) : RBACChecksGroup<D, P>(checks, inverted) {

    override suspend fun check(data: D, principal: P) : PermissionDeniedExceptionCause? {
        for (check in checks) {
            val cause = check.check(data, principal)
            if (cause != null) return cause
        }
        return null
    }

    override fun invert() = AndChecksGroup(this.checks.map { it.invert() }.toMutableList(), inverted = !inverted)
}

class OrChecksGroup<D: Any, P: Principal>(
    checks: MutableList<RBACCheck<D, P>> = mutableListOf(),
    inverted: Boolean = false
) : RBACChecksGroup<D, P>(checks, inverted) {
    override suspend fun check(data: D, principal: P) : PermissionDeniedExceptionCause? {
        val causes = mutableListOf<PermissionDeniedExceptionCause>()
        for (check in checks) {
            val cause = check.check(data, principal) ?: return null
            causes.add(cause)
        }
        return PermissionDeniedExceptionCause.OneOf(causes.toList())
    }

    override fun invert() = OrChecksGroup(this.checks.map { it.invert() }.toMutableList(), inverted = !inverted)

}
