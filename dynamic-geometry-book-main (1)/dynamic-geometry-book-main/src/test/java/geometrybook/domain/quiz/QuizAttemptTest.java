package geometrybook.domain.quiz;

import geometrybook.application.quiz.DemoQuizSource;
import java.util.List;

public final class QuizAttemptTest {
    public static void main(String[] args) {
        DemoQuizSource source = new DemoQuizSource();
        check(source.findByLevelId("SQUARE").isEmpty(), "미구현 단원");
        List<QuizQuestion> questions = source.findByLevelId("TRIANGLE");
        check(questions.size() == 5, "삼각형 다섯 문제");
        QuizAttempt attempt = new QuizAttempt(questions);
        try {
            attempt.submit(null);
            throw new AssertionError("미선택 허용");
        } catch (IllegalArgumentException expected) { }
        check(!attempt.isSubmitted(), "미선택 후 제출 가능");
        String wrong = questions.get(0).choices.stream().filter(c -> !c.isCorrect).findFirst().orElseThrow().id;
        check(!attempt.submit(wrong), "오답 채점");
        try {
            attempt.submit(questions.get(0).correctChoice().id);
            throw new AssertionError("중복 제출 허용");
        } catch (IllegalStateException expected) { }
        attempt.next();
        while (!attempt.isFinished()) {
            attempt.submit(attempt.current().correctChoice().id);
            attempt.next();
        }
        check(attempt.correctCount() == 4 && attempt.wrongQuestions().size() == 1, "최종 점수와 오답");
        QuizAttempt retry = new QuizAttempt(attempt.wrongQuestions());
        check(retry.total() == 1 && !retry.isSubmitted(), "오답만 새 시도");
        retry.submit(retry.current().correctChoice().id);
        retry.next();
        check(retry.correctCount() == 1 && retry.wrongQuestions().isEmpty(), "오답 재도전 성공");
        check(attempt.correctCount() == 4, "이전 시도 결과 불변");
        System.out.println("QuizAttempt tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
