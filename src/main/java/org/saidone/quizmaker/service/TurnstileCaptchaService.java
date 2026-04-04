/*
 * QuizMaker - fun quizzes for curious minds
 * Copyright (C) 2026 Saidone
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.logging.log4j.util.Strings;
import org.saidone.quizmaker.config.TurnstileProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TurnstileCaptchaService {

    private final TurnstileProperties turnstileProperties;

    private final RestClient restClient = RestClient.create();

    public boolean isEnabled() {
        return turnstileProperties.isEnabled();
    }

    public String getSiteKey() {
        return turnstileProperties.getSiteKey();
    }

    public boolean verifyToken(String token, String remoteIp) {
        if (!turnstileProperties.isEnabled()) {
            return true;
        }
        if (Strings.isBlank(token)) {
            return false;
        }
        if (Strings.isBlank(turnstileProperties.getSecretKey())) {
            log.warn("Turnstile abilitato ma app.turnstile.secret-key non configurata");
            return false;
        }

        val payload = new LinkedMultiValueMap<String, String>();
        payload.add("secret", turnstileProperties.getSecretKey());
        payload.add("response", token);
        if (Strings.isNotBlank(remoteIp)) {
            payload.add("remoteip", remoteIp);
        }

        try {
            val result = restClient.post()
                    .uri(turnstileProperties.getVerifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(payload)
                    .retrieve()
                    .body(TurnstileVerifyResponse.class);

            return result != null && result.success();
        } catch (RuntimeException ex) {
            log.warn("Verifica Turnstile fallita: {}", ex.getMessage());
            return false;
        }
    }

    private record TurnstileVerifyResponse(boolean success, List<String> errorCodes) {
    }
}
