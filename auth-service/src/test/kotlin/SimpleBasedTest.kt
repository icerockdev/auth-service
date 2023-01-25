/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.assertEquals
import org.junit.Test
import simple.ROLE_ADMIN
import simple.ROLE_OTHER
import simple.jwtTokenGenerator
import simple.simpleBasedModule

class SimpleBasedTest {

    @Test
    fun testAdminAuth() = withServer {
        val tokens = jwtTokenGenerator.makeTokens(1, ROLE_ADMIN)
        val response = client.get("/") {
            addJwtHeader(tokens.accessToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testRevokeAdminAuth() = withServer {
        val tokens = jwtTokenGenerator.makeTokens(2, ROLE_ADMIN)
        val response = client.get("/") {
            addJwtHeader(tokens.accessToken)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testOtherAuth() = withServer {
        val tokens = jwtTokenGenerator.makeTokens(1, ROLE_OTHER)
        val response = client.get("/") {
            addJwtHeader(tokens.accessToken)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

private fun HttpRequestBuilder.addJwtHeader(token: String) = header("Authorization", "Bearer $token")


private fun withServer(block: suspend ApplicationTestBuilder.() -> Unit) {
    testApplication {
        application {
            simpleBasedModule()
        }
        block()
    }
}
