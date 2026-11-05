package geometrybook.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 읽기 전용 CSV 파일(퀴즈 문제·보기 같은 고정 콘텐츠)을 객체 목록으로 읽는 공통 부모 클래스.
 * 파일 읽기 → 헤더 확인 → 열 개수 확인 → 한 행씩 객체로 변환 → 파일 안 규칙 검사 순서는 여기서 처리하고,
 * 행을 어떤 객체로 바꿀지(fromRow)와 파일 안 규칙(validate)만 자식 클래스가 정한다.
 *
 * @param <T> 한 행이 변환될 객체 타입
 */
public abstract class CsvRecordReader<T> {

    private final Path file;

    protected CsvRecordReader(Path dataDir, String fileName) {
        this.file = dataDir.resolve(fileName);
    }

    /** 이 파일의 고정 헤더 (첫 행과 정확히 같아야 함) */
    protected abstract List<String> header();

    /**
     * CSV 한 행을 객체로 바꾼다.
     * @param row   열 개수가 헤더와 같은 것이 보장된 행
     * @param where 오류 메시지 앞에 붙일 위치 (예: "questions.csv 3행: ")
     */
    protected abstract T fromRow(List<String> row, String where);

    /** 파일 전체에 대한 규칙(중복 금지 등). 필요한 자식 클래스만 재정의한다. */
    protected void validate(List<T> records) {
    }

    /** 파일을 읽어 변환된 객체 목록(수정 불가)을 돌려준다. 파일이 없거나 형식이 틀리면 DataFileException. */
    public List<T> readAll() {
        String name = file.getFileName().toString();
        if (!Files.exists(file)) {
            throw new DataFileException(name + " 파일이 없습니다: " + file.toAbsolutePath());
        }
        List<List<String>> rows;
        try {
            rows = CsvCodec.read(file);
        } catch (IOException e) {
            throw new UncheckedIOException(name + "을(를) 읽지 못했습니다.", e);
        }
        if (rows.isEmpty() || !rows.get(0).equals(header())) {
            throw new DataFileException(name + " 헤더가 올바르지 않습니다. 기대값: " + String.join(",", header()));
        }
        List<T> records = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String where = name + " " + (i + 1) + "행: ";
            List<String> row = rows.get(i);
            if (row.size() != header().size()) {
                throw new DataFileException(where + "열 개수가 " + row.size() + "개입니다 (" + header().size() + "개여야 함).");
            }
            records.add(fromRow(row, where));
        }
        validate(records);
        return List.copyOf(records);
    }

    // ---------- 자식 클래스가 쓰는 값 변환 도우미 ----------

    /** 비어 있으면 안 되는 글자 칸 (앞뒤 공백 제거) */
    protected static String requireText(String value, String where, String column) {
        if (value == null || value.isBlank()) {
            throw new DataFileException(where + column + "은(는) 비울 수 없습니다.");
        }
        return value.trim();
    }

    /** 1 이상의 정수 칸 */
    protected static int requirePositiveInt(String value, String where, String column) {
        int n;
        try {
            n = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new DataFileException(where + column + "은(는) 정수여야 합니다: " + value);
        }
        if (n < 1) {
            throw new DataFileException(where + column + "은(는) 1 이상이어야 합니다.");
        }
        return n;
    }

    /** true/false 칸 */
    protected static boolean requireBoolean(String value, String where, String column) {
        String v = value.trim();
        if (v.equals("true")) return true;
        if (v.equals("false")) return false;
        throw new DataFileException(where + column + "은(는) true 또는 false여야 합니다: " + value);
    }
}
