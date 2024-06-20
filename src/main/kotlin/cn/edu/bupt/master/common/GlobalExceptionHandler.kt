package cn.edu.bupt.master.common

import org.hibernate.exception.ConstraintViolationException
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@ResponseBody
@ControllerAdvice(annotations = [RestController::class, Controller::class])
class GlobalExceptionHandler {

    /**
     * 请求时主机未开机, 状态错误
     */
    @ExceptionHandler(IllegalStateException::class)
    fun exceptionHandler(e: IllegalStateException): R<String> {
        var message = e.message
        return if (message!!.contains("主机未启动"))
            R.error("主机未启动")
        else
            R.error(message)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun exceptionHandler(e: IllegalArgumentException): R<String> {
        var message = e.message
        return R.error(message!!)
    }

    /**
     * 参数错误
     * <p>
     * TODO: 优化返回的错误信息, 修改成不携带后端函数名
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun exceptionHandler(e: ConstraintViolationException): R<String> {
        var message = e.message
        return R.error(message!!)
    }
}