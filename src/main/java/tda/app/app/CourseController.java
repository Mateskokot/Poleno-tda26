package tda.app.app;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    // In-memory data (později DB + seed)
    private static final List<Course> COURSES = List.of(
            new Course(
                    "11111111-1111-1111-1111-111111111111",
                    "Základy HTML",
                    "Základní tagy, struktura stránky, odkazy, formuláře.",
                    "Lektor A"
            ),
            new Course(
                    "22222222-2222-2222-2222-222222222222",
                    "Úvod do JavaScriptu",
                    "Proměnné, podmínky, funkce, práce s DOM.",
                    "Lektor B"
            )
    );

    // GET /api/courses?search=něco
    @GetMapping
    public List<Course> list(@RequestParam(name = "search", required = false) String search) {
        if (search == null || search.isBlank()) {
            return COURSES;
        }

        String s = search.toLowerCase().trim();
        return COURSES.stream()
                .filter(c -> c.title().toLowerCase().contains(s))
                .collect(Collectors.toList());
    }

    // GET /api/courses/{id}  (id je UUID ve stringu)
    @GetMapping("/{id}")
    public Course get(@PathVariable String id) {
        return COURSES.stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    // DTO/record pro kurz
    public record Course(String id, String title, String description, String lecturer) {}
}
