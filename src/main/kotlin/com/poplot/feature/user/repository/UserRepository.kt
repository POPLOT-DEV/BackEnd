package com.poplot.feature.user.repository

import com.poplot.feature.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>
