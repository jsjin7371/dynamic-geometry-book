package geometrybook.application.quiz;

import geometrybook.domain.quiz.QuizQuestion;
import java.util.List;

/** 심화 퀴즈 조회 계약. storage 구현이 준비되면 어댑터로 연결한다. */
public interface QuizSource {
    /** 레벨 ID에 속하는 심화 문제와 보기를 반환한다. 문제가 없으면 빈 목록을 반환한다. */
    List<QuizQuestion> findByLevelId(String levelId);
}
