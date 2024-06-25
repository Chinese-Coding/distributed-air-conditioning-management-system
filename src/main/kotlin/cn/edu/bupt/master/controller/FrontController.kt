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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.annotation.Resource
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val str = "在进行此操作前首先会检查主机是否已经启动"

@Validated
@RestController
@RequestMapping(value = ["/api", ""])
@Tag(name = "前端控制器", description = "用于前端请求的控制器")
class FrontController {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Resource
    private lateinit var masterService: MasterService

    @Resource
    private lateinit var requestService: RequestService

    @Resource
    private lateinit var slaveStatusService: SlaveStatusService

    @GetMapping("/powerOn")
    @Operation(summary = "主机开机", description = str)
    fun powerOn(): R<String> {
        if (masterService.getWorkMode() == WorkMode.OFF) {
            masterService.powerOn()
            return R.success("启动成功")
        } else return R.error("主机已经启动了, 请勿重复启动")
    }

    @CheckWorkMode
    @GetMapping("/powerOff")
    @Operation(summary = "主机关机", description = str)
    fun powerOff(): R<String> {
        masterService.powerOff()
        return R.success("主机关机成功")
    }

    /**
     * 设置主机工作模式
     */
    @CheckWorkMode
    @PostMapping("/workMode")
    @Operation(
        summary = "设置主机工作模式",
        description = "参数: HEATING, REFRIGERATION; $str"
    )
    fun workMode(@NotBlank workMode: String): R<String> {
        logger.info("设置主机工作模式: {}", workMode)
        if (workMode == "HEATING")
            masterService.setWorkMode(WorkMode.HEATING)
        else if (workMode == "REFRIGERATION")
            masterService.setWorkMode(WorkMode.REFRIGERATION)
        else
            return R.error("参数错误")
        return R.success("设置成功")
    }

    @CheckWorkMode
    @PostMapping("/range")
    @Operation(
        summary = "设置主机工作的温度范围",
        description = "参数: firstValue (最低温度), secondValue (最高温度); $str"
    )
    fun range(@NotNull firstValue: Int, @NotNull secondValue: Int): R<String> {
        logger.info("主机工作的温度范围: {}, {}", firstValue * 100, secondValue * 100)
        val range = listOf(firstValue * 100, secondValue * 100)
        masterService.range = range
        return R.success("设置成功")
    }

    @GetMapping("/workMode")
    @Operation(summary = "获取主机工作模式")
    fun workMode() = R.success(masterService.getWorkMode())

    @CheckWorkMode
    @GetMapping("/workStatus")
    @Operation(summary = "获取主机工作状态和温度", description = "主机前端和从机后端共用此的函数")
    fun workStatus(): R<Any> {
        /**
         * 直接返回一个匿名类, 没有必要再单独为其声明一个类了
         */
        val r = object {
            val workMode = masterService.getWorkMode()
            val range = masterService.range
        }
        return R.success(r)
    }

    /**
     * 获取从机状态列表
     */
    @CheckWorkMode
    @GetMapping("/slaveStatus")
    @Operation(summary = "获取从机状态列表", description = "$str, 注意这里的费用和能量数据是历史数据, 而并非实时数据")
    fun slaveStatus(): R<List<SlaveStatus>> {
        val list = slaveStatusService.getSlaveStatusList()
        if (list.isEmpty())
            return R.error("没有从机状态可以获取")
        /**
         * 之前的代码: 循环遍历 `list`, 每次都需要调用 `masterService.contains()` 方法
         * 现在改为获取 `set` 集合, 然后判断 `set` 中是否包含 `roomId`
         */
        val set = masterService.getSendAirRoomId()
        list.forEach { slaveStatus ->
            slaveStatus.wind = if (set.contains(slaveStatus.roomId)) "送风" else "无风"
        }
        return R.success(list)
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