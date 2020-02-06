/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

import com.icerockdev.service.auth.cache.IMemoryCacheHook
import com.icerockdev.service.auth.cache.InMemoryCache

data class UserKey (
    val userId: Int,
    val roleId: Int
)

class RevokeTokenService<T : Any>(
    private val repository: ITokenDataRepository<T>,
    configure: Configuration<T>.() -> Unit = {}
) : IRevokeTokenService<T> {

    open class Configuration<T : Any> {
        /**
         * Time to token alive
         */
        var tokenTtl: Long = 3600 * 1000L
        var cacheCapacity: Int = 10000
        var notifier: TokenNotifyBus<T>? = null
    }

    private val cache: InMemoryCache<T, Long>

    init {
        val configuration =
            Configuration<T>()
        configuration.configure()

        cache = InMemoryCache(
            capacity = configuration.cacheCapacity,
            hook = object : IMemoryCacheHook<T, Long> {
                override suspend fun loader(): Map<T, Long> {
                    return repository.getAllNotExpired()
                }

                override fun isExpired(now: Long, key: T, value: Long): Boolean {
                    return value < now - configuration.tokenTtl
                }

                override fun cleanUpCallback() {
                    repository.cleanUp()
                }
            }
        )

        val notifier = configuration.notifier
        if (notifier !== null) {
            notifier.subscribe { key, revokeAt ->
                return@subscribe cache.put(key, revokeAt)
            }
        }
    }

    override fun checkIsActive(key: T, issuedAt: Long): Boolean {
        val revokeAt = cache.get(key)
        if (revokeAt === null) {
            return true
        }

        return revokeAt <= issuedAt
    }

    override fun putRevoked(key: T, revokeAt: Long): Boolean {
        if (cache.put(key, revokeAt)) {
            return repository.insertOrUpdate(key, revokeAt)
        }
        return false
    }

    override fun getAll(): List<Long> {
        return cache.getAll()
    }
}
