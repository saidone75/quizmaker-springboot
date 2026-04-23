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

import lombok.val;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

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

    @Test
    void shouldPreferDescriptionWeightWhenDescriptionIsMeaningful() throws Exception {
        val candidate = newCandidate(28.0, "Ancient temple at sunrise in Kyoto", 0.82, 0.73);
        val profile = invokeWeightProfile(candidate);

        val descriptionWeight = (double) profile.getClass().getDeclaredMethod("descriptionWeight").invoke(profile);
        val semanticWeight = (double) profile.getClass().getDeclaredMethod("semanticWeight").invoke(profile);
        val lexicalWeight = (double) profile.getClass().getDeclaredMethod("lexicalWeight").invoke(profile);

        assertThat(descriptionWeight).isGreaterThan(semanticWeight);
        assertThat(lexicalWeight + descriptionWeight + semanticWeight).isEqualTo(1.0);
    }

    @Test
    void shouldDisableDescriptionWeightWhenDescriptionIsMissing() throws Exception {
        val candidate = newCandidate(22.0, " ", 0.75, 0.68);
        val profile = invokeWeightProfile(candidate);

        val descriptionWeight = (double) profile.getClass().getDeclaredMethod("descriptionWeight").invoke(profile);
        val semanticWeight = (double) profile.getClass().getDeclaredMethod("semanticWeight").invoke(profile);
        val lexicalWeight = (double) profile.getClass().getDeclaredMethod("lexicalWeight").invoke(profile);

        assertThat(descriptionWeight).isZero();
        assertThat(semanticWeight).isEqualTo(1.0 - lexicalWeight);
    }

    @Test
    void shouldGiveMoreWeightToPrimaryKeywordThanSecondary() throws Exception {
        val service = newServiceForReflectionCalls();
        Method method = WikimediaSemanticImageSearchService.class
                .getDeclaredMethod("scoreKeywordHit", boolean.class, boolean.class, boolean.class);
        method.setAccessible(true);

        double primaryScore = (double) method.invoke(service, true, true, true);
        double secondaryScore = (double) method.invoke(service, true, true, false);

        assertThat(primaryScore).isGreaterThan(secondaryScore);
    }

    @Test
    void shouldApplyPrimaryKeywordFullMatchBonus() throws Exception {
        val service = newServiceForReflectionCalls();
        Method method = WikimediaSemanticImageSearchService.class
                .getDeclaredMethod("scoreKeywordHit", boolean.class, boolean.class, boolean.class);
        method.setAccessible(true);

        double withFullMatch = (double) method.invoke(service, true, true, true);
        double titleOnly = (double) method.invoke(service, true, false, true);
        double textOnly = (double) method.invoke(service, false, true, true);

        assertThat(withFullMatch).isGreaterThan(titleOnly + textOnly);
    }

    private static Object invokeWeightProfile(Object candidate) throws Exception {
        Method method = WikimediaSemanticImageSearchService.class.getDeclaredMethod("buildWeightProfile", candidate.getClass());
        method.setAccessible(true);
        return method.invoke(null, candidate);
    }

    private static WikimediaSemanticImageSearchService newServiceForReflectionCalls() {
        return new WikimediaSemanticImageSearchService(null, null, null);
    }

    private static Object newCandidate(double lexicalScore,
                                       String descriptionText,
                                       double descriptionSemanticScore,
                                       double semanticScore) throws Exception {
        Class<?> candidateClass = Class.forName("org.saidone.quizmaker.service.WikimediaSemanticImageSearchService$Candidate");
        Object candidate = candidateClass.getDeclaredConstructor().newInstance();

        setField(candidateClass, candidate, "lexicalScore", lexicalScore);
        setField(candidateClass, candidate, "descriptionText", descriptionText);
        setField(candidateClass, candidate, "descriptionSemanticScore", descriptionSemanticScore);
        setField(candidateClass, candidate, "semanticScore", semanticScore);
        return candidate;
    }

    private static void setField(Class<?> clazz, Object target, String fieldName, Object value) throws Exception {
        val field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
