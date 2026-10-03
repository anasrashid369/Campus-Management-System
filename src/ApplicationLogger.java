import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class ApplicationLogger {
    private static final Path LOG_FILE = Path.of("logs", "app.log");

    private ApplicationLogger() {
    }

    public static synchronized void info(String event) {
        write("INFO", event, null);
    }

    public static synchronized void error(String event, Exception exception) {
        write("ERROR", event, exception);
    }

    private static void write(String level, String event, Exception exception) {
        StringBuilder entry = new StringBuilder()
                .append(Instant.now())
                .append(" [").append(level).append("] ")
                .append(singleLine(event));
        if (exception != null) {
            entry.append(" | ").append(exception.getClass().getSimpleName());
            if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
                entry.append(": ").append(singleLine(exception.getMessage()));
            }
        }
        entry.append(System.lineSeparator());

        try {
            Files.createDirectories(LOG_FILE.getParent());
            Files.writeString(LOG_FILE, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException loggingFailure) {
            System.err.println("Unable to write application log: " + loggingFailure.getMessage());
        }
    }

    private static String singleLine(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }
}