/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke.rolebased

import com.icerockdev.service.auth.cache.IMemoryCacheHook
import com.icerockdev.service.auth.cache.InMemoryCache
import com.icerockdev.service.auth.revoke.ITokenDataRepository
import com.icerockdev.service.auth.revoke.TokenNotifyBus

data class UserKey (
    val userId: Int,
    val roleId: Int
)

class RevokeTokenService(
    private val repository: ITokenDataRepository<UserKey, RevokeAtDto>,
    configure: Configuration.() -> Unit = {}
) : IRevokeTokenService {

    open class Configuration {
        /**
         * Time to token alive
         */
        var tokenTtl: Long = 3600 * 1000L
        var cacheCapacity: Int = 10000
        var notifier: TokenNotifyBus<RevokeAtDto>? = null
    }

    private val cache: InMemoryCache<UserKey, RevokeAtDto>

    init {
        val configuration = Configuration()
        configuration.configure()

        cache = InMemoryCache(
            capacity = configuration.cacheCapacity,
            hook = object : IMemoryCacheHook<UserKey, RevokeAtDto> {
                override suspend fun loader(): Map<UserKey, RevokeAtDto> {
                    return repository.getAllNotExpired()
                }

                override fun isExpired(now: Long, key: UserKey, value: RevokeAtDto): Boolean {
                    return value.revokeAt < now - configuration.tokenTtl
                }

                override fun cleanUpCallback() {
                    repository.cleanUp()
                }
            }
        )

        val notifier = configuration.notifier
        if (notifier !== null) {
            notifier.subscribe { dto ->
                return@subscribe cache.put(UserKey(dto.userId, dto.roleId), dto)
            }
        }
    }

    override fun checkIsActive(userId: Int, roleId: Int, issuedAt: Long): Boolean {
        val blocked = cache.get(UserKey(userId, roleId))
        if (blocked === null) {
            return true
        }

        return blocked.revokeAt <= issuedAt
    }

    override fun putRevoked(userId: Int, roleId: Int, revokeAt: Long): Boolean {
        val dto = RevokeAtDto(userId, revokeAt, roleId)
        if (cache.put(UserKey(userId, roleId), dto)) {
            return repository.insertOrUpdate(dto)
        }
        return false
    }

    override fun getAll(): List<RevokeAtDto> {
        return cache.getAll()
    }
}
