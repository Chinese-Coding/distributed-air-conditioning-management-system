package cn.edu.bupt.slave.common

import cn.edu.bupt.slave.service.SlaveService
import cn.edu.bupt.slave.service.Status
import jakarta.annotation.Resource
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Check


@Aspect
@Component
class CheckAspect {
    @Resource
    private lateinit var slaveService: SlaveService

    @Pointcut("@annotation(cn.edu.bupt.slave.common.Check)")
    fun cut() = Unit

    @Before("cut()")
    fun check(joinPoint: JoinPoint) {
        if (slaveService.ROOM_ID == null)
            throw IllegalArgumentException("请求参数错误, 当前主机尚未获取到 ROOM_ID")
        if (slaveService.getStatus() == Status.OFF)
            throw IllegalStateException("空调尚未启动")
    }
}

@ResponseBody
@ControllerAdvice(annotations = [RestController::class, Controller::class])
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalStateException::class)
    fun exceptionHandler(e: IllegalStateException): R<String> {
        var message = e.message
        return R.error(message!!)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun exceptionHandler(e: IllegalArgumentException): R<String> {
        var message = e.message
        return R.error(message!!)
    }
}