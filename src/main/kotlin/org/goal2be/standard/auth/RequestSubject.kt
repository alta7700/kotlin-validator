package org.goal2be.standard.auth

import io.ktor.server.application.*
import io.ktor.util.*

enum class RequestSubject(val value: String) {
    Anonymous("anonymous"),
    User("user"),
    Service("service");
}

val RequestSubjectAttrKey = AttributeKey<RequestSubject>("RequestSubject")
var ApplicationCall.requestSubject: RequestSubject
    get() = attributes.getOrNull(RequestSubjectAttrKey) ?: RequestSubject.Anonymous
    set(value) {
        attributes.put(RequestSubjectAttrKey, value)
    }

