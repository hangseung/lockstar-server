package com.app.lockstar;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FileRepository extends CrudRepository<File, Integer> {
    Optional<File> findById(Integer id);
}
