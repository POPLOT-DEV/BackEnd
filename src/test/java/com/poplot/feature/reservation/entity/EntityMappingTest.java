package com.poplot.feature.reservation.entity;

import com.poplot.feature.auth.entity.AuthSession;
import com.poplot.feature.auth.entity.SocialAccount;
import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.event.entity.Event;
import com.poplot.feature.store.entity.Store;
import com.poplot.feature.user.entity.User;
import com.poplot.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class EntityMappingTest {
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("all core entities persist with their associations")
    void allCoreEntitiesPersistWithAssociations() {
        User user = User.create("user@example.com", "user");
        Store store = new Store("store");
        entityManager.persist(user);
        entityManager.persist(store);

        Event event = new Event(store, "event", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"),
            LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 4), 4, 10);
        entityManager.persist(event);
        TimeSlot slot = new TimeSlot(event, LocalDate.of(2026, 1, 3), LocalTime.of(10, 0), LocalTime.of(11, 0), 10);
        entityManager.persist(slot);
        Reservation reservation = new Reservation(user, slot, "R-2026-1", 2);
        entityManager.persist(reservation);
        entityManager.persist(new Waitlist(user, slot, 2));
        entityManager.persist(new QrTicket(reservation, "ticket-hash", Instant.now()));
        entityManager.persist(new SocialAccount(user, SocialProvider.KAKAO, "provider-id", null));
        entityManager.persist(new AuthSession(user, "refresh-hash", Instant.now().plusSeconds(3600), UUID.randomUUID()));
        entityManager.flush();

        assertThat(user.getPublicId()).isNotNull();
        assertThat(reservation.getId()).isNotNull();
        assertThat(slot.getRemainingCount()).isEqualTo(10);
        assertThat(slot.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("user public id is unique")
    void userPublicIdIsUnique() {
        UUID publicId = UUID.randomUUID();
        entityManager.persist(User.builder().email("first@example.com").publicId(publicId).build());

        assertThatThrownBy(() -> {
            entityManager.persist(User.builder().email("second@example.com").publicId(publicId).build());
            entityManager.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("to-one associations are lazy")
    void toOneAssociationsAreLazy() throws Exception {
        assertLazy(SocialAccount.class, "user");
        assertLazy(AuthSession.class, "user");
        assertLazy(Event.class, "store");
        assertLazy(TimeSlot.class, "event");
        assertLazy(Reservation.class, "user");
        assertLazy(Reservation.class, "timeSlot");
        assertLazy(Waitlist.class, "user");
        assertLazy(Waitlist.class, "timeSlot");
        assertLazy(QrTicket.class, "reservation");
    }

    private void assertLazy(Class<?> type, String name) throws Exception {
        assertThat(entityManager.getMetamodel().entity(type).getAttribute(name).isAssociation()).isTrue();
        Field field = type.getDeclaredField(name);
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        FetchType fetch = manyToOne == null ? field.getAnnotation(OneToOne.class).fetch() : manyToOne.fetch();
        assertThat(fetch).isEqualTo(FetchType.LAZY);
    }
}
