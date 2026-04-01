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
        val safeLineCount = Math.max(1, Math.min(lines, 500));
        if (!Files.exists(DEFAULT_LOG_PATH)) {
            return new LogTailResult(DEFAULT_LOG_PATH.toString(), List.of(), "File di log non trovato.");
        }

        try {
            return new LogTailResult(DEFAULT_LOG_PATH.toString(), tailFile(DEFAULT_LOG_PATH, safeLineCount), null);
        } catch (IOException e) {
            return new LogTailResult(DEFAULT_LOG_PATH.toString(), List.of(), "Errore lettura log: " + e.getMessage());
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
                    if (currentLine.length() > 0) {
                        collectedLines.add(currentLine.reverse().toString());
                        currentLine.setLength(0);
                    }
                } else if (readByte != '\r') {
                    currentLine.append((char) readByte);
                }

                filePointer--;
            }

            if (currentLine.length() > 0 && collectedLines.size() < lines) {
                collectedLines.add(currentLine.reverse().toString());
            }
        }

        Collections.reverse(collectedLines);
        return collectedLines;
    }

    public record LogTailResult(String path, List<String> lines, String warning) {
    }
}
