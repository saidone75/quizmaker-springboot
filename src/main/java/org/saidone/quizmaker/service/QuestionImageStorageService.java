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

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.tika.Tika;
import org.saidone.quizmaker.dto.QuestionImageUploadDto;
import org.saidone.quizmaker.entity.UploadedImage;
import org.saidone.quizmaker.repository.UploadedImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionImageStorageService {

    private static final Tika TIKA_DETECTOR = new Tika();

    private final UploadedImageRepository uploadedImageRepository;

    @Value("${app.upload.directory:./upload}")
    private String uploadDirectory;

    @Transactional
    public QuestionImageUploadDto store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Seleziona un file immagine da caricare.");
        }
        if (!isImage(file)) {
            throw new IllegalArgumentException("Il file selezionato non è un'immagine valida.");
        }

        try {
            val baseDir = Path.of(uploadDirectory).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);

            val imageId = UUID.randomUUID();
            val extension = extractExtension(file.getOriginalFilename());
            val fileName = imageId + extension;
            val destination = baseDir.resolve(fileName).normalize();

            if (!destination.startsWith(baseDir)) {
                throw new IllegalStateException("Percorso di destinazione non valido.");
            }

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            val image = new UploadedImage();
            image.setId(imageId);
            image.setFilePath(destination.toString());
            uploadedImageRepository.save(image);

            return QuestionImageUploadDto.builder()
                    .id(imageId)
                    .url(imageUrl(imageId))
                    .build();
        } catch (IOException exception) {
            throw new IllegalStateException("Errore durante il salvataggio del file immagine.", exception);
        }
    }

    @Transactional(readOnly = true)
    public Resource load(UUID imageId) {
        val image = uploadedImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Immagine non trovata: " + imageId));

        try {
            val resource = new UrlResource(Path.of(image.getFilePath()).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new EntityNotFoundException("File immagine non disponibile: " + imageId);
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Percorso file immagine non valido.", exception);
        }
    }

    @Transactional(readOnly = true)
    public MediaType resolveMediaType(UUID imageId) {
        val image = uploadedImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Immagine non trovata: " + imageId));

        try {
            val contentType = Files.probeContentType(Path.of(image.getFilePath()));
            if (contentType == null || contentType.isBlank()) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
            return MediaType.parseMediaType(contentType);
        } catch (IOException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @Transactional
    public void delete(UUID imageId) {
        val image = uploadedImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Immagine non trovata: " + imageId));

        try {
            Files.deleteIfExists(Path.of(image.getFilePath()));
        } catch (IOException exception) {
            throw new IllegalStateException("Errore durante la rimozione del file immagine.", exception);
        }

        uploadedImageRepository.delete(image);
    }

    public String imageUrl(UUID imageId) {
        return "/api/quizzes/images/" + imageId;
    }

    private boolean isImage(MultipartFile file) {
        if (!hasAllowedTypeHint(file)) {
            return false;
        }
        return hasAllowedImageSignature(file);
    }

    private boolean hasAllowedTypeHint(MultipartFile file) {
        val contentType = file.getContentType();
        if (contentType != null && isAllowedContentType(contentType)) {
            return true;
        }

        val extension = extractExtension(file.getOriginalFilename());
        return extension.equals(".png")
                || extension.equals(".jpg")
                || extension.equals(".jpeg")
                || extension.equals(".gif")
                || extension.equals(".webp")
                || extension.equals(".bmp");
    }

    private boolean isAllowedContentType(String contentType) {
        val normalizedContentType = contentType.toLowerCase(Locale.ROOT).trim();
        return normalizedContentType.equals("image/png")
                || normalizedContentType.equals("image/jpeg")
                || normalizedContentType.equals("image/gif")
                || normalizedContentType.equals("image/webp")
                || normalizedContentType.equals("image/bmp")
                || normalizedContentType.equals("image/x-ms-bmp");
    }

    private boolean hasAllowedImageSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            val signature = inputStream.readNBytes(16384);
            val detectedContentType = TIKA_DETECTOR.detect(signature, file.getOriginalFilename());
            return isAllowedContentType(detectedContentType);
        } catch (IOException ignored) {
            return false;
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        val dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
