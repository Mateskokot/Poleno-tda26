package tda.app.app;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory store used only for the public (non-/api) endpoints exercised by
 * the Vitest phases (e.g. /courses, /courses/:id/materials, /courses/:id/quizzes).
 *
 * It purposefully does NOT integrate with the existing persistence layer. The intent
 * is to provide a deterministic contract for the automated tests.
 */
@Component
public class PublicTestStore {

    // -------------------- data models (match test expectations) --------------------

    public static class CoursePublic {
        public String uuid;
        public String name;
        public String description;
        public List<MaterialPublic> materials = new ArrayList<>();
        public List<QuizPublic> quizzes = new ArrayList<>();
    }

    public static class MaterialPublic {
        public String uuid;
        public String type; // "url" | "file"
        public String name;
        public String description;
        public String url;     // for type=url
        public String fileUrl; // for type=file
        public String mimeType;
        public long createdAtEpochMs;
    }

    public static class QuizPublic {
        public String uuid;
        public String title;
        public List<QuizQuestionPublic> questions = new ArrayList<>();
        public long createdAtEpochMs;
    }

    public static class QuizQuestionPublic {
        public String uuid;
        public String type; // singleChoice | multipleChoice
        public String question;
        public List<String> options = new ArrayList<>();
        public Integer correctIndex; // singleChoice
        public List<Integer> correctIndices; // multipleChoice
    }

    public static class QuizSubmitResponsePublic {
        public String quizUuid;
        public int score;
        public int maxScore;
        public String submittedAt;
    }

    // -------------------- storage --------------------

    private final Map<String, CoursePublic> courses = new ConcurrentHashMap<>();

    // -------------------- courses --------------------

    public CoursePublic createCourse(String name, String description) {
        CoursePublic c = new CoursePublic();
        c.uuid = uuidV4();
        c.name = name;
        c.description = description;
        courses.put(c.uuid, c);
        return c;
    }

    public Optional<CoursePublic> getCourse(String courseId) {
        return Optional.ofNullable(courses.get(courseId));
    }

    public void deleteCourse(String courseId) {
        courses.remove(courseId);
    }

    // -------------------- quizzes --------------------

    public List<QuizPublic> listQuizzes(String courseId) {
        CoursePublic c = requireCourse(courseId);
        // newest first
        List<QuizPublic> out = new ArrayList<>(c.quizzes);
        out.sort(Comparator.comparingLong((QuizPublic q) -> q.createdAtEpochMs).reversed());
        return out;
    }

    public QuizPublic createQuiz(String courseId, String title, List<QuizQuestionPublic> questions) {
        CoursePublic c = requireCourse(courseId);
        QuizPublic q = new QuizPublic();
        q.uuid = uuidV4();
        q.title = title;
        q.createdAtEpochMs = System.currentTimeMillis();
        q.questions = normalizeQuestions(questions);
        c.quizzes.add(q);
        return q;
    }

    public QuizPublic getQuiz(String courseId, String quizId) {
        CoursePublic c = requireCourse(courseId);
        return c.quizzes.stream()
                .filter(q -> Objects.equals(q.uuid, quizId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Quiz not found"));
    }

    public QuizPublic upsertQuiz(String courseId, String quizId, String title, List<QuizQuestionPublic> questions) {
        CoursePublic c = requireCourse(courseId);
        QuizPublic existing = c.quizzes.stream()
                .filter(q -> Objects.equals(q.uuid, quizId))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            QuizPublic created = new QuizPublic();
            created.uuid = quizId;
            created.title = title;
            created.createdAtEpochMs = System.currentTimeMillis();
            created.questions = normalizeQuestions(questions);
            c.quizzes.add(created);
            return created;
        }

        existing.title = title;
        // For simplicity and determinism, replace questions wholesale.
        existing.questions = normalizeQuestions(questions);
        return existing;
    }

    public void deleteQuiz(String courseId, String quizId) {
        CoursePublic c = requireCourse(courseId);
        c.quizzes.removeIf(q -> Objects.equals(q.uuid, quizId));
    }

    public QuizSubmitResponsePublic submitQuiz(String courseId, String quizId, List<Map<String, Object>> answers) {
        QuizPublic quiz = getQuiz(courseId, quizId);

        // index questions by uuid
        Map<String, QuizQuestionPublic> byId = new HashMap<>();
        for (QuizQuestionPublic qq : quiz.questions) {
            byId.put(qq.uuid, qq);
        }

        int max = quiz.questions.size();
        int score = 0;

        if (answers != null) {
            for (Map<String, Object> a : answers) {
                if (a == null) continue;
                Object uuidObj = a.get("uuid");
                if (!(uuidObj instanceof String uuid)) continue;
                QuizQuestionPublic qq = byId.get(uuid);
                if (qq == null) continue;

                if ("singleChoice".equals(qq.type)) {
                    Object selectedIndexObj = a.get("selectedIndex");
                    Integer selected = toInteger(selectedIndexObj);
                    if (selected != null && qq.correctIndex != null && selected.equals(qq.correctIndex)) {
                        score += 1;
                    }
                } else if ("multipleChoice".equals(qq.type)) {
                    Object selectedIndicesObj = a.get("selectedIndices");
                    List<Integer> selected = toIntegerList(selectedIndicesObj);
                    if (selected != null && qq.correctIndices != null) {
                        Set<Integer> s1 = new HashSet<>(selected);
                        Set<Integer> s2 = new HashSet<>(qq.correctIndices);
                        if (s1.equals(s2)) {
                            score += 1;
                        }
                    }
                }
            }
        }

        QuizSubmitResponsePublic resp = new QuizSubmitResponsePublic();
        resp.quizUuid = quiz.uuid;
        resp.score = score;
        resp.maxScore = max;
        resp.submittedAt = Instant.now().toString();
        return resp;
    }

    // -------------------- helpers --------------------

    private CoursePublic requireCourse(String courseId) {
        CoursePublic c = courses.get(courseId);
        if (c == null) throw new NoSuchElementException("Course not found");
        return c;
    }

    private static String uuidV4() {
        // Random UUID is v4.
        return UUID.randomUUID().toString();
    }

    private static List<QuizQuestionPublic> normalizeQuestions(List<QuizQuestionPublic> incoming) {
        List<QuizQuestionPublic> out = new ArrayList<>();
        if (incoming == null) return out;

        for (QuizQuestionPublic q : incoming) {
            if (q == null) continue;
            QuizQuestionPublic copy = new QuizQuestionPublic();
            copy.uuid = (q.uuid != null && !q.uuid.isBlank()) ? q.uuid : uuidV4();
            copy.type = q.type;
            copy.question = q.question;
            copy.options = (q.options != null) ? new ArrayList<>(q.options) : new ArrayList<>();
            copy.correctIndex = q.correctIndex;
            copy.correctIndices = (q.correctIndices != null) ? new ArrayList<>(q.correctIndices) : null;
            out.add(copy);
        }
        return out;
    }

    private static Integer toInteger(Object o) {
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> toIntegerList(Object o) {
        if (o instanceof List<?> list) {
            List<Integer> out = new ArrayList<>();
            for (Object x : list) {
                Integer i = toInteger(x);
                if (i != null) out.add(i);
            }
            return out;
        }
        return null;
    }
}
