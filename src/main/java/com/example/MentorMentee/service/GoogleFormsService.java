package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.FormDtos.FormQuestionDto;
import com.example.MentorMentee.model.FormQuestion;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Talks to the Google Forms REST API directly (via {@link RestClient}) instead of the generated
 * client library. Creates a form + questions in one batchUpdate and reads back the Google-assigned
 * question ids, and pulls responses for syncing.
 */
@Service
public class GoogleFormsService {

    private static final String FORMS_BASE = "https://forms.googleapis.com/v1/forms";

    private final RestClient http = RestClient.create();

    public record CreatedForm(String formId, String responderUri, List<FormQuestion> questions) {}

    public record RawResponse(String responseId, String respondentEmail, Instant submittedAt,
                              Map<String, List<String>> answersByQuestionId) {}

    /** Creates the form, adds the questions, and returns identifiers + questions with Google ids filled in. */
    public CreatedForm createForm(String accessToken, String title, String description, List<FormQuestionDto> questions) {
        // 1) create the form shell (only the title is accepted here)
        Map<String, Object> created = post(accessToken, FORMS_BASE,
                Map.of("info", Map.of("title", title == null ? "Untitled form" : title)));
        String formId = str(created.get("formId"));
        String responderUri = str(created.get("responderUri"));
        if (formId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google did not return a form id");
        }

        // 2) batch-add description + questions
        List<Map<String, Object>> requests = new ArrayList<>();
        if (description != null && !description.isBlank()) {
            requests.add(Map.of("updateFormInfo", Map.of(
                    "info", Map.of("description", description),
                    "updateMask", "description")));
        }
        for (int i = 0; i < questions.size(); i++) {
            requests.add(Map.of("createItem", Map.of(
                    "item", Map.of(
                            "title", questions.get(i).title(),
                            "questionItem", Map.of("question", questionBody(questions.get(i)))),
                    "location", Map.of("index", i))));
        }
        Map<String, Object> batchResult = post(accessToken, FORMS_BASE + "/" + formId + ":batchUpdate",
                Map.of("requests", requests, "includeFormInResponse", true));

        // 3) read back the assigned question ids (items come back in creation order)
        List<FormQuestion> stored = new ArrayList<>();
        List<Object> items = listOf(mapOf(batchResult.get("form")).get("items"));
        for (int i = 0; i < questions.size(); i++) {
            FormQuestionDto dto = questions.get(i);
            FormQuestion q = new FormQuestion();
            q.setType(dto.type());
            q.setTitle(dto.title());
            q.setRequired(dto.required());
            q.setOptions(dto.options() == null ? List.of() : dto.options());
            if (i < items.size()) {
                Map<String, Object> question = mapOf(mapOf(mapOf(items.get(i)).get("questionItem")).get("question"));
                q.setQuestionId(str(question.get("questionId")));
            }
            stored.add(q);
        }
        return new CreatedForm(formId, responderUri, stored);
    }

    /** Pulls every response for a form (following pagination). */
    public List<RawResponse> fetchResponses(String accessToken, String formId) {
        List<RawResponse> out = new ArrayList<>();
        String pageToken = null;
        do {
            String uri = FORMS_BASE + "/" + formId + "/responses?pageSize=1000"
                    + (pageToken == null ? "" : "&pageToken=" + pageToken);
            Map<String, Object> body = get(accessToken, uri);
            for (Object r : listOf(body.get("responses"))) {
                out.add(parseResponse(mapOf(r)));
            }
            pageToken = str(body.get("nextPageToken"));
        } while (pageToken != null && !pageToken.isBlank());
        return out;
    }

    // --- request building ---

    private Map<String, Object> questionBody(FormQuestionDto q) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("required", q.required());
        List<String> options = q.options() == null ? List.of() : q.options();
        switch (q.type() == null ? "" : q.type().toUpperCase()) {
            case "PARAGRAPH" -> question.put("textQuestion", Map.of("paragraph", true));
            case "MULTIPLE_CHOICE" -> question.put("choiceQuestion", choice("RADIO", options));
            case "CHECKBOX" -> question.put("choiceQuestion", choice("CHECKBOX", options));
            case "DROPDOWN" -> question.put("choiceQuestion", choice("DROP_DOWN", options));
            case "SCALE" -> question.put("scaleQuestion", Map.of("low", 1, "high", 5));
            default -> question.put("textQuestion", Map.of("paragraph", false)); // TEXT
        }
        return question;
    }

    private Map<String, Object> choice(String type, List<String> options) {
        List<Map<String, Object>> opts = new ArrayList<>();
        for (String o : options) {
            opts.add(Map.of("value", o));
        }
        if (opts.isEmpty()) {
            opts.add(Map.of("value", "Option 1"));
        }
        return Map.of("type", type, "options", opts);
    }

    // --- response parsing ---

    private RawResponse parseResponse(Map<String, Object> r) {
        String responseId = str(r.get("responseId"));
        String email = str(r.get("respondentEmail"));
        Instant submitted = parseInstant(str(r.get("lastSubmittedTime")));
        Map<String, List<String>> answers = new LinkedHashMap<>();
        Map<String, Object> answerMap = mapOf(r.get("answers"));
        for (Map.Entry<String, Object> e : answerMap.entrySet()) {
            Map<String, Object> ans = mapOf(e.getValue());
            List<String> values = new ArrayList<>();
            Map<String, Object> textAnswers = mapOf(ans.get("textAnswers"));
            for (Object a : listOf(textAnswers.get("answers"))) {
                String v = str(mapOf(a).get("value"));
                if (v != null) {
                    values.add(v);
                }
            }
            answers.put(e.getKey(), values);
        }
        return new RawResponse(responseId, email, submitted, answers);
    }

    private static Instant parseInstant(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    // --- HTTP + casting helpers ---

    private Map<String, Object> post(String accessToken, String url, Object body) {
        try {
            return http.post().uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Forms API error: " + e.getMessage());
        }
    }

    private Map<String, Object> get(String accessToken, String url) {
        try {
            return http.get().uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Forms API error: " + e.getMessage());
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> listOf(Object o) {
        return o instanceof List ? (List<Object>) o : List.of();
    }
}
