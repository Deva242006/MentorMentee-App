package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Metadata for a file attached to a mentee's profile. The raw bytes live in GridFS
 * (referenced by {@link #gridFsId}); this document holds everything needed to list and download it.
 */
@Document(collection = "documents")
public class DocumentMeta {

    @Id
    private String id;
    private String menteeId;
    private String uploadedBy;      // user id
    private String category;        // e.g. Resume, Certificate, Report, Submission
    private String originalName;
    private String contentType;
    private long size;
    private String gridFsId;
    private Instant uploadedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMenteeId() { return menteeId; }
    public void setMenteeId(String menteeId) { this.menteeId = menteeId; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getGridFsId() { return gridFsId; }
    public void setGridFsId(String gridFsId) { this.gridFsId = gridFsId; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
