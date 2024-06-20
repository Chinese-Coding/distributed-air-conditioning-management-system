package cn.edu.bupt.master.service

import cn.edu.bupt.master.TEST
import cn.edu.bupt.master.entity.SlaveStatus
import cn.edu.bupt.master.entity.Status
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

@Service
class SlaveStatusService {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val lock = ReentrantReadWriteLock()

    @Resource
    private lateinit var masterService: MasterService

    @Resource
    private lateinit var roomRepository: RoomRepository

    @Value("\${master.EXPIRY_DURATION}")
    private lateinit var EXPIRY_DURATION: String

    private var slaveStatusMap = HashMap<Long, SlaveStatus>()

    fun register(roomId: Long) {
        lock.writeLock().lock()
        try {
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
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun unregister(roomId: Long) {
        lock.writeLock().lock()
        try {
            if (slaveStatusMap.containsKey(roomId)) {
                var slaveStatus = slaveStatusMap[roomId]!!
                slaveStatus.status = Status.关机
                slaveStatus.registeredTime = LocalDateTime.now()
                slaveStatusMap.put(roomId, slaveStatus)
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun isRegistered(roomId: Long): Boolean {
        lock.readLock().lock()
        try {
            return slaveStatusMap.containsKey(roomId)
        } finally {
            lock.readLock().unlock()
        }
    }


    fun updateEnergyAndFee(roomId: Long, energy: BigDecimal, fee: BigDecimal) {
        lock.writeLock().lock()
        try {
            if (slaveStatusMap.containsKey(roomId)) {
                var slaveStatus = slaveStatusMap[roomId]!!
                slaveStatus = updateEnergyAndFee(slaveStatus, energy, fee)
                slaveStatusMap.put(roomId, slaveStatus)
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * 更新除去能量和费用以外的其他参数
     */
    fun updateSlaveStatus(roomId: Long, curTemp: Int, setTemp: Int, status: Status, fanSpeed: String) {
        lock.writeLock().lock()
        try {
            if (slaveStatusMap.containsKey(roomId)) {
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
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * 不加锁的更新方法
     */
    private fun updateEnergyAndFee(slaveStatus: SlaveStatus, energy: BigDecimal, fee: BigDecimal): SlaveStatus {
        with(slaveStatus) {
            slaveStatus.energy += energy
            slaveStatus.fee += fee
            slaveStatus.registeredTime = LocalDateTime.now()
        }
        if (TEST)
            logger.info(
                "更新从机({}), 当前历史能量和费用为: {}, {}",
                slaveStatus.roomId,
                slaveStatus.energy,
                slaveStatus.fee
            )
        return slaveStatus
    }


    fun getSlaveStatusList(): List<SlaveStatus> {
        lock.readLock().lock()
        try {
            return ArrayList(slaveStatusMap.values)
        } finally {
            lock.readLock().unlock()
        }
    }


    fun getEnergyAndFee(roomId: Long): List<BigDecimal>? {
        lock.readLock().lock()
        try {
            return slaveStatusMap[roomId]?.let { listOf(it.energy, it.fee) }
        } finally {
            lock.readLock().unlock()
        }
    }

    /**
     * 检查从机的注册状态
     */
    @Scheduled(fixedRateString = "\${master.pollingInterval}")
    fun checkRegisteredId() {
        lock.writeLock().lock()
        try {
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
                    // 更新从机花费和能量
                    val energyAndFee = masterService.slavePowerOff(roomId)
                    if (energyAndFee != null)
                        slaveStatusMap[roomId] = updateEnergyAndFee(entry.value, energyAndFee[0], energyAndFee[1])

                    val room = roomRepository.findById(roomId).get()
                    room.inuse = false
                    roomRepository.save(room)
                }
            }
        } finally {
            lock.writeLock().unlock()
        }
    }
}