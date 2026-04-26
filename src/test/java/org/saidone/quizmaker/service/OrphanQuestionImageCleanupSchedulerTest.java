/*
 * Alice's Simple Quiz Maker - fun quizzes for curious minds
 * Copyright (C) 2026 Miss Alice & Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.quizmaker.service;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.saidone.quizmaker.entity.Question;
import org.saidone.quizmaker.entity.Quiz;
import org.saidone.quizmaker.entity.UploadedImage;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.UploadedImageRepository;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrphanQuestionImageCleanupSchedulerTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private UploadedImageRepository uploadedImageRepository;

    @Mock
    private QuestionImageStorageService questionImageStorageService;

    @InjectMocks
    private OrphanQuestionImageCleanupScheduler cleanupScheduler;

    @Test
    void cleanupOrphanImages_deletesOnlyUnreferencedImages() {
        val usedImageId = UUID.randomUUID();
        val orphanImageId = UUID.randomUUID();

        val question = new Question();
        question.setImageId(usedImageId.toString());

        val quiz = Quiz.builder()
                .id(UUID.randomUUID())
                .title("Quiz")
                .emoji("🧪")
                .questions(List.of(question))
                .published(true)
                .archived(false)
                .build();

        val usedImage = new UploadedImage();
        usedImage.setId(usedImageId);
        usedImage.setFilePath("/tmp/used.png");

        val orphanImage = new UploadedImage();
        orphanImage.setId(orphanImageId);
        orphanImage.setFilePath("/tmp/orphan.png");

        when(quizRepository.findAll()).thenReturn(List.of(quiz));
        when(uploadedImageRepository.findAll()).thenReturn(List.of(usedImage, orphanImage));

        cleanupScheduler.cleanupOrphanImages();

        verify(questionImageStorageService, times(1)).delete(orphanImageId);
        verify(questionImageStorageService, never()).delete(usedImageId);
    }

    @Test
    void cleanupOrphanImages_ignoresInvalidImageIdsAndDeletesStoredImages() {
        val question = new Question();
        question.setImageId("not-a-uuid");

        val quiz = Quiz.builder()
                .id(UUID.randomUUID())
                .title("Quiz")
                .emoji("🧪")
                .questions(List.of(question))
                .published(true)
                .archived(false)
                .build();

        val storedImage = new UploadedImage();
        storedImage.setId(UUID.randomUUID());
        storedImage.setFilePath("/tmp/orphan.png");

        when(quizRepository.findAll()).thenReturn(List.of(quiz));
        when(uploadedImageRepository.findAll()).thenReturn(List.of(storedImage));

        cleanupScheduler.cleanupOrphanImages();

        verify(questionImageStorageService, times(1)).delete(storedImage.getId());
    }
}
