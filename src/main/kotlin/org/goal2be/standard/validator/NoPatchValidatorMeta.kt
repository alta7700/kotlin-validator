package org.goal2be.standard.validator

abstract class NoPatchValidatorMeta<T: Any?> : ValidatorMeta<T>() {
    override fun patchValidator(validator: RealValidator<T>) {}
}
