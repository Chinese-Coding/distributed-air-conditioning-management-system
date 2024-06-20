package cn.edu.bupt.master.service


import cn.edu.bupt.master.entity.Period
import cn.edu.bupt.master.entity.RequestDetail
import cn.edu.bupt.master.entity.Request
import cn.edu.bupt.master.entity.Room
import cn.edu.bupt.master.entity.User
import jakarta.annotation.Resource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Repository
interface RequestRepository : JpaRepository<Request, Long> {
    /**
     * 查询房间的全部请求
     *
     * 这个函数的作用是用户获取账单时使用, 将用户的数据一次性返还给请求方.
     * 请求方根据自己的需要计算相应的数据
     *
     * @return 请求列表
     */
    fun findAllByRoomId(roomId: Long): List<Request>

    fun findAllByStartTimeBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<Request>
}

@Repository
interface RequestDetailRepository : JpaRepository<RequestDetail, Long> {
    fun findAllByRequestId(requestId: Long): List<RequestDetail>
}

@Repository
interface RoomRepository : JpaRepository<Room, Long>

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByNameAndPassword(name: String, password: String): User?
}

@Service
class RequestService {
    @Resource
    private lateinit var requestRepository: RequestRepository


    fun save(request: Request) = requestRepository.save(request)

    /**
     * 查询房间的全部请求
     * <p>
     * 这个函数的作用是用户获取账单时使用, 将用户的数据一次性返还给请求方.
     * 请求方根据自己的需要计算相应的数据
     *
     * @return 请求列表
     */
    fun findAllByRoomId(roomId: Long) = requestRepository.findAllByRoomId(roomId)


    /**
     * 通过时间获取请求, 完成日常报表工作
     * <p>
     * TODO: 这个方法对应于需求文档中的第12条, 第12条所需要的信息更为具体,
     *       所以我希望放到接口层完成, 如果有需要可以分成不同层完成此工作
     */
    fun findAllByPeriod(period: Period): List<Request> {
        return when (period) {
            Period.MONTH -> {
                var now = LocalDateTime.now()
                var startTime = now.minusDays(30) // 从今天向前减 30 天
                println("$startTime $now")
                return requestRepository.findAllByStartTimeBetween(startTime, now)
            }

            Period.WEEK -> {
                var now = LocalDateTime.now()
                var startTime = now.minusDays(7) // 从今天向前减 7 天
                return requestRepository.findAllByStartTimeBetween(startTime, now)
            }

            Period.DAY -> {
                var now = LocalDateTime.now()
                var startTime = now.minusDays(1) // 从今天向前减 1 天
                return requestRepository.findAllByStartTimeBetween(startTime, now)
            }
        }
    }
}

@Service
class UserService {
    @Resource
    private lateinit var userRepository: UserRepository

    fun getById(id: Long): User? {
        var r = userRepository.findById(id)
        return if (r.isPresent) r.get() else null
    }


    fun save(user: User) = userRepository.save(user)

    fun login(name: String, password: String = "123456") = userRepository.findByNameAndPassword(name, password)
}

@Service
class RoomService {
    @Resource
    private lateinit var roomRepository: RoomRepository

    fun findById(id: Long): Room? {
        var r = roomRepository.findById(id)
        return if (r.isPresent) r.get() else null
    }

    fun save(room: Room) = roomRepository.save(room)

    fun updateById(room: Room) = roomRepository.save(room)
}

@Service
class RequestDetailService {
    @Resource
    private lateinit var requestDetailRepository: RequestDetailRepository

    fun findAllByRequestId(roomId: Long) = requestDetailRepository.findAllByRequestId(roomId)
}