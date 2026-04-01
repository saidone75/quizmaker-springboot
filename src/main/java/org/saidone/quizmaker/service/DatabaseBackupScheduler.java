package org.saidone.quizmaker.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.db-backup", name = "enabled", havingValue = "true")
public class DatabaseBackupScheduler {

    private static final String SQLITE_JDBC_PREFIX = "jdbc:sqlite:";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${app.db-backup.directory:./backups}")
    private String backupDirectory;

    @Value("${app.db-backup.retention-count:14}")
    private int retentionCount;

    @Scheduled(cron = "${app.db-backup.cron:0 0 2 * * *}")
    public void backupDatabase() {
        try {
            val sourceDbPath = resolveSqliteFilePath();
            if (sourceDbPath == null) {
                return;
            }

            if (!Files.exists(sourceDbPath)) {
                log.warn("Database backup skipped. SQLite file not found at {}", sourceDbPath);
                return;
            }

            val backupDirPath = Path.of(backupDirectory).toAbsolutePath().normalize();
            Files.createDirectories(backupDirPath);

            val sourceFileName = sourceDbPath.getFileName().toString();
            val baseName = sourceFileName.endsWith(".db")
                    ? sourceFileName.substring(0, sourceFileName.length() - 3)
                    : sourceFileName;

            val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            val backupFile = backupDirPath.resolve(baseName + "-" + timestamp + ".db");

            Files.copy(sourceDbPath, backupFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            log.info("Database backup completed: {}", backupFile);

            cleanupOldBackups(backupDirPath, baseName);
        } catch (Exception ex) {
            log.error("Database backup failed.", ex);
        }
    }

    private Path resolveSqliteFilePath() {
        if (datasourceUrl == null || !datasourceUrl.startsWith(SQLITE_JDBC_PREFIX)) {
            log.debug("Database backup skipped. Datasource is not SQLite: {}", datasourceUrl);
            return null;
        }

        val dbPathString = datasourceUrl.substring(SQLITE_JDBC_PREFIX.length());
        if (dbPathString.isBlank() || ":memory:".equalsIgnoreCase(dbPathString)) {
            log.debug("Database backup skipped. SQLite datasource is in-memory.");
            return null;
        }

        return Path.of(dbPathString).toAbsolutePath().normalize();
    }

    private void cleanupOldBackups(Path backupDirPath, String baseName) throws IOException {
        if (retentionCount < 1) {
            return;
        }

        try (val pathStream = Files.list(backupDirPath)) {
            val backups = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        val filename = path.getFileName().toString();
                        return filename.startsWith(baseName + "-") && filename.endsWith(".db");
                    })
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();

            for (int i = retentionCount; i < backups.size(); i++) {
                Files.deleteIfExists(backups.get(i));
                log.info("Deleted old database backup: {}", backups.get(i));
            }
        }
    }
}
