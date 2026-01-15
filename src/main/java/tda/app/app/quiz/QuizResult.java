package tda.app.app.quiz;

import java.time.Instant;

public record QuizResult(
        String id,
        String courseId,
        String quizId,
        Instant submittedAt,
        int totalQuestions,
        int correctQuestions
) {}
