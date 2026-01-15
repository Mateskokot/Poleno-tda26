package tda.app.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LegacyCourseRedirectController {

    // Přesměruje starou URL /course.html?id=... na /courseDetail.html?id=...
    @GetMapping("/course.html")
    public String redirectCourse(@RequestParam(name = "id", required = false) String id) {
        if (id == null || id.isBlank()) {
            return "redirect:/courses.html";
        }
        return "redirect:/courseDetail.html?id=" + id;
    }
}
