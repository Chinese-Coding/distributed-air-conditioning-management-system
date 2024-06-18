package cn.edu.bupt.slave.controller

import cn.edu.bupt.slave.common.Check
import cn.edu.bupt.slave.common.R
import cn.edu.bupt.slave.service.BASE_URL
import cn.edu.bupt.slave.service.FanSpeed
import cn.edu.bupt.slave.service.SlaveService
import cn.edu.bupt.slave.service.getRequestEntity
import jakarta.annotation.Resource
import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal

@RestController
class FrontController {
    @Resource
    private lateinit var slaveService: SlaveService

    @Resource
    private lateinit var restTemplate: RestTemplate

    /**
     * 获取从前运行的全部信息 (仅供调试使用)
     */
    @GetMapping("/info")
    fun info(): R<Any?> {
        var r = R<Any?>()
        with(r) {
            add("ROOM_ID", slaveService.ROOM_ID.toString())
            add("status", slaveService.getStatus())
            add("curTemp", slaveService.getCurTemp())
            add("setTemp", slaveService.getSetTemp())
            add("fanSpeed", slaveService.getFanSpeed())
            add("wind", slaveService.getWind())
        }
        return r
    }

    /**
     * 获取从机状态
     */
    @GetMapping("/status")
    fun status() = R.success(slaveService.getStatus())

    /**
     * 从机开机
     */
    @PostMapping("/powerOn")
    fun powerOn() = R.success(slaveService.powerOn())


    @PostMapping("/powerOff")
    fun powerOff() = R.success(slaveService.powerOff())

    @Check
    @PostMapping("/setTemp")
    fun setTemp(action: Boolean): R<Int> {
        return if (slaveService.setSetTemp(action))
            R.success(slaveService.getSetTemp())
        else
            R.error("发生错误, 无法升高 (降低) 设定温度")
    }

    @Check
    @GetMapping("/curTemp")
    fun curTemp() = R.success(slaveService.getCurTemp())

    @Check
    @GetMapping
    fun speed() = R.success(slaveService.getFanSpeed())

    /**
     * TODO: 如果用户输入的 FanSpeed 不正确怎么办
     */
    @Check
    @PostMapping("/speed")
    fun speed(fanSpeed: FanSpeed): R<FanSpeed> {
        if (slaveService.setFanSpeed(fanSpeed))
            return R.success(slaveService.getFanSpeed())
        else
            return R.error("发生错误, 无法设置风速")
    }

    @Check
    @GetMapping("/fee")
    fun fee(): R<BigDecimal> {
        var requestEntity = getRequestEntity("roomId" to slaveService.ROOM_ID)
        var r = restTemplate.exchange<R<*>>(
            "${BASE_URL}/fee",
            HttpMethod.GET,
            requestEntity,
            R::class.java
        ).body
        return if (r == null)
            R.error("发生错误, 无法获取当前从机费用")
        else if (r.code == 0)
            R.error("发生错误, 无法获取当前从机费用, 主机错误信息: ${r.msg}")
        else if (r.code == 1)
            R.success(r.data!! as BigDecimal)
        else
            R.error("发生错误, 无法获取当前从机费用, 主机返回状态码错误")
    }


    @GetMapping("/masterStatus")
    fun masterStatus(): R<Any> {
        var r = restTemplate.getForEntity<R<*>>(
            "${BASE_URL}/workStatus",
            R::class.java
        ).body
        return if (r == null)
            R.error("发生错误, 无法获取主机状态")
        else if (r.code == 0)
            R.error("发生错误, 无法获取主机状态, 主机错误信息: ${r.msg}")
        else if (r.code == 1)
            R.success(r.data!!)
        else
            R.error("发生错误, 无法获取主机状态, 主机返回状态码错误")
    }

    @Check
    @GetMapping("/wind")
    fun wind() = R.success(slaveService.getWind())
}