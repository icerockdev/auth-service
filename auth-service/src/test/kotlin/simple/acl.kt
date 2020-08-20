/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package simple

import com.auth0.jwt.JWTVerifier
import com.icerockdev.service.auth.jwt.audienceValidate
import com.icerockdev.service.auth.jwt.checkIsAccessToken
import com.icerockdev.service.auth.jwt.inArrayValidate
import com.icerockdev.service.auth.jwt.revokeValidate
import com.icerockdev.service.auth.jwt.userValidate
import com.icerockdev.service.auth.revoke.IRevokeTokenService
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTAuthenticationProvider
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt

fun Application.installAuth(
    verifier: JWTVerifier,
    audience: String,
    revokeTokenService: IRevokeTokenService<Int>
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

            if (!credential.inArrayValidate(roleListAccess, "role")) {
                return@validate null
            }

            if (!credential.userValidate()) {
                return@validate null
            }

            if (!credential.revokeValidate(Int::class.java, revokeTokenService)) {
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
const val ROLE_OTHER = 20