package geometrybook.storage;

import java.util.List;

/**
 * 학생 계정 (ERD A안: student → students.csv).
 * CSV 열: id, loginId, passwordHash
 */
public class Student {

    public static final List<String> HEADER = List.of("id", "loginId", "passwordHash");

    private final String id;              // PK, 바뀌지 않는 UUID
    private final String loginId;         // UK, 로그인 아이디
    private String passwordHash;          // PasswordHasher 형식 문자열 (평문 저장 안 함)

    /**
     * 잘못된 값(빈 아이디, 평문 비밀번호 등)을 가진 객체가 만들어져 파일에 저장되면
     * 다음에 파일을 읽을 때 전체가 손상으로 처리되므로, 만들 때부터 막는다.
     */
    public Student(String id, String loginId, String passwordHash) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id는 비울 수 없습니다.");
        if (loginId == null || loginId.isBlank()) throw new IllegalArgumentException("loginId는 비울 수 없습니다.");
        if (!PasswordHasher.isValidFormat(passwordHash)) {
            throw new IllegalArgumentException("passwordHash는 PasswordHasher가 만든 형식이어야 합니다.");
        }
        this.id = id;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
    }

    /** CSV 한 행 → 객체 (형식이 틀리면 DataFileException) */
    static Student fromRow(List<String> r, int lineNo) {
        String where = "students.csv " + lineNo + "행: ";
        if (r.size() != HEADER.size()) {
            throw new DataFileException(where + "열 개수가 " + r.size() + "개입니다 (" + HEADER.size() + "개여야 함).");
        }
        if (r.get(0).isBlank() || r.get(1).isBlank()) {
            throw new DataFileException(where + "id·loginId는 비울 수 없습니다.");
        }
        if (!PasswordHasher.isValidFormat(r.get(2))) {
            throw new DataFileException(where + "passwordHash 형식이 올바르지 않습니다 (평문이거나 지원하지 않는 형식).");
        }
        return new Student(r.get(0), r.get(1), r.get(2));
    }

    /** 객체 → CSV 한 행 */
    List<String> toRow() {
        return List.of(id, loginId, passwordHash);
    }

    public String getId() { return id; }
    public String getLoginId() { return loginId; }
    String getPasswordHash() { return passwordHash; }   // 같은 패키지(저장 모듈)만 사용

    void setPasswordHash(String passwordHash) {
        if (!PasswordHasher.isValidFormat(passwordHash)) {
            throw new IllegalArgumentException("passwordHash는 PasswordHasher가 만든 형식이어야 합니다.");
        }
        this.passwordHash = passwordHash;
    }

    @Override
    public String toString() {
        return "Student{" + loginId + "}";
    }
}
