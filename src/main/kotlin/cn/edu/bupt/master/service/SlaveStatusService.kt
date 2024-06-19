package cn.edu.bupt.master.service

import cn.edu.bupt.master.TEST
import cn.edu.bupt.master.entity.SlaveStatus
import cn.edu.bupt.master.entity.Status
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class SlaveStatusService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var masterService: MasterService

    @Autowired
    private lateinit var roomRepository: RoomRepository

    @Value("\${master.EXPIRY_DURATION}")
    private lateinit var EXPIRY_DURATION: String

    private var slaveStatusMap = HashMap<Long, SlaveStatus>()

    fun register(roomId: Long) {
        synchronized(slaveStatusMap) {
            if (slaveStatusMap.containsKey(roomId)) {
                var slaveStatus = slaveStatusMap[roomId]!!
                if (slaveStatus.status == Status.离线 || slaveStatus.status == Status.关机) {
                    slaveStatus.status = Status.正常
                    slaveStatus.registeredTime = LocalDateTime.now()
                    slaveStatusMap.put(roomId, slaveStatus)
                }
            } else {
                var slaveStatus =
                    SlaveStatus(roomId = roomId, status = Status.正常, registeredTime = LocalDateTime.now())
                slaveStatusMap.put(roomId, slaveStatus)
            }
        }
    }

    fun unregister(roomId: Long) {
        synchronized(slaveStatusMap) {
            if (slaveStatusMap.containsKey(roomId)) {
                var slaveStatus = slaveStatusMap[roomId]!!
                slaveStatus.status = Status.关机
                slaveStatus.registeredTime = LocalDateTime.now()
                // 用户如果登出, 则需要重置费用和能量
                slaveStatus.zero()
                slaveStatusMap.put(roomId, slaveStatus)
            }
        }
    }

    fun isRegistered(roomId: Long): Boolean {
        synchronized(slaveStatusMap) {
            return slaveStatusMap.containsKey(roomId)
        }
    }


    fun updateEnergyAndFee(roomId: Long, energy: BigDecimal, fee: BigDecimal) {
        synchronized(slaveStatusMap) {
            if (slaveStatusMap.containsKey(roomId)) {
                var slaveStatus = slaveStatusMap[roomId]!!
                slaveStatus.energy += energy
                slaveStatus.fee += fee
                slaveStatus.registeredTime = LocalDateTime.now()
                if (TEST)
                    logger.info(
                        "更新从机({}), 当前历史能量和费用为: {}, {}",
                        roomId,
                        slaveStatus.energy,
                        slaveStatus.fee
                    )
                slaveStatusMap.put(roomId, slaveStatus)
            }
        }
    }

    /**
     * 更新除去能量和费用以外的其他参数
     */
    fun updateSlaveStatus(roomId: Long, curTemp: Int, setTemp: Int, status: Status, fanSpeed: String) {
        synchronized(slaveStatusMap) {
            if (isRegistered(roomId)) {
                // 如果存在则直接更新该对象
                val slaveStatus = slaveStatusMap[roomId]!!
                with(slaveStatus) {
                    this.curTemp = curTemp
                    this.setTemp = setTemp
                    this.status = status
                    this.fanSpeed = fanSpeed
                    this.registeredTime = LocalDateTime.now()
                }
                slaveStatusMap[roomId] = slaveStatus
            }
        }
    }


    fun getSlaveStatusList(): List<SlaveStatus> {
        synchronized(slaveStatusMap) {
            return ArrayList(slaveStatusMap.values)
        }
    }


    fun getEnergyAndFee(roomId: Long): List<BigDecimal>? {
        synchronized(slaveStatusMap) {
            var slaveStatus = slaveStatusMap[roomId]
            return if (slaveStatus != null) listOf(slaveStatus.energy, slaveStatus.fee) else null
        }
    }

    /**
     * 检查从机的注册状态
     */
    @Scheduled(fixedRateString = "\${master.pollingInterval}")
    fun checkRegisteredId() {
        synchronized(slaveStatusMap) {
            var now = LocalDateTime.now()

            // 在 GPT 的建议下改为使用迭代器
            val iterator = slaveStatusMap.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.status == Status.离线)
                    continue

                if (ChronoUnit.SECONDS.between(entry.value.registeredTime, now) > EXPIRY_DURATION.toInt()) {
                    val roomId = entry.key
                    if (entry.value.status == Status.关机) {
                        iterator.remove()
                        continue
                    }
                    logger.error("从机 {} 超时, 离线", roomId)
                    entry.value.status = Status.离线
                    val list = masterService.slavePowerOff(roomId)
                    if (list != null)
                        this.updateEnergyAndFee(roomId, list[0], list[1])

                    val room = roomRepository.findById(roomId).get()
                    room.inuse = false
                    roomRepository.save(room)
                }
            }
        }
    }
}