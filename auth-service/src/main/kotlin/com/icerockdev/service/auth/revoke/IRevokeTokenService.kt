/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

interface IRevokeTokenService<TUserKey: Any> {
    fun checkIsActive(key: TUserKey, issuedAt: Long): Boolean
    fun putRevoked(key: TUserKey, revokeAt: Long): Boolean
    fun getAll(): Map<TUserKey, Long>
}
