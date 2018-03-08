package no.nav.altinnkanal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {
    public static String readToString(String resource) throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(Paths.get(Utils.class.getResource(resource).toURI())), "UTF-8");
    }
}
