package com.poplot.feature.example.service

import com.poplot.feature.example.entity.Example
import com.poplot.feature.example.exception.ExampleErrorCode
import com.poplot.feature.example.repository.ExampleRepository
import com.poplot.global.exception.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ExampleService(
    private val exampleRepository: ExampleRepository,
) {
    @Transactional
    fun create(name: String): Example =
        exampleRepository.save(Example(name = name.trim()))

    fun getById(id: Long): Example =
        exampleRepository.findById(id).orElseThrow {
            GeneralException(ExampleErrorCode.EXAMPLE_NOT_FOUND)
        }
}
