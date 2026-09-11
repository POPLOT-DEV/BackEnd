package com.poplot.feature.example.dto.response

import com.poplot.feature.example.entity.Example

data class ExampleResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(example: Example) = ExampleResponse(requireNotNull(example.id), example.name)
    }
}
