package com.connort6.expensemonitor.model.entity

import java.math.BigDecimal

data class Account(
    var id: String? = null,
    val name: String,
    var balance: BigDecimal
)
