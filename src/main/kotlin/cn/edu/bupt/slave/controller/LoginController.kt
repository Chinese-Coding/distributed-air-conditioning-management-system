package cn.edu.bupt.slave.controller

import cn.edu.bupt.slave.common.R
import cn.edu.bupt.slave.service.BASE_URL
import cn.edu.bupt.slave.service.SlaveService
import cn.edu.bupt.slave.service.Status
import cn.edu.bupt.slave.service.getRequestEntity
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

@RestController
class LoginController {
    @Resource
    private lateinit var slaveService: SlaveService

    @Resource
    private lateinit var restTemplate: RestTemplate

    /**
     * 登录
     */
    @PostMapping("/login")
    fun login(roomId: Long, name: String, password: String): R<String> {
        if (slaveService.ROOM_ID != null)
            return R.error("发生错误, 从机已登录")
        var responseRequestEntity =
            getRequestEntity("roomId" to roomId, "name" to name, "password" to password)
        var r = restTemplate.postForEntity<R<*>>(
            "${BASE_URL}/login",
            responseRequestEntity,
            R::class.java
        ).body!!
        if (r.code == 1) {
            slaveService.login(roomId, r.data as Int)
            return R.success("登录成功")
        } else
            return R.error("发生错误, 登录失败, 主机错误信息: ${r.msg}")
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    fun logout(roomId: Long): R<String> {
        if (slaveService.getStatus() == Status.ON)
            return R.error("发生错误, 从机处于开机状态, 请先关机")
        var responseRequestEntity =
            getRequestEntity("roomId" to roomId)
        var r = restTemplate.postForEntity<R<*>>(
            "${BASE_URL}/logout",
            responseRequestEntity,
            R::class.java
        )
        if (r.body!!.code == 1) {
            slaveService.logout()
            return R.success("登出成功")
        } else return R.error("失败")

    }
}