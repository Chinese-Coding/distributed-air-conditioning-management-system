package cn.edu.bupt.master.controller

import cn.edu.bupt.master.TEST
import cn.edu.bupt.master.common.CheckLogin
import cn.edu.bupt.master.common.CheckWorkMode
import cn.edu.bupt.master.common.R
import cn.edu.bupt.master.entity.RequestDetail
import cn.edu.bupt.master.entity.Status
import cn.edu.bupt.master.service.MasterService
import cn.edu.bupt.master.service.RoomService
import cn.edu.bupt.master.service.SlaveStatusService
import cn.edu.bupt.master.service.UserService
import jakarta.annotation.Resource
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@Validated
@RestController
@RequestMapping(value = ["/api", ""])
class SlaveController {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Resource
    private lateinit var masterService: MasterService

    @Resource
    private lateinit var userService: UserService

    @Resource
    private lateinit var roomService: RoomService

    @Resource
    private lateinit var slaveStatusService: SlaveStatusService

    @Value("\${slave.defaultTemp}")
    private var defaultTemp: Int = 0

    /**
     * 从机登录请求
     *
     * @return 登录成功后返回从机默认工作温度, 否则返回错误信息
     */
    @CheckWorkMode
    @PostMapping("/login")
    fun login(@NotNull name: String, password: String?, @NotNull roomId: Long): R<Int> {
        logger.info("从机请求登录(roomId={}, name={}, password={})", roomId, name, password)
        val r = if (password == null)
            userService.login(name)
        else
            userService.login(name, password)

        if (r == null) {
            return R.error("用户名或密码错误")
        } else {
            val userId = r.id!!
            val room = roomService.findById(roomId)
            if (room == null)
                return R.error("房间不存在")

            if (userId != room.userId)
                return R.error("你不是这个房间的主人")

            if (room.inuse!!)
                return R.error("房间正在使用")

            var requestId = masterService.slaveLogin(roomId, userId)
            slaveStatusService.register(roomId)

            room.inuse = true
            roomService.updateById(room)
            logger.info("从机({})登录成功, 本次登录分配的 requestId 为: {}", roomId, requestId)
            return R.success(defaultTemp)
        }
    }

    /**
     * 从机关机请求
     */
    @CheckLogin
    @CheckWorkMode
    @PostMapping("/logout")
    fun logout(@NotNull roomId: Long): R<String> {
        val room = roomService.findById(roomId)
        if (room == null)
            return R.error("房间不存在")
        if (room.inuse == true) {
            room.inuse = false
            roomService.updateById(room)
            masterService.slaveLogout(roomId)
            slaveStatusService.unregister(roomId)
            logger.info("从机({})关机成功", roomId)
            return R.success("关机成功")
        }

        return R.error("关机失败")
    }

    /**
     * 接收从机请求, 从机给出的每个参数都不应为 null
     * <p>
     * TODO: 可以直接将参数改为 RequestData 类型，利用Kotlin数据类的验证特性校验参数
     */
    @CheckLogin
    @CheckWorkMode
    @PostMapping("/request")
    fun request(
        @NotNull roomId: Long, @NotNull setTemp: Int,
        @NotNull curTemp: Int, @NotBlank fanSpeed: String,
    ): R<String> {
        logger.info("从机请求参数: {}, {}, {}, {}", roomId, setTemp, curTemp, fanSpeed)

        val requestDetail = RequestDetail(stopTemp = setTemp, startTemp = curTemp, fanSpeed = fanSpeed)
        // 收到从机的新请求时, 记得也要更新一下从机能量和费用
        val energyAndFee = masterService.slaveRequest(roomId, requestDetail)
        return if (energyAndFee != null) {
            slaveStatusService.updateEnergyAndFee(roomId, energyAndFee[0], energyAndFee[1])
            R.success("添加请求成功")
        } else {
            R.error("添加请求失败")
        }
    }

    /**
     * 从机暂停送风
     */
    @CheckLogin
    @CheckWorkMode
    @DeleteMapping("/request")
    fun request(@NotNull roomId: Long): R<Boolean> {
        logger.info("从机请求关闭: {}", roomId)
        val energyAndFee = masterService.slavePowerOff(roomId)
        if (energyAndFee != null) {
            slaveStatusService.updateEnergyAndFee(roomId, energyAndFee[0], energyAndFee[1])
            return R.success(true)
        }
        return R.success(false)
    }

    /**
     * 检查从机 roomId 是否在可送风集合中
     *
     * @param roomId 从机的 roomId
     * @param setTemp 设定温度
     * @param curTemp 当前温度
     * @param fanSpeed 运行模式
     * @return 如果在返回 true, 如果不在返回 false
     */
    @CheckLogin
    @CheckWorkMode
    @PostMapping("/wind")
    fun wind(
        @NotNull roomId: Long, @NotNull setTemp: Int,
        @NotNull curTemp: Int, @NotBlank fanSpeed: String,
        @NotNull needWind: Boolean,
    ): R<Boolean> {
        slaveStatusService.updateSlaveStatus(roomId, curTemp, setTemp, Status.正常, fanSpeed)
        if (!needWind)
            return R.success(false)

        val r = masterService.contains(roomId)
        if (TEST)
            logger.info("收到来自从机的查询: {}, 查询结果: {}", roomId, r)

        return R.success(r)
    }

    /**
     * 获取当前从机请求费用
     */
    @CheckLogin
    @CheckWorkMode
    @GetMapping("/fee")
    fun slaveFee(@NotNull roomId: Long): R<List<BigDecimal>> {
        /*
         * 如果有历史和现在数据, 则将二者相加, 如果只有历史数据则返回历史数据,
         * 如果只有现在数据则返回现在数据, 如果两者都没有数据则返回 0
         */
        val current = masterService.getEnergyAndFee(roomId)
        val history = slaveStatusService.getEnergyAndFee(roomId)
        return when {
            current != null && history != null -> {
                val sumEnergy = current[0].add(history[0])
                val sumFee = current[1].add(history[1])
                R.success(listOf(sumEnergy, sumFee))
            }

            current != null -> R.success(current)
            history != null -> R.success(history)
            else -> R.success(listOf(BigDecimal.ZERO, BigDecimal.ZERO))
        }
    }
}