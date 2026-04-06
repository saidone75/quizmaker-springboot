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

package org.saidone.quizmaker.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.quizmaker.dto.QuestionDto;
import org.saidone.quizmaker.dto.QuizDto;
import org.saidone.quizmaker.dto.QuizGenerationRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGenerationApplicationService {

    private final QuizGenerationService quizGenerationService;

    @Value("${app.ai.generation.max-questions:20}")
    private int maxQuestionsPerRequest;

    @Value("${app.ai.generation.max-attachment-chars:60000}")
    private int maxAttachmentChars;

    @Value("${app.ai.generation.max-attempts:2}")
    private int maxAttempts;

    @Value("${app.ai.generation.fallback-enabled:true}")
    private boolean fallbackEnabled;

    public QuizDto.Request generateQuiz(QuizGenerationRequestDto request, String attachmentText) {
        validateRequestLimits(request);

        val safeAttachmentText = truncateAttachmentText(attachmentText);
        val attempts = Math.max(1, maxAttempts);

        RuntimeException lastError = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                val generated = quizGenerationService.generateQuiz(request, safeAttachmentText);
                sanitize(generated, request.getNumberOfQuestions());
                return generated;
            } catch (RuntimeException ex) {
                lastError = ex;
                log.warn("Tentativo generazione quiz {} di {} fallito", i, attempts, ex);
            }
        }

        if (fallbackEnabled) {
            log.warn("Uso fallback locale per la generazione quiz dopo {} tentativi falliti", attempts);
            return buildFallbackQuiz(request);
        }

        throw new IllegalStateException("Impossibile generare il quiz con l'AI al momento.", lastError);
    }

    private void validateRequestLimits(QuizGenerationRequestDto request) {
        if (request == null || request.getNumberOfQuestions() == null) {
            throw new IllegalStateException("Richiesta di generazione non valida.");
        }

        int effectiveMaxQuestions = Math.max(1, maxQuestionsPerRequest);
        if (request.getNumberOfQuestions() > effectiveMaxQuestions) {
            throw new IllegalStateException(String.format("Numero massimo domande superato. Limite: %d", effectiveMaxQuestions));
        }
    }

    private String truncateAttachmentText(String attachmentText) {
        if (!StringUtils.hasText(attachmentText)) {
            return null;
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

        generated.setQuestions(generated.getQuestions().stream()
                .filter(this::hasValidQuestion)
                .limit(maxQuestions)
                .toList());

        if (generated.getQuestions().isEmpty()) {
            throw new IllegalStateException("L'IA non ha generato domande utilizzabili.");
        }
    }

    private boolean hasValidQuestion(QuestionDto question) {
        return question != null
                && StringUtils.hasText(question.getText())
                && StringUtils.hasText(question.getEmoji())
                && StringUtils.hasText(question.getFeedback())
                && question.getOptions() != null
                && question.getOptions().size() == 4
                && question.getOptions().stream().allMatch(StringUtils::hasText)
                && question.getAnswer() != null
                && question.getAnswer() >= 0
                && question.getAnswer() < 4;
    }

    private QuizDto.Request buildFallbackQuiz(QuizGenerationRequestDto request) {
        int questionsToBuild = Math.max(1, request.getNumberOfQuestions());
        val fallbackQuestions = java.util.stream.IntStream.range(0, questionsToBuild)
                .mapToObj(i -> buildFallbackQuestion(i, request.getTopic()))
                .toList();

        return QuizDto.Request.builder()
                .title("Quiz provvisorio: " + request.getTopic())
                .emoji("⚠️")
                .questions(fallbackQuestions)
                .build();
    }

    private QuestionDto buildFallbackQuestion(int index, String topic) {
        val question = new QuestionDto();
        question.setText("Domanda %d su %s".formatted(index + 1, topic));
        question.setEmoji("🧠");
        question.setOptions(List.of("Opzione A", "Opzione B", "Opzione C", "Opzione D"));
        question.setAnswer(0);
        question.setFeedback("Questa è una domanda provvisoria. Rigenera il quiz per ottenere contenuti AI reali.");
        return question;
    }
}
