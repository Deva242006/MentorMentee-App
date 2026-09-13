package com.example.MentorMentee.web;

import com.example.MentorMentee.model.Notification;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationsResponse getMyNotifications(@AuthenticationPrincipal AuthUser me) {
        return new NotificationsResponse(
                notificationService.getUserNotifications(me.id()),
                notificationService.getUnreadCount(me.id())
        );
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        notificationService.markAsRead(id);
    }
    
    @PutMapping("/read-all")
    public void markAllAsRead(@AuthenticationPrincipal AuthUser me) {
        notificationService.markAllAsRead(me.id());
    }

    public record NotificationsResponse(List<Notification> notifications, long unreadCount) {}
}
