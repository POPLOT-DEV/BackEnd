package com.poplot.feature.example.repository

import com.poplot.feature.example.entity.Example
import org.springframework.data.jpa.repository.JpaRepository

interface ExampleRepository : JpaRepository<Example, Long>
