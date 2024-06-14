package cn.edu.bupt.master.common


import cn.edu.bupt.master.entity.WorkMode
import cn.edu.bupt.master.service.MasterService
import cn.edu.bupt.master.service.SlaveStatusService
import jakarta.annotation.Resource
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.springframework.stereotype.Component

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CheckLogin

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CheckWorkMode

/**
 * 从机发送请求时, 检查从机是否登录
 */
@Aspect
@Component
class LoginAspect {
    @Resource
    private lateinit var slaveStatusService: SlaveStatusService

    @Pointcut("@annotation(cn.edu.bupt.master.common.CheckLogin)")
    fun cut() = Unit

    @Before("cut()")
    fun checkLogin(joinPoint: JoinPoint) {
        var args = joinPoint.args
        var roomId = args[0] as Long?
        if (roomId == null)
            throw IllegalArgumentException("请求参数错误, roomId 为 null")
        if (!slaveStatusService.isRegistered(roomId))
            throw IllegalStateException("未登录")
    }
}

@Aspect
@Component
class WorkModeAspect {
    @Resource
    private lateinit var masterService: MasterService

    @Pointcut("@annotation(cn.edu.bupt.master.common.CheckWorkMode)")
    fun cut() = Unit

    @Before("cut()")
    fun checkWorkMode() {
        if (masterService.checkWorkMode(WorkMode.OFF))
            throw IllegalStateException("主机未启动")
    }
}
