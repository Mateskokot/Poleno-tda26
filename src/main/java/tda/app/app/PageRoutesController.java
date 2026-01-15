package tda.app.app;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

/**
 * Mapování URL podle požadavků zadání (Fáze 0 a 1).
 *
 * Zadání často očekává cesty jako /courses, /courses/{uuid}, /login, /dashboard.
 * V aplikaci používáme statické HTML stránky, proto tyto cesty jen přesměrujeme.
 */
@Controller
public class PageRoutesController {

    @GetMapping(value = "/", produces = TEXT_HTML_VALUE)
    public String root() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/courses", produces = TEXT_HTML_VALUE)
    public String courses() {
        return "forward:/courses.html";
    }

    @GetMapping(value = "/courses/{id}", produces = TEXT_HTML_VALUE)
    public ResponseEntity<String> courseDetail(@PathVariable String id) throws IOException {
        // Testy navigují přímo na /courses/{id} a očekávají 200 + text/html.
        // Současně naše statická stránka courseDetail.html používá query parametr ?id=.
        // Proto vrátíme HTML obsah courseDetail.html, ale s drobnou úpravou JS tak,
        // aby při chybějícím ?id= použila {id} z path parametru.

        var res = new ClassPathResource("static/courseDetail.html");
        String html = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // Minimal-intrusive patch: nahraďte řádek s načtením ID z query parametru
        // tak, aby měl fallback na path param.
        String needle = "const courseId = getCourseId();";
        String replacement = "const courseId = getCourseId() || \"" + escapeJs(id) + "\";";
        if (html.contains(needle)) {
            html = html.replace(needle, replacement);
        }

        return ResponseEntity.ok()
                .header("content-type", "text/html; charset=utf-8")
                .body(html);
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/loginLec.html";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/dashboardLec.html";
    }
}
