package tda.app.app;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Public (non-/api) course endpoints required by the Vitest phases.
 *
 * Only implements what the tests call.
 */
@RestController
@RequestMapping("/courses")
public class PublicCoursesController {

    private final PublicTestStore store;

    public record CourseCreateRequest(String name, String description) {}

    public PublicCoursesController(PublicTestStore store) {
        this.store = store;
    }

    @PostMapping
    public PublicTestStore.CoursePublic create(@RequestBody CourseCreateRequest req) {
        if (req == null || req.name() == null || req.name().isBlank() || req.description() == null) {
            throw new IllegalArgumentException("Missing fields");
        }
        return store.createCourse(req.name(), req.description());
    }

    @GetMapping("/{courseId}")
    public PublicTestStore.CoursePublic get(@PathVariable String courseId) {
        return store.getCourse(courseId).orElseThrow(() -> new java.util.NoSuchElementException("Course not found"));
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String courseId) {
        // If missing, tests don't hit it; still behave as 204 for idempotency.
        store.deleteCourse(courseId);
    }
}
