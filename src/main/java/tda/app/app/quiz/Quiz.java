package tda.app.app.quiz;

import java.time.Instant;
import java.util.List;

public record Quiz(
        String id,
        String courseId,
        String title,
        Instant createdAt,
        List<QuizQuestion> questions
) {}
