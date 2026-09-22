package com.poplot.feature.example.service;

import com.poplot.feature.example.entity.Example;
import com.poplot.feature.example.exception.ExampleErrorCode;
import com.poplot.feature.example.repository.ExampleRepository;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService {
    private final ExampleRepository exampleRepository;

    @Transactional
    public Example create(String name) {
        return exampleRepository.save(new Example(name.trim()));
    }

    public Example getById(long id) {
        return exampleRepository.findById(id).orElseThrow(() -> new GeneralException(ExampleErrorCode.EXAMPLE_NOT_FOUND));
    }
}
