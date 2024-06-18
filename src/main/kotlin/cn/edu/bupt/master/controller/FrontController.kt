package cn.edu.bupt.master.controller


import cn.edu.bupt.master.common.CheckWorkMode
import cn.edu.bupt.master.common.R
import cn.edu.bupt.master.entity.Period
import cn.edu.bupt.master.entity.Request
import cn.edu.bupt.master.entity.SlaveStatus
import cn.edu.bupt.master.entity.WorkMode
import cn.edu.bupt.master.service.MasterService
import cn.edu.bupt.master.service.RequestService
import cn.edu.bupt.master.service.SlaveStatusService
import jakarta.annotation.Resource
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping(value = ["/api", ""])
class FrontController {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Resource
    private lateinit var masterService: MasterService

    @Resource
    private lateinit var requestService: RequestService

    @Resource
    private lateinit var slaveStatusService: SlaveStatusService

    /**
     * 前端-主机开机
     */
    @GetMapping("/powerOn")
    fun powerOn(): R<String> {
        masterService.powerOn()
        return R.success("启动成功")
    }

    /**
     * 前端-主机关机
     */
    @GetMapping("/powerOff")
    fun powerOff(): R<String> {
        masterService.powerOff()
        return R.success("启动成功")
    }

    @CheckWorkMode
    @PostMapping("/workMode")
    fun workMode(@NotBlank workMode: String): R<String> {
        logger.info("设置主机工作模式: {}", workMode)
        masterService
        if (workMode == "HEATING")
            masterService.workMode = WorkMode.HEATING
        else if (workMode == "REFRIGERATION")
            masterService.workMode = WorkMode.REFRIGERATION
        else
            return R.error("参数错误")
        return R.success("设置成功")
    }

    @CheckWorkMode
    @PostMapping("/range")
    fun range(@NotNull firstValue: Int, @NotNull secondValue: Int): R<String> {
        logger.info("主机工作的温度范围: {}, {}", firstValue * 100, secondValue * 100)
        val range = listOf(firstValue * 100, secondValue * 100)
        masterService.range = range
        return R.success("设置成功")
    }

    /**
     * 获取主机工作状态
     */
    @GetMapping("/workMode")
    fun workMode() = R.success(masterService.workMode.toString())


    /**
     * 获取主机工作状态和温度, 主机前端和从机后端共用的函数
     */
    @CheckWorkMode
    @GetMapping("/workStatus")
    fun workStatus(): R<Any> {
        val r = object {
            val workMode = masterService.workMode
            val range = masterService.range
        }
        return R.success(r)
    }

    /**
     * 获取从机状态列表
     */
    @CheckWorkMode
    @GetMapping("/slaveStatus")
    fun slaveStatus(): R<List<SlaveStatus>> {
        val list = slaveStatusService.getSlaveStatusList()
        return if (list.isNotEmpty()) {
            list.forEach { slaveStatus ->
                if (masterService.contains(slaveStatus.roomId!!))
                    slaveStatus.wind = "送风"
                else
                    slaveStatus.wind = "无风"
            }
            R.success(list)
        } else
            R.error("没有从机状态可以获取")
    }

    /**
     * 获取房间报表
     */
    @CheckWorkMode
    @GetMapping("/roomTable")
    fun roomTable(@NotNull roomId: Long): R<List<Request>> {
        val list = requestService.findAllByRoomId(roomId)
        return if (list.isNotEmpty()) R.success(list) else R.error("没有该房间报表！")
    }

    /**
     * 按年月日获取报表
     */
    @CheckWorkMode
    @GetMapping("/table")
    fun table(@NotNull period: String): R<out Any> {
        return try {
            if (period.isEmpty()) {
                R.error("参数错误")
            } else {
                val list = requestService.findAllByPeriod(Period.valueOf(period))
                if (list.isNotEmpty()) R.success(list) else R.error("没有报表！")
            }
        } catch (_: IllegalArgumentException) {
            R.error("无效的时间段参数")
        }
    }
}