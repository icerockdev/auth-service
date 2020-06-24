/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

import com.icerockdev.service.auth.cache.IMemoryCacheHook
import com.icerockdev.service.auth.cache.InMemoryCache

class RevokeTokenService<TUserKey : Any>(
    private val repository: ITokenDataRepository<TUserKey>,
    configure: Configuration<TUserKey>.() -> Unit = {}
) : IRevokeTokenService<TUserKey> {

    open class Configuration<T : Any> {
        /**
         * Time to token alive
         */
        var tokenTtl: Long = 3600 * 1000L
        var cacheCapacity: Int = 10000
        var notifier: TokenNotifyBus<T>? = null
    }

    private val cache: InMemoryCache<TUserKey, Long>

    init {
        val configuration =
            Configuration<TUserKey>()
        configuration.configure()

        cache = InMemoryCache(
            capacity = configuration.cacheCapacity,
            hook = object : IMemoryCacheHook<TUserKey, Long> {
                override suspend fun loader(): Map<TUserKey, Long> {
                    return repository.getAllNotExpired()
                }

                override fun isExpired(now: Long, key: TUserKey, value: Long): Boolean {
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

    override fun checkIsActive(key: TUserKey, issuedAt: Long): Boolean {
        val revokeAt = cache.get(key)
        if (revokeAt === null) {
            return true
        }

        return revokeAt <= issuedAt
    }

    override fun putRevoked(key: TUserKey, revokeAt: Long): Boolean {
        if (cache.put(key, revokeAt)) {
            return repository.insertOrUpdate(key, revokeAt)
        }
        return false
    }

    override fun getAll(): Map<TUserKey, Long> {
        return cache.getMap()
    }
}
