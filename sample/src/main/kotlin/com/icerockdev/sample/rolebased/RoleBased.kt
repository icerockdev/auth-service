/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.sample.rolebased

import com.icerockdev.sample.simple.TOKEN_TTL
import com.icerockdev.service.auth.jwt.JwtConfig
import com.icerockdev.service.auth.revoke.IRevokeTokenService
import com.icerockdev.service.auth.revoke.ITokenDataRepository
import com.icerockdev.service.auth.revoke.RevokeTokenService
import com.icerockdev.service.auth.revoke.TokenNotifyBus
import com.icerockdev.service.auth.revoke.UserKey
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.CallLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object RoleBased {
    private val logger = LoggerFactory.getLogger(RoleBased::class.java)

    fun main(args: Array<String>): NettyApplicationEngine {
        val jwtTokenGenerator = CustomJwtTokenGenerator(
            JwtConfig(
                secret = "secret",
                audience = AUDIENCE,
                issuer = "localhost",
                accessTokenTtl = 3600,
                refreshTokenTtl = 36000
            )
        )

        val tokens = jwtTokenGenerator.makeTokens(1, ROLE_ADMIN)
        println("token:")
        println(tokens.accessToken)

        val notifier = TokenNotifyBus<UserKey>()
        val revokeTokenService: IRevokeTokenService<UserKey> = RevokeTokenService(
            TokenRepository()
        ) {
            this.tokenTtl = TOKEN_TTL
            this.notifier = notifier
        }

        val env = applicationEngineEnvironment {
            this.log = logger
            module {
                intercept(ApplicationCallPipeline.Monitoring) {
                    try {
                        proceed()
                    } catch (e: Throwable) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            e.message ?: ""
                        )
                        proceed()
                        logger.error(e.localizedMessage, e)
                    }
                }


                install(CallLogging) {
                    level = Level.TRACE
                }
                installAuth(
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
            // Public API
            connector {
                this.port = 8083
            }
        }

        return embeddedServer(Netty, env)
    }
}

const val AUDIENCE = "audience-rolebased"

class TokenRepository : ITokenDataRepository<UserKey> {

    override suspend fun getAllNotExpired(): Map<UserKey, Long> {
        return mapOf(
            UserKey(2, ROLE_ADMIN) to System.currentTimeMillis() + TOKEN_TTL
        )
    }

    override fun insertOrUpdate(key: UserKey, revokeAt: Long): Boolean {
        return true
    }

    override fun cleanUp() {

    }
}
