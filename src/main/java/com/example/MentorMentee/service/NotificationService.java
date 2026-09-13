package com.example.MentorMentee.service;

import com.example.MentorMentee.model.Notification;
import com.example.MentorMentee.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    public void notify(String userId, String message, String type, String link) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setMessage(message);
        n.setType(type);
        n.setLink(link);
        n.setRead(false);
        n.setTimestamp(Instant.now());
        notifications.save(n);
    }

    public List<Notification> getUserNotifications(String userId) {
        return notifications.findByUserIdOrderByTimestampDesc(userId);
    }

    public long getUnreadCount(String userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    public void markAsRead(String id) {
        notifications.findById(id).ifPresent(n -> {
            n.setRead(true);
            notifications.save(n);
        });
    }
    
    public void markAllAsRead(String userId) {
        List<Notification> unread = notifications.findByUserIdOrderByTimestampDesc(userId).stream().filter(n -> !n.isRead()).toList();
        for (Notification n : unread) {
            n.setRead(true);
        }
        notifications.saveAll(unread);
    }
}
