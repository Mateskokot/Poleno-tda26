package tda.app.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Mapování URL pro statické HTML stránky.
 * API je oddělené pod /api/**
 */
@Controller
public class PageRoutesController {

    // Home page – test TdA očekává HTML
    @GetMapping(value = {"/", ""})
    public String root() {
        return "forward:/index.html";
    }

    // Seznam kurzů – HTML stránka
    @GetMapping(value = {"/courses", "/courses/"})
    public String courses() {
        return "forward:/courses.html";
    }

    // Detail kurzu – přesměrování na statické HTML s query parametrem
    @GetMapping(value = {"/courses/{id}", "/courses/{id}/"})
    public String courseDetail(@PathVariable String id) {
        return "redirect:/courseDetail.html?id=" + id;
    }

    // Přihlášení lektora
    @GetMapping(value = {"/login", "/login/"})
    public String login() {
        return "forward:/loginLec.html";
    }

    // Dashboard lektora
    @GetMapping(value = {"/dashboard", "/dashboard/"})
    public String dashboard() {
        return "forward:/dashboardLec.html";
    }
}
