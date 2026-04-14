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

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WikimediaSearcherTest {

    @Test
    void shouldReturnImageUrlWhenWikimediaReturnsSearchAndImageInfo() {
        var wikimediaRestClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        var searcher = new WikimediaSearcher(wikimediaRestClient);

        var searchResponse = """
                {
                  "query": {
                    "search": [
                      {
                        "title": "File:Mars Valles Marineris.jpeg"
                      }
                    ]
                  }
                }
                """;

        var imageInfoResponse = """
                {
                  "query": {
                    "pages": {
                      "123": {
                        "imageinfo": [
                          {
                            "url": "https://upload.wikimedia.org/example.jpg"
                          }
                        ]
                      }
                    }
                  }
                }
                """;

        when(wikimediaRestClient.get().uri(anyString()).retrieve().body(String.class))
                .thenReturn(searchResponse, imageInfoResponse);

        var imageUrl = searcher.searchImage(new String[]{"mars", "planet"});

        assertThat(imageUrl).isEqualTo("https://upload.wikimedia.org/example.jpg");
    }

    @Test
    void shouldReturnNullWhenKeywordsAreBlankOrMissing() {
        var wikimediaRestClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        var searcher = new WikimediaSearcher(wikimediaRestClient);

        assertThat(searcher.searchImage(null)).isNull();
        assertThat(searcher.searchImage(new String[]{})).isNull();
        assertThat(searcher.searchImage(new String[]{" ", "\t"})).isNull();
    }

}
