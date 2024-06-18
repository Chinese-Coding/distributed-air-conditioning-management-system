package cn.edu.bupt.master.entity

import java.math.BigDecimal
import java.time.LocalDateTime

data class SlaveStatus(
    var roomId: Long? = null,

    var curTemp: Int? = null,

    var setTemp: Int? = null,

    var status: Status? = null,

    var fanSpeed: String? = null,

    var wind: String? = null,

    var energy: BigDecimal = BigDecimal.ZERO,

    var fee: BigDecimal = BigDecimal.ZERO,

    var registeredTime: LocalDateTime? = null,
) {
    fun zero() {
        this.energy = BigDecimal.ZERO
        this.fee = BigDecimal.ZERO
    }
}