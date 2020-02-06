/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke.rolebased

import com.icerockdev.service.auth.revoke.RevokeAtDto

class RevokeAtDto(
    userId: Int,
    revokeAt: Long,
    val roleId: Int
) : RevokeAtDto(userId, revokeAt)
