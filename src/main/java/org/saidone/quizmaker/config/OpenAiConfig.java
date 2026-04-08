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

package org.saidone.quizmaker.config;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "app.openai", name = "enabled", havingValue = "true")
public class OpenAiConfig {

    @Bean("openAiRestClient")
    public RestClient openAiRestClient(
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.openai.http.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${app.openai.http.read-timeout-ms:30000}") long readTimeoutMs
    ) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean("openAiRetry")
    public Retry openAiRetry(
            @Value("${app.openai.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.openai.retry.initial-backoff-ms:400}") long initialBackoffMs,
            @Value("${app.openai.retry.max-backoff-ms:3000}") long maxBackoffMs
    ) {
        var config = RetryConfig.custom()
                .maxAttempts(Math.max(1, maxAttempts))
                .intervalFunction(IntervalFunction.ofExponentialBackoff(initialBackoffMs, 2.0, maxBackoffMs))
                .retryOnException(ex ->
                        ex instanceof HttpClientErrorException.TooManyRequests
                                || ex instanceof HttpServerErrorException
                                || ex instanceof ResourceAccessException
                )
                .build();
        return Retry.of("openAiRetry", config);
    }

}
