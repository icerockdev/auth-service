/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

interface IRevokeTokenService {
    fun checkIsActive(userId: Int, issuedAt: Long): Boolean
    fun putRevoked(userId: Int, revokeAt: Long): Boolean
    fun getAll(): List<RevokeAtDto>
}