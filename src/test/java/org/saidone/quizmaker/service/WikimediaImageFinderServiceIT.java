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

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ZooModel;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WikimediaImageFinderServiceIT {

    /*
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_WIKIMEDIA_IT", matches = "true")
    void shouldFindRealImageFromWikimediaUsingKeywords() throws Exception {
        val service = new WikimediaImageFinderService(mockEmbeddingModel());

        val result = service.findMostRelevantImage(new String[]{"orion", "nasa", "spacecraft"});

        assertThat(result).isNotNull();
        assertThat(result).startsWith("https://upload.wikimedia.org/");
    }

     */

    @SuppressWarnings("unchecked")
    private ZooModel<String, float[]> mockEmbeddingModel() throws Exception {
        val embeddingModel = mock(ZooModel.class);
        val predictor = mock(Predictor.class);

        when(embeddingModel.newPredictor()).thenReturn(predictor);
        when(predictor.predict(anyString())).thenAnswer(invocation -> embeddingFor(invocation.getArgument(0)));

        return embeddingModel;
    }

    private float[] embeddingFor(String text) {
        val safe = text == null ? "" : text;
        float length = safe.length();
        float vowels = (float) safe.chars()
                .filter(ch -> "aeiouAEIOU".indexOf(ch) >= 0)
                .count();
        float checksum = (float) safe.chars().sum();
        return new float[]{length, vowels, checksum == 0 ? 1.0f : checksum};
    }
}
