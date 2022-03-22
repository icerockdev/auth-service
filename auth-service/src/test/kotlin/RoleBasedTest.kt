/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

import rolebased.ROLE_ADMIN
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.Test
import rolebased.ROLE_OTHER
import rolebased.jwtTokenGenerator
import rolebased.roleBasedModule
import kotlin.test.assertEquals

class RoleBasedTest {

    @Test
    fun testAdminAuth() = withServer {
        val tokens = jwtTokenGenerator.makeTokens(1, ROLE_ADMIN)
        val req = handleRequest(HttpMethod.Get, "/") {
            addJwtHeader(tokens.accessToken)
        }

        req.run {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun testRevokeAdminAuth() = withServer {
        val tokens = jwtTokenGenerator.makeTokens(2, ROLE_ADMIN)
        val req = handleRequest(HttpMethod.Get, "/") {
            addJwtHeader(tokens.accessToken)
        }

        req.run {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Test
    fun testOtherAuth() = withServer {
        val tokens = jwtTokenGenerator.makeTokens(1, ROLE_OTHER)
        val req = handleRequest(HttpMethod.Get, "/") {
            addJwtHeader(tokens.accessToken)
        }

        req.run {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }
}

private fun TestApplicationRequest.addJwtHeader(token: String) = addHeader("Authorization", "Bearer $token")


private fun withServer(block: TestApplicationEngine.() -> Unit) {
    withTestApplication({ roleBasedModule() }, block)
}
