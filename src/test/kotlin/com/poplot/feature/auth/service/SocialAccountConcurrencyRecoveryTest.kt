package com.poplot.feature.auth.service

import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.oauth.OAuthUserInfo
import com.poplot.feature.auth.repository.SocialAccountRepository
import com.poplot.feature.user.entity.User
import com.poplot.feature.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest(
    properties = [
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class SocialAccountConcurrencyRecoveryTest @Autowired constructor(
    private val socialAccountService: SocialAccountService,
    private val socialAccountRepository: SocialAccountRepository,
    private val userRepository: UserRepository,
    private val jdbcTemplate: JdbcTemplate,
) {
    @BeforeEach
    fun setUpInsertDelay() {
        socialAccountRepository.deleteAll()
        userRepository.deleteAll()
        jdbcTemplate.execute("ALTER SEQUENCE users_user_id_seq RESTART WITH 1")
        jdbcTemplate.execute(
            """
            CREATE OR REPLACE FUNCTION delay_social_account_insert()
            RETURNS trigger AS ${'$'}${'$'}
            BEGIN
                PERFORM pg_advisory_xact_lock(30003);
                PERFORM pg_sleep(0.5);
                RETURN NEW;
            END;
            ${'$'}${'$'} LANGUAGE plpgsql
            """.trimIndent(),
        )
        jdbcTemplate.execute(
            """
            CREATE TRIGGER delay_social_account_insert_trigger
            BEFORE INSERT ON social_account
            FOR EACH ROW EXECUTE FUNCTION delay_social_account_insert()
            """.trimIndent(),
        )
    }

    @AfterEach
    fun removeInsertDelay() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS delay_social_account_insert_trigger ON social_account")
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS delay_social_account_insert()")
    }

    @Test
    fun `concurrent first logins return the one account committed in PostgreSQL`() {
        val userInfo = OAuthUserInfo("provider-user-id", null, null)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val results = List(2) {
                executor.submit(Callable<User> {
                    ready.countDown()
                    check(start.await(5, TimeUnit.SECONDS))
                    socialAccountService.findOrCreateUser(SocialProvider.KAKAO, userInfo)
                })
            }
            check(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            val users = results.map { it.get(15, TimeUnit.SECONDS) }

            assertThat(users.map { it.id }.distinct()).hasSize(1)
            assertThat(userRepository.count()).isEqualTo(1)
            assertThat(socialAccountRepository.count()).isEqualTo(1)
            assertThat(jdbcTemplate.queryForObject("SELECT last_value FROM users_user_id_seq", Long::class.java))
                .isEqualTo(2)
            assertThat(
                socialAccountRepository.findByProviderAndProviderUserId(
                    SocialProvider.KAKAO,
                    userInfo.providerUserId,
                )?.user?.id,
            ).isEqualTo(users.first().id)
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }

    private companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }
}
