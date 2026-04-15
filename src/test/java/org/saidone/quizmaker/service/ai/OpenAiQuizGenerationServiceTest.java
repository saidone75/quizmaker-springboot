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

package org.saidone.quizmaker.service.ai;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.saidone.quizmaker.dto.QuestionDto;
import org.saidone.quizmaker.dto.QuizDto;
import org.saidone.quizmaker.service.WikimediaSearcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiQuizGenerationServiceTest {

    @Test
    void shouldShuffleOptionsAndKeepCorrectAnswerConsistent() {
        val service = new OpenAiQuizGenerationService(null, null, null);

        val question = new QuestionDto();
        question.setText("Qual è il pianeta rosso?");
        question.setOptions(new ArrayList<>(List.of("Marte", "Venere", "Giove", "Mercurio")));
        question.setAnswer(0);

        val quiz = QuizDto.Request.builder()
                .title("Spazio")
                .emoji("🚀")
                .questions(new ArrayList<>(List.of(question)))
                .build();

        service.randomizeAnswerPositions(quiz, new Random(1));

        assertThat(question.getOptions()).containsExactly("Mercurio", "Marte", "Venere", "Giove");
        assertThat(question.getAnswer()).isEqualTo(1);
        assertThat(question.getOptions().get(question.getAnswer())).isEqualTo("Marte");
    }

    @Test
    void shouldResolveImageUrlFromEnglishKeywords() {
        val wikimediaSearcher = mock(WikimediaSearcher.class);
        when(wikimediaSearcher.searchImage(any())).thenReturn("https://upload.wikimedia.org/example.jpg");
        val service = new OpenAiQuizGenerationService(null, null, wikimediaSearcher);

        val question = new QuestionDto();
        question.setImageKeywords("red planet, mars astronomy");

        val quiz = QuizDto.Request.builder()
                .title("Spazio")
                .emoji("🚀")
                .questions(new ArrayList<>(List.of(question)))
                .build();

        service.checkGeneratedImageUrls(quiz, true);

        assertThat(question.getImageUrl()).isEqualTo("https://upload.wikimedia.org/example.jpg");
        assertThat(question.getImageKeywords()).isEqualTo("red planet, mars astronomy");
        verify(wikimediaSearcher).searchImage(eq(new String[]{"red planet", "mars astronomy"}));
    }

    @Test
    void shouldTrimLeadingAndTrailingSpacesFromImageKeywords() {
        val wikimediaSearcher = mock(WikimediaSearcher.class);
        when(wikimediaSearcher.searchImage(any())).thenReturn("https://upload.wikimedia.org/example.jpg");
        val service = new OpenAiQuizGenerationService(null, null, wikimediaSearcher);

        val question = new QuestionDto();
        question.setImageKeywords("  Kennedy Space Center  ,  LC-39B  ");

        val quiz = QuizDto.Request.builder()
                .title("Spazio")
                .emoji("🚀")
                .questions(new ArrayList<>(List.of(question)))
                .build();

        service.checkGeneratedImageUrls(quiz, true);

        assertThat(question.getImageKeywords()).isEqualTo("Kennedy Space Center, LC-39B");
        verify(wikimediaSearcher).searchImage(eq(new String[]{"Kennedy Space Center", "LC-39B"}));
    }

}
