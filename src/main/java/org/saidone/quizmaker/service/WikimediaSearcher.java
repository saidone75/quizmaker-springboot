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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikimediaSearcher {

    @Qualifier("wikimediaRestClient")
    private final RestClient wikimediaRestClient;
    private final ObjectMapper objectMapper;

    public String searchImage(String[] keywords) {
        // Nessuna keyword => nessuna ricerca
        if (keywords == null || keywords.length == 0) {
            return null;
        }

        // Normalizza l'input eliminando valori vuoti e spazi superflui
        val normalizedKeywords = Arrays.stream(keywords)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toArray(String[]::new);

        if (normalizedKeywords.length == 0) {
            return null;
        }

        log.debug("Ricerca immagine per: {}", String.join(", ", keywords));

        try {
            // 1) Cerca il primo file immagine su Wikimedia Commons
            val queryTerms = String.join(" OR ", normalizedKeywords);
            val searchRoot = wikimediaRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("action", "query")
                            .queryParam("list", "search")
                            .queryParam("srsearch", queryTerms)
                            .queryParam("srnamespace", "6")
                            .queryParam("format", "json")
                            .queryParam("srlimit", "1")
                            .build())
                    .retrieve()
                    .body(String.class);

            val searchRootNode = objectMapper.readTree(searchRoot);
            val searchResults = searchRootNode.path("query").path("search");

            if (searchResults.isEmpty()) {
                return null;
            }

            val fileTitle = searchResults.get(0).path("title").asText();

            // 2) Recupera i dettagli (URL) dell'immagine trovata
            val infoRoot = wikimediaRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("action", "query")
                            .queryParam("titles", fileTitle)
                            .queryParam("prop", "imageinfo")
                            .queryParam("iiprop", "url")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(String.class);

            val infoRootNode = objectMapper.readTree(infoRoot);
            val pages = infoRootNode.path("query").path("pages");

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
            log.warn("Errore durante la ricerca immagine su Wikimedia: {}", e.getMessage());
            return null;
        }
    }

}
