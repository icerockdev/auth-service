/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke.rolebased

interface IRevokeTokenService {
    fun checkIsActive(userId: Int, roleId: Int, issuedAt: Long): Boolean
    fun putRevoked(userId: Int, roleId: Int, revokeAt: Long): Boolean
    fun getAll(): List<RevokeAtDto>
}
