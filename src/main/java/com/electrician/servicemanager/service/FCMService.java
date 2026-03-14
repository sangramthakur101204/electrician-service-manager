package com.electrician.servicemanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class FCMService {

    @Value("${fcm.server.key:}")
    private String serverKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (serverKey == null || serverKey.isEmpty() || fcmToken == null || fcmToken.isEmpty()) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "key=" + serverKey);

            Map<String, Object> notification = Map.of(
                    "title", title,
                    "body", body,
                    "sound", "default",
                    "priority", "high"
            );

            Map<String, Object> payload = Map.of(
                    "to", fcmToken,
                    "notification", notification,
                    "data", data != null ? data : Map.of(),
                    "priority", "high",
                    "android", Map.of(
                            "priority", "high",
                            "notification", Map.of(
                                    "sound", "default",
                                    "channel_id", "job_notifications"
                            )
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity("https://fcm.googleapis.com/fcm/send", entity, String.class);
            System.out.println("[FCM] Notification sent to: " + fcmToken.substring(0, 10) + "...");
        } catch (Exception e) {
            System.err.println("[FCM] Failed to send notification: " + e.getMessage());
        }
    }
}