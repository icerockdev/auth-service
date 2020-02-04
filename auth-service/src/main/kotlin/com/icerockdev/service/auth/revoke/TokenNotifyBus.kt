/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.auth.revoke

class TokenNotifyBus {
    private val subscribeList = ArrayList<(RevokeAtDto) -> Boolean>()

    fun subscribe(block: (RevokeAtDto) -> Boolean) {
        subscribeList.add(block)
    }

    fun sendEvent(value: RevokeAtDto) {
        subscribeList.forEach { block ->
            block(value)
        }
    }
}