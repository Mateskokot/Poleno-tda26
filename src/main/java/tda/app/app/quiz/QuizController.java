package tda.app.app.quiz;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tda.app.app.Auth;
import tda.app.app.feed.FeedService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses/{courseId}/quizzes")
public class QuizController {

    private final QuizStore store;
    private final FeedService feed;

    public record TitleRequest(String title) {}

    public QuizController(QuizStore store, FeedService feed) {
        this.store = store;
        this.feed = feed;
    }

    @GetMapping
    public List<QuizStore.QuizSummaryDto> list(@PathVariable String courseId) {
        return store.listSummaries(courseId);
    }

    @GetMapping("/{quizId}")
    public QuizStore.PublicQuizDto getPublic(@PathVariable String courseId, @PathVariable String quizId) {
        return store.getPublicQuiz(courseId, quizId);
    }

    @PostMapping(value = "/{quizId}/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public QuizStore.SubmitResponse submit(
            @PathVariable String courseId,
            @PathVariable String quizId,
            @RequestHeader(value = "X-Student-Key", required = false) String studentKey,
            @RequestBody QuizStore.SubmitRequest req
    ) throws IOException {
        return store.submit(courseId, quizId, req, studentKey);
    }

    // ---------------- lecturer endpoints ----------------

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Quiz create(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @RequestBody TitleRequest req
    ) throws IOException {
        requireLecturer(auth);
        if (req == null) throw new IllegalArgumentException("Chybí data.");
        Quiz quiz = store.createQuiz(courseId, req.title());
        feed.createAutoEvent(courseId, "Vytvořen nový kvíz: " + quiz.title());
        return quiz;
    }

    @PutMapping(value = "/{quizId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Quiz updateTitle(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String quizId,
            @RequestBody TitleRequest req
    ) throws IOException {
        requireLecturer(auth);
        if (req == null) throw new IllegalArgumentException("Chybí data.");
        return store.updateQuizTitle(courseId, quizId, req.title());
    }

    @DeleteMapping("/{quizId}")
    public Map<String, Object> delete(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String quizId
    ) throws IOException {
        requireLecturer(auth);
        store.deleteQuiz(courseId, quizId);
        return Map.of("deleted", true, "id", quizId);
    }

    @GetMapping("/{quizId}/full")
    public Quiz getFull(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String quizId
    ) {
        requireLecturer(auth);
        return store.getFullQuiz(courseId, quizId);
    }

    @PostMapping(value = "/{quizId}/questions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Quiz upsertQuestion(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String quizId,
            @RequestBody QuizQuestion question
    ) throws IOException {
        requireLecturer(auth);
        return store.upsertQuestion(courseId, quizId, question);
    }

    @DeleteMapping("/{quizId}/questions/{questionId}")
    public Quiz deleteQuestion(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String quizId,
            @PathVariable String questionId
    ) throws IOException {
        requireLecturer(auth);
        return store.deleteQuestion(courseId, quizId, questionId);
    }

    @GetMapping("/{quizId}/results")
    public List<QuizStore.TeacherAttemptDto> results(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String quizId
    ) {
        requireLecturer(auth);
        return store.results(courseId, quizId);
    }

    @PutMapping(value = "/{quizId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Quiz updateStatus(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String quizId,
            @RequestBody QuizStore.StatusRequest req
    ) throws IOException {
        requireLecturer(auth);
        if (req == null) throw new IllegalArgumentException("Chybí data.");
        return store.updateStatus(courseId, quizId, req.status());
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
