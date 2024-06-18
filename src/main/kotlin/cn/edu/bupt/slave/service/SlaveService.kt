package cn.edu.bupt.slave.service


import cn.edu.bupt.slave.common.R
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.Resource
import org.springframework.boot.json.JsonParseException
import org.springframework.core.io.ClassPathResource
import org.springframework.http.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.abs
import kotlin.system.exitProcess

enum class Status {
    OFF, // 手动关闭，温度不变
    AUTO_OFF, // 自动关闭，温度变化
    ON // 打开
}

enum class FanSpeed {
    FAST,
    MIDDLE,
    SLOW,
    NO
}

// 配置参数
lateinit var FAN_SPEED: Map<String, Int>
var CHANGE_TEMP: Int = 0
var BASE_URL: String = ""
var NOW_TEMP: Int = 0

fun init() {
    data class BasicData(
        var FAN_SPEED: Map<String, Int>,
        var CHANGE_TEMP: Int = 0,
        var NOW_TEMP: Int = 0,
        var BASE_URL: String = ""
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
    FAN_SPEED = basicData.FAN_SPEED
    CHANGE_TEMP = basicData.CHANGE_TEMP
    NOW_TEMP = basicData.NOW_TEMP
    BASE_URL = basicData.BASE_URL
}

fun getRequestEntity(vararg params: Pair<String, Any?>): HttpEntity<MultiValueMap<String, String>> {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
    val map = LinkedMultiValueMap<String, String>()
    params.forEach { (name, value) ->
        if (value != null)
            map.add(name, value.toString())
    }
    return HttpEntity(map, headers)
}

/**
 * @author zfq, czl
 */
@Service
class SlaveService {
    @Resource
    private lateinit var restTemplate: RestTemplate

    var ROOM_ID: Long? = null

    private var setTemp = 3000

    private var curTemp = 2800

    private var fanSpeed = FanSpeed.FAST

    private var wind = false

    private val lock = ReentrantReadWriteLock()

    private var status = Status.OFF

    fun login(roomId: Long, setTemp: Int) {
        ROOM_ID = roomId
        this.setTemp = setTemp
    }

    fun logout() {
        ROOM_ID = null
    }

    /**
     * 从机开机, 并向后端发送一个请求
     *
     * @return 从机当前状态
     */
    fun powerOn(): Status {
        status = Status.ON
        request()
        return status
    }

    /**
     * 从机关机, 并向后端发送一个关机请求
     *
     * @return 从机当前状态
     */
    fun powerOff(): Status {
        status = Status.OFF
        slaveOff()
        return status
    }

    /**
     * @return 返回从机当前状态
     */
    fun getStatus(): Status {
        lock.readLock().lock()
        try {
            return status
        } finally {
            lock.readLock().unlock()
        }
    }

    /**
     * @return 返回从机当前设定温度
     */
    fun getSetTemp(): Int {
        lock.readLock().lock()
        try {
            return setTemp
        } finally {
            lock.readLock().unlock()
        }
    }

    /**
     * 从机升 (降) 温, 并向从机发送一个新的请求
     *
     * @param action 如果升温则填 true, 如果降温则填 false
     * @return 返回与主机通信的结果
     */
    fun setSetTemp(action: Boolean): Boolean {
        lock.writeLock().lock()
        try {
            if (action)
                setTemp += 100
            else
                setTemp -= 100
        } finally {
            lock.writeLock().unlock()
        }
        return request()
    }

    /**
     * 从机改变风速,  并向主机发送一个新请求
     *
     * @param fanSpeed 设定的风速值
     * @return 返回与主机通信的结果
     */
    fun setFanSpeed(fanSpeed: FanSpeed): Boolean {
        lock.writeLock().lock()
        try {
            this.fanSpeed = fanSpeed
        } finally {
            lock.writeLock().unlock()
        }
        return request()
    }

    fun getFanSpeed(): FanSpeed {
        lock.readLock().lock()
        try {
            return fanSpeed
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getWind(): Boolean {
        lock.readLock().lock()
        try {
            return wind
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getCurTemp(): Int {
        lock.readLock().lock()
        try {
            return curTemp
        } finally {
            lock.readLock().unlock()
        }
    }


    /**
     * 从机向主机发送请求
     */
    private fun request(): Boolean {
        var requestEntity: HttpEntity<MultiValueMap<String, String>>
        lock.readLock().lock()
        try {
            requestEntity = getRequestEntity(
                "roomId" to ROOM_ID,
                "setTemp" to setTemp, "curTemp" to curTemp,
                "fanSpeed" to fanSpeed
            )
        } finally {
            lock.readLock().unlock()
        }
        val r = restTemplate.postForObject<R<*>>(
            "${BASE_URL}/request",
            requestEntity,
            R::class.java
        )
        return r!!.code == 1
    }

    private fun slaveOff(): Boolean {
        val requestEntity = getRequestEntity("roomId" to ROOM_ID)
        val r = restTemplate.exchange<R<*>>(
            "${BASE_URL}/request",
            HttpMethod.DELETE,
            requestEntity,
            R::class.java
        ).body
        return r!!.code == 1
    }

    /**
     * 向主机发送是否可以向从机送风的请求, 并发送从机当前状态
     */
    private fun toMaster(needWind: Boolean): Boolean {
        var requestEntity =
            getRequestEntity(
                "roomId" to ROOM_ID,
                "setTemp" to setTemp, "curTemp" to curTemp,
                "fanSpeed" to fanSpeed, "needWind" to needWind
            )
        var r = restTemplate.postForEntity(
            "${BASE_URL}/wind",
            requestEntity,
            R::class.java
        )
        var body = r.body
        return if (body != null && body.code == 1)
            body.data as Boolean
        else
            false
    }

    /**
     * 检查是否可以关机
     */
    private fun checkPowerOff(curTemp: Int, setTemp: Int, speed: Int): Boolean =
        (curTemp > setTemp && curTemp - speed <= setTemp) ||
                (curTemp < setTemp && curTemp + speed >= setTemp)

    @Scheduled(fixedRateString = "\${slave.fixedRate}")
    private fun schedule() {
        // 写锁
        lock.writeLock().lock()
        try {
            if (curTemp < NOW_TEMP)
                curTemp += CHANGE_TEMP
            else if (curTemp > NOW_TEMP)
                curTemp -= CHANGE_TEMP

            if (status == Status.OFF)
                return

            wind = // 虽然 `status == Status.ON` 和 `status = Status.ON` 有些重复, 但是这样写起来代码比较简单
                if (status == Status.ON || (status == Status.AUTO_OFF && abs(curTemp - setTemp) >= 100 && request())) {
                    status = Status.ON
                    toMaster(true)
                } else
                    toMaster(false)
            if (status == Status.ON && wind) {
                val speed = FAN_SPEED[fanSpeed.toString()]!!
                var adjustment = if (curTemp > setTemp) -speed else speed
                curTemp += adjustment
                if (checkPowerOff(curTemp, setTemp, speed) && slaveOff()) {
                    status = Status.AUTO_OFF
                    wind = false
                }
            }
        } finally {
            lock.writeLock().unlock()
        }
    }
}