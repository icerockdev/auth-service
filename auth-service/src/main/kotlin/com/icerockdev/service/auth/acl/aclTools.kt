/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.acl

import com.icerockdev.service.auth.jwt.TokenTypes
import com.icerockdev.service.auth.revoke.IRevokeTokenService
import com.icerockdev.service.auth.revoke.UserKey
import io.ktor.auth.jwt.JWTCredential

fun JWTCredential.intRoleValidate(roleListAccess: List<Int>, roleClaim: String = "role"): Boolean {
    val role = payload.getClaim(roleClaim).asInt()
    if (roleListAccess.isEmpty()) {
        return true
    }

    if (!roleListAccess.contains(role)) {
        return false
    }
    return true
}

fun JWTCredential.audienceValidate(audience: String): Boolean {
    return payload.audience.contains(audience)
}

fun JWTCredential.userIdValidate(userIdClaim: String = "id"): Boolean {
    return !payload.getClaim(userIdClaim).isNull
}

fun JWTCredential.revokeValidate(revokeTokenService: IRevokeTokenService<Int>, userIdClaim: String = "id"): Boolean {
    val userId = payload.getClaim(userIdClaim).asInt()
    if (payload.issuedAt === null || userId === null) {
        return false
    }

    return revokeTokenService.checkIsActive(userId, payload.issuedAt.time)
}

fun JWTCredential.revokeValidate(
    revokeTokenService: IRevokeTokenService<UserKey>,
    userIdClaim: String = "id",
    roleClaim: String = "role"
): Boolean {
    val userId = payload.getClaim(userIdClaim).asInt()
    val roleId = payload.getClaim(roleClaim).asInt()
    if (payload.issuedAt === null || userId === null || roleId === null) {
        return false
    }

    return revokeTokenService.checkIsActive(UserKey(userId, roleId), payload.issuedAt.time)
}

fun JWTCredential.checkIsAccessToken(typeClaim: String = "type"): Boolean {
    if (payload.getClaim(typeClaim).asString() == TokenTypes.TOKEN_TYPE_ACCESS.value) {
        return false
    }
    return true
}
