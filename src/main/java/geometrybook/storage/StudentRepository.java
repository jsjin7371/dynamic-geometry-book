package geometrybook.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * students.csv 저장소. 파일 ↔ Student 객체 변환과 PK·UK 검사를 맡는다.
 * 화면 코드는 이 클래스를 직접 쓰지 않고 AuthService를 쓴다.
 *
 * 쓰기(추가·수정)는 "읽기 → 검사 → 저장" 전체를 잠금으로 묶는다 (ERD 6장 단일 쓰기 잠금).
 * - 같은 프로그램 안: 파일 경로별 ReentrantLock
 * - 다른 프로그램(관리 도구 등)과: data 폴더의 students.csv.lock 파일 잠금
 */
public class StudentRepository {

    public static final String FILE_NAME = "students.csv";
    private static final String LOCK_FILE_NAME = FILE_NAME + ".lock";
    private static final ConcurrentHashMap<Path, ReentrantLock> JVM_LOCKS = new ConcurrentHashMap<>();

    private final Path file;
    private final Path lockFile;

    public StudentRepository(Path dataDir) {
        this.file = dataDir.resolve(FILE_NAME).toAbsolutePath().normalize();
        this.lockFile = dataDir.resolve(LOCK_FILE_NAME).toAbsolutePath().normalize();
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

    /** 새 학생 추가 (loginId·id 중복이면 예외). 읽기~저장 전체가 잠금 안에서 실행된다. */
    public void insert(Student student) {
        withWriteLock(() -> {
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
            return null;
        });
    }

    /** 기존 학생 정보 수정 (id 기준으로 교체). 다른 학생과 loginId가 겹치면 예외. */
    public void update(Student student) {
        withWriteLock(() -> {
            List<Student> all = findAll();
            int index = -1;
            for (int i = 0; i < all.size(); i++) {
                Student s = all.get(i);
                if (s.getId().equals(student.getId())) {
                    index = i;
                } else if (s.getLoginId().equals(student.getLoginId())) {
                    throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
                }
            }
            if (index < 0) throw new IllegalArgumentException("없는 학생입니다: " + student.getId());
            all.set(index, student);
            saveAll(all);
            return null;
        });
    }

    /** 같은 프로그램 안의 스레드 + 다른 프로그램까지 한 번에 하나만 쓰도록 잠근다. */
    private <T> T withWriteLock(Supplier<T> action) {
        ReentrantLock jvmLock = JVM_LOCKS.computeIfAbsent(file, p -> new ReentrantLock());
        jvmLock.lock();
        try {
            Files.createDirectories(lockFile.getParent());
            try (FileChannel channel = FileChannel.open(lockFile,
                         StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return action.get();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("students.csv 쓰기 잠금을 얻지 못했습니다.", e);
        } finally {
            jvmLock.unlock();
        }
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
