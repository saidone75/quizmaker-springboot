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

package org.saidone.quizmaker.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDateTimeColumnConverterTest {

    private final LocalDateTimeColumnConverter converter = new LocalDateTimeColumnConverter();

    @Test
    void shouldParseEpochMillis() {
        LocalDateTime parsed = converter.convertToEntityAttribute("1774946614762");

        assertThat(parsed).isEqualTo(LocalDateTime.ofEpochSecond(1774946614L, 762_000_000, ZoneOffset.UTC));
    }

    @Test
    void shouldParseSqliteTimestamp() {
        LocalDateTime parsed = converter.convertToEntityAttribute("2026-03-31 10:43:48.256");

        assertThat(parsed).isEqualTo(LocalDateTime.of(2026, 3, 31, 10, 43, 48, 256_000_000));
    }

    @Test
    void shouldFormatTimestampForDatabase() {
        String formatted = converter.convertToDatabaseColumn(LocalDateTime.of(2026, 3, 31, 10, 43, 48, 256_000_000));

        assertThat(formatted).isEqualTo("2026-03-31 10:43:48.256");
    }
}
