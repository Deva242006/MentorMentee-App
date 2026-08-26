package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.DocumentMeta;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocumentMetaRepository extends MongoRepository<DocumentMeta, String> {
    List<DocumentMeta> findByMenteeIdOrderByUploadedAtDesc(String menteeId);
    long countByMenteeId(String menteeId);
    void deleteByMenteeId(String menteeId);
}
