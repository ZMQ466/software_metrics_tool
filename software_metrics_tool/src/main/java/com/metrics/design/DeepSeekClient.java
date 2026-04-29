package com.metrics.design;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal DeepSeek chat client based on OpenAI-compatible API.
 */
public class DeepSeekClient {
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public DeepSeekClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.apiKey = readEnv("DEEPSEEK_API_KEY", System.getenv("DEEPSEEK_API_KEY"));
        this.baseUrl = readEnv("DEEPSEEK_BASE_URL", "https://api.deepseek.com");
        this.model = readEnv("DEEPSEEK_MODEL", "deepseek-chat");
    }

    public String analyze(String prompt) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing DEEPSEEK_API_KEY environment variable.");
        }
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        String body = "{\"model\":\"" + jsonEscape(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"你是一名资深软件架构评审专家，擅长基于软件度量做工程化改进建议。\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}"
                + "],"
                + "\"temperature\":0.3}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String snippet = response.body();
            if (snippet != null && snippet.length() > 600) {
                snippet = snippet.substring(0, 600);
            }
            throw new IOException("DeepSeek API error " + response.statusCode() + ": " + snippet);
        }
        String content = extractLastContent(response.body());
        if (content == null || content.trim().isEmpty()) {
            throw new IOException("DeepSeek API returned empty content.");
        }
        return content;
    }

    public String analyzeClassDiagramImage(Path imagePath) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing DEEPSEEK_API_KEY environment variable.");
        }
        if (imagePath == null) {
            throw new IllegalArgumentException("Image path is null.");
        }
        byte[] bytes = Files.readAllBytes(imagePath);
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Image file is empty.");
        }
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String dataUrl = "data:image/png;base64," + base64;
        String visionModel = readEnv("DEEPSEEK_VISION_MODEL", model);

        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        // Some DeepSeek-compatible gateways expect "messages=plain text + top-level images".
        String bodyImagesTopLevel = "{\"model\":\"" + jsonEscape(visionModel) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"你是UML建模助手。请把输入图片中的类图解析为可运行的PlantUML类图代码。\"},"
                + "{\"role\":\"user\",\"content\":\"请将这张类图图片转换成PlantUML类图代码。仅输出代码，不要解释。\"}"
                + "],"
                + "\"images\":[\"" + dataUrl + "\"],"
                + "\"temperature\":0.5}";

        // OpenAI-style multimodal format (fallback).
        String bodyOpenAiStyle = "{\"model\":\"" + jsonEscape(visionModel) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"你是UML建模助手。请把输入图片中的类图解析为可运行的PlantUML类图代码。\"},"
                + "{\"role\":\"user\",\"content\":["
                + "{\"type\":\"text\",\"text\":\"请将这张类图图片转换成PlantUML类图代码。仅输出代码，不要解释。\"},"
                + "{\"type\":\"image_url\",\"image_url\":{\"url\":\"" + dataUrl + "\"}}"
                + "]}"
                + "],"
                + "\"temperature\":0.5}";

        HttpResponse<String> response = sendJson(url, bodyImagesTopLevel, 120);
        if (response.statusCode() == 400 && response.body() != null
                && (response.body().contains("unknown variant `image_url`")
                || response.body().contains("expected `text`")
                || response.body().contains("invalid_request_error"))) {
            response = sendJson(url, bodyOpenAiStyle, 120);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String snippet = response.body();
            if (snippet != null && snippet.length() > 600) {
                snippet = snippet.substring(0, 600);
            }
            throw new IOException("DeepSeek API error " + response.statusCode() + ": " + snippet);
        }
        String content = extractLastContent(response.body());
        if (content == null || content.trim().isEmpty()) {
            throw new IOException("DeepSeek API returned empty content.");
        }
        return content;
    }

    private HttpResponse<String> sendJson(String url, String body, int timeoutSeconds) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String extractLastContent(String body) {
        Matcher m = CONTENT_PATTERN.matcher(body == null ? "" : body);
        String last = null;
        while (m.find()) {
            last = unescapeJson(m.group(1));
        }
        return last;
    }

    private static String jsonEscape(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case '"':
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                out.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                out.append("\\u").append(hex);
                                i += 4;
                            }
                        } else {
                            out.append("\\u");
                        }
                        break;
                    default:
                        out.append(n);
                        break;
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String readEnv(String key, String defaultValue) {
        String v = System.getenv(key);
        return v == null || v.trim().isEmpty() ? defaultValue : v.trim();
    }
}
