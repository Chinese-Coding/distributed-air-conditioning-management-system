package cn.edu.bupt.master.controller

import cn.edu.bupt.master.TEST
import cn.edu.bupt.master.common.R
import cn.edu.bupt.master.entity.Room
import cn.edu.bupt.master.entity.User
import cn.edu.bupt.master.service.RequestDetailService
import cn.edu.bupt.master.service.RequestService
import cn.edu.bupt.master.service.RoomService
import cn.edu.bupt.master.service.UserService
import jakarta.annotation.Resource
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 前台登录, 结账窗口
 *
 * @author zfq
 */
@RestController
@Validated
@RequestMapping(value = ["/api", ""])
class LoginController {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Resource
    private lateinit var userService: UserService

    @Resource
    private lateinit var roomService: RoomService

    @Resource
    private lateinit var requestService: RequestService

    @Resource
    private lateinit var requestDetailService: RequestDetailService

    /**
     * 新用户登记, 用户自己输入名称和房间 Id
     *
     * @return 注册成功返回用户 id, 以便后续其他功能使用
     */
    @PostMapping("/register")
    fun register(@NotNull userId: Long, @NotNull name: String, @NotNull roomId: Long): R<String> {
        if (TEST)
            logger.info("{}, {}", name, roomId)

        val room = roomService.findById(roomId)
        return if (room != null) R.error("房间已有人使用") else {
            val user = userService.getById(userId)
            when {
                user != null -> { // 如果查询这个姓名的用户存在, 则直接添加房间
                    val newRoom = Room(roomId, userId, false)
                    roomService.save(newRoom)
                }

                else -> { // 如果用户不存在, 则需要在 user 表中添加用户
                    val newUser = User(userId, name, "123456")
                    userService.save(newUser)
                    val newRoom = Room(roomId, userId, false)
                    roomService.save(newRoom)
                }
            }
            logger.info("新用户注册: {}, 分配房间: {}", user, room)
            // TODO: 用户注册时可能存在用户名重复的情况, 以后需要增加异常处理
            R.success("添加成功")
        }
    }

    /**
     * 根据 roomId 获取用户的账单列表
     */
    @GetMapping("/bill")
    fun findRequestByRoomId(@NotNull roomId: Long) =
        R.success(requestService.findAllByRoomId(roomId))


    @GetMapping("/bill/detail")
    fun findRequestDetailByRequestId(@NotNull requestId: Long) =
        R.success(requestDetailService.findAllByRequestId(requestId))

}