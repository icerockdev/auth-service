/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.sample.simple

import com.auth0.jwt.JWTVerifier
import com.icerockdev.service.auth.acl.*
import com.icerockdev.service.auth.revoke.simple.IRevokeTokenService
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTAuthenticationProvider
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt

fun Application.installAuth(
    verifier: JWTVerifier,
    audience: String,
    revokeTokenService: IRevokeTokenService
) {

    fun JWTAuthenticationProvider.Configuration.accessVerify(
        roleListAccess: List<Int> = emptyList()
    ) {
        verifier(verifier)
        validate { credential ->
            if (credential.checkIsAccessToken()) {
                return@validate null
            }

            if (!credential.audienceValidate(audience)) {
                return@validate null
            }

            if (!credential.intRoleValidate(roleListAccess)) {
                return@validate null
            }

            if (!credential.revokeValidate(revokeTokenService)) {
                return@validate null
            }

            if (!credential.userIdValidate()) {
                return@validate null
            }

            return@validate JWTPrincipal(credential.payload)
        }
    }

    install(Authentication) {

        jwt {
            accessVerify()
        }

        jwt(ACCESS_ADMIN_ONLY) {
            accessVerify(listOf(ROLE_ADMIN))
        }
    }
}

const val ACCESS_ADMIN_ONLY = "admin"
const val ROLE_ADMIN = 10
