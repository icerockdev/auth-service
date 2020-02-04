/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.sample

import com.icerockdev.sample.Main.TOKEN_TTL
import com.icerockdev.service.auth.jwt.JwtConfig
import com.icerockdev.service.auth.jwt.JwtTokenGenerator
import com.icerockdev.service.auth.revoke.*
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
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object Main {
    val TOKEN_TTL: Long = 3600 * 1000L
    private val logger = LoggerFactory.getLogger(Main::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        val jwtTokenGenerator = JwtTokenGenerator(
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

        val notifier = TokenNotifyBus()
        val revokeTokenService: IRevokeTokenService = RevokeTokenService(
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
                this.port = 8080
            }
        }

        val server = embeddedServer(Netty, env)

        server.start(wait = true)
    }
}

const val AUDIENCE = "audience"

class TokenRepository : ITokenDataRepository {

    override suspend fun getAllNotExpired(): Map<Int, RevokeAtDto> {
        return mapOf(
            2 to RevokeAtDto(2, System.currentTimeMillis() - TOKEN_TTL)
        )
    }

    override fun insertOrUpdate(key: Int, value: RevokeAtDto): Boolean {
        return true
    }

    override fun cleanUp() {

    }

}