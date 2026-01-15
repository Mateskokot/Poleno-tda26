package tda.app.app.quiz;

import java.util.List;

public record QuizQuestion(
        String id,
        QuestionType type,
        String text,
        List<QuizOption> options,
        List<String> correctOptionIds
) {}
