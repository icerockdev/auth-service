/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

import com.icerockdev.service.auth.jwt.JwtConfig
import com.icerockdev.service.auth.jwt.JwtTokenGenerator
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class JwtTest {

    @Test
    fun testJwt() {
        val jwtTokenGenerator = JwtTokenGenerator<Int>(
            JwtConfig(
                secret = "secret",
                audience = "audience",
                issuer = "localhost",
                accessTokenTtl = 10,
                refreshTokenTtl = 10
            )
        )

        val tokens = jwtTokenGenerator.makeTokens(1)

        assertNotNull(tokens)
        assertNotEquals(tokens.accessToken, "")
        assertNotEquals(tokens.refreshToken, "")
    }
}