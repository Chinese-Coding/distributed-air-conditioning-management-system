package cn.edu.bupt.master

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.EnableScheduling
import java.io.IOException
import kotlin.system.exitProcess



@EnableScheduling
@SpringBootApplication
@EnableAspectJAutoProxy
private class MasterApplication

lateinit var HEATING_TEMP: List<Int>
lateinit var REFRIGERATION_TEMP: List<Int>
lateinit var FAX_COST: Map<String, Double>
var EXPIRY_DURATION: Int = 0
var TEST = false

private fun init() {
    data class BasicData(
        val HEATING_TEMP: List<Int>,
        val REFRIGERATION_TEMP: List<Int>,
        val FAX_COST: Map<String, Double>,
        val EXPIRY_DURATION: Int
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
}

fun main(args: Array<String>) {
    init()
    runApplication<MasterApplication>(*args)
}
