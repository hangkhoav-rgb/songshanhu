package com.songshanhu.blog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI", description = "通义千问摘要")
public class AiController {

    private final ObjectMapper objectMapper;

    @Value("${dashscope.api-key:${DASHSCOPE_API_KEY:}}")
    private String dashscopeApiKey;

    @Value("${dashscope.model:qwen-turbo}")
    private String model;

    @Value("${dashscope.endpoint:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String endpoint;

    @Value("${openai.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${openai.endpoint:https://api.openai.com/v1/responses}")
    private String openaiEndpoint;

    public AiController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static class SummarizeReq {
        public String title;
        public String content;
    }

    @PostMapping("/summary")
    @Operation(summary = "生成文章摘要（通义千问）")
    public Result<Map<String, String>> summarize(@RequestBody SummarizeReq req) {
        if (!StringUtils.hasText(req.content)) {
            return Result.error("内容不能为空");
        }
        String prompt = buildPrompt(req.title, req.content);
        try {
            if (StringUtils.hasText(dashscopeApiKey)) {
                return summarizeWithDashscope(prompt);
            }
            if (StringUtils.hasText(openaiApiKey)) {
                return summarizeWithOpenAI(prompt);
            }
            return Result.error("未配置 AI Key，请设置 DASHSCOPE_API_KEY 或 OPENAI_API_KEY");
        } catch (Exception e) {
            return Result.error("AI 摘要生成失败: " + e.getMessage());
        }
    }

    private Result<Map<String, String>> summarizeWithDashscope(String prompt) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();

        Map<String, Object> body = Map.of(
                "model", model,
                "input", Map.of(
                        "messages", List.of(Map.of(
                                "role", "user",
                                "content", prompt
                        ))
                ),
                "parameters", Map.of(
                        "result_format", "message",
                        "temperature", 0.6,
                        "max_tokens", 256
                )
        );

        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(18))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + dashscopeApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            return Result.error("AI 摘要服务异常（HTTP " + resp.statusCode() + "）");
        }
        JsonNode root = objectMapper.readTree(resp.body());
        String summary = root.path("output").path("choices").path(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(summary)) {
            return Result.error("AI 未返回摘要");
        }
        return Result.success(Map.of("summary", summary.trim()));
    }

    private Result<Map<String, String>> summarizeWithOpenAI(String prompt) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();

        Map<String, Object> body = Map.of(
                "model", openaiModel,
                "input", prompt,
                "max_output_tokens", 256
        );
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(openaiEndpoint))
                .timeout(Duration.ofSeconds(18))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            return Result.error("AI 摘要服务异常（HTTP " + resp.statusCode() + "）");
        }

        JsonNode root = objectMapper.readTree(resp.body());
        String summary = extractOpenAIText(root);
        if (!StringUtils.hasText(summary)) {
            return Result.error("AI 未返回摘要");
        }
        return Result.success(Map.of("summary", summary.trim()));
    }

    private String extractOpenAIText(JsonNode root) {
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode c : content) {
                        String text = c.path("text").asText("");
                        if (StringUtils.hasText(text)) {
                            return text;
                        }
                    }
                }
            }
        }
        return root.path("output_text").asText("");
    }

    private String buildPrompt(String title, String content) {
        String safeTitle = StringUtils.hasText(title) ? title.trim() : "";
        String safeContent = content.trim();
        if (safeContent.length() > 8000) {
            safeContent = safeContent.substring(0, 8000);
        }
        String header = "请为以下文章生成一段中文摘要（100-160字），要求客观精炼，保留关键信息，不要使用列表，不要出现\"摘要\"二字，不要添加与原文无关的内容。";
        if (StringUtils.hasText(safeTitle)) {
            return header + "\n\n标题：" + safeTitle + "\n\n正文：\n" + safeContent;
        }
        return header + "\n\n正文：\n" + safeContent;
    }
}
