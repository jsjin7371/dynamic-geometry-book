package geometrybook.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 학생별 레벨 완료 기록(data/progress.csv) 읽기·쓰기.
 * 열: id, studentId, levelId, completed. levelId는 Level 이름(TRIANGLE 등)이다.
 *
 * 사용 예:
 *   ProgressRepository progress = new ProgressRepository(Path.of("data"));
 *   Set<String> done = progress.findCompletedLevelIds(studentId);
 *   progress.markCompleted(studentId, "TRIANGLE");
 */
public final class ProgressRepository {

    public static final String FILE_NAME = "progress.csv";
    private static final List<String> HEADER = List.of("id", "studentId", "levelId", "completed");

    private final Path file;

    public ProgressRepository(Path dataDir) {
        this.file = dataDir.resolve(FILE_NAME);
    }

    /** 학생이 완료한 레벨 ID. 파일이 없으면(아직 아무도 완료하지 않음) 빈 목록. */
    public synchronized Set<String> findCompletedLevelIds(String studentId) {
        Set<String> result = new HashSet<>();
        for (List<String> r : readRows()) {
            if (r.get(1).equals(studentId) && Boolean.parseBoolean(r.get(3))) {
                result.add(r.get(2));
            }
        }
        return result;
    }

    /** 레벨 완료를 저장한다. 이미 저장된 학생·레벨이면 그대로 둬서 한 행만 유지한다. */
    public synchronized void markCompleted(String studentId, String levelId) {
        List<List<String>> rows = readRows();
        for (List<String> r : rows) {
            if (r.get(1).equals(studentId) && r.get(2).equals(levelId)) {
                return;
            }
        }
        rows.add(List.of(UUID.randomUUID().toString(), studentId, levelId, "true"));
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
