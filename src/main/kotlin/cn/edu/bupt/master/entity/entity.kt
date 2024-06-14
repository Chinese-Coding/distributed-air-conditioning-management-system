package cn.edu.bupt.master.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 从机每次启动都创建一个启动请求记录
 */
@Entity
data class Request(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var roomId: Long? = null,

    var userId: Long? = null,

    var startTime: LocalDateTime? = null,

    var stopTime: LocalDateTime? = null,

    var normalExit: Boolean? = null
)

/**
 * 从机请求的详细记录, 记录每次从机启动后与主机的交互的数据
 */
@Entity
data class RequestDetail(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var requestId: Long? = null,

    var startTime: LocalDateTime? = null,

    var stopTime: LocalDateTime? = null,

    var startTemp: Int? = null,

    var stopTemp: Int? = null,

    var fanSpeed: String? = null,

    var totalFee: BigDecimal? = null,
)

@Entity
data class Room(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var userId: Long? = null,

    var inuse: Boolean? = null
)

@Entity(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String? = null,

    var password: String? = null
)

