package cn.edu.bupt.master.common

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.HashMap

/**
 * 统一返回结果
 *
 * 每个字段必须有 `@JsonProperty()` 注解, 不然不进行序列化
 * 可能是这些属性为 `private` 的关系, 原先的 Java 代码中有 `@Data` 注解, 可能起到了和
 * 这个注解类似的作用
 */
class R<T> {
    @JsonProperty("code")
    private var code: Int = 0

    @JsonProperty("msg")
    private var msg: String? = null

    @JsonProperty("data")
    private var data: T? = null

    @JsonProperty("map")
    private var map = HashMap<Any, Any>()

    fun add(key: String, value: Any): R<T> {
        this.map.put(key, value)
        return this
    }

    companion object {
        fun <T> success(obj: T): R<T> {
            var r = R<T>()
            r.data = obj
            r.code = 1
            return r
        }

        fun <T> error(msg: String): R<T> {
            var r = R<T>()
            r.msg = msg
            r.code = 0
            return r
        }
    }
}