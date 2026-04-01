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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.val;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

@Converter
public class LocalDateTimeColumnConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter SQLITE_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        if (attribute == null) {
            return null;
        }

        return SQLITE_TIMESTAMP_FORMATTER.format(attribute);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        val rawValue = dbData.trim();
        if (rawValue.chars().allMatch(Character::isDigit)) {
            long epoch = Long.parseLong(rawValue);
            Instant instant = rawValue.length() <= 10
                    ? Instant.ofEpochSecond(epoch)
                    : Instant.ofEpochMilli(epoch);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }

        try {
            return LocalDateTime.parse(rawValue, SQLITE_TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(rawValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
