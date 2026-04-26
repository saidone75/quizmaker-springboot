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

package org.saidone.quizmaker.service;

import ai.djl.repository.zoo.ZooModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.saidone.quizmaker.config.EmbeddingModelConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WikimediaSemanticImageSearchServiceIT {

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_WIKIMEDIA_IT", matches = "true")
    void shouldReturnRealImageUrlFromWikimedia() throws Exception {
        try (val context = embeddingModelContext()) {
            val service = new WikimediaSemanticImageSearchService(
                    wikimediaRestClient(),
                    textEmbeddingModel(context),
                    new ObjectMapper()
            );

            val result = service.findMostRelevantImage(new String[]{"apollo", "nasa", "spacecraft"});

            assertThat(result)
                    .isNotNull()
                    .startsWith("https://upload.wikimedia.org/");
        }
    }

    @Test
    void shouldReturnNullWhenKeywordsAreBlankOrMissing() {
        val service = new WikimediaSemanticImageSearchService(
                wikimediaRestClient(),
                null,
                new ObjectMapper()
        );

        assertThat(service.searchImage(null)).isNull();
        assertThat(service.searchImage(new String[]{})).isNull();
        assertThat(service.searchImage(new String[]{" ", "\t"})).isNull();
    }

    @SuppressWarnings("unchecked")
    private ZooModel<String, float[]> textEmbeddingModel(AnnotationConfigApplicationContext context) {
        return (ZooModel<String, float[]>) context.getBean("textEmbeddingModel");
    }

    private AnnotationConfigApplicationContext embeddingModelContext() {
        val context = new AnnotationConfigApplicationContext();

        val overrideModelUrl = System.getenv("WIKIMEDIA_EMBEDDING_MODEL_URL");
        if (overrideModelUrl != null && !overrideModelUrl.isBlank()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("wikimediaItOverrides",
                            Map.of("app.ai.embedding.model-url", overrideModelUrl))
            );
        }

        context.register(EmbeddingModelConfig.class);
        context.refresh();
        return context;
    }

    private RestClient wikimediaRestClient() {
        return RestClient.builder()
                .baseUrl("https://commons.wikimedia.org/w/api.php")
                .defaultHeader("User-Agent", "QuizMaker/1.0")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
