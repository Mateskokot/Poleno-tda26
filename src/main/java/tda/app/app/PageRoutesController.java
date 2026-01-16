package tda.app.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageRoutesController {

    // Domovská stránka
    @RequestMapping(value = {"/", ""})
    public String root() {
        return "forward:/index.html";
    }

    // Seznam kurzů – bere GET, OPTIONS atd.
    @RequestMapping(value = {"/courses", "/courses/"})
    public String courses() {
        return "forward:/courses.html";
    }

    // ⚠️ Pojistka: kdyby na /courses přišel POST (form, JS, test)
    @PostMapping(value = {"/courses", "/courses/"})
    public String coursesPost() {
        return "forward:/courses.html";
    }

    // Detail kurzu
    @RequestMapping(value = {"/courses/{id}", "/courses/{id}/"})
    public String courseDetail(@PathVariable String id) {
        return "redirect:/courseDetail.html?id=" + id;
    }

    // Přihlášení lektora
    @RequestMapping(value = {"/login", "/login/"})
    public String login() {
        return "forward:/loginLec.html";
    }

    // Dashboard lektora
    @RequestMapping(value = {"/dashboard", "/dashboard/"})
    public String dashboard() {
        return "forward:/dashboardLec.html";
    }
}

