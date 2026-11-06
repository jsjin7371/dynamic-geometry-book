package geometrybook.application.quiz;

import geometrybook.domain.quiz.QuizQuestion;
import java.util.ArrayList;
import java.util.List;

/** storage API가 준비되기 전 사용하는 삼각형 심화 문제. 파일에 접근하지 않는다. */
public final class DemoQuizSource implements QuizSource {
    @Override
    public List<QuizQuestion> findByLevelId(String levelId) {
        if (!"TRIANGLE".equals(levelId)) return List.of();
        return List.of(
                question("triangle-quiz-1", "삼각형의 두 각이 50도와 60도라면 나머지 각은 몇 도일까요?",
                        QuizQuestion.Type.MULTIPLE_CHOICE, new String[]{"60도", "70도", "80도"}, 1),
                question("triangle-quiz-2", "한 삼각형 안에 직각(90도)이 두 개 있을 수 있다.",
                        QuizQuestion.Type.OX, new String[]{"O", "X"}, 1),
                question("triangle-quiz-3", "세 각의 크기가 모두 같은 삼각형의 한 각은 몇 도일까요?",
                        QuizQuestion.Type.MULTIPLE_CHOICE, new String[]{"45도", "60도", "90도"}, 1),
                question("triangle-quiz-4", "삼각형을 더 크게 그리면 세 내각의 합도 커진다.",
                        QuizQuestion.Type.OX, new String[]{"O", "X"}, 1),
                question("triangle-quiz-5", "한 각이 100도이고 나머지 두 각이 같다면, 같은 두 각은 각각 몇 도일까요?",
                        QuizQuestion.Type.MULTIPLE_CHOICE, new String[]{"40도", "50도", "80도"}, 0));
    }

    private QuizQuestion question(String id, String prompt, QuizQuestion.Type type, String[] options, int answer) {
        List<QuizQuestion.Choice> choices = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            choices.add(new QuizQuestion.Choice(id + "-" + (i + 1), id, i + 1, options[i], i == answer));
        }
        // 현재 설명은 하나의 애니메이션 화면이므로 복습 장면 번호는 1이다.
        return new QuizQuestion(id, "TRIANGLE", type, prompt, 1, choices);
    }
}
