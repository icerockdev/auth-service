/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.jwt

import com.auth0.jwt.JWT
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.icerockdev.service.auth.revoke.IRevokeTokenService
import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal

internal val mapper = ObjectMapper().apply {
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    dateFormat = StdDateFormat()
    registerKotlinModule()
}

fun ApplicationCall.getJwtClaim(name: String) = authentication.principal<JWTPrincipal>()?.payload?.getClaim(name)

fun <T> getObjAsString(obj: T): String {
    return mapper.writeValueAsString(obj)
}

fun <T> readObjFromString(string: String?, clazz: Class<T>): T? {
    return try {
        mapper.readValue(string, clazz)
    } catch (e: Exception) {
        null
    }
}

fun <T> readObjFromString(string: String?, typeRef: TypeReference<T>): T? {
    return try {
        mapper.readValue(string, typeRef)
    } catch (e: Exception) {
        null
    }
}

inline fun <reified TUserKey : Any> JWTCredential.inArrayValidate(
    accessList: List<TUserKey>,
    claimName: String
): Boolean {
    if (accessList.isEmpty()) {
        return true
    }

    val role = try {
        payload.getClaim(claimName).`as`(TUserKey::class.java) ?: return false
    } catch (e: Exception) {
        return false
    }

    if (!accessList.contains(role)) {
        return false
    }
    return true
}

fun JWTCredential.audienceValidate(audience: String): Boolean {
    return payload.audience.contains(audience)
}

fun JWTCredential.userValidate(userClaim: String = "user"): Boolean {
    return !payload.getClaim(userClaim).isNull
}

fun <TUserKey : Any> JWTCredential.revokeValidate(
    userKeyClass: Class<TUserKey>,
    revokeTokenService: IRevokeTokenService<TUserKey>,
    userClaim: String = "user"
): Boolean {
    if (payload.issuedAt === null) {
        return false
    }

    val userStr = payload.getClaim(userClaim).asString() ?: ""
    val user = readObjFromString(userStr, userKeyClass) ?: return false

    return revokeTokenService.checkIsActive(user, payload.issuedAt.time)
}

fun JWTCredential.checkIsAccessToken(typeClaim: String = "type"): Boolean {
    if (payload.getClaim(typeClaim).asString() == TokenTypes.TOKEN_TYPE_ACCESS.value) {
        return false
    }
    return true
}

/**
 * Extract UserKey from Jwt token
 */
inline fun <reified TUserKey> getUserKey(token: String, userClaim: String = "user"): TUserKey? {
    val userStr = JWT.decode(token).getClaim(userClaim).asString() ?: return null
    return readObjFromString(userStr, jacksonTypeRef<TUserKey>())
}

inline fun <reified TUserKey> JWTCredential.getUserKey(userClaim: String = "user"): TUserKey? {
    return readObjFromString(payload.getClaim(userClaim).asString(), jacksonTypeRef<TUserKey>())
}

inline fun <reified TUserKey> ApplicationCall.getJwtUserKey(userClaim: String = "user"): TUserKey? {
    return readObjFromString(
        authentication.principal<JWTPrincipal>()?.payload?.getClaim(userClaim)?.asString(),
        jacksonTypeRef<TUserKey>()
    )
}