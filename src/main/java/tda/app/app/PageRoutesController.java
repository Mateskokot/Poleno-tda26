package tda.app.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PageRoutesController {

    @RequestMapping(value = {"/", ""})
    public String root() {
        return "forward:/index.html";
    }

    @RequestMapping(value = {"/courses", "/courses/"})
    public String courses() {
        return "forward:/courses.html";
    }

    @RequestMapping(value = {"/courses/{id}", "/courses/{id}/"})
    public String courseDetail(@PathVariable String id) {
        return "redirect:/courseDetail.html?id=" + id;
    }

    @RequestMapping(value = {"/login", "/login/"})
    public String login() {
        return "forward:/loginLec.html";
    }

    @RequestMapping(value = {"/dashboard", "/dashboard/"})
    public String dashboard() {
        return "forward:/dashboardLec.html";
    }
}
