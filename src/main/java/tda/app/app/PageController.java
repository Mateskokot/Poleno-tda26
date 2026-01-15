package tda.app.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // Zobrazení login stránky
    @GetMapping("/login")
    public String loginPage() {
        return "loginLec"; // zobrazí login stránku
    }

    // /courses → kontrola session
    @GetMapping("/courses")
    public String courses() {
         return "courses";
    }

    @GetMapping("/dashboardLec")
    public String dashboardLec() {
            return "dashboardLec";
    }

    // /courseDetail → kontrola session
    @GetMapping("/courseDetail")
    public String courseDetail() {
            return "courseDetail";
    }

    // Další redirecty pro zjednodušení
    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @GetMapping("/loginLec")
    public String loginLec() {
        return "redirect:/login"; // zobrazí login stránku
    }

    @GetMapping("/indexlec")
    public String indexLec() {
            return "indexlec";
    }

    @GetMapping("/manageLec")
    public String manageLec() {
            return "manageLec";
    }

    @GetMapping("/quiz")
    public String quiz() {
            return "quiz";
    }

    @GetMapping("/quizManage")
    public String quizManage() {
            return "quizManage";
    }

}
