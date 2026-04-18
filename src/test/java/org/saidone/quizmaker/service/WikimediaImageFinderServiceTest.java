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

class WikimediaImageFinderServiceTest {

    @Test
    void shouldMarkDjvuMimeAsUnsupported() {
        assertThat(WikimediaImageFinderService.isUnsupportedMedia(
                "File:Example.jpg",
                "image/vnd.djvu",
                "https://upload.wikimedia.org/example.jpg")
        ).isTrue();
    }

    @Test
    void shouldMarkDjvuExtensionInTitleAsUnsupported() {
        assertThat(WikimediaImageFinderService.isUnsupportedMedia(
                "File:Scan.djvu",
                "image/jpeg",
                "https://upload.wikimedia.org/scan.jpg")
        ).isTrue();
    }

    @Test
    void shouldMarkDjvuExtensionInUrlAsUnsupported() {
        assertThat(WikimediaImageFinderService.isUnsupportedMedia(
                "File:Scan.jpg",
                "image/jpeg",
                "https://upload.wikimedia.org/scan.djvu")
        ).isTrue();
    }

    @Test
    void shouldKeepJpegAsSupported() {
        assertThat(WikimediaImageFinderService.isUnsupportedMedia(
                "File:Photo.jpg",
                "image/jpeg",
                "https://upload.wikimedia.org/photo.jpg")
        ).isFalse();
    }
}
