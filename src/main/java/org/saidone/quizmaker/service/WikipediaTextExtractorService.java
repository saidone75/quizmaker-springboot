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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikipediaTextExtractorService {

    private static final String WIKIPEDIA_HOST_SUFFIX = ".wikipedia.org";

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public WikipediaExtractionResult extractFromUrl(String input) {
        if (!StringUtils.hasText(input)) {
            return WikipediaExtractionResult.none();
        }

        val trimmedInput = input.trim();
        URI uri;
        try {
            uri = URI.create(trimmedInput);
        } catch (IllegalArgumentException ex) {
            return WikipediaExtractionResult.none();
        }

        val host = uri.getHost();
        if (!isWikipediaHost(host)) {
            return WikipediaExtractionResult.none();
        }

        val title = extractTitle(uri.getPath());
        if (!StringUtils.hasText(title)) {
            return WikipediaExtractionResult.none();
        }

        try {
            val apiUrl = buildApiUrl(host.toLowerCase(Locale.ROOT), title);
            val payload = restClient.get()
                    .uri(apiUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            val extractedText = extractPageText(payload);
            if (!StringUtils.hasText(extractedText)) {
                log.warn("Wikipedia URL valida ma testo non trovato: {}", trimmedInput);
                return WikipediaExtractionResult.none();
            }

            return new WikipediaExtractionResult(true, normalizeTopicFromTitle(title), extractedText);
        } catch (Exception ex) {
            log.warn("Impossibile estrarre testo da Wikipedia URL: {}", trimmedInput, ex);
            return WikipediaExtractionResult.none();
        }
    }

    private boolean isWikipediaHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }

        val normalizedHost = host.toLowerCase(Locale.ROOT);
        return normalizedHost.endsWith(WIKIPEDIA_HOST_SUFFIX);
    }

    private String extractTitle(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }

        val wikiPrefix = "/wiki/";
        if (!path.startsWith(wikiPrefix) || path.length() <= wikiPrefix.length()) {
            return null;
        }

        return URLDecoder.decode(path.substring(wikiPrefix.length()), StandardCharsets.UTF_8);
    }

    private String buildApiUrl(String host, String title) {
        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        return "https://%s/w/api.php?action=query&format=json&prop=extracts&explaintext=1&redirects=1&titles=%s"
                .formatted(host, encodedTitle);
    }

    private String extractPageText(String rawJson) throws Exception {
        if (!StringUtils.hasText(rawJson)) {
            return null;
        }

        val root = objectMapper.readTree(rawJson);
        val pages = root.path("query").path("pages");
        if (!pages.isObject()) {
            return null;
        }

        Iterator<JsonNode> iterator = pages.elements();
        if (!iterator.hasNext()) {
            return null;
        }

        return iterator.next().path("extract").asText(null);
    }

    private String normalizeTopicFromTitle(String title) {
        return title.replace('_', ' ');
    }

    public record WikipediaExtractionResult(boolean extracted, String topic, String text) {
        static WikipediaExtractionResult none() {
            return new WikipediaExtractionResult(false, null, null);
        }
    }
}
