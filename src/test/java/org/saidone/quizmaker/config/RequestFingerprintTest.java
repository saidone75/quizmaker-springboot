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

package org.saidone.quizmaker.config;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RequestFingerprintTest {

    @Test
    void clientIpShouldUseFirstForwardedAddressWhenAvailable() {
        val request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        assertThat(RequestFingerprint.clientIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void clientIpShouldFallbackToRemoteAddressWhenForwardedHeaderMissing() {
        val request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(RequestFingerprint.clientIp(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void loginKeyShouldNormalizeUsernameAndIncludeClientIp() {
        val request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addParameter("username", "  TeAcHeR ");

        assertThat(RequestFingerprint.loginKey(request)).isEqualTo("teacher|127.0.0.1");
    }
}
