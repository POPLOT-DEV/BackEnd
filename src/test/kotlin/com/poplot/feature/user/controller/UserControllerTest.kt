package com.poplot.feature.user.controller

import com.poplot.feature.user.entity.User
import com.poplot.feature.user.enums.UserStatus
import com.poplot.feature.user.repository.UserRepository
import com.poplot.global.security.jwt.JwtProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
) {
    @Test
    fun `Access Token으로 현재 사용자 정보를 조회한다`() {
        val user = userRepository.save(User(email = "user@example.com", name = "은우"))

        mockMvc.get("/users/me") {
            header("Authorization", "Bearer ${jwtProvider.createAccessToken(checkNotNull(user.id))}")
        }.andExpect {
            status { isOk() }
            jsonPath("$.isSuccess") { value(true) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data.userPublicId") { value(user.publicId.toString()) }
            jsonPath("$.data.name") { value("은우") }
            jsonPath("$.data.email") { value("user@example.com") }
            jsonPath("$.data.role") { value("USER") }
        }
    }

    @Test
    fun `인증 정보에 해당하는 사용자가 없으면 찾을 수 없음 오류를 반환한다`() {
        mockMvc.get("/users/me") {
            header("Authorization", "Bearer ${jwtProvider.createAccessToken(Long.MAX_VALUE)}")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("USER_404_01") }
        }
    }

    @Test
    fun `비활성 사용자는 자신의 정보를 조회할 수 없다`() {
        val user = userRepository.save(User(status = UserStatus.INACTIVE))

        mockMvc.get("/users/me") {
            header("Authorization", "Bearer ${jwtProvider.createAccessToken(checkNotNull(user.id))}")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("AUTH_403_01") }
        }
    }
}
