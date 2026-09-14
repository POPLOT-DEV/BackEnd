package com.poplot.feature.reservation.entity

import com.poplot.feature.auth.entity.AuthSession
import com.poplot.feature.auth.entity.SocialAccount
import com.poplot.feature.event.entity.Event
import com.poplot.feature.store.entity.Store
import com.poplot.feature.user.entity.User
import jakarta.persistence.EntityManager
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@DataJpaTest
@ActiveProfiles("test")
class EntityMappingTest @Autowired constructor(private val entityManager: EntityManager) {
    @Test
    fun `all core entities persist with their associations`() {
        val user = User(email = "user@example.com", name = "user")
        val store = Store(name = "store")
        entityManager.persist(user)
        entityManager.persist(store)

        val event = Event(
            store = store, title = "event", reservationOpenAt = Instant.parse("2026-01-01T00:00:00Z"),
            reservationCloseAt = Instant.parse("2026-01-02T00:00:00Z"), startDate = LocalDate.of(2026, 1, 3),
            endDate = LocalDate.of(2026, 1, 4), maxPartySize = 4, holdMinutes = 10,
        )
        entityManager.persist(event)
        val slot = TimeSlot(event, LocalDate.of(2026, 1, 3), LocalTime.of(10, 0), LocalTime.of(11, 0), 10)
        entityManager.persist(slot)
        val reservation = Reservation(user, slot, "R-2026-1", 2)
        entityManager.persist(reservation)
        entityManager.persist(Waitlist(user, slot, 2))
        entityManager.persist(QrTicket(reservation, "ticket-hash", Instant.now()))
        entityManager.persist(SocialAccount(user, "KAKAO", "provider-id"))
        entityManager.persist(AuthSession(user, "refresh-hash", Instant.now().plusSeconds(3600)))
        entityManager.flush()

        assertThat(reservation.id).isNotNull()
        assertThat(slot.remainingCount).isEqualTo(10)
        assertThat(slot.isSoldOut).isFalse()
    }

    @Test
    fun `to-one associations are lazy`() {
        val metamodel = entityManager.metamodel
        listOf(
            SocialAccount::class.java to "user", AuthSession::class.java to "user",
            Event::class.java to "store", TimeSlot::class.java to "event",
            Reservation::class.java to "user", Reservation::class.java to "timeSlot",
            Waitlist::class.java to "user", Waitlist::class.java to "timeSlot",
            QrTicket::class.java to "reservation",
        ).forEach { (type, property) ->
            assertThat(metamodel.entity(type).getAttribute(property).isAssociation).isTrue()
            val field = type.getDeclaredField(property)
            val fetch = field.getAnnotation(ManyToOne::class.java)?.fetch
                ?: field.getAnnotation(OneToOne::class.java).fetch
            assertThat(fetch).isEqualTo(FetchType.LAZY)
        }
    }
}
