package com.master.excel.parser.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String askGemini(String prompt, String callType) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String raw = rest.postForObject(url, body, String.class);

//        System.out.println("Raw Output of Gemini: " + raw);

        if ("Bundle".equals(callType)) {
            return extractJson(raw);
        }

        return extractText(raw);

    }

    private String extractJson(String apiResponse) {
        try {
            JsonNode node = mapper.readTree(apiResponse);
            String text = node
                    .get("candidates")
                    .get(0)
                    .get("content")
                    .get("parts")
                    .get(0)
                    .get("text")
                    .asText();

            String cleaned = text.trim();

            // Strip ```json ... ``` if present
            if (cleaned.startsWith("```")) {
                int firstNewline = cleaned.indexOf('\n');
                if (firstNewline != -1) {
                    cleaned = cleaned.substring(firstNewline + 1);
                }
                int lastFence = cleaned.lastIndexOf("```");
                if (lastFence != -1) {
                    cleaned = cleaned.substring(0, lastFence);
                }
                cleaned = cleaned.trim();
            }

            // As extra safety, extract only the JSON array part
            int startIdx = cleaned.indexOf('[');
            int endIdx   = cleaned.lastIndexOf(']');
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                cleaned = cleaned.substring(startIdx, endIdx + 1);
            }

            return cleaned;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract JSON from Gemini response", e);
        }
    }


    private String extractText(String json) {
        try {
            JsonNode node = new ObjectMapper().readTree(json);

            return node
                    .get("candidates")
                    .get(0)
                    .get("content")
                    .get("parts")
                    .get(0)
                    .get("text")
                    .asText()
                    .trim();
        } catch (Exception e) {
            return "#N/A";
        }
    }

}
