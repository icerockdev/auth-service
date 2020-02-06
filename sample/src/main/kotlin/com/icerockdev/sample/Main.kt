package com.icerockdev.sample

import com.icerockdev.sample.rolebased.RoleBased
import com.icerockdev.sample.simple.Simple

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        RoleBased.main(args)
        Simple.main(args)
    }
}
