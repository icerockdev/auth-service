/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */
import com.icerockdev.service.auth.revoke.*
import com.icerockdev.service.auth.revoke.rolebased.IRevokeTokenService
import com.icerockdev.service.auth.revoke.rolebased.RevokeTokenService
import com.icerockdev.service.auth.revoke.rolebased.RevokeAtDto
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RevokeRoleTest {
    companion object {
        val TOKEN_TTL: Long = 3600 * 1000L
        val now = System.currentTimeMillis()
    }


    class TokenRepository : ITokenDataRepository<RevokeAtDto> {


        override suspend fun getAllNotExpired(): Map<Int, RevokeAtDto> {
            return mapOf(
                1 to RevokeAtDto(1, now - 10 * 1000L, 1),
                2 to RevokeAtDto(2, now - TOKEN_TTL, 2)
            )
        }

        override fun insertOrUpdate(value: RevokeAtDto): Boolean {
            return true
        }

        override fun cleanUp() {

        }

    }

    @Test
    fun testTtl() {
        val notifier = TokenNotifyBus<RevokeAtDto>()
        val service: IRevokeTokenService = RevokeTokenService(
            TokenRepository()
        ) {
            this.tokenTtl = TOKEN_TTL
            this.notifier = notifier
        }

        // await cache loading
        Thread.sleep(1000)

        assertTrue { service.checkIsActive(1, 1, now) }
        assertTrue { service.checkIsActive(1, 1,now - 1 * 1000L) }
        assertFalse { service.checkIsActive(1, 1,now - TOKEN_TTL + 1000L) }
        assertFalse { service.checkIsActive(1, 1,now - TOKEN_TTL - 1000L) }

        assertTrue { service.checkIsActive(2, 2,now - TOKEN_TTL) }

        assertTrue { service.checkIsActive(3, 2, now) }

        notifier.sendEvent(RevokeAtDto(4, now, 2))

        assertFalse { service.checkIsActive(4, 2,now - 1) }
        assertTrue { service.checkIsActive(4, 2, now + 1) }
    }
}
