package tda.app.app.feed;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tda.app.app.Auth;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses/{courseId}/feed")
public class FeedController {

    private final FeedService feed;

    public record PostRequest(String message) {}

    public FeedController(FeedService feed) {
        this.feed = feed;
    }

    @GetMapping
    public List<FeedService.FeedItemDto> list(@PathVariable String courseId) {
        return feed.list(courseId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String courseId) {
        return feed.subscribe(courseId);
    }

    // lecturer posts
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public FeedService.FeedItemDto create(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @RequestBody PostRequest req
    ) {
        requireLecturer(auth);
        if (req == null) throw new IllegalArgumentException("Chybí data.");
        return feed.createLecturerPost(courseId, req.message());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FeedService.FeedItemDto update(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String id,
            @RequestBody PostRequest req
    ) {
        requireLecturer(auth);
        if (req == null) throw new IllegalArgumentException("Chybí data.");
        return feed.updateLecturerPost(courseId, id, req.message());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String id
    ) {
        requireLecturer(auth);
        feed.deleteLecturerPost(courseId, id);
        return Map.of("deleted", true, "id", id);
    }

    private void requireLecturer(String authHeader) {
        if (!Auth.isBearerValid(authHeader)) {
            throw new Unauthorized();
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class Unauthorized extends RuntimeException {}

    @ExceptionHandler(Unauthorized.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> unauthorized() {
        return Map.of("error", "Unauthorized");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, String> badRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
