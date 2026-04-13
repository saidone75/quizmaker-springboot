/*
 * Alice's Simple Quiz Maker - fun quizzes for curious minds
 * Copyright (C) 2026 Miss Alice & Saidone
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.UploadedImageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.image-cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrphanQuestionImageCleanupScheduler {

    private final QuizRepository quizRepository;
    private final UploadedImageRepository uploadedImageRepository;
    private final QuestionImageStorageService questionImageStorageService;

    @Scheduled(cron = "${app.image-cleanup.cron:0 0 3 * * *}")
    public void cleanupOrphanImages() {
        try {
            val usedImageIds = new HashSet<UUID>();

            quizRepository.findAll().forEach(quiz -> {
                if (quiz.getQuestions() == null) {
                    return;
                }
                for (val question : quiz.getQuestions()) {
                    if (question == null || question.getImageId() == null || question.getImageId().isBlank()) {
                        continue;
                    }
                    parseUuid(question.getImageId()).ifPresent(usedImageIds::add);
                }
            });

            var deletedCount = 0;
            for (val image : uploadedImageRepository.findAll()) {
                if (usedImageIds.contains(image.getId())) {
                    continue;
                }
                questionImageStorageService.delete(image.getId());
                deletedCount++;
            }

            log.info("Pulizia immagini orfane completata. Immagini referenziate: {}, immagini eliminate: {}",
                    usedImageIds.size(), deletedCount);
        } catch (Exception ex) {
            log.error("Pulizia immagini orfane fallita.", ex);
        }
    }

    private java.util.Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            log.debug("Image ID non valido ignorato durante la pulizia immagini orfane: {}", value);
            return Optional.empty();
        }
    }
}
