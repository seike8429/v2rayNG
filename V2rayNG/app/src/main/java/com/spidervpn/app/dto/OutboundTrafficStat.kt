package com.spidervpn.app.dto

data class OutboundTrafficStat(
    val tag: String,
    val direction: String,
    val value: Long,
)