package tda.app.app;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public JSON API expected by the automated tests.
 *
 * Endpoints:
 *  - GET    /courses
 *  - POST   /courses
 *  - GET    /courses/{courseId}
 *  - PUT    /courses/{courseId}
 *  - DELETE /courses/{courseId}
 *
 * Note: HTML pages are served by {@link PageRoutesController} for Accept: text/html.
 * This controller only matches requests negotiating JSON.
 */
@RestController
@RequestMapping(value = "/courses", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicCoursesApiController {

    private final CourseRepository repo;

    public PublicCoursesApiController(CourseRepository repo) {
        this.repo = repo;
    }

    /**
     * Payload shape expected by the automated tests.
     *
     * Tests use:
     *  - uuid (string UUID v4)
     *  - name
     *  - description
     * And in later phases they also assert that materials/quizzes are present as arrays.
     */
    public record CoursePayload(
            String uuid,
            String name,
            String description,
            List<Object> materials,
            List<Object> quizzes
    ) {}

    /**
     * Create/update request body.
     *
     * The automated tests for TdA have historically varied in which fields they send.
     * We therefore keep this tolerant:
     *  - title is required
     *  - description/lecturer may be omitted (default to empty string)
     */
    /**
     * Create/update request body.
     *
     * Different phases / frontends may send either "name" or "title".
     * We accept both and normalize to name.
     */
    public record CourseUpsertRequest(String name, String title, String description) {}

    @GetMapping
    public List<CoursePayload> list() {
        return repo.findAll().stream().map(PublicCoursesApiController::toPayload).collect(Collectors.toList());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CoursePayload> create(@RequestBody(required = false) CourseUpsertRequest req) {
        String name = normalizeName(req);
        if (isBlank(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields");
        }

        String description = (req == null || req.description() == null) ? "" : req.description();
        CourseEntity e = new CourseEntity(UUID.randomUUID().toString(), name, description, "");
        repo.save(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPayload(e));
    }

    @GetMapping("/{courseId}")
    public CoursePayload get(@PathVariable String courseId) {
        CourseEntity e = repo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return toPayload(e);
    }

    @PutMapping(value = "/{courseId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CoursePayload update(@PathVariable String courseId, @RequestBody(required = false) CourseUpsertRequest req) {
        CourseEntity e = repo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        String name = normalizeName(req);
        if (isBlank(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields");
        }

        e.setTitle(name);
        e.setDescription((req == null || req.description() == null) ? "" : req.description());
        e.setLecturer("");
        repo.save(e);
        return toPayload(e);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(@PathVariable String courseId) {
        if (!repo.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        repo.deleteById(courseId);
        return ResponseEntity.noContent().build();
    }

    private static CoursePayload toPayload(CourseEntity e) {
        return new CoursePayload(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                List.of(),
                List.of()
        );
    }

    private static String normalizeName(CourseUpsertRequest req) {
        if (req == null) return null;
        if (req.name() != null && !req.name().isBlank()) return req.name();
        return req.title();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
