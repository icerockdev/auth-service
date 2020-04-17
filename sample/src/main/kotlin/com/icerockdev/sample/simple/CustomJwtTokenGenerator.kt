/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.sample.simple

import com.icerockdev.service.auth.jwt.JwtConfig
import com.icerockdev.service.auth.jwt.JwtTokenGenerator
import com.icerockdev.service.auth.jwt.JwtTokens

class CustomJwtTokenGenerator(config: JwtConfig): JwtTokenGenerator<Int>(config) {
    fun makeTokens(userId: Int, role: Int): JwtTokens {
        return makeTokens(userId) {
            withClaim("role", role)
        }
    }
}