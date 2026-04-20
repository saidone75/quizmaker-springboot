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

import static org.assertj.core.api.Assertions.assertThat;

class WikimediaSemanticImageSearchServiceTest {

    @Test
    void shouldMarkDjvuMimeAsUnsupported() {
        assertThat(WikimediaSemanticImageSearchService.isUnsupportedMedia(
                "File:Example.jpg",
                "image/vnd.djvu",
                "https://upload.wikimedia.org/example.jpg")
        ).isTrue();
    }

    @Test
    void shouldMarkDjvuExtensionInTitleAsUnsupported() {
        assertThat(WikimediaSemanticImageSearchService.isUnsupportedMedia(
                "File:Scan.djvu",
                "image/jpeg",
                "https://upload.wikimedia.org/scan.jpg")
        ).isTrue();
    }

    @Test
    void shouldMarkDjvuExtensionInUrlAsUnsupported() {
        assertThat(WikimediaSemanticImageSearchService.isUnsupportedMedia(
                "File:Scan.jpg",
                "image/jpeg",
                "https://upload.wikimedia.org/scan.djvu")
        ).isTrue();
    }

    @Test
    void shouldKeepJpegAsSupported() {
        assertThat(WikimediaSemanticImageSearchService.isUnsupportedMedia(
                "File:Photo.jpg",
                "image/jpeg",
                "https://upload.wikimedia.org/photo.jpg")
        ).isFalse();
    }

    @Test
    void shouldMatchOnlyWholeTokens() {
        assertThat(WikimediaSemanticImageSearchService.containsTokenish(
                "File:Marseille Harbor", "mars")
        ).isFalse();

        assertThat(WikimediaSemanticImageSearchService.containsTokenish(
                "NASA mission to Mars", "mars")
        ).isTrue();
    }

    @Test
    void shouldMatchMultiWordKeywordAsTokenSequence() {
        assertThat(WikimediaSemanticImageSearchService.containsTokenish(
                "Apollo 11 lunar landing photo", "lunar landing")
        ).isTrue();

        assertThat(WikimediaSemanticImageSearchService.containsTokenish(
                "Landing on the moon during lunar module operation", "lunar landing")
        ).isFalse();
    }
}
