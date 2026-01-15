package tda.app.app.quiz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Service
public class QuizStore {

    public record QuizSummaryDto(
            String id,
            String title,
            Instant createdAt,
            int filledCount,
            QuizStatus status
    ) {}

    public record PublicQuizDto(
            String id,
            String courseId,
            String title,
            Instant createdAt,
            List<PublicQuestionDto> questions,
            QuizStatus status
    ) {}

    public record PublicQuestionDto(
            String id,
            QuestionType type,
            String text,
            List<QuizOption> options
    ) {}

    private static final Path UPLOAD_ROOT = Paths.get("uploads");
    private static final Path QUIZZES_FILE = UPLOAD_ROOT.resolve("quizzes.json");
    private static final Path RESULTS_FILE = UPLOAD_ROOT.resolve("quiz_results.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // courseId -> quizzes
    private final Map<String, List<Quiz>> quizzesByCourse = new HashMap<>();
    // quizId -> results
    private final Map<String, List<QuizResult>> resultsByQuiz = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(UPLOAD_ROOT);
            load();
        } catch (Exception e) {
            System.err.println("WARN: Nelze načíst kvízy z disku: " + e.getMessage());
        }
    }

    public List<QuizSummaryDto> listSummaries(String courseId) {
        synchronized (quizzesByCourse) {
            List<Quiz> list = new ArrayList<>(quizzesByCourse.getOrDefault(courseId, List.of()));
            list.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));

            return list.stream()
                    .map(q -> new QuizSummaryDto(
                            q.id(),
                            q.title(),
                            q.createdAt(),
                            resultsByQuiz.getOrDefault(q.id(), List.of()).size(),
                            statusOrOpen(q)
                    ))
                    .toList();
        }
    }

    public PublicQuizDto getPublicQuiz(String courseId, String quizId) {
        Quiz q = getQuizOrThrow(courseId, quizId);
        List<PublicQuestionDto> questions = (q.questions() == null ? List.<QuizQuestion>of() : q.questions())
                .stream()
                .map(qq -> new PublicQuestionDto(qq.id(), qq.type(), qq.text(), qq.options() == null ? List.of() : qq.options()))
                .toList();
        return new PublicQuizDto(q.id(), q.courseId(), q.title(), q.createdAt(), questions, statusOrOpen(q));
    }

    public Quiz getFullQuiz(String courseId, String quizId) {
        return getQuizOrThrow(courseId, quizId);
    }

    public Quiz createQuiz(String courseId, String title) throws IOException {
        String t = (title == null) ? "" : title.trim();
        if (t.isBlank()) throw new IllegalArgumentException("Chybí název kvízu.");

        Quiz q = new Quiz(
                UUID.randomUUID().toString(),
                courseId,
                t,
                Instant.now(),
                new ArrayList<>(),
                QuizStatus.OPEN
        );

        synchronized (quizzesByCourse) {
            quizzesByCourse.computeIfAbsent(courseId, k -> new ArrayList<>()).add(q);
            save();
        }
        return q;
    }

    public Quiz updateQuizTitle(String courseId, String quizId, String title) throws IOException {
        String t = (title == null) ? "" : title.trim();
        if (t.isBlank()) throw new IllegalArgumentException("Chybí název kvízu.");

        synchronized (quizzesByCourse) {
            List<Quiz> list = new ArrayList<>(quizzesByCourse.getOrDefault(courseId, List.of()));
            int idx = indexOfQuiz(list, quizId);
            if (idx < 0) throw new IllegalArgumentException("Kvíz nebyl nalezen.");
            Quiz old = list.get(idx);
            Quiz updated = new Quiz(old.id(), old.courseId(), t, old.createdAt(), old.questions(), statusOrOpen(old));
            list.set(idx, updated);
            quizzesByCourse.put(courseId, list);
            save();
            return updated;
        }
    }

    public void deleteQuiz(String courseId, String quizId) throws IOException {
        synchronized (quizzesByCourse) {
            List<Quiz> list = new ArrayList<>(quizzesByCourse.getOrDefault(courseId, List.of()));
            int idx = indexOfQuiz(list, quizId);
            if (idx < 0) throw new IllegalArgumentException("Kvíz nebyl nalezen.");
            list.remove(idx);
            quizzesByCourse.put(courseId, list);
            resultsByQuiz.remove(quizId);
            save();
        }
    }

    public Quiz upsertQuestion(String courseId, String quizId, QuizQuestion incoming) throws IOException {
        if (incoming == null) throw new IllegalArgumentException("Chybí otázka.");
        if (incoming.type() == null) throw new IllegalArgumentException("Chybí typ otázky.");

        String text = (incoming.text() == null) ? "" : incoming.text().trim();
        if (text.isBlank()) throw new IllegalArgumentException("Text otázky nesmí být prázdný.");

        List<QuizOption> options = normalizeOptions(incoming.options());
        if (options.size() < 2) throw new IllegalArgumentException("Otázka musí mít alespoň 2 možnosti.");

        List<String> correct = normalizeCorrect(incoming.type(), incoming.correctOptionIds(), options);

        String qId = (incoming.id() == null || incoming.id().isBlank()) ? UUID.randomUUID().toString() : incoming.id();
        QuizQuestion normalized = new QuizQuestion(qId, incoming.type(), text, options, correct);

        synchronized (quizzesByCourse) {
            List<Quiz> list = new ArrayList<>(quizzesByCourse.getOrDefault(courseId, List.of()));
            int idx = indexOfQuiz(list, quizId);
            if (idx < 0) throw new IllegalArgumentException("Kvíz nebyl nalezen.");
            Quiz old = list.get(idx);
            List<QuizQuestion> qs = new ArrayList<>(old.questions() == null ? List.of() : old.questions());

            int qIdx = indexOfQuestion(qs, qId);
            if (qIdx < 0) {
                qs.add(normalized);
            } else {
                qs.set(qIdx, normalized);
            }

            Quiz updated = new Quiz(old.id(), old.courseId(), old.title(), old.createdAt(), qs, statusOrOpen(old));
            list.set(idx, updated);
            quizzesByCourse.put(courseId, list);
            save();
            return updated;
        }
    }

    public Quiz deleteQuestion(String courseId, String quizId, String questionId) throws IOException {
        synchronized (quizzesByCourse) {
            List<Quiz> list = new ArrayList<>(quizzesByCourse.getOrDefault(courseId, List.of()));
            int idx = indexOfQuiz(list, quizId);
            if (idx < 0) throw new IllegalArgumentException("Kvíz nebyl nalezen.");
            Quiz old = list.get(idx);
            List<QuizQuestion> qs = new ArrayList<>(old.questions() == null ? List.of() : old.questions());
            int qIdx = indexOfQuestion(qs, questionId);
            if (qIdx < 0) throw new IllegalArgumentException("Otázka nebyla nalezena.");
            qs.remove(qIdx);
            Quiz updated = new Quiz(old.id(), old.courseId(), old.title(), old.createdAt(), qs, statusOrOpen(old));
            list.set(idx, updated);
            quizzesByCourse.put(courseId, list);
            save();
            return updated;
        }
    }

    public record StatusRequest(QuizStatus status) {}

    public Quiz updateStatus(String courseId, String quizId, QuizStatus status) throws IOException {
        if (status == null) throw new IllegalArgumentException("Chybí status.");
        synchronized (quizzesByCourse) {
            List<Quiz> list = new ArrayList<>(quizzesByCourse.getOrDefault(courseId, List.of()));
            int idx = indexOfQuiz(list, quizId);
            if (idx < 0) throw new IllegalArgumentException("Kvíz nebyl nalezen.");
            Quiz old = list.get(idx);
            Quiz updated = new Quiz(old.id(), old.courseId(), old.title(), old.createdAt(), old.questions(), status);
            list.set(idx, updated);
            quizzesByCourse.put(courseId, list);
            save();
            return updated;
        }
    }

    public record SubmitRequest(Map<String, List<String>> answers) {}

    public record SubmitResponse(
            int totalQuestions,
            int correctQuestions,
            double scorePercent,
            List<QuestionResult> details
    ) {}

    public record QuestionResult(
            String questionId,
            boolean correct,
            List<String> correctOptionIds,
            List<String> selectedOptionIds
    ) {}

    public SubmitResponse submit(String courseId, String quizId, SubmitRequest req, String studentKey) throws IOException {
        Quiz quiz = getQuizOrThrow(courseId, quizId);
        if (statusOrOpen(quiz) == QuizStatus.CLOSED) {
            throw new IllegalArgumentException("Kvíz je uzavřený. Nový pokus nelze odevzdat.");
        }

        Map<String, List<String>> answers = (req == null || req.answers() == null) ? Map.of() : req.answers();
        List<QuizQuestion> questions = quiz.questions() == null ? List.of() : quiz.questions();

        int total = questions.size();
        int correct = 0;
        List<QuestionResult> details = new ArrayList<>();

        for (QuizQuestion q : questions) {
            List<String> selected = new ArrayList<>(answers.getOrDefault(q.id(), List.of()));
            Set<String> sel = new HashSet<>(selected);
            Set<String> cor = new HashSet<>(q.correctOptionIds() == null ? List.of() : q.correctOptionIds());

            boolean ok = sel.equals(cor);
            if (ok) correct++;
            details.add(new QuestionResult(
                    q.id(),
                    ok,
                    new ArrayList<>(cor),
                    new ArrayList<>(sel)
            ));
        }

        double percent = total == 0 ? 0.0 : (correct * 100.0 / total);

        QuizResult result = new QuizResult(
                UUID.randomUUID().toString(),
                courseId,
                quizId,
                Instant.now(),
                total,
                correct,
                (studentKey == null || studentKey.isBlank()) ? null : studentKey
        );

        synchronized (quizzesByCourse) {
            resultsByQuiz.computeIfAbsent(quizId, k -> new ArrayList<>()).add(result);
            save();
        }

        return new SubmitResponse(total, correct, Math.round(percent * 10.0) / 10.0, details);
    }

    public record TeacherAttemptDto(
            String id,
            String courseId,
            String quizId,
            Instant submittedAt,
            int totalQuestions,
            int correctQuestions
    ) {}

    public List<TeacherAttemptDto> results(String courseId, String quizId) {
        // courseId is checked only for safety (quiz must exist in that course)
        getQuizOrThrow(courseId, quizId);
        synchronized (quizzesByCourse) {
            List<QuizResult> list = new ArrayList<>(resultsByQuiz.getOrDefault(quizId, List.of()));
            list.sort((a, b) -> b.submittedAt().compareTo(a.submittedAt()));
            return list.stream()
                    .map(r -> new TeacherAttemptDto(r.id(), r.courseId(), r.quizId(), r.submittedAt(), r.totalQuestions(), r.correctQuestions()))
                    .toList();
        }
    }

    // Pozn.: požadavek zadání: výsledky jsou dostupné pouze lektorovi.
    // Studentům zobrazujeme historii pokusů pouze lokálně v prohlížeči (viz quiz.html).

    // --------------- helpers ---------------

    private Quiz getQuizOrThrow(String courseId, String quizId) {
        synchronized (quizzesByCourse) {
            return quizzesByCourse.getOrDefault(courseId, List.of())
                    .stream()
                    .filter(q -> Objects.equals(q.id(), quizId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Kvíz nebyl nalezen."));
        }
    }

    private QuizStatus statusOrOpen(Quiz q) {
        return (q == null || q.status() == null) ? QuizStatus.OPEN : q.status();
    }

    private int indexOfQuiz(List<Quiz> list, String quizId) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).id(), quizId)) return i;
        }
        return -1;
    }

    private int indexOfQuestion(List<QuizQuestion> list, String questionId) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).id(), questionId)) return i;
        }
        return -1;
    }

    private List<QuizOption> normalizeOptions(List<QuizOption> incoming) {
        List<QuizOption> out = new ArrayList<>();
        if (incoming != null) {
            for (QuizOption o : incoming) {
                if (o == null) continue;
                String txt = (o.text() == null) ? "" : o.text().trim();
                if (txt.isBlank()) continue;
                String id = (o.id() == null || o.id().isBlank()) ? UUID.randomUUID().toString() : o.id();
                out.add(new QuizOption(id, txt));
            }
        }
        return out;
    }

    private List<String> normalizeCorrect(QuestionType type, List<String> incomingCorrect, List<QuizOption> options) {
        Set<String> allowed = new HashSet<>();
        for (QuizOption o : options) allowed.add(o.id());

        Set<String> correct = new LinkedHashSet<>();
        if (incomingCorrect != null) {
            for (String id : incomingCorrect) {
                if (id == null) continue;
                if (allowed.contains(id)) correct.add(id);
            }
        }

        if (type == QuestionType.SINGLE && correct.size() != 1) {
            throw new IllegalArgumentException("Otázka SINGLE musí mít právě 1 správnou odpověď.");
        }
        if (type == QuestionType.MULTI && correct.isEmpty()) {
            throw new IllegalArgumentException("Otázka MULTI musí mít alespoň 1 správnou odpověď.");
        }

        return new ArrayList<>(correct);
    }

    private void load() throws IOException {
        if (Files.exists(QUIZZES_FILE)) {
            Map<String, List<Quiz>> map = MAPPER.readValue(QUIZZES_FILE.toFile(), new TypeReference<>() {});
            quizzesByCourse.clear();
            if (map != null) quizzesByCourse.putAll(map);
        }
        if (Files.exists(RESULTS_FILE)) {
            Map<String, List<QuizResult>> map = MAPPER.readValue(RESULTS_FILE.toFile(), new TypeReference<>() {});
            resultsByQuiz.clear();
            if (map != null) resultsByQuiz.putAll(map);
        }
    }

    private void save() throws IOException {
        // keep the on-disk format stable
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(QUIZZES_FILE.toFile(), quizzesByCourse);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(RESULTS_FILE.toFile(), resultsByQuiz);
    }
}
