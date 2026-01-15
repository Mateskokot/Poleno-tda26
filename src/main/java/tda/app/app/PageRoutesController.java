package tda.app.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Mapování URL podle požadavků zadání (Fáze 0 a 1).
 *
 * Zadání často očekává cesty jako /courses, /courses/{uuid}, /login, /dashboard.
 * V aplikaci používáme statické HTML stránky, proto tyto cesty jen přesměrujeme.
 */
@Controller
public class PageRoutesController {

    @GetMapping("/")
    public String root() {
        return "forward:/index.html";
    }

    @GetMapping("/courses")
    public String courses() {
        return "forward:/courses.html";
    }

    @GetMapping("/courses/{id}")
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
