/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */
import com.icerockdev.service.auth.revoke.IRevokeTokenService
import com.icerockdev.service.auth.revoke.ITokenDataRepository
import com.icerockdev.service.auth.revoke.RevokeTokenService
import com.icerockdev.service.auth.revoke.TokenNotifyBus
import com.icerockdev.service.auth.revoke.UserKey
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RevokeRoleTest {
    companion object {
        val TOKEN_TTL: Long = 3600 * 1000L
        val now = System.currentTimeMillis()
    }


    class TokenRepository : ITokenDataRepository<UserKey> {

        override suspend fun getAllNotExpired(): Map<UserKey, Long> {
            return mapOf(
                UserKey(1, 1) to now - 10 * 1000L,
                UserKey(2, 2) to now - TOKEN_TTL
            )
        }

        override fun insertOrUpdate(key: UserKey, revokeAt: Long): Boolean {
            return true
        }

        override fun cleanUp() {

        }
    }

    @Test
    fun testTtl() {
        val notifier = TokenNotifyBus<UserKey>()
        val service: IRevokeTokenService<UserKey> = RevokeTokenService(
            TokenRepository()
        ) {
            this.tokenTtl = TOKEN_TTL
            this.notifier = notifier
        }

        // await cache loading
        Thread.sleep(1000)

        assertTrue { service.checkIsActive(UserKey(1, 1), now) }
        assertTrue { service.checkIsActive(UserKey(1, 1),now - 1 * 1000L) }
        assertFalse { service.checkIsActive(UserKey(1, 1),now - TOKEN_TTL + 1000L) }
        assertFalse { service.checkIsActive(UserKey(1, 1),now - TOKEN_TTL - 1000L) }

        assertTrue { service.checkIsActive(UserKey(2, 2),now - TOKEN_TTL) }

        assertTrue { service.checkIsActive(UserKey(3, 2), now) }

        notifier.sendEvent(UserKey(4, 2), now)

        assertFalse { service.checkIsActive(UserKey(4, 2),now - 1) }
        assertTrue { service.checkIsActive(UserKey(4, 2), now + 1) }
    }
}
