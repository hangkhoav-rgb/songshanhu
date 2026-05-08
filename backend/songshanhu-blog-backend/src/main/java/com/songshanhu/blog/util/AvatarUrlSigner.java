package com.songshanhu.blog.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class AvatarUrlSigner {

    @Value("${upload.avatar-signing-secret:${jwt.secret}}")
    private String secret;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${upload.avatar-url-exp-seconds:3600}")
    private long expSeconds;

    public String signFilename(String filename) {
        long exp = Instant.now().getEpochSecond() + expSeconds;
        String path = "/user/avatar/" + filename;
        String payload = path + "|" + exp;
        String sig = hmacSha256Hex(secret, payload);
        return contextPath + path + "?exp=" + exp + "&sig=" + sig;
    }

    public String signUserAvatarLg(String avatarKeyOrFilename) {
        if (avatarKeyOrFilename == null || avatarKeyOrFilename.isBlank()) {
            return null;
        }
        if (avatarKeyOrFilename.contains(".")) {
            return signFilename(avatarKeyOrFilename);
        }
        return signFilename(avatarKeyOrFilename + "_256.jpg");
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
