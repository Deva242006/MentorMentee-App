package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    List<Notification> findByUserIdOrderByTimestampDesc(String userId);
    
    long countByUserIdAndReadFalse(String userId);
}
