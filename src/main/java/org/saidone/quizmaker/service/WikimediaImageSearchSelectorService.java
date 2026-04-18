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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class WikimediaImageSearchSelectorService implements WikimediaImageSearchService {

    private static final String SEARCH_MODE_PROPERTY = "app.wikimedia.image-search.mode";
    private static final String MODE_SIMPLE = "simple";
    private static final String MODE_ADVANCED = "advanced";

    private final WikimediaSimpleImageFinderService simpleImageFinderService;
    private final WikimediaImageFinderService advancedImageFinderService;
    private final Environment environment;

    @Override
    public String searchImage(String[] keywords) {
        val mode = environment.getProperty(SEARCH_MODE_PROPERTY, MODE_ADVANCED).trim().toLowerCase();
        if (MODE_SIMPLE.equals(mode)) {
            return simpleImageFinderService.searchImage(keywords);
        }
        if (!MODE_ADVANCED.equals(mode)) {
            log.warn("Modalità '{}' non riconosciuta per {}. Uso '{}'.", mode, SEARCH_MODE_PROPERTY, MODE_ADVANCED);
        }
        return advancedImageFinderService.searchImage(keywords);
    }

}
