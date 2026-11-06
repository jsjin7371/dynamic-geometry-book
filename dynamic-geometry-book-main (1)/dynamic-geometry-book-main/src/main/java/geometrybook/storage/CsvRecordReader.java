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

    /** CSV 한 행(열 개수가 헤더와 같은 것이 보장됨)을 객체로 바꾼다. */
    protected abstract T fromRow(List<String> row);

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
            List<String> row = rows.get(i);
            if (row.size() != header().size()) {
                throw new DataFileException(name + " " + (i + 1) + "행: 열 개수가 헤더와 다릅니다.");
            }
            records.add(fromRow(row));
        }
        validate(records);
        return List.copyOf(records);
    }
}
