package geometrybook.storage;

import geometrybook.domain.quiz.QuizQuestion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 외부 테스트 라이브러리 없이 실행하는 퀴즈 CSV 읽기 검증. 임시 폴더에서 실행한다. */
public final class CsvQuizSourceTest {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("quiz");
        Files.writeString(dir.resolve("questions.csv"), String.join("\n",
                "id,levelId,type,prompt,reviewSceneNo,figure,hint,explanation",
                "t1,TRIANGLE,OX,문제 하나,1,\"angles:?,50,60\",힌트 글,\"해설, 쉼표 포함\"",
                "t2,TRIANGLE,OX,문제 둘,1,,,",
                "s1,SQUARE,OX,다른 단원,1,,,"));
        Files.writeString(dir.resolve("answer_choices.csv"), String.join("\n",
                "id,questionId,choiceNo,content,isCorrect",
                "t1-1,t1,1,O,true", "t1-2,t1,2,X,false",
                "t2-1,t2,1,O,false", "t2-2,t2,2,X,true",
                "s1-1,s1,1,O,true", "s1-2,s1,2,X,false"));

        List<QuizQuestion> triangle = new CsvQuizSource(dir).findByLevelId("TRIANGLE");
        check(triangle.size() == 2, "단원별로 골라 읽기");
        QuizQuestion first = triangle.get(0);
        check(first.figure.equals("angles:?,50,60"), "그림 칸 읽기 (쉼표가 있어도 따옴표로 한 칸)");
        check(first.hint.equals("힌트 글"), "힌트 칸 읽기");
        check(first.explanation.equals("해설, 쉼표 포함"), "해설 칸 읽기");
        QuizQuestion second = triangle.get(1);
        check(second.figure.isEmpty() && second.hint.isEmpty() && second.explanation.isEmpty(), "빈 칸은 빈 문자열");
        check(new CsvQuizSource(dir).findByIds(List.of("s1", "t1", "없음")).size() == 2, "ID로 찾기, 없는 ID는 건너뜀");

        Files.writeString(dir.resolve("questions.csv"), "id,levelId,type,prompt,reviewSceneNo\nt1,TRIANGLE,OX,옛 형식,1");
        try {
            new CsvQuizSource(dir).findByLevelId("TRIANGLE");
            throw new AssertionError("옛 헤더를 그대로 읽음");
        } catch (DataFileException expected) { }
        System.out.println("CsvQuizSourceTest 통과");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
