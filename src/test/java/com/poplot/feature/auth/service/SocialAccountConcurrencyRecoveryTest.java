package com.poplot.feature.auth.service;

import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.oauth.OAuthUserInfo;
import com.poplot.feature.auth.repository.SocialAccountRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@Testcontainers(disabledWithoutDocker = true)
class SocialAccountConcurrencyRecoveryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private SocialAccountService socialAccountService;
    @Autowired
    private SocialAccountRepository socialAccountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpInsertDelay() {
        socialAccountRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("ALTER SEQUENCE users_user_id_seq RESTART WITH 1");
        jdbcTemplate.execute("CREATE OR REPLACE FUNCTION delay_social_account_insert() RETURNS trigger AS $$ BEGIN PERFORM pg_advisory_xact_lock(30003); PERFORM pg_sleep(0.5); RETURN NEW; END; $$ LANGUAGE plpgsql");
        jdbcTemplate.execute("CREATE TRIGGER delay_social_account_insert_trigger BEFORE INSERT ON social_account FOR EACH ROW EXECUTE FUNCTION delay_social_account_insert()");
    }

    @AfterEach
    void removeInsertDelay() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS delay_social_account_insert_trigger ON social_account");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS delay_social_account_insert()");
    }

    @Test
    @DisplayName("concurrent first logins return the one account committed in PostgreSQL")
    void concurrentFirstLoginsReturnOneCommittedAccount() throws Exception {
        OAuthUserInfo userInfo = new OAuthUserInfo("provider-user-id", null, null);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<User> login = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out");
                }
                return socialAccountService.findOrCreateUser(SocialProvider.KAKAO, userInfo);
            };
            List<Future<User>> results = List.of(executor.submit(login), executor.submit(login));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Long> ids = List.of(results.get(0).get(15, TimeUnit.SECONDS).getId(), results.get(1).get(15, TimeUnit.SECONDS).getId());

            assertThat(ids.stream().distinct().toList()).hasSize(1);
            assertThat(userRepository.count()).isEqualTo(1);
            assertThat(socialAccountRepository.count()).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT last_value FROM users_user_id_seq", Long.class)).isEqualTo(2L);
            assertThat(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, userInfo.providerUserId()).orElseThrow().getUser().getId())
                .isEqualTo(ids.getFirst());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
