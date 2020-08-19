/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package rolebased

import com.icerockdev.service.auth.jwt.JwtConfig
import com.icerockdev.service.auth.jwt.JwtTokenGenerator
import com.icerockdev.service.auth.jwt.JwtTokens
import com.icerockdev.service.auth.revoke.UserKey

class CustomJwtTokenGenerator(config: JwtConfig): JwtTokenGenerator<UserKey>(config) {
    fun makeTokens(userId: Int, role: Int): JwtTokens {
        return makeTokens(UserKey(userId, 1)) {
            withClaim("role", role)
        }
    }
}