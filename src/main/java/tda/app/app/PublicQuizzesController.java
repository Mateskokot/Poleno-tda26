package tda.app.app;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Public (non-/api) quiz endpoints required by Vitest phase 3.
 */
@RestController
@RequestMapping("/courses/{courseId}/quizzes")
public class PublicQuizzesController {

    private final PublicTestStore store;

    public PublicQuizzesController(PublicTestStore store) {
        this.store = store;
    }

    // ---------- request payloads (match test) ----------

    public record QuizUpsertRequest(String title, List<QuestionRequest> questions) {}

    public record QuestionRequest(
            String type,
            String question,
            List<String> options,
            Integer correctIndex,
            List<Integer> correctIndices
    ) {}

    public record SubmitRequest(List<Map<String, Object>> answers) {}

    // ---------- CRUD ----------

    @GetMapping
    public List<PublicTestStore.QuizPublic> list(@PathVariable String courseId) {
        return store.listQuizzes(courseId);
    }

    @PostMapping
    public PublicTestStore.QuizPublic create(
            @PathVariable String courseId,
            @RequestBody QuizUpsertRequest req
    ) {
        validateQuiz(req);
        return store.createQuiz(courseId, req.title(), toQuestions(req.questions()));
    }

    @GetMapping("/{quizId}")
    public PublicTestStore.QuizPublic get(
            @PathVariable String courseId,
            @PathVariable String quizId
    ) {
        return store.getQuiz(courseId, quizId);
    }

    @PutMapping("/{quizId}")
    public PublicTestStore.QuizPublic update(
            @PathVariable String courseId,
            @PathVariable String quizId,
            @RequestBody QuizUpsertRequest req
    ) {
        validateQuiz(req);
        return store.upsertQuiz(courseId, quizId, req.title(), toQuestions(req.questions()));
    }

    @DeleteMapping("/{quizId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String courseId,
            @PathVariable String quizId
    ) {
        store.deleteQuiz(courseId, quizId);
    }

    // ---------- submit ----------

    @PostMapping("/{quizId}/submit")
    public PublicTestStore.QuizSubmitResponsePublic submit(
            @PathVariable String courseId,
            @PathVariable String quizId,
            @RequestBody SubmitRequest req
    ) {
        return store.submitQuiz(courseId, quizId, req == null ? null : req.answers());
    }

    // ---------- helpers ----------

    private static void validateQuiz(QuizUpsertRequest req) {
        if (req == null || req.title() == null || req.title().isBlank()) {
            throw new IllegalArgumentException("Missing title");
        }
        if (req.questions() == null) {
            throw new IllegalArgumentException("Missing questions");
        }
    }

    private static List<PublicTestStore.QuizQuestionPublic> toQuestions(List<QuestionRequest> questions) {
        List<PublicTestStore.QuizQuestionPublic> out = new ArrayList<>();
        if (questions == null) return out;
        for (QuestionRequest q : questions) {
            if (q == null) continue;
            PublicTestStore.QuizQuestionPublic qq = new PublicTestStore.QuizQuestionPublic();
            qq.type = q.type();
            qq.question = q.question();
            qq.options = q.options() == null ? new ArrayList<>() : new ArrayList<>(q.options());
            qq.correctIndex = q.correctIndex();
            qq.correctIndices = q.correctIndices() == null ? null : new ArrayList<>(q.correctIndices());
            // uuid assigned in store normalize
            out.add(qq);
        }
        return out;
    }
}
