package org.goal2be.standard.auth.user

import kotlinx.serialization.Serializable
import org.goal2be.standard.serializers.UserTypeSerializer
import org.goal2be.standard.utils.*
import org.goal2be.standard.validator.Validated
import org.goal2be.standard.validator.types.extended.DefaultEnumMapper
import org.goal2be.standard.validator.types.extended.EnumValidator

@Suppress("UNUSED")
@Serializable(with = UserTypeSerializer::class)
@Validated(EnumValidator::class)
enum class UserType(@DefaultEnumMapper val value: Short, val fullName: String, val isMain: Boolean = false) {
    Fan(1, "Footballer Fan", true),
    Footballer(2, "Footballer Footballer", true),

    None(-1, "Default value if not found");  // for cases when lib is updating and new user type adds, but dependent service don't need to update

    companion object {
        val fromShort = enumFinderD(createEnumMap(UserType::value), None)
        val fromInt = enumFinderD(createEnumMap(UserType::class) { it.value.toInt() }, None)
        val fromString = enumFinderD(createEnumMap(UserType::class) { it.value.toString() }, None)

        val persons = listOf(Fan, Footballer)
        val organizations = listOf<UserType>()
    }

    fun isPerson() = this in persons
    fun isOrganization() = this in organizations
}

@Suppress("UNUSED")
fun UserTokenData.isFan() = type == UserType.Fan
@Suppress("UNUSED")
fun UserTokenData.isFootballer() = type == UserType.Footballer


@Suppress("UNUSED")
@Validated(EnumValidator::class)
enum class UserRole(@DefaultEnumMapper val value: Short) {
    Verified(1),
    Completed(2),
    Person(3),
    Organization(4),

    None(-1);  // for cases when lib is updating and new user type adds, but dependent service don't need to update

    companion object {
        val fromListShort = enumListFinderD(createEnumMap(UserRole::value), None)
        val fromListInt = enumListFinderD(createEnumMap(UserRole::class) { it.value.toInt() }, None)
    }
}

fun UserTokenData.isVerified() = UserRole.Verified in roles
fun UserTokenData.isCompleted() = UserRole.Completed in roles
fun UserTokenData.isPerson() = UserRole.Person in roles
fun UserTokenData.isOrganization() = UserRole.Organization in roles
