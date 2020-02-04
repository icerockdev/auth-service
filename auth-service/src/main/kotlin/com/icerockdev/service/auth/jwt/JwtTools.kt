/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.jwt

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal

fun ApplicationCall.getJwtClaim(name: String) = authentication.principal<JWTPrincipal>()?.payload?.getClaim(name)
