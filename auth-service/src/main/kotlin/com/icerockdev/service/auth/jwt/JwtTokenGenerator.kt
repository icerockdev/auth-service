/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

const val TTL_MULTIPLIER = 1000

enum class TokenTypes(val value: String) {
    TOKEN_TYPE_ACCESS("ACCESS"),
    TOKEN_TYPE_REFRESH("REFRESH")
}

class JwtTokenGenerator(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC512(config.secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(config.issuer)
        .build()

    fun makeTokens(userId: Int, userRole: Int? = null): JwtTokens {
        val currentTimeMillis: Long = System.currentTimeMillis()
        val accessTokenExpiredAt: Long = currentTimeMillis + (config.accessTokenTtl * TTL_MULTIPLIER)
        val refreshTokenExpiredAt: Long = currentTimeMillis + (config.refreshTokenTtl * TTL_MULTIPLIER)

        val accessToken = JWT.create()
            .withSubject("Authentication")
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withClaim("type", TokenTypes.TOKEN_TYPE_ACCESS.value)
            .withClaim("id", userId)
            .withExpiresAt(Date(accessTokenExpiredAt))
            .withIssuedAt(Date(currentTimeMillis))

        userRole?.let {
            accessToken.withClaim("role", userRole)
        }

        val refreshToken = JWT.create()
            .withSubject("Authentication")
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withClaim("type", TokenTypes.TOKEN_TYPE_REFRESH.value)
            .withClaim("id", userId)
            .withExpiresAt(Date(refreshTokenExpiredAt))
            .withIssuedAt(Date(currentTimeMillis))

        userRole?.let {
            refreshToken.withClaim("role", userRole)
        }

        return JwtTokens(
            accessToken.sign(algorithm),
            refreshToken.sign(algorithm)
        )
    }
}

data class JwtTokens(val accessToken: String, val refreshToken: String)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenTtl: Long,
    val refreshTokenTtl: Long
)
