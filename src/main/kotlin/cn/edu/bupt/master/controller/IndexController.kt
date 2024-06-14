package cn.edu.bupt.master.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import java.util.Collections

@Controller
@CrossOrigin
class IndexController(
    errorAttributes: ErrorAttributes, serverProperties: ServerProperties,
    errorViewResolvers: List<ErrorViewResolver>
) : BasicErrorController(errorAttributes, serverProperties.error, errorViewResolvers) {

    override fun errorHtml(request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
        val status = getStatus(request)
        // 获取 Spring Boot 默认提供的错误信息，然后添加一个自定义的错误信息
        var model = Collections
            .unmodifiableMap(
                getErrorAttributes(
                    request,
                    getErrorAttributeOptions(request, MediaType.TEXT_HTML)
                )
            );
        return ModelAndView("index.html", model, status)
    }

    override fun error(request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val status = getStatus(request)
        val body = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL))
            .toMutableMap().apply { put("customError", "自定义错误信息") }
        return ResponseEntity(body, status)
    }

    @GetMapping("/")
    fun index(): String = "index"
}