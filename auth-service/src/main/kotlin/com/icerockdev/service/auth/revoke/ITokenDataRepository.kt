/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

interface ITokenDataRepository<T: RevokeAtDto> {
    suspend fun getAllNotExpired(): Map<Int, T>
    fun insertOrUpdate(value: T): Boolean
    fun cleanUp()
}
