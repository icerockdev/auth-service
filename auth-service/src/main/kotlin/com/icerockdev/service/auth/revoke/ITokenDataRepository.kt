/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

interface ITokenDataRepository<T: Any> {
    suspend fun getAllNotExpired(): Map<T, Long>
    fun insertOrUpdate(key: T, revokeAt: Long): Boolean
    fun cleanUp()
}
