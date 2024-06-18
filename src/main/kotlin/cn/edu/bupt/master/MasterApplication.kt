package cn.edu.bupt.master

import cn.edu.bupt.master.service.init
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@EnableAspectJAutoProxy
private class MasterApplication

const val TEST = false

fun main(args: Array<String>) {
    init()
    runApplication<MasterApplication>(*args)
}
