package cn.edu.bupt.slave

import cn.edu.bupt.slave.service.init
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
private class SlaveApplication


fun main(args: Array<String>) {
    init()
    runApplication<SlaveApplication>(*args)
}
