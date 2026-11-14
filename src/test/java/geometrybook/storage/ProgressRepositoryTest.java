package geometrybook.storage;

import geometrybook.domain.Level;
import geometrybook.domain.StudyProgress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** 외부 테스트 라이브러리 없이 실행하는 학습 진도 저장 검증. 임시 폴더에서 실행한다. */
public final class ProgressRepositoryTest {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("progress");
        ProgressRepository repo = new ProgressRepository(dir);
        check(repo.findCompletedLevelIds("a").isEmpty(), "파일이 없으면 빈 진도");

        repo.markCompleted("a", "TRIANGLE");
        repo.markCompleted("a", "TRIANGLE");
        check(Files.readAllLines(dir.resolve("progress.csv")).size() == 2, "학생·레벨당 한 행만 저장");
        check(new ProgressRepository(dir).findCompletedLevelIds("a").equals(Set.of("TRIANGLE")), "재시작 후에도 완료 유지");
        check(repo.findCompletedLevelIds("b").isEmpty(), "학생별 진도 분리");

        StudyProgress study = new StudyProgress(List.of(Level.TRIANGLE),
                topic -> repo.markCompleted("a", topic.name()));
        check(study.start(Level.SQUARE) != null, "저장된 완료로 다음 레벨 열림");
        StudyProgress.Session square = study.start(Level.SQUARE);
        square.confirmExplanation();
        for (int i = 0; i < 3; i++) square.submit(i, Level.SQUARE.answer(i));
        study.complete(square);
        check(repo.findCompletedLevelIds("a").equals(Set.of("TRIANGLE", "SQUARE")), "레벨 완료 시 파일에 저장");

        StudyProgress failing = new StudyProgress(List.of(), topic -> {
            throw new IllegalStateException("디스크 오류");
        });
        StudyProgress.Session triangle = failing.start(Level.TRIANGLE);
        triangle.confirmExplanation();
        for (int i = 0; i < 3; i++) triangle.submit(i, Level.TRIANGLE.answer(i));
        try {
            failing.complete(triangle);
            throw new AssertionError("저장 실패인데 예외 없음");
        } catch (IllegalStateException expected) {
        }
        check(!failing.isCompleted(Level.TRIANGLE), "저장 실패면 완료로 표시하지 않음");
        System.out.println("ProgressRepositoryTest 통과");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
