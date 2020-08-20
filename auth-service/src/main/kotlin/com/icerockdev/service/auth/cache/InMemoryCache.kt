/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

internal class InMemoryCache<K : Any, V : Any>(
    private val hook: IMemoryCacheHook<K, V>,
    private val capacity: Int = 1000,
    private val cleanUpIntervalMillis: Long = 3600L
): AutoCloseable {
    private val cache = ConcurrentHashMap<K, V>(capacity, 1f)
    private val logger = LoggerFactory.getLogger(InMemoryCache::class.java)

    private val loadContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
    private val loadScope: CoroutineScope = CoroutineScope(loadContext)
    private var lastCleanUp: Long = 0L

    @Volatile
    private var _cleanUpJob: Job? = null

    var isLoaded: Boolean = false
        private set

    init {
        loadScope.launch {
            loadAll()
        }
    }

    private suspend fun loadAll() {
        val data = hook.loader()
        cache.putAll(data)
        isLoaded = true
    }

    @Throws(CacheUnavailableException::class)
    fun get(key: K): V? {
        if (!isLoaded) {
            throw CacheUnavailableException()
        }

        val value = cache[key]
        if (value === null) {
            return null
        }

        val now = System.currentTimeMillis()
        if (hook.isExpired(now, key, value)) {
            remove(key)
            return null
        }

        return value
    }

    @Throws(CacheUnavailableException::class)
    fun getAll(): List<V> {
        if (!isLoaded) {
            throw CacheUnavailableException()
        }
        val now = System.currentTimeMillis()
        return cache.filter { !hook.isExpired(now, it.key, it.value) }.values.toList()
    }

    @Throws(CacheUnavailableException::class)
    fun getMap(): Map<K, V> {
        if (!isLoaded) {
            throw CacheUnavailableException()
        }
        val now = System.currentTimeMillis()
        return cache.filter { !hook.isExpired(now, it.key, it.value) }.toMap()
    }

    @Throws(CacheUnavailableException::class)
    fun remove(key: K): V? {
        if (!isLoaded) {
            throw CacheUnavailableException()
        }
        return cache.remove(key)
    }

    /**
     * Put value with hook execution
     */
    @Throws(CacheUnavailableException::class)
    fun put(key: K, value: V): Boolean {
        if (!isLoaded) {
            throw CacheUnavailableException()
        }
        cache[key] = value
        autoCleanUp()
        return true
    }

    private fun autoCleanUp() {
        var job = _cleanUpJob
        if (job != null && job.isActive) {
            return
        }

        val now = System.currentTimeMillis()
        if (cache.size > capacity && now > lastCleanUp + cleanUpIntervalMillis) {
            return
        }

        if (now > lastCleanUp + cleanUpIntervalMillis) {
            return
        }

        job = loadScope.launch {
            cleanUp()
            lastCleanUp = System.currentTimeMillis()
        }
        _cleanUpJob = job
    }

    /**
     * CleanUp collection with hook execution (if exists)
     */
    private fun cleanUp() {
        logger.info("Cache cleanup started")
        val now = System.currentTimeMillis()
        for (entry in cache.entries) {
            if (hook.isExpired(now, entry.key, entry.value)) {
                remove(entry.key)
            }
        }
        hook.cleanUpCallback()
    }

    override fun close() {
        loadScope.cancel()
    }
}

interface IMemoryCacheHook<K : Any, V : Any> {
    suspend fun loader(): Map<K, V>
    fun isExpired(now: Long, key: K, value: V): Boolean
    fun cleanUpCallback() {}
}