package cn.edu.bupt.master.entity

enum class WorkMode {
    HEATING,
    REFRIGERATION,
    OFF
}

enum class Period {
    MONTH,
    WEEK,
    DAY
}

/**
 * 从机与主机的连接状态
 */
enum class Status {
    正常,
    离线,
    关机
}