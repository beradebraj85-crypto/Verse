package com.debraj.verse.repository;

import com.debraj.verse.entity.DocumentMetadata;
import com.debraj.verse.entity.DocumentStatus;
import com.debraj.verse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentMetadataRepo extends JpaRepository<DocumentMetadata, UUID> {

    List<DocumentMetadata> findByStatus(DocumentStatus status);

    List<DocumentMetadata> findAllByOrderByCreatedAtDesc();


    List<DocumentMetadata> findByUserOrderByCreatedAtDesc(User user);

    Optional<DocumentMetadata> findByIdAndUser(UUID id, User user);

}

