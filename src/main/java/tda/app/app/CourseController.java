package tda.app.app;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API pro kurzy.
 *
 * - GET /api/courses
 * - GET /api/courses/{uuid}
 * - POST /api/courses (Bearer lecturer)
 * - PUT /api/courses/{uuid} (Bearer lecturer)
 * - DELETE /api/courses/{uuid} (Bearer lecturer)
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository repo;

    public CourseController(CourseRepository repo) {
        this.repo = repo;
    }

    public record CourseDto(String id, String title, String description, String lecturer) {}

    public record CourseCreateUpdateRequest(String title, String description, String lecturer) {}

    @GetMapping
    public List<CourseDto> list(@RequestParam(name = "search", required = false) String search) {
        List<CourseEntity> all = repo.findAll();
        if (search == null || search.isBlank()) {
            return all.stream().map(CourseController::toDto).collect(Collectors.toList());
        }

        String s = search.toLowerCase().trim();
        return all.stream()
                .filter(c -> (c.getTitle() != null && c.getTitle().toLowerCase().contains(s))
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(s))
                        || (c.getLecturer() != null && c.getLecturer().toLowerCase().contains(s)))
                .map(CourseController::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public CourseDto get(@PathVariable String id) {
        CourseEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return toDto(e);
    }

    @PostMapping
    public CourseDto create(
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            @RequestBody CourseCreateUpdateRequest req
    ) {
        requireLecturer(authHeader);
        if (req == null || isBlank(req.title()) || isBlank(req.description()) || isBlank(req.lecturer())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields");
        }

        CourseEntity e = new CourseEntity(UUID.randomUUID().toString(), req.title(), req.description(), req.lecturer());
        repo.save(e);
        return toDto(e);
    }

    @PutMapping("/{id}")
    public CourseDto update(
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @RequestBody CourseCreateUpdateRequest req
    ) {
        requireLecturer(authHeader);
        CourseEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (req == null || isBlank(req.title()) || isBlank(req.description()) || isBlank(req.lecturer())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields");
        }

        e.setTitle(req.title());
        e.setDescription(req.description());
        e.setLecturer(req.lecturer());
        repo.save(e);
        return toDto(e);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            @PathVariable String id
    ) {
        requireLecturer(authHeader);
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        repo.deleteById(id);
    }

    private static CourseDto toDto(CourseEntity e) {
        return new CourseDto(e.getId(), e.getTitle(), e.getDescription(), e.getLecturer());
    }

    private static void requireLecturer(String authHeader) {
        if (!Auth.isBearerValid(authHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
