package achijones.footballcoach.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Classpath loader for the canonical FBS team JSON shipped in :sim resources.
 * Retains the historical {@code FbsCsv} name used by tests.
 */
public final class FbsCsv {

    private FbsCsv() {
    }

    /** @return raw {@code fbs_2026.json} text */
    public static String read() {
        try (InputStream in = FbsCsv.class.getClassLoader().getResourceAsStream("fbs_2026.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource fbs_2026.json");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
