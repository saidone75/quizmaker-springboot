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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikimediaSearcher {

    private static final String API_ENDPOINT = "https://commons.wikimedia.org/w/api.php";
    private static final String USER_AGENT = "QuizMaker/1.0";

    private final ObjectMapper objectMapper;

    public String searchImage(String[] keywords) {
        try {
            val queryTerms = String.join(" OR ", keywords);
            val searchUrl = String.format("%s?action=query&list=search&srsearch=%s&srnamespace=6&format=json&srlimit=1",
                    API_ENDPOINT,
                    URLEncoder.encode(queryTerms, StandardCharsets.UTF_8));
            val searchRoot = fetchData(searchUrl);
            val searchResults = searchRoot.path("query").path("search");
            if (searchResults.isEmpty()) {
                return null;
            }
            val fileTitle = searchResults.get(0).path("title").asText();

            val infoUrl = String.format("%s?action=query&titles=%s&prop=imageinfo&iiprop=url&format=json",
                    API_ENDPOINT,
                    URLEncoder.encode(fileTitle, StandardCharsets.UTF_8));

            val infoRoot = fetchData(infoUrl);

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
            System.err.println("Errore durante la ricerca: " + e.getMessage());
            return null;
        }
    }

    private JsonNode fetchData(String urlString) throws IOException {
        val url = new URL(urlString);
        val conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/json");

        // ObjectMapper può leggere direttamente dall'InputStream
        return objectMapper.readTree(conn.getInputStream());
    }

}

