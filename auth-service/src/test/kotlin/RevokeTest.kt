/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */
import com.icerockdev.service.auth.revoke.*
import com.icerockdev.service.auth.revoke.IRevokeTokenService
import com.icerockdev.service.auth.revoke.RevokeTokenService
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RevokeTest {
    companion object {
        val TOKEN_TTL: Long = 3600 * 1000L
        val now = System.currentTimeMillis()
    }


    class TokenRepository : ITokenDataRepository<Int> {

        override suspend fun getAllNotExpired(): Map<Int, Long> {
            return mapOf(
                1 to now - 10 * 1000L,
                2 to now - TOKEN_TTL
            )
        }

        override fun insertOrUpdate(key: Int, revokeAt: Long): Boolean {
            return true
        }

        override fun cleanUp() {

        }

    }

    @Test
    fun testTtl() {
        val notifier = TokenNotifyBus<Int>()
        val service: IRevokeTokenService<Int> = RevokeTokenService(
            TokenRepository()
        ) {
            this.tokenTtl = TOKEN_TTL
            this.notifier = notifier
        }

        // await cache loading
        Thread.sleep(1000)

        assertTrue { service.checkIsActive(1, now) }
        assertTrue { service.checkIsActive(1, now - 1 * 1000L) }
        assertFalse { service.checkIsActive(1, now - TOKEN_TTL + 1000L) }
        assertFalse { service.checkIsActive(1, now - TOKEN_TTL - 1000L) }

        assertTrue { service.checkIsActive(2, now - TOKEN_TTL) }

        assertTrue { service.checkIsActive(3, now) }

        notifier.sendEvent(4, now)

        assertFalse { service.checkIsActive(4, now - 1) }
        assertTrue { service.checkIsActive(4, now + 1) }
    }
}
