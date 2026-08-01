package achijones.footballcoach.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Classpath loader for {@code names.json} in :sim resources. */
public final class NamesJson {

    private NamesJson() {
    }

    public static String read() {
        try (InputStream in = NamesJson.class.getClassLoader().getResourceAsStream("names.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource names.json");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
