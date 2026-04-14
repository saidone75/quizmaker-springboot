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

import lombok.val;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Service
public class WikimediaResolver {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String getValidImageUrl(String fakeUrl) {
        try {
            val fileName = extractFileName(fakeUrl);
            if (fileName == null) return "Errore: Formato URL non valido";
            val apiQuery = String.format("https://commons.wikimedia.org/w/api.php?action=query&prop=imageinfo&iiprop=url&format=json&titles=File:%s",
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            val request = HttpRequest.newBuilder()
                    .uri(URI.create(apiQuery))
                    .header("User-Agent", "QuizMakerBot/1.0")
                    .build();
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseUrlFromJson(response.body());
        } catch (Exception e) {
            return "Errore durante il recupero: " + e.getMessage();
        }
    }

    private String extractFileName(String url) {
        val parts = url.split("/");
        return parts[parts.length - 1];
    }

    private String parseUrlFromJson(String json) {
        val pattern = Pattern.compile("\"url\":\"(https://upload\\.wikimedia\\.org/[^\"]+)\"");
        val matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Immagine non trovata su Wikimedia";
    }

}