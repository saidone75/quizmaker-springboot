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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WikimediaImageFinderServiceTest {

    /*
    @Test
    void shouldBuildBatchedQueriesWithOrAsLastFallback() {
        val service = new WikimediaImageFinderService(mock());

        val batches = service.buildSearchQueryBatches(
                List.of("orion spacecraft", "nasa", "crew capsule", "lunar mission")
        );

        assertThat(batches).hasSize(3);
        assertThat(batches.get(0)).extracting(WikimediaImageFinderService.SearchQuerySpec::label)
                .containsExactly("and", "title", "title-boost");
        assertThat(batches.get(1)).extracting(WikimediaImageFinderService.SearchQuerySpec::label)
                .containsExactly("phrase", "plain");
        assertThat(batches.get(2)).extracting(WikimediaImageFinderService.SearchQuerySpec::label)
                .containsExactly("or");

        assertThat(batches.get(2).getFirst().query()).contains(" OR ");
    }

     */
}
