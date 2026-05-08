package com.songshanhu.blog.controller;

import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "健康检查", description = "服务健康状态与依赖可用性检查")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    @Value("${server.port:8080}")
    private Integer port;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${upload.avatar-signing-secret:}")
    private String avatarSigningSecret;

    @Value("${dashscope.api-key:${DASHSCOPE_API_KEY:}}")
    private String dashscopeApiKey;

    @Value("${openai.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    @GetMapping
    @Operation(summary = "健康检查（DB/Redis/Schema）")
    public ResponseEntity<Result<Map<String, Object>>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", Instant.now().toString());

        Map<String, Object> app = new HashMap<>();
        app.put("port", port);
        app.put("contextPath", contextPath);
        app.put("avatarSigningConfigured", avatarSigningSecret != null && !avatarSigningSecret.isBlank());
        app.put("aiConfigured", (dashscopeApiKey != null && !dashscopeApiKey.isBlank()) || (openaiApiKey != null && !openaiApiKey.isBlank()));
        app.put("dashscopeConfigured", dashscopeApiKey != null && !dashscopeApiKey.isBlank());
        app.put("openaiConfigured", openaiApiKey != null && !openaiApiKey.isBlank());
        data.put("app", app);

        Map<String, Object> checks = new HashMap<>();
        boolean dbUp = checkDb();
        boolean redisUp = checkRedis();
        checks.put("db", dbUp ? "UP" : "DOWN");
        checks.put("redis", redisUp ? "UP" : "DOWN");

        Map<String, Object> schemaCheck = checkSchema();
        checks.put("schema", schemaCheck.get("ok").equals(Boolean.TRUE) ? "UP" : "DOWN");
        if (schemaCheck.containsKey("missing")) {
            checks.put("missing", schemaCheck.get("missing"));
        }
        data.put("checks", checks);

        boolean ok = dbUp && redisUp && schemaCheck.get("ok").equals(Boolean.TRUE);
        data.put("status", ok ? "UP" : "DOWN");

        if (ok) {
            return ResponseEntity.ok(Result.success(data));
        }
        return ResponseEntity.status(503).body(Result.success(data));
    }

    private boolean checkDb() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
            ps.execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return pong != null && !pong.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> checkSchema() {
        Map<String, Object> out = new HashMap<>();
        List<String> missing = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            String catalog = conn.getCatalog();
            DatabaseMetaData meta = conn.getMetaData();

            requireColumn(meta, catalog, "user", "gender", missing);
            requireColumn(meta, catalog, "user", "bio", missing);
            requireColumn(meta, catalog, "user", "extra", missing);

            requireColumn(meta, catalog, "article", "longitude", missing);
            requireColumn(meta, catalog, "article", "latitude", missing);

            out.put("ok", missing.isEmpty());
            if (!missing.isEmpty()) {
                out.put("missing", missing);
            }
            return out;
        } catch (Exception e) {
            out.put("ok", false);
            out.put("missing", List.of("schema-check-failed"));
            return out;
        }
    }

    private void requireColumn(DatabaseMetaData meta, String catalog, String table, String column, List<String> missing) throws Exception {
        try (ResultSet rs = meta.getColumns(catalog, null, table, column)) {
            if (!rs.next()) {
                missing.add(table + "." + column);
            }
        }
    }
}
