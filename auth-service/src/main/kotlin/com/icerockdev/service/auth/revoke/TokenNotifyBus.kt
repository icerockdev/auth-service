/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

class TokenNotifyBus<T: Any> {
    private val subscribeList = ArrayList<(T, Long) -> Boolean>()

    fun subscribe(block: (key: T, Long) -> Boolean) {
        subscribeList.add(block)
    }

    fun sendEvent(key: T, value: Long) {
        subscribeList.forEach { block ->
            block(key, value)
        }
    }
}
