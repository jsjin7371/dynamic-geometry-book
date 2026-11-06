package geometrybook.domain.quiz;

import java.util.ArrayList;
import java.util.List;

/** 한 번의 퀴즈 시도. 학습 진도 및 영구 저장과 독립적이다. */
public final class QuizAttempt {
    private final List<QuizQuestion> questions;
    private final List<QuizQuestion> wrong = new ArrayList<>();
    private int index;
    private int correct;
    private boolean submitted;

    public QuizAttempt(List<QuizQuestion> questions) {
        if (questions.isEmpty()) throw new IllegalArgumentException("문제가 없습니다.");
        this.questions = List.copyOf(questions);
    }

    public QuizQuestion current() { return questions.get(index); }
    public int number() { return index + 1; }
    public int total() { return questions.size(); }
    public int correctCount() { return correct; }
    public boolean isSubmitted() { return submitted; }
    public boolean isFinished() { return index == questions.size(); }
    public List<QuizQuestion> wrongQuestions() { return List.copyOf(wrong); }

    /** 보기 ID로 한 번만 채점한다. 미선택·다른 문제의 보기·중복 제출을 거부한다. */
    public boolean submit(String choiceId) {
        if (isFinished() || submitted) throw new IllegalStateException("이미 제출했습니다.");
        QuizQuestion.Choice choice = current().choices.stream()
                .filter(c -> c.id.equals(choiceId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("보기를 선택해 주세요."));
        submitted = true;
        if (choice.isCorrect) correct++;
        else wrong.add(current());
        return choice.isCorrect;
    }

    public void next() {
        if (!submitted || isFinished()) throw new IllegalStateException("먼저 제출해 주세요.");
        index++;
        submitted = false;
    }
}
