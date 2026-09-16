package com.poplot.feature.auth.repository

import com.poplot.feature.auth.entity.SocialAccount
import com.poplot.feature.auth.enums.SocialProvider
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface SocialAccountRepository : JpaRepository<SocialAccount, Long> {
    @EntityGraph(attributePaths = ["user"])
    fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): SocialAccount?
}
