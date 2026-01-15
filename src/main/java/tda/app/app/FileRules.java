package tda.app.app;

import java.util.Set;

public class FileRules {
    // povolené přípony
    public static final Set<String> ALLOWED_EXT = Set.of(
            "pdf","docx","txt",
            "png","jpg","jpeg","gif",
            "mp4",
            "mp3"
    );

    public static String extOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "";
        return filename.substring(dot + 1).toLowerCase();
    }
}
