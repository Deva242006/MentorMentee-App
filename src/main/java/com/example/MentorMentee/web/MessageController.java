package com.example.MentorMentee.web;

import com.example.MentorMentee.model.Message;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.service.MessageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{otherUserId}")
    public List<Message> getConversation(@AuthenticationPrincipal AuthUser me, @PathVariable String otherUserId) {
        return messageService.getConversation(me.id(), otherUserId);
    }

    @PostMapping
    public Message sendMessage(@AuthenticationPrincipal AuthUser me, @RequestBody SendMessageRequest req) {
        return messageService.sendMessage(me.id(), req.receiverId(), req.content());
    }

    public record SendMessageRequest(String receiverId, String content) {}
}
