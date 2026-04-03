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

package org.saidone.quizmaker.service;

import lombok.val;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LogTailService {

    private static final Path DEFAULT_LOG_PATH = Path.of("./log/quizmaker.log");

    public LogTailResult readLastLines(int lines) {
        val safeLineCount = Math.clamp(lines, 1, 500);
        if (!Files.exists(DEFAULT_LOG_PATH)) {
            return new LogTailResult(DEFAULT_LOG_PATH.toString(), List.of(), "File di log non trovato.");
        }

        try {
            return new LogTailResult(DEFAULT_LOG_PATH.toString(), tailFile(DEFAULT_LOG_PATH, safeLineCount), null);
        } catch (IOException e) {
            return new LogTailResult(DEFAULT_LOG_PATH.toString(), List.of(), String.format("Errore lettura log: %s", e.getMessage()));
        }
    }

    private List<String> tailFile(Path filePath, int lines) throws IOException {
        val collectedLines = new ArrayList<String>();
        try (val file = new RandomAccessFile(filePath.toFile(), "r")) {
            long filePointer = file.length() - 1;
            val currentLine = new StringBuilder();

            while (filePointer >= 0 && collectedLines.size() < lines) {
                file.seek(filePointer);
                int readByte = file.read();

                if (readByte == '\n') {
                    if (!currentLine.isEmpty()) {
                        collectedLines.add(currentLine.reverse().toString());
                        currentLine.setLength(0);
                    }
                } else if (readByte != '\r') {
                    currentLine.append((char) readByte);
                }

                filePointer--;
            }

            if (!currentLine.isEmpty() && collectedLines.size() < lines) {
                collectedLines.add(currentLine.reverse().toString());
            }
        }

        Collections.reverse(collectedLines);
        return collectedLines;
    }

    public record LogTailResult(String path, List<String> lines, String warning) {
    }
}
