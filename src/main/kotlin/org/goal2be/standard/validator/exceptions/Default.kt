package org.goal2be.standard.validator.exceptions

val NonNullableException = ValidationException("non_nullable", "Value can't be null.")
val IncorrectTypeException = ValidationException("incorrect_type", "Value's type is incorrect.")
val RequiredValueException = ValidationException("required_value", "Value is required, but missed.")
