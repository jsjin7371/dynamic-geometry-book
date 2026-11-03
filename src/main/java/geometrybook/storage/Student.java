package geometrybook.storage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 학생 계정 (ERD: student → students.csv).
 * CSV 열: id, loginId, passwordHash, nickname, grade, avatarKey, mustChangePassword, createdAt
 */
public class Student {

    public static final List<String> HEADER = List.of(
            "id", "loginId", "passwordHash", "nickname", "grade",
            "avatarKey", "mustChangePassword", "createdAt");

    private final String id;              // PK, 바뀌지 않는 UUID
    private final String loginId;         // UK, 로그인 아이디
    private String passwordHash;          // PasswordHasher 형식 문자열 (평문 저장 안 함)
    private String nickname;
    private Integer grade;                // 선택, 1~6 (없으면 null)
    private String avatarKey;             // 앱 내 기본 캐릭터 키
    private boolean mustChangePassword;
    private final OffsetDateTime createdAt;

    public Student(String id, String loginId, String passwordHash, String nickname, Integer grade,
                   String avatarKey, boolean mustChangePassword, OffsetDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.loginId = Objects.requireNonNull(loginId, "loginId");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.nickname = Objects.requireNonNull(nickname, "nickname");
        this.grade = grade;
        this.avatarKey = avatarKey;
        this.mustChangePassword = mustChangePassword;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** CSV 한 행 → 객체 (형식이 틀리면 DataFileException) */
    static Student fromRow(List<String> r, int lineNo) {
        String where = "students.csv " + lineNo + "행: ";
        if (r.size() != HEADER.size()) {
            throw new DataFileException(where + "열 개수가 " + r.size() + "개입니다 (" + HEADER.size() + "개여야 함).");
        }
        if (r.get(0).isBlank() || r.get(1).isBlank() || r.get(3).isBlank()) {
            throw new DataFileException(where + "id·loginId·nickname은 비울 수 없습니다.");
        }
        if (!PasswordHasher.isValidFormat(r.get(2))) {
            throw new DataFileException(where + "passwordHash 형식이 올바르지 않습니다 (평문이거나 지원하지 않는 형식).");
        }
        String mcp = r.get(6).trim();
        if (!mcp.equals("true") && !mcp.equals("false")) {
            throw new DataFileException(where + "mustChangePassword는 true/false여야 합니다.");
        }
        try {
            String gradeText = r.get(4).trim();
            Integer grade = gradeText.isEmpty() ? null : Integer.parseInt(gradeText);
            if (grade != null && (grade < 1 || grade > 6)) {
                throw new DataFileException(where + "grade는 1~6 또는 빈 칸이어야 합니다.");
            }
            return new Student(r.get(0), r.get(1), r.get(2), r.get(3), grade,
                    r.get(5), Boolean.parseBoolean(mcp), OffsetDateTime.parse(r.get(7)));
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            throw new DataFileException(where + "숫자 또는 날짜 형식이 잘못됐습니다.");
        }
    }

    /** 객체 → CSV 한 행 */
    List<String> toRow() {
        return List.of(id, loginId, passwordHash, nickname,
                grade == null ? "" : grade.toString(),
                avatarKey == null ? "" : avatarKey,
                Boolean.toString(mustChangePassword),
                createdAt.toString());
    }

    public String getId() { return id; }
    public String getLoginId() { return loginId; }
    String getPasswordHash() { return passwordHash; }   // 같은 패키지(저장 모듈)만 사용
    public String getNickname() { return nickname; }
    public Integer getGrade() { return grade; }
    public String getAvatarKey() { return avatarKey; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    void setPasswordHash(String passwordHash) { this.passwordHash = Objects.requireNonNull(passwordHash); }
    void setMustChangePassword(boolean value) { this.mustChangePassword = value; }
    public void setNickname(String nickname) { this.nickname = Objects.requireNonNull(nickname); }
    public void setGrade(Integer grade) { this.grade = grade; }
    public void setAvatarKey(String avatarKey) { this.avatarKey = avatarKey; }

    @Override
    public String toString() {
        return "Student{" + loginId + ", " + nickname + (grade == null ? "" : ", " + grade + "학년") + "}";
    }
}
