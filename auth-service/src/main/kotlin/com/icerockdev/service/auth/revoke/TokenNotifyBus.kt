/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

class TokenNotifyBus<T: RevokeAtDto> {
    private val subscribeList = ArrayList<(T) -> Boolean>()

    fun subscribe(block: (T) -> Boolean) {
        subscribeList.add(block)
    }

    fun sendEvent(value: T) {
        subscribeList.forEach { block ->
            block(value)
        }
    }
}
