package tda.app.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
    public String courseDetail(@PathVariable String id) {
        // courseDetail.html používá query parametr id
        return "redirect:/courseDetail.html?id=" + id;
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
