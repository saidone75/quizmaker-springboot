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
