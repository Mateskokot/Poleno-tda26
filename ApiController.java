package tda.app.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiController {

    // Vitest expects the endpoint to work on /api/ (with a trailing slash).
    // To be robust, we serve both /api and /api/.
    @GetMapping({"/api", "/api/"})
    public Map<String, String> apiRoot() {
        return Map.of("organization", "Student Cyber Games");
    }
}
