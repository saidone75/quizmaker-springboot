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
import org.saidone.quizmaker.dto.QuestionImageUploadDto;
import org.saidone.quizmaker.entity.UploadedImage;
import org.saidone.quizmaker.repository.UploadedImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionImageStorageService {

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
            val extension = extractExtension(file.getOriginalFilename());
            return storeBytes(file.getInputStream().readAllBytes(), extension);
        } catch (IOException exception) {
            throw new IllegalStateException("Errore durante il salvataggio del file immagine.", exception);
        }
    }


    @Transactional
    public QuestionImageUploadDto storeFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Inserisci un URL immagine valido.");
        }

        val normalizedUrl = imageUrl.trim();
        val imageUri = URI.create(normalizedUrl);
        val scheme = imageUri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Sono accettati solo URL http/https.");
        }

        try {
            val response = RestClient.create()
                    .get()
                    .uri(imageUri)
                    .retrieve()
                    .toEntity(byte[].class);

            val imageBytes = response.getBody();
            if (imageBytes == null || imageBytes.length == 0) {
                throw new IllegalArgumentException("Impossibile scaricare l'immagine indicata.");
            }

            val contentType = response.getHeaders().getContentType();
            if (contentType == null || !contentType.getType().equalsIgnoreCase("image")) {
                throw new IllegalArgumentException("L'URL fornito non punta a un'immagine valida.");
            }
            val extension = extensionFromContentType(contentType.toString());
            return storeBytes(imageBytes, extension);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Errore durante il download dell'immagine da URL.", exception);
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


    private QuestionImageUploadDto storeBytes(byte[] imageBytes, String extension) {
        try {
            val baseDir = Path.of(uploadDirectory).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);

            val imageId = UUID.randomUUID();
            val sanitizedExtension = (extension == null || extension.isBlank()) ? ".img" : extension;
            val fileName = imageId + sanitizedExtension;
            val destination = baseDir.resolve(fileName).normalize();

            if (!destination.startsWith(baseDir)) {
                throw new IllegalStateException("Percorso di destinazione non valido.");
            }

            Files.write(destination, imageBytes);

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

    private String extensionFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return ".img";
        }
        val normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        return switch (normalized) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            case "image/svg+xml" -> ".svg";
            default -> ".img";
        };
    }

    private boolean isImage(MultipartFile file) {
        val contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        val extension = extractExtension(file.getOriginalFilename());
        return extension.equals(".png")
                || extension.equals(".jpg")
                || extension.equals(".jpeg")
                || extension.equals(".gif")
                || extension.equals(".webp")
                || extension.equals(".bmp")
                || extension.equals(".svg");
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
