package cn.edu.bupt.master.service

import cn.edu.bupt.master.entity.Request
import cn.edu.bupt.master.entity.RequestDetail
import cn.edu.bupt.master.entity.WorkMode
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.boot.json.JsonParseException
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.IOException
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.system.exitProcess

private lateinit var HEATING_TEMP: List<Int>
private lateinit var REFRIGERATION_TEMP: List<Int>
private lateinit var FAX_COST: Map<String, Double>
private var EXPIRY_DURATION: Int = 0

fun init() {
    data class BasicData(
        val HEATING_TEMP: List<Int>,
        val REFRIGERATION_TEMP: List<Int>,
        val FAX_COST: Map<String, Double>,
        val EXPIRY_DURATION: Int,
    )

    // 读取 JSON 文件
    val mapper = jacksonObjectMapper()
    val resource = ClassPathResource("config.json")
    val basicData = try {
        resource.inputStream.use {
            mapper.readValue(it, BasicData::class.java)
        }
    } catch (e: JsonParseException) {
        System.err.println("JSON 解析错误: ${e.message}")
        exitProcess(0)
    } catch (e: JsonMappingException) {
        System.err.println("JSON 映射错误: ${e.message}")
        exitProcess(0)
    } catch (e: IOException) {
        System.err.println("文件读取错误: ${e.message}")
        exitProcess(0)
    }
    HEATING_TEMP = basicData.HEATING_TEMP
    REFRIGERATION_TEMP = basicData.REFRIGERATION_TEMP
    FAX_COST = basicData.FAX_COST
    EXPIRY_DURATION = basicData.EXPIRY_DURATION
    println("配置参数: HEATING_TEMP: $HEATING_TEMP\tREFRIGERATION_TEMP: $REFRIGERATION_TEMP\tFAX_COST: $FAX_COST\tEXPIRY_DURATION: $EXPIRY_DURATION")
}

