/*
 * QuizMaker - fun quizzes for curious minds
 * Copyright (C) 2026 Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.quizmaker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.quizmaker.dto.QuizDto;
import org.saidone.quizmaker.dto.QuizGenerationRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiQuizGeneratorService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().baseUrl("https://api.openai.com/v1").build();

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.model:gpt-5.4-mini}")
    private String model;

    @Value("${app.openai.max-attachment-chars:30000}")
    private int maxAttachmentChars;

    public QuizDto.Request generateQuiz(QuizGenerationRequestDto request, String attachmentText) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Chiave API di OpenAI non configurata. Imposta app.openai.api-key.");
        }

        val payload = buildPayload(request, attachmentText);
        val responseBody = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        try {
            val root = objectMapper.readTree(responseBody);
            val rawJson = root.path("choices").path(0).path("message").path("content").asText();
            val generated = objectMapper.readValue(rawJson, QuizDto.Request.class);
            sanitize(generated, request.getNumberOfQuestions());
            return generated;
        } catch (Exception e) {
            log.error("Risposta OpenAI non valida: {}", responseBody, e);
            throw new IllegalStateException("La risposta di OpenAI non è valida o è incompleta.");
        }
    }

    private Map<String, Object> buildPayload(QuizGenerationRequestDto request, String attachmentText) {
        val userPrompt = """
                Crea un quiz in italiano e rispondi SOLO con JSON valido compatibile con QuizDto.Request.
                Campi obbligatori: title (string), emoji (string), questions (array).
                Ogni question deve avere: text, emoji, options (4 risposte), answer (indice corretto 0-3), feedback.
                
                Vincoli:
                - Argomento: %s
                - Numero domande: %d
                - Difficoltà: %s
                - Tono: %s
                - Evita domande duplicate.
                - Le risposte devono essere chiare e plausibili.
                - answer deve sempre puntare a un indice valido
                - feedback deve contenere una curiosità sulla risposta corretta
                
                Testo di riferimento allegato (se presente):
                %s
                """.formatted(
                request.getTopic(),
                request.getNumberOfQuestions(),
                request.getDifficulty(),
                request.getTone(),
                truncateAttachmentText(attachmentText)
        );

        Map<String, Object> responseFormat = Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "quiz_dto_request",
                        "strict", true,
                        "schema", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "required", List.of("title", "emoji", "questions"),
                                "properties", Map.of(
                                        "title", Map.of("type", "string"),
                                        "emoji", Map.of("type", "string"),
                                        "questions", Map.of(
                                                "type", "array",
                                                "minItems", 1,
                                                "items", Map.of(
                                                        "type", "object",
                                                        "additionalProperties", false,
                                                        "required", List.of("text", "emoji", "options", "answer", "feedback"),
                                                        "properties", Map.of(
                                                                "text", Map.of("type", "string"),
                                                                "emoji", Map.of("type", "string"),
                                                                "options", Map.of("type", "array", "minItems", 4, "maxItems", 4, "items", Map.of("type", "string")),
                                                                "answer", Map.of("type", "integer", "minimum", 0, "maximum", 3),
                                                                "feedback", Map.of("type", "string")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        val payload = new HashMap<String, Object>();
        payload.put("model", model);
        payload.put("response_format", responseFormat);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", "Sei un assistente che crea quiz didattici accurati in italiano."),
                Map.of("role", "user", "content", userPrompt)
        ));
        payload.put("temperature", 0.7);
        return payload;
    }

    private String truncateAttachmentText(String attachmentText) {
        if (!StringUtils.hasText(attachmentText)) {
            return "N/A";
        }
        int effectiveMaxAttachmentChars = maxAttachmentChars > 0 ? maxAttachmentChars : 30000;
        return attachmentText.substring(0, Math.min(effectiveMaxAttachmentChars, attachmentText.length()));
    }

    private void sanitize(QuizDto.Request generated, int maxQuestions) {
        if (generated == null || generated.getQuestions() == null || generated.getQuestions().isEmpty()) {
            throw new IllegalStateException("L'IA non ha generato domande valide.");
        }
        if (!StringUtils.hasText(generated.getTitle())) {
            generated.setTitle("Quiz generato dall'IA");
        }
        if (!StringUtils.hasText(generated.getEmoji())) {
            generated.setEmoji("🤖");
        }
        if (generated.getQuestions().size() > maxQuestions) {
            generated.setQuestions(generated.getQuestions().subList(0, maxQuestions));
        }
    }
}
