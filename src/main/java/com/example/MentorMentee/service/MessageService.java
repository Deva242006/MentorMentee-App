package com.example.MentorMentee.service;

import com.example.MentorMentee.model.Message;
import com.example.MentorMentee.model.Role;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messages;
    private final UserService users;
    private final NotificationService notifications;

    public MessageService(MessageRepository messages, UserService users, NotificationService notifications) {
        this.messages = messages;
        this.users = users;
        this.notifications = notifications;
    }

    public List<Message> getConversation(String currentUserId, String otherUserId) {
        validateRelationship(currentUserId, otherUserId);
        
        List<Message> conversation = messages.findConversation(currentUserId, otherUserId);
        conversation.sort(Comparator.comparing(Message::getTimestamp));
        
        // Mark messages sent to current user as read
        boolean updated = false;
        for (Message m : conversation) {
            if (m.getReceiverId().equals(currentUserId) && !m.isRead()) {
                m.setRead(true);
                updated = true;
            }
        }
        if (updated) {
            messages.saveAll(conversation);
        }
        
        return conversation;
    }

    public Message sendMessage(String senderId, String receiverId, String content) {
        validateRelationship(senderId, receiverId);
        
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        Message m = new Message();
        m.setSenderId(senderId);
        m.setReceiverId(receiverId);
        m.setContent(content.trim());
        m.setTimestamp(Instant.now());
        m.setRead(false);
        messages.save(m);
        
        User sender = users.getUser(senderId);
        notifications.notify(receiverId, "New message from " + sender.getFullName(), "MESSAGE", "/messages");
        
        return m;
    }

    private void validateRelationship(String userId1, String userId2) {
        User u1 = users.getUser(userId1);
        User u2 = users.getUser(userId2);
        
        boolean valid = false;
        if (u1.getRole() == Role.MENTOR && u2.getRole() == Role.MENTEE && userId1.equals(u2.getMentorId())) {
            valid = true;
        } else if (u1.getRole() == Role.MENTEE && u2.getRole() == Role.MENTOR && userId2.equals(u1.getMentorId())) {
            valid = true;
        }
        
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only message your assigned mentor/mentees");
        }
    }
}
