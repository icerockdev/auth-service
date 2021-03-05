/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package simple

import com.icerockdev.service.auth.jwt.JwtConfig
import com.icerockdev.service.auth.revoke.IRevokeTokenService
import com.icerockdev.service.auth.revoke.ITokenDataRepository
import com.icerockdev.service.auth.revoke.RevokeTokenService
import com.icerockdev.service.auth.revoke.TokenNotifyBus
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing

const val TOKEN_TTL: Long = 3600 * 1000L
private const val AUDIENCE = "audience-simple"

val jwtTokenGenerator = CustomJwtTokenGenerator(
    JwtConfig(
        secret = "secret",
        audience = AUDIENCE,
        issuer = "localhost",
        accessTokenTtl = TOKEN_TTL,
        refreshTokenTtl = TOKEN_TTL * 10
    )
)

fun Application.simpleBasedModule() {
    val notifier = TokenNotifyBus<Int>()
    val revokeTokenService: IRevokeTokenService<Int> = RevokeTokenService(
        TokenRepository()
    ) {
        this.tokenTtl = TOKEN_TTL
        this.notifier = notifier
    }

    installAuth(
        verifier = jwtTokenGenerator.verifier,
        audience = AUDIENCE,
        revokeTokenService = revokeTokenService
    )

    routing {
        authenticate(ACCESS_ADMIN_ONLY) {
            get("") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }
        }
    }
}

class TokenRepository : ITokenDataRepository<Int> {

    override suspend fun getAllNotExpired(): Map<Int, Long> {
        return mapOf(
            2 to System.currentTimeMillis() + TOKEN_TTL
        )
    }

    override fun insertOrUpdate(key: Int, revokeAt: Long): Boolean {
        return true
    }

    override fun cleanUp() {

    }
}
