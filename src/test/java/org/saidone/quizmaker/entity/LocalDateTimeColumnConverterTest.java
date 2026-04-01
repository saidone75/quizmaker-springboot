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
