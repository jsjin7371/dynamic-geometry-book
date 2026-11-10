package geometrybook.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 학생별 오답 기록(data/wrong_answers.csv) 읽기·쓰기.
 * 열: id, studentId, questionId. 학생·문제당 한 행이고, 다시 맞히면 행을 지운다.
 *
 * 사용 예:
 *   WrongAnswerRepository wrongs = new WrongAnswerRepository(Path.of("data"));
 *   wrongs.add(studentId, "triangle-quiz-2");      // 틀렸을 때
 *   wrongs.remove(studentId, "triangle-quiz-2");   // 다시 맞혔을 때
 *   Set<String> ids = wrongs.findQuestionIds(studentId);
 */
public final class WrongAnswerRepository {

    public static final String FILE_NAME = "wrong_answers.csv";
    private static final List<String> HEADER = List.of("id", "studentId", "questionId");

    private final Path file;

    public WrongAnswerRepository(Path dataDir) {
        this.file = dataDir.resolve(FILE_NAME);
    }

    /** 학생이 틀린 문제 ID (틀린 순서). 파일이 없으면 빈 목록. */
    public synchronized Set<String> findQuestionIds(String studentId) {
        Set<String> result = new LinkedHashSet<>();
        for (List<String> r : readRows()) {
            if (r.get(1).equals(studentId)) result.add(r.get(2));
        }
        return result;
    }

    /** 오답을 기록한다. 이미 있으면 그대로 둔다. */
    public synchronized void add(String studentId, String questionId) {
        List<List<String>> rows = readRows();
        for (List<String> r : rows) {
            if (r.get(1).equals(studentId) && r.get(2).equals(questionId)) return;
        }
        rows.add(List.of(UUID.randomUUID().toString(), studentId, questionId));
        write(rows);
    }

    /** 다시 맞힌 문제의 오답 기록을 지운다. 기록이 없으면 아무것도 하지 않는다. */
    public synchronized void remove(String studentId, String questionId) {
        List<List<String>> rows = readRows();
        boolean removed = rows.removeIf(r -> r.get(1).equals(studentId) && r.get(2).equals(questionId));
        if (removed) write(rows);
    }

    private void write(List<List<String>> rows) {
        try {
            CsvCodec.writeAtomically(file, HEADER, rows);
        } catch (IOException e) {
            throw new UncheckedIOException(FILE_NAME + "에 저장하지 못했습니다.", e);
        }
    }

    /** 헤더를 뺀 행 목록. 헤더가 다르면 다른 파일을 덮어쓰지 않도록 오류를 낸다. */
    private List<List<String>> readRows() {
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            List<List<String>> rows = CsvCodec.read(file);
            if (rows.isEmpty() || !rows.get(0).equals(HEADER)) {
                throw new DataFileException(FILE_NAME + " 헤더가 올바르지 않습니다. 기대값: " + String.join(",", HEADER));
            }
            return new ArrayList<>(rows.subList(1, rows.size()));
        } catch (IOException e) {
            throw new UncheckedIOException(FILE_NAME + "을(를) 읽지 못했습니다.", e);
        }
    }
}
