/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

interface IRevokeTokenService<T: Any> {
    fun checkIsActive(key: T, issuedAt: Long): Boolean
    fun putRevoked(key: T, revokeAt: Long): Boolean
    fun getAll(): List<Long>
}
