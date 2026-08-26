package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.DocumentDtos.DocumentView;
import com.example.MentorMentee.model.DocumentMeta;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.DocumentMetaRepository;
import com.example.MentorMentee.repository.UserRepository;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Stores mentee documents in GridFS and keeps searchable metadata alongside. */
@Service
public class DocumentService {

    private final GridFsTemplate gridFs;
    private final DocumentMetaRepository docs;
    private final UserRepository users;

    public DocumentService(GridFsTemplate gridFs, DocumentMetaRepository docs, UserRepository users) {
        this.gridFs = gridFs;
        this.docs = docs;
        this.users = users;
    }

    public DocumentMeta store(String menteeId, String uploadedBy, String category, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        String name = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        ObjectId oid;
        try {
            oid = gridFs.store(file.getInputStream(), name, contentType);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read upload");
        }
        DocumentMeta d = new DocumentMeta();
        d.setMenteeId(menteeId);
        d.setUploadedBy(uploadedBy);
        d.setCategory(category == null || category.isBlank() ? "Other" : category.trim());
        d.setOriginalName(name);
        d.setContentType(contentType);
        d.setSize(file.getSize());
        d.setGridFsId(oid.toHexString());
        return docs.save(d);
    }

    public DocumentMeta getMeta(String docId) {
        return docs.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    /** Resolves the stored bytes for a document. */
    public GridFsResource open(DocumentMeta meta) {
        GridFSFile file = gridFs.findOne(Query.query(Criteria.where("_id").is(new ObjectId(meta.getGridFsId()))));
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File contents missing");
        }
        return gridFs.getResource(file);
    }

    public void delete(DocumentMeta meta) {
        gridFs.delete(Query.query(Criteria.where("_id").is(new ObjectId(meta.getGridFsId()))));
        docs.delete(meta);
    }

    /** Removes every document (bytes + metadata) belonging to a mentee. Used when a mentee is deleted. */
    public void deleteAllForMentee(String menteeId) {
        for (DocumentMeta d : docs.findByMenteeIdOrderByUploadedAtDesc(menteeId)) {
            gridFs.delete(Query.query(Criteria.where("_id").is(new ObjectId(d.getGridFsId()))));
        }
        docs.deleteByMenteeId(menteeId);
    }

    public List<DocumentView> listForMentee(String menteeId) {
        List<DocumentMeta> list = docs.findByMenteeIdOrderByUploadedAtDesc(menteeId);
        Map<String, String> names = users.findAllById(
                        list.stream().map(DocumentMeta::getUploadedBy).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));
        return list.stream().map(d -> toView(d, names::get)).toList();
    }

    public DocumentView toView(DocumentMeta d) {
        return toView(d, id -> users.findById(id).map(User::getFullName).orElse(null));
    }

    private DocumentView toView(DocumentMeta d, Function<String, String> nameLookup) {
        return new DocumentView(
                d.getId(), d.getMenteeId(), d.getCategory(), d.getOriginalName(),
                d.getContentType(), d.getSize(), d.getUploadedBy(),
                d.getUploadedBy() == null ? null : nameLookup.apply(d.getUploadedBy()),
                d.getUploadedAt());
    }
}
