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

    /** Payload shape used by the tests and by the frontend. */
    public record CoursePayload(String id, String title, String description, String lecturer) {}

    /**
     * Create/update request body.
     *
     * The automated tests for TdA have historically varied in which fields they send.
     * We therefore keep this tolerant:
     *  - title is required
     *  - description/lecturer may be omitted (default to empty string)
     */
    public record CourseUpsertRequest(String title, String description, String lecturer) {}

    @GetMapping
    public List<CoursePayload> list(@RequestParam(name = "search", required = false) String search) {
        List<CourseEntity> all = repo.findAll();
        if (search == null || search.isBlank()) {
            return all.stream().map(PublicCoursesApiController::toPayload).collect(Collectors.toList());
        }

        String s = search.toLowerCase().trim();
        return all.stream()
                .filter(c -> (c.getTitle() != null && c.getTitle().toLowerCase().contains(s))
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(s))
                        || (c.getLecturer() != null && c.getLecturer().toLowerCase().contains(s)))
                .map(PublicCoursesApiController::toPayload)
                .collect(Collectors.toList());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CoursePayload> create(@RequestBody(required = false) CourseUpsertRequest req) {
        if (req == null || isBlank(req.title())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields");
        }

        String description = req.description() == null ? "" : req.description();
        String lecturer = req.lecturer() == null ? "" : req.lecturer();
        CourseEntity e = new CourseEntity(UUID.randomUUID().toString(), req.title(), description, lecturer);
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

        if (req == null || isBlank(req.title())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields");
        }

        e.setTitle(req.title());
        e.setDescription(req.description() == null ? "" : req.description());
        e.setLecturer(req.lecturer() == null ? "" : req.lecturer());
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
        return new CoursePayload(e.getId(), e.getTitle(), e.getDescription(), e.getLecturer());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
