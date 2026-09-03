package com.rahul.repository;

import com.rahul.entity.DerivativeType;
import com.rahul.entity.FileDerivative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileDerivativeRepository extends JpaRepository<FileDerivative, UUID> {

    Optional<FileDerivative> findByFileIdAndDerivativeType(UUID fileId, DerivativeType derivativeType);

    boolean existsByFileIdAndDerivativeType(UUID fileId, DerivativeType derivativeType);
}