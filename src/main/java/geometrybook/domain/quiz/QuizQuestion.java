package geometrybook.domain.quiz;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** ERD의 question과 answerChoice를 조회 화면에서 함께 사용하는 불변 모델. */
public final class QuizQuestion {
    public enum Type { OX, MULTIPLE_CHOICE }

    public static final class Choice {
        public final String id;
        public final String questionId;
        public final int choiceNo;
        public final String content;
        public final boolean isCorrect;

        public Choice(String id, String questionId, int choiceNo, String content, boolean isCorrect) {
            this.id = id;
            this.questionId = questionId;
            this.choiceNo = choiceNo;
            this.content = content;
            this.isCorrect = isCorrect;
        }
    }

    public final String id;
    public final String levelId;
    public final Type type;
    public final String prompt;
    public final int reviewSceneNo;
    public final List<Choice> choices;
    /** 문제 옆에 그릴 그림 설명 (예: angles:?,50,60). 없으면 빈 문자열. */
    public final String figure;
    /** 힌트 버튼을 누르면 보여 줄 글. 없으면 빈 문자열. */
    public final String hint;
    /** 채점 후 보여 줄 해설. 없으면 빈 문자열. */
    public final String explanation;

    /** 그림·힌트·해설이 없는 문제. */
    public QuizQuestion(String id, String levelId, Type type, String prompt,
                        int reviewSceneNo, List<Choice> choices) {
        this(id, levelId, type, prompt, reviewSceneNo, choices, "", "", "");
    }

    public QuizQuestion(String id, String levelId, Type type, String prompt, int reviewSceneNo,
                        List<Choice> choices, String figure, String hint, String explanation) {
        this.id = id;
        this.levelId = levelId;
        this.type = type;
        this.prompt = prompt;
        this.reviewSceneNo = reviewSceneNo;
        this.choices = List.copyOf(choices);
        this.figure = figure;
        this.hint = hint;
        this.explanation = explanation;
        Set<Integer> numbers = new HashSet<>();
        Set<String> ids = new HashSet<>();
        if (type == null || prompt == null || prompt.isBlank() || reviewSceneNo < 1
                || choices.size() < 2 || choices.stream().filter(c -> c.isCorrect).count() != 1) {
            throw new IllegalArgumentException("문제 형식 또는 정답 구성이 올바르지 않습니다.");
        }
        for (Choice choice : choices) {
            if (!id.equals(choice.questionId) || !numbers.add(choice.choiceNo) || !ids.add(choice.id)) {
                throw new IllegalArgumentException("보기의 참조 또는 번호가 올바르지 않습니다.");
            }
        }
        if (type == Type.OX && (choices.size() != 2
                || !Set.of("O", "X").equals(new HashSet<>(List.of(choices.get(0).content, choices.get(1).content))))) {
            throw new IllegalArgumentException("OX 문제는 O와 X 보기가 필요합니다.");
        }
    }

    public Choice correctChoice() {
        return choices.stream().filter(c -> c.isCorrect).findFirst().orElseThrow();
    }
}
