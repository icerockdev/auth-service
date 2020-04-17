/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

class TokenNotifyBus<TUserKey: Any> {
    private val subscribeList = ArrayList<(TUserKey, Long) -> Boolean>()

    fun subscribe(block: (key: TUserKey, expiredAt: Long) -> Boolean) {
        subscribeList.add(block)
    }

    fun sendEvent(key: TUserKey, expiredAt: Long) {
        subscribeList.forEach { block ->
            block(key, expiredAt)
        }
    }
}
