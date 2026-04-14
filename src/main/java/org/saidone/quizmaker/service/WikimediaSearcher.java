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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
@Slf4j
public class WikimediaSearcher {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient wikimediaRestClient;

    public WikimediaSearcher(@Qualifier("wikimediaRestClient") RestClient wikimediaRestClient) {
        this.wikimediaRestClient = wikimediaRestClient;
    }

    public String searchImage(String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return null;
        }

        val normalizedKeywords = Arrays.stream(keywords)
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::trim)
                .toArray(String[]::new);

        if (normalizedKeywords.length == 0) {
            return null;
        }

        log.debug("Ricerca immagine per: {}", String.join(", ", normalizedKeywords));

        try {
            // 1) Cerco un file su Wikimedia Commons che combaci con le keyword.
            val queryTerms = String.join(" OR ", normalizedKeywords);
            val searchRoot = fetchData(buildSearchQuery(queryTerms));
            val searchResults = searchRoot.path("query").path("search");

            if (searchResults.isEmpty()) {
                return null;
            }

            // 2) Dal primo risultato estraggo il titolo del file e chiedo l'URL dell'immagine.
            val fileTitle = searchResults.get(0).path("title").asText();
            val infoRoot = fetchData(buildImageInfoQuery(fileTitle));
            val pages = infoRoot.path("query").path("pages");

            if (pages.isEmpty()) {
                return null;
            }

            val firstPage = pages.elements().next();
            val imageInfo = firstPage.path("imageinfo");

            if (imageInfo.isEmpty()) {
                return null;
            }

            return imageInfo.get(0).path("url").asText();
        } catch (Exception e) {
            log.warn("Errore durante la ricerca dell'immagine Wikimedia", e);
            return null;
        }
    }

    private String buildSearchQuery(String queryTerms) {
        return String.format(
                "?action=query&list=search&srsearch=%s&srnamespace=6&format=json&srlimit=1",
                URLEncoder.encode(queryTerms, StandardCharsets.UTF_8)
        );
    }

    private String buildImageInfoQuery(String fileTitle) {
        return String.format(
                "?action=query&titles=%s&prop=imageinfo&iiprop=url&format=json",
                URLEncoder.encode(fileTitle, StandardCharsets.UTF_8)
        );
    }

    private JsonNode fetchData(String queryString) {
        val responseBody = wikimediaRestClient.get()
                .uri(queryString)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return JsonNodeFactory.instance.objectNode();
        }

        try {
            return OBJECT_MAPPER.readTree(responseBody);
        } catch (Exception e) {
            log.warn("Impossibile leggere la risposta JSON Wikimedia", e);
            return JsonNodeFactory.instance.objectNode();
        }
    }

}
