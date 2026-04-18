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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikimediaSimpleImageSearchService implements WikimediaImageSearchService {

    private static final int MAX_KEYWORDS = 4;
    private static final Set<String> ALLOWED_WEB_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml"
    );
    private static final Set<String> ALLOWED_WEB_IMAGE_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".gif",
            ".webp",
            ".svg"
    );

    @Qualifier("wikimediaRestClient")
    private final RestClient wikimediaRestClient;
    private final ObjectMapper objectMapper;

    public String searchImage(String[] keywords) {
        if (!hasKeywords(keywords)) {
            return null;
        }

        val normalizedKeywords = Arrays.stream(keywords)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(MAX_KEYWORDS)
                .toArray(String[]::new);

        if (normalizedKeywords.length == 0) {
            return null;
        }

        log.debug("Ricerca immagine per: {}", String.join(", ", keywords));

        try {
            for (val queryTerms : buildSearchQueries(normalizedKeywords)) {
                val imageUrl = searchFirstMatchingImageUrl(queryTerms);
                if (imageUrl != null) {
                    return imageUrl;
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Errore durante la ricerca immagine su Wikimedia: {}", e.getMessage());
            return null;
        }
    }

    private boolean hasKeywords(String[] keywords) {
        return keywords != null && keywords.length > 0;
    }

    private String searchFirstMatchingImageUrl(String queryTerms) throws Exception {
        val searchRoot = wikimediaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("action", "query")
                        .queryParam("list", "search")
                        .queryParam("srsearch", queryTerms)
                        .queryParam("srnamespace", "6")
                        .queryParam("format", "json")
                        .queryParam("srlimit", "10")
                        .build())
                .retrieve()
                .body(String.class);

        val searchResponse = objectMapper.readValue(searchRoot, SearchApiResponse.class);
        for (val result : searchResponse.searchResults()) {
            val imageUrl = resolveImageUrl(result.title());
            if (imageUrl != null) {
                return imageUrl;
            }
        }
        return null;
    }

    private String resolveImageUrl(String fileTitle) throws Exception {
        if (!StringUtils.hasText(fileTitle)) {
            return null;
        }

        val infoRoot = wikimediaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("action", "query")
                        .queryParam("titles", fileTitle)
                        .queryParam("prop", "imageinfo")
                        .queryParam("iiprop", "url|mime")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .body(String.class);

        val infoResponse = objectMapper.readValue(infoRoot, ImageInfoApiResponse.class);
        val firstImageInfo = infoResponse.firstImageInfo();
        if (firstImageInfo == null) {
            return null;
        }

        val mimeType = firstImageInfo.mime();
        val imageUrl = firstImageInfo.url();
        return isWebImage(mimeType, imageUrl) ? imageUrl : null;
    }

    private List<String> buildSearchQueries(String[] normalizedKeywords) {
        val rawQueries = new LinkedHashSet<String>();
        val phraseQuery = String.join(" ", normalizedKeywords);
        if (normalizedKeywords.length > 1) {
            rawQueries.add("\"" + phraseQuery + "\"");
            rawQueries.add(String.join(" AND ", normalizedKeywords));
        }
        rawQueries.add(String.join(" OR ", normalizedKeywords));
        return List.copyOf(rawQueries);
    }

    private boolean isWebImage(String mimeType, String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return false;
        }

        if (StringUtils.hasText(mimeType)) {
            return ALLOWED_WEB_IMAGE_MIME_TYPES.contains(mimeType.toLowerCase(Locale.ROOT));
        }

        val normalizedUrl = imageUrl.toLowerCase(Locale.ROOT);
        return ALLOWED_WEB_IMAGE_EXTENSIONS.stream()
                .anyMatch(normalizedUrl::endsWith);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchApiResponse(Query query) {
        List<SearchResult> searchResults() {
            if (query == null || query.search == null) {
                return List.of();
            }
            return query.search;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Query(List<SearchResult> search) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResult(String title) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ImageInfoApiResponse(Query query) {
        ImageInfo firstImageInfo() {
            if (query == null || query.pages == null || query.pages.isEmpty()) {
                return null;
            }

            for (val page : query.pages.values()) {
                if (page == null || page.imageInfo == null || page.imageInfo.isEmpty()) {
                    continue;
                }
                return page.imageInfo.getFirst();
            }

            return null;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Query(Map<String, Page> pages) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Page(@JsonProperty("imageinfo") List<ImageInfo> imageInfo) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ImageInfo(String url, String mime) {
    }

}
