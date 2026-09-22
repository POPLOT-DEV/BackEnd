package com.poplot.feature.example.controller;

import com.poplot.feature.example.dto.request.ExampleRequest;
import com.poplot.feature.example.dto.response.ExampleResponse;
import com.poplot.feature.example.service.ExampleService;
import com.poplot.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/examples")
public class ExampleController {
    private final ExampleService exampleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExampleResponse> create(@Valid @RequestBody ExampleRequest request) {
        return ApiResponse.success(ExampleResponse.from(exampleService.create(request.name())));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExampleResponse> getById(@PathVariable long id) {
        return ApiResponse.success(ExampleResponse.from(exampleService.getById(id)));
    }
}
