package com.example.MentorMentee.dto;

import java.time.Instant;

/** Document metadata returned to the client (bytes are downloaded separately). */
public final class DocumentDtos {

    public record DocumentView(
            String id,
            String menteeId,
            String category,
            String originalName,
            String contentType,
            long size,
            String uploadedBy,
            String uploadedByName,
            Instant uploadedAt) {}

    private DocumentDtos() {}
}
