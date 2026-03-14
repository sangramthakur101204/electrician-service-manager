package com.electrician.servicemanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Base64;

@Service
public class FCMService {

    private static final String PROJECT_ID = "electroserve-3b80a";
    private static final String FCM_URL = "https://fcm.googleapis.com/v1/projects/" + PROJECT_ID + "/messages:send";

    // Service account credentials embedded
    private static final String SERVICE_ACCOUNT_JSON = """
{
  "type": "service_account",
  "project_id": "electroserve-3b80a",
  "private_key_id": "0890fe00dec05c8210ca7f3cd49cb26a65fb5dd6",
  "private_key": "-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCxvEDIMkJVUiCB\\nwMC8sHa/OKIZFfRqF/aqxNtZVB0qwUw3J/A84CflINQNhl0fGnT83wDHuwnMGw8F\\nxoQPz1ke4xSQ2DYlCJBEW4P7mmm/wM0bHriP3jxpsYkc2tihPDpot2At111Ulrjm\\n94HQbbJtn/17njOHeOF+LdhChSUR1pi36cpX0NeAqTk+9p4A5z9s6ZFshKDxSgme\\nbjC0OPgMySARMA4xTD1NGTOtFUoJn0fry9TqKLyE3VwVLFFwPeQ7v3IOhGW94qn+\\nZZAlLw9/R3SPK+DUTOkLXL+zoLFqU72ITxkPQOFDLLVAC/gheJjLhZvLy2HlQzGL\\nsI5gZCmjAgMBAAECggEAVXFEDVnH9L9V/Dp/DVahx1yA5KFezc/2V3LqZf+fQ+oH\\n6y4KeiFiXG6TMmtJAgZUfinwdJQQaiwJ5UNhQj5yP/x3awhdwyDiRdJe2QoK01Fq\\n6KI0pIj4LvFXkmUMpjpPc/7PwEnbxqnserA6mknZ8IRstcxARlyQvnlokNX0E6Xy\\nUX53zNIcnOa1qFAwiAkiZOe5deGyJzlOjsCrOK2Fo93V6+9NX+l0tKEZlTUb71Cb\\n1saa+vGy0CTrO+HttHb+1UaonxePtAcZy8RwuP7SuAILCQOjDMGtJFNDJPU0EOI9\\nilbi+eagK1mFsGZyadRVN4QWNC5cwlKmcj8FxM0lEQKBgQDkkqG65lhjpuTObytD\\nPMmo3hE8p+Inmc8pSnchS6qUZ7rsUfwsBj56EuE0E82hyg6xKEDe3ftu8reNaoPe\\nIWJQM4Ca+pu0VBsuddDBlSLVnDj+9KWIlKaEy9olh84XqBMRDwKaWoTVW+/YAIho\\nVjXGDcA9+7/QX/MNjXwoP+wsEwKBgQDHD/tnyT5b/m9wiTV0lERPojgwg9knmm5P\\n1rUoctdYml7cvofOTKeDe05cwX/5b30xI2mqHzOXEWJPrYdxlAI7s3eqyIS628Me\\n3M4OPkaYKT8+s+hb3H70ehNx2roIZEgz4r6vXRVQOi+fklYxsZHu7yXGRdQrN+fU\\nnXyAQpSeMQKBgQDWDbPD0f+ooxzXbih3uS+pIv3FbftO1q24n3HdDn1aqaHVuhmM\\nKPTNYWzhVkJWZ2FG6smFbSEQD+FxX/TKSz1EmlavzE2QnMsvwUmUYFPU8440xWtX\\n8s3Wwwkx6HJKmS3x5bqsTmTjYkvEXwtfmoyVQz9rTJ2fSJvXb058axCDXQKBgHLO\\nzurO8H5SQqefGwt/r95V6x5gDNCAivrVFGqdwYHCls+tu9nJ8Bsu9MUefNa57HR4\\nvip2EGUii5b0uFdTS+5u1afUOmki8mhMZTNly4Va1LUvQeYpR7ds6OYThpPscpAq\\nSFwPzYkV5f2cgsXqagZqO1kdUP8UIUtJAVNgEyMhAoGAdf6cQI2dJ3WP4q6nXJ4K\\nz5MEMAac2Q4UbMuruvfkVqRYTA8fj6oEnM+ubLDjCcQWArkFghqzUQu76RLk6ah2\\naA4ZAzPrz7LjalJVYrBtXecWuu8xYU0RQOa1XCWhw8tzkGukbYANXLznpI5AwbjU\\njF1FRsOdnx863/vtp2x8G10=\\n-----END PRIVATE KEY-----\\n",
  "client_email": "firebase-adminsdk-fbsvc@electroserve-3b80a.iam.gserviceaccount.com",
  "client_id": "115460716972847748872",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token"
}
""";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedAccessToken = null;
    private long tokenExpiry = 0;

    private String getAccessToken() {
        try {
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiry) {
                return cachedAccessToken;
            }

            // Parse service account
            @SuppressWarnings("unchecked")
            Map<String, String> sa = objectMapper.readValue(SERVICE_ACCOUNT_JSON, Map.class);
            String clientEmail = sa.get("client_email");
            String privateKeyStr = sa.get("private_key")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            // Build JWT
            long now = System.currentTimeMillis() / 1000;
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(("{\"iss\":\"" + clientEmail + "\","
                            + "\"scope\":\"https://www.googleapis.com/auth/firebase.messaging\","
                            + "\"aud\":\"https://oauth2.googleapis.com/token\","
                            + "\"exp\":" + (now + 3600) + ","
                            + "\"iat\":" + now + "}").getBytes(StandardCharsets.UTF_8));

            // Sign with RSA
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            java.security.PrivateKey privateKey = java.security.KeyFactory.getInstance("RSA").generatePrivate(spec);
            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update((header + "." + payload).getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());

            String jwt = header + "." + payload + "." + signature;

            // Exchange JWT for access token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String body = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + jwt;
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", entity, Map.class);

            if (response.getBody() != null) {
                cachedAccessToken = (String) response.getBody().get("access_token");
                tokenExpiry = System.currentTimeMillis() + 3500000; // 58 min
                return cachedAccessToken;
            }
        } catch (Exception e) {
            System.err.println("[FCM] Token error: " + e.getMessage());
        }
        return null;
    }

    public void sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isEmpty()) return;
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) return;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> notification = Map.of("title", title, "body", body);
            Map<String, Object> android = Map.of(
                    "priority", "high",
                    "notification", Map.of("sound", "default", "channel_id", "job_notifications")
            );
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("token", fcmToken);
            message.put("notification", notification);
            message.put("android", android);
            if (data != null) message.put("data", data);

            Map<String, Object> payload = Map.of("message", message);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(FCM_URL, entity, String.class);
            System.out.println("[FCM] Notification sent successfully");
        } catch (Exception e) {
            System.err.println("[FCM] Send error: " + e.getMessage());
        }
    }
}