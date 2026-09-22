package com.poplot.feature.example.repository;

import com.poplot.feature.example.entity.Example;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleRepository extends JpaRepository<Example, Long> {
}
