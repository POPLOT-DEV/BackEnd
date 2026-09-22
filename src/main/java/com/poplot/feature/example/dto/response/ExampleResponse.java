package com.poplot.feature.example.dto.response;

import com.poplot.feature.example.entity.Example;

public record ExampleResponse(long id, String name) {
    public static ExampleResponse from(Example example) {
        if (example.getId() == null) throw new IllegalStateException("Example ID must not be null.");
        return new ExampleResponse(example.getId(), example.getName());
    }
}
