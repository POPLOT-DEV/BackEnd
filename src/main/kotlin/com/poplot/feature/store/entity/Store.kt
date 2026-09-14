package com.poplot.feature.store.entity

import com.poplot.global.common.entity.BaseTimeEntity
import com.poplot.feature.store.enums.StoreStatus
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "store",
    uniqueConstraints = [UniqueConstraint(name = "uk_store_public_id", columnNames = ["public_id"])],
)
class Store(
    @Column(nullable = false, length = 255)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(length = 500)
    var location: String? = null,

    @Column(name = "banner_img_url", length = 2048)
    var bannerImgUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StoreStatus = StoreStatus.ACTIVE,

    @Column(name = "public_id", nullable = false, updatable = false)
    val publicId: UUID = UUID.randomUUID(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    val id: Long? = null,
) : BaseTimeEntity()
