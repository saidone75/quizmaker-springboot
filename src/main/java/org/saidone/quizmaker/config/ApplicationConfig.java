/*
 * Alice's Simple Quiz Maker - fun quizzes for curious minds
 * Copyright (C) 2026 Miss Alice & Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.quizmaker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ApplicationConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean(name = "openAiRestTemplate")
    public RestTemplate openAiRestTemplate() {
        val requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(180).toMillis());
        return new RestTemplate(requestFactory);
    }

    @Bean(name = "openAiRestClient")
    public RestClient openAiRestClient(@Qualifier("openAiRestTemplate") RestTemplate openAiRestTemplate) {
        return RestClient.builder(openAiRestTemplate)
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    @Bean(name = "wikimediaRestTemplate")
    public RestTemplate wikimediaRestTemplate() {
        val requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(15).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        return new RestTemplate(requestFactory);
    }

    @Bean(name = "wikimediaRestClient")
    public RestClient wikimediaRestClient(@Qualifier("wikimediaRestTemplate") RestTemplate wikimediaRestTemplate) {
        return RestClient.builder(wikimediaRestTemplate)
                .baseUrl("https://commons.wikimedia.org/w/api.php")
                .defaultHeader("User-Agent", "QuizMaker/1.0")
                .defaultHeader("Accept", "application/json")
                .build();
    }

}