@Service
class MasterService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Resource
    private lateinit var requestService: RequestService

    @Resource
    private lateinit var requestDetailRepository: RequestDetailRepository

    // TODO: 如何只让其变为只有 setter 而没有 getter
    var workMode = WorkMode.OFF
        /**
         * 设置主机的工作模式, 设置工作模式的同时也要修改主机的工作温度
         */
        set(value) {
            field = value
            when (value) {
                WorkMode.HEATING -> this.range = HEATING_TEMP
                WorkMode.REFRIGERATION -> this.range = REFRIGERATION_TEMP
                WorkMode.OFF -> {}
            }
        }

    lateinit var range: List<Int>

    private lateinit var requestList: MutableList<Request>

    private lateinit var requestDetailMap: MutableMap<Long, RequestDetail>

    private lateinit var sendAirRoomId: MutableSet<Long>

    private val lock = ReentrantReadWriteLock()

    fun checkWorkMode(workMode: WorkMode) = this.workMode == workMode

    /**
     * 主机开机
     *
     * 因为此时没有其他线程的访问, 所以不加锁了
     */
    fun powerOn() {
        lock.readLock().lock()
        try {
            if (workMode != WorkMode.OFF)
                return
        } finally {
            lock.readLock().unlock()
        }
        lock.writeLock().lock()
        try {
            this.workMode = WorkMode.REFRIGERATION
            this.range = REFRIGERATION_TEMP

            this.requestList = LinkedList()
            this.requestDetailMap = HashMap()
            this.sendAirRoomId = HashSet()
            logger.info("主机启动成功! 主机工作参数: {}, {}, {}", workMode, range, FAX_COST)
        } finally {
            lock.writeLock().unlock()
        }

    }

    fun powerOff() {
        lock.writeLock().lock()
        logger.info("获得锁")
        try {
            this.workMode = WorkMode.OFF
            // 主机关机, 清空所有请求
            for (request in requestList) {
                // 找出对应的详细请求, 将其结束并存入数据库
                var requestId = request.id!!
                var requestDetail = requestDetailMap[requestId]
                if (requestDetail != null)
                    calcFeeAndSave(requestDetail)

                with(request) {
                    this.stopTime = LocalDateTime.now()
                    this.normalExit = false
                }
                requestService.save(request)
            }
            // 其实加不加都无所谓
            requestList.clear()
            requestDetailMap.clear()
            sendAirRoomId.clear()
            logger.info("主机关闭成功!")
        } finally {
            lock.writeLock().unlock()
        }
    }

    private fun checkRequestTemp(requestDetail: RequestDetail): Boolean {
        var startTemp = requestDetail.startTemp
        var stopTemp = requestDetail.stopTemp

        // 检查设定温度是否在范围内
        if (stopTemp!! > range[1] || stopTemp < range[0])
            return false
        // 根据工作模式比较起始温度和停止温度
        return (workMode == WorkMode.HEATING && startTemp!! < stopTemp) ||
                (workMode == WorkMode.REFRIGERATION && startTemp!! > stopTemp)
    }


    /**
     * 计算一个请求的总花费并写入数据库
     *
     * @param requestDetail 请求
     * @return 返回一个包含 energy, fee 的 list
     */
    private fun calcFeeAndSave(requestDetail: RequestDetail): List<BigDecimal> {
        val seconds = Duration.between(requestDetail.startTime, requestDetail.stopTime).seconds
        val minutes = (seconds / 60) + if (seconds % 60 > 0) 1 else 0
        val energy = BigDecimal(minutes) * BigDecimal(FAX_COST[requestDetail.fanSpeed]!!)
        val fee = energy * BigDecimal(5)
        requestDetail.totalFee = fee
        requestDetailRepository.save(requestDetail)
        return listOf(energy, fee)
    }


    /**
     * 从机登录, 为其分配一个 requestId
     */
    fun slaveLogin(roomId: Long, userId: Long): Long {
        val request = Request(roomId = roomId, userId = userId, startTime = LocalDateTime.now())
        // 先存入数据库, `id` 回填后, 再存入 `requestList`
        requestService.save(request)
        lock.writeLock().lock()
        try {
            requestList.add(request)
        } finally {
            lock.writeLock().unlock()
        }
        return request.id!!
    }

    /**
     * 接收到来自从机的请求
     * <p>
     * 无论收到的请求是开机, 还是调整工作参数 都应该调用这个函数,
     * 相当于`slavePowerOn` 与 `slaveChangeParams` 合并了
     *
     * @param newRequestDetail 从机的请求
     * @return 处理成功返回返回上一次请求所消耗的能量和费用
     * (如果没有上一次的请求, 则新建请求, 并返回 [0, 0]), 否则返回 null
     */
    fun slaveRequest(roomId: Long, newRequestDetail: RequestDetail): List<BigDecimal>? {
        lock.writeLock().lock()
        try {
            var request = requestList.find { it.roomId == roomId }
            if (request == null)
                return null
            var requestId = request.id!!
            var oldRequestDetail = requestDetailMap[requestId]
            if (oldRequestDetail == null) {
                if (!checkRequestTemp(newRequestDetail))
                    return null
                with(newRequestDetail) {
                    this.requestId = requestId
                    this.startTime = LocalDateTime.now()
                }
                requestDetailMap.put(requestId, newRequestDetail)
                return listOf(BigDecimal.ZERO, BigDecimal.ZERO)
            } else {
                requestDetailMap.remove(requestId)
                if (checkRequestTemp(newRequestDetail)) {
                    with(newRequestDetail) {
                        this.requestId = requestId
                        this.startTime = LocalDateTime.now()
                    }
                    with(oldRequestDetail) {
                        this.stopTime = LocalDateTime.now()
                        this.stopTemp = newRequestDetail.startTemp
                    }
                    requestDetailMap.put(requestId, newRequestDetail)
                    return calcFeeAndSave(oldRequestDetail)
                } else { // 如果原先的那个新请求的参数不合法, 则还是处理原先的那个请求
                    requestDetailMap.put(requestId, oldRequestDetail)
                    return null
                }
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * 接收到来自从机的关机 (自动关机) 请求
     * <p>
     * 将从机的详细请求从等待队列中移出, 并计算总费用
     *
     * @param roomId 从机的 roomId
     * @return 如果从机在请求列表中，则返回 true, 否则返回 false
     */
    fun slavePowerOff(roomId: Long): List<BigDecimal>? {
        // 找出对应的 request
        lock.readLock().lock()
        try {
            var request = requestList.find { it.roomId == roomId }
            if (request == null)
                return null
            val requestDetail = requestDetailMap[request.id!!]
            if (requestDetail == null)
                return null
            else {
                requestDetailMap.remove(request.id!!)
                requestDetail.stopTime = LocalDateTime.now()
                return calcFeeAndSave(requestDetail)
            }
        } finally {
            lock.readLock().unlock()
        }
    }

    /**
     * 从机注销, 将其从请求列表中移除
     */
    fun slaveLogout(roomId: Long, normalExit: Boolean = true) {
        lock.writeLock().lock()
        try {
            val index = requestList.indexOfFirst { it.roomId == roomId }
            if (index != -1) {
                val request = requestList.removeAt(index)
                val requestDetail = requestDetailMap[request.id!!]
                if (requestDetail != null) {
                    requestDetailMap.remove(request.id!!)
                    requestDetail.stopTime = LocalDateTime.now()
                    calcFeeAndSave(requestDetail)
                }

                with(request) {
                    this.stopTime = LocalDateTime.now()
                    this.normalExit = normalExit
                }
                requestService.save(request)
            } else
                throw IllegalArgumentException("请求关闭的roomId($roomId)不存在")
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun contains(roomId: Long): Boolean {
        lock.readLock().lock()
        try {
            return sendAirRoomId.contains(roomId)
        } finally {
            lock.readLock().unlock()
        }
    }

    /**
     * 供从机和前端调用, 实时获取本次请求所需要的消耗的能量和所需支付的金额
     * <p>
     * 能量和金额都是使用了 `BigDecimal` 类, 可能不太方便.
     * TODO: 后续可能会因此调整为 `Double` 类型
     * <p>
     * TODO: 值得注意的是, 如果从机修改了风速请求, 则相当于重新发送一次请求, 费用从 0 开始计算,
     *       而从机改变温度则不会, 这一点可能会根据后续业务进行调整
     *
     * @param roomId 从机的 roomId
     * @return 消耗的能量和金额
     */
    fun getEnergyAndFee(roomId: Long): List<BigDecimal>? {
        lock.readLock().lock()
        try {
            val request = requestList.firstOrNull { it.roomId == roomId }
            if (request == null)
                return null
            val requestId = request.roomId!!
            val requestDetail = requestDetailMap[requestId]

            return requestDetail?.let {
                val now = LocalDateTime.now()
                val seconds = Duration.between(it.startTime, now).seconds
                val minutes = (seconds / 60) + (if (seconds % 60 > 0) 1 else 0)
                val energy = BigDecimal(minutes) * BigDecimal(FAX_COST[it.fanSpeed]!!)
                listOf(energy, energy * BigDecimal(5))
            }
        } finally {
            lock.readLock().unlock()
        }
    }


    /**
     * 对请求队列中的请求进行调度
     * <p>
     * 目前的调度策略为时间片轮询, 轮询时长为 1 分钟
     */
    @Scheduled(fixedRateString = "\${master.fixedRate}")
    fun schedule() {
        // 交给 spring boot 管理后, 主机没有启动(各项参数未初始化)就进行调度, 需要判断一下
        lock.readLock().lock()
        try {
            if (workMode == WorkMode.OFF)
                return
        } finally {
            lock.readLock().unlock()
        }

        lock.writeLock().lock()
        try {
            // 每次添加之前记得清零......
            sendAirRoomId.clear()
            var size = requestList.size
            if (size == 0)
                return

            var count = 0
            repeat(size) {
                // 调度的时候, 遍历 `requestList`, 其中一些从机可能只是开机, 但是没有详细请求.
                var request = requestList.removeFirst()
                requestList.addLast(request)
                var requestId = request.id!!
                var requestDetail = requestDetailMap[requestId]
                if (requestDetail != null) {
                    sendAirRoomId.add(request.roomId!!)
                    count++
                }
                if (count == 3)
                    return
            }
        } finally {
            lock.writeLock().unlock()
        }
    }
}
