package com.example.MentorMentee.web;

import com.example.MentorMentee.dto.DocumentDtos.DocumentView;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentView;
import com.example.MentorMentee.model.DocumentMeta;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.service.AssignmentService;
import com.example.MentorMentee.service.DocumentService;
import com.example.MentorMentee.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Document upload/download for mentee profiles (GridFS-backed) plus mentee assignment submission.
 * Mentor paths are gated to MENTOR, mentee paths to MENTEE; ownership is enforced per request.
 */
@RestController
public class DocumentController {

    private final DocumentService documents;
    private final UserService users;
    private final AssignmentService assignments;

    public DocumentController(DocumentService documents, UserService users, AssignmentService assignments) {
        this.documents = documents;
        this.users = users;
        this.assignments = assignments;
    }

    // --- mentor: manage a mentee's documents ---

    @GetMapping("/api/mentor/mentees/{menteeId}/documents")
    public List<DocumentView> listForMentee(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId) {
        users.requireMentee(me.id(), menteeId);
        return documents.listForMentee(menteeId);
    }

    @PostMapping("/api/mentor/mentees/{menteeId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentView uploadForMentee(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId,
                                        @RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "category", required = false) String category) {
        users.requireMentee(me.id(), menteeId);
        return documents.toView(documents.store(menteeId, me.id(), category, file));
    }

    @GetMapping("/api/mentor/documents/{id}/download")
    public ResponseEntity<Resource> mentorDownload(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        DocumentMeta meta = documents.getMeta(id);
        users.requireMentee(me.id(), meta.getMenteeId());
        return stream(meta);
    }

    @DeleteMapping("/api/mentor/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mentorDelete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        DocumentMeta meta = documents.getMeta(id);
        users.requireMentee(me.id(), meta.getMenteeId());
        documents.delete(meta);
    }

    // --- mentee: own documents ---

    @GetMapping("/api/mentee/documents")
    public List<DocumentView> myDocuments(@AuthenticationPrincipal AuthUser me) {
        return documents.listForMentee(me.id());
    }

    @PostMapping("/api/mentee/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentView uploadOwn(@AuthenticationPrincipal AuthUser me,
                                  @RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "category", required = false) String category) {
        return documents.toView(documents.store(me.id(), me.id(), category, file));
    }

    @GetMapping("/api/mentee/documents/{id}/download")
    public ResponseEntity<Resource> menteeDownload(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        DocumentMeta meta = documents.getMeta(id);
        if (!me.id().equals(meta.getMenteeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your document");
        }
        return stream(meta);
    }

    @DeleteMapping("/api/mentee/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void menteeDelete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        DocumentMeta meta = documents.getMeta(id);
        if (!me.id().equals(meta.getMenteeId()) || !me.id().equals(meta.getUploadedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete documents you uploaded");
        }
        documents.delete(meta);
    }

    // --- mentee: submit an assignment (uploads a document and links it) ---

    @PostMapping("/api/mentee/assignments/{id}/submit")
    public AssignmentView submit(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                 @RequestParam("file") MultipartFile file) {
        assignments.getForMentee(me.id(), id); // validates ownership before storing
        DocumentMeta doc = documents.store(me.id(), me.id(), "Submission", file);
        return assignments.submit(me.id(), id, doc);
    }

    // --- helper ---

    private ResponseEntity<Resource> stream(DocumentMeta meta) {
        GridFsResource resource = documents.open(meta);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(meta.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(meta.getOriginalName()).build().toString())
                .contentType(mediaType)
                .contentLength(meta.getSize())
                .body(resource);
    }
}
