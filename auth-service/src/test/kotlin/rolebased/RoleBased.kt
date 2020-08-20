/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package rolebased

import com.icerockdev.service.auth.jwt.JwtConfig
import com.icerockdev.service.auth.revoke.IRevokeTokenService
import com.icerockdev.service.auth.revoke.ITokenDataRepository
import com.icerockdev.service.auth.revoke.RevokeTokenService
import com.icerockdev.service.auth.revoke.TokenNotifyBus
import com.icerockdev.service.auth.revoke.UserKey
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing

private const val TOKEN_TTL = 3600L
const val AUDIENCE = "audience-rolebased"

val jwtTokenGenerator = CustomJwtTokenGenerator(
    JwtConfig(
        secret = "secret",
        audience = AUDIENCE,
        issuer = "localhost",
        accessTokenTtl = TOKEN_TTL,
        refreshTokenTtl = TOKEN_TTL * 10
    )
)

fun Application.roleBasedModule() {
    val notifier = TokenNotifyBus<UserKey>()
    val revokeTokenService: IRevokeTokenService<UserKey> = RevokeTokenService(
        TokenRepository()
    ) {
        this.tokenTtl = TOKEN_TTL
        this.notifier = notifier
    }

    installAuth(
        userKeyClass = UserKey::class.java,
        verifier = jwtTokenGenerator.verifier,
        audience = AUDIENCE,
        revokeTokenService = revokeTokenService
    )

    routing {
        authenticate(ACCESS_ADMIN_ONLY) {
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }
        }
    }
}

class TokenRepository : ITokenDataRepository<UserKey> {

    override suspend fun getAllNotExpired(): Map<UserKey, Long> {
        return mapOf(
            UserKey(2, USER_TYPE) to System.currentTimeMillis() + TOKEN_TTL
        )
    }

    override fun insertOrUpdate(key: UserKey, revokeAt: Long): Boolean {
        return true
    }

    override fun cleanUp() {

    }
}
