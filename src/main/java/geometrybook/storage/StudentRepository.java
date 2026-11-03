package geometrybook.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * students.csv 저장소. 파일 ↔ Student 객체 변환과 PK·UK 검사를 맡는다.
 * 화면 코드는 이 클래스를 직접 쓰지 않고 AuthService를 쓴다.
 */
public class StudentRepository {

    public static final String FILE_NAME = "students.csv";
    private final Path file;

    public StudentRepository(Path dataDir) {
        this.file = dataDir.resolve(FILE_NAME);
    }

    /** 전체 학생 읽기. 파일이 없으면(최초 설치) 빈 목록, 형식이 깨졌으면 예외. */
    public List<Student> findAll() {
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            List<List<String>> rows = CsvCodec.read(file);
            if (rows.isEmpty() || !rows.get(0).equals(Student.HEADER)) {
                throw new DataFileException("students.csv 헤더가 올바르지 않습니다. 기대값: "
                        + String.join(",", Student.HEADER));
            }
            List<Student> list = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            Set<String> loginIds = new HashSet<>();
            for (int i = 1; i < rows.size(); i++) {
                Student s = Student.fromRow(rows.get(i), i + 1);
                if (!ids.add(s.getId())) {
                    throw new DataFileException("students.csv " + (i + 1) + "행: id 중복 " + s.getId());
                }
                if (!loginIds.add(s.getLoginId())) {
                    throw new DataFileException("students.csv " + (i + 1) + "행: loginId 중복 " + s.getLoginId());
                }
                list.add(s);
            }
            return list;
        } catch (IOException e) {
            throw new UncheckedIOException("students.csv를 읽지 못했습니다.", e);
        }
    }

    public Optional<Student> findByLoginId(String loginId) {
        return findAll().stream().filter(s -> s.getLoginId().equals(loginId)).findFirst();
    }

    public Optional<Student> findById(String id) {
        return findAll().stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    public boolean existsByLoginId(String loginId) {
        return findByLoginId(loginId).isPresent();
    }

    /** 새 학생 추가 (loginId·id 중복이면 예외). */
    public void insert(Student student) {
        List<Student> all = findAll();
        for (Student s : all) {
            if (s.getLoginId().equals(student.getLoginId())) {
                throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
            }
            if (s.getId().equals(student.getId())) {
                throw new DataFileException("이미 있는 학생 id입니다: " + student.getId());
            }
        }
        all.add(student);
        saveAll(all);
    }

    /** 기존 학생 정보 수정 (id 기준으로 교체). */
    public void update(Student student) {
        List<Student> all = findAll();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(student.getId())) {
                all.set(i, student);
                found = true;
                break;
            }
        }
        if (!found) throw new IllegalArgumentException("없는 학생입니다: " + student.getId());
        saveAll(all);
    }

    private void saveAll(List<Student> all) {
        List<List<String>> rows = new ArrayList<>();
        for (Student s : all) rows.add(s.toRow());
        try {
            CsvCodec.writeAtomically(file, Student.HEADER, rows);
        } catch (IOException e) {
            throw new UncheckedIOException("students.csv를 저장하지 못했습니다.", e);
        }
    }
}
