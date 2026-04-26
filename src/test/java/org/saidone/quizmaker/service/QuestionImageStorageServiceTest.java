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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.saidone.quizmaker.repository.UploadedImageRepository;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionImageStorageServiceTest {

    @Mock
    private UploadedImageRepository uploadedImageRepository;

    @TempDir
    Path tempDir;

    private QuestionImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new QuestionImageStorageService(uploadedImageRepository);
        ReflectionTestUtils.setField(service, "uploadDirectory", tempDir.toString());
    }

    @Test
    void store_rejectsSvgUpload() {
        val file = new MockMultipartFile(
                "file",
                "dangerous.svg",
                "image/svg+xml",
                "<svg><script>alert('xss')</script></svg>".getBytes()
        );

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Il file selezionato non è un'immagine valida.");
        verify(uploadedImageRepository, never()).save(any());
    }

    @Test
    void store_acceptsPngUpload() {
        val file = new MockMultipartFile(
                "file",
                "safe.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3}
        );
        when(uploadedImageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        val result = service.store(file);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getUrl()).isEqualTo("/api/quizzes/images/" + result.getId());
        verify(uploadedImageRepository).save(any());
    }

    @Test
    void store_rejectsSpoofedPngWithSvgPayload() {
        val file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "<svg><script>alert('xss')</script></svg>".getBytes()
        );

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Il file selezionato non è un'immagine valida.");
        verify(uploadedImageRepository, never()).save(any());
    }
}
