package geometrybook.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 외부 테스트 라이브러리 없이 실행하는 오답 기록 저장 검증. 임시 폴더에서 실행한다. */
public final class WrongAnswerRepositoryTest {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wrong");
        WrongAnswerRepository repo = new WrongAnswerRepository(dir);
        check(repo.findQuestionIds("a").isEmpty(), "파일이 없으면 빈 오답");
        repo.remove("a", "q1");
        check(!Files.exists(dir.resolve("wrong_answers.csv")), "없는 오답 삭제는 파일을 만들지 않음");

        repo.add("a", "q2");
        repo.add("a", "q1");
        repo.add("a", "q2");
        check(Files.readAllLines(dir.resolve("wrong_answers.csv")).size() == 3, "학생·문제당 한 행만 저장");
        check(List.copyOf(new WrongAnswerRepository(dir).findQuestionIds("a")).equals(List.of("q2", "q1")),
                "재시작 후에도 틀린 순서대로 유지");
        check(repo.findQuestionIds("b").isEmpty(), "학생별 오답 분리");

        repo.add("b", "q2");
        repo.remove("a", "q2");
        check(List.copyOf(repo.findQuestionIds("a")).equals(List.of("q1")), "다시 맞히면 삭제");
        check(List.copyOf(repo.findQuestionIds("b")).equals(List.of("q2")), "다른 학생 오답은 그대로");
        System.out.println("WrongAnswerRepositoryTest 통과");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
