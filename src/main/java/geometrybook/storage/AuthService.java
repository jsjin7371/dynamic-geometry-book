package geometrybook.storage;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 로그인·회원가입 창구. 화면(프론트)은 이 클래스의 메서드만 부르면 된다.
 * 앱 시작 시 하나만 만들어서 화면들이 함께 쓴다 (현재 로그인 학생을 이 객체가 기억함).
 *
 * 사용 예:
 *   AuthService auth = new AuthService(Path.of("data"));
 *   auth.register("jy523", "pass1234", "정연", 3);
 *   Optional<Student> me = auth.login("jy523", "pass1234");
 *   if (me.isPresent()) { ... 메인 화면으로 ... } else { ... "아이디 또는 비밀번호가 틀렸어요" ... }
 */
public class AuthService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    // 비밀번호 규칙은 ERD 7장 미정 사항 → 팀 결정 후 아래 값만 바꾸면 된다
    private static final Pattern LOGIN_ID_RULE = Pattern.compile("^[A-Za-z0-9_]{4,20}$");
    private static final int PASSWORD_MIN = 4;
    private static final int PASSWORD_MAX = 30;
    private static final int NICKNAME_MAX = 12;
    private static final String DEFAULT_AVATAR = "avatar_default";

    private final StudentRepository repository;
    private Student currentStudent;   // 현재 로그인한 학생 (메모리에만, ERD 3장)

    public AuthService(Path dataDir) {
        this.repository = new StudentRepository(dataDir);
    }

    /**
     * 회원가입. 규칙에 안 맞으면 IllegalArgumentException(화면에 그대로 보여 줄 한국어 메시지).
     * @param grade 학년 1~6, 입력 안 하면 null
     */
    public Student register(String loginId, String password, String nickname, Integer grade) {
        loginId = loginId == null ? "" : loginId.trim();
        nickname = nickname == null ? "" : nickname.trim();

        if (!LOGIN_ID_RULE.matcher(loginId).matches()) {
            throw new IllegalArgumentException("아이디는 영문·숫자·_ 4~20자로 입력해 주세요.");
        }
        checkPasswordRule(password);
        if (nickname.isEmpty() || nickname.length() > NICKNAME_MAX) {
            throw new IllegalArgumentException("닉네임은 1~" + NICKNAME_MAX + "자로 입력해 주세요.");
        }
        if (grade != null && (grade < 1 || grade > 6)) {
            throw new IllegalArgumentException("학년은 1~6 중에서 골라 주세요.");
        }
        if (repository.existsByLoginId(loginId)) {   // 빠른 안내용. 최종 중복 검사는 insert가 잠금 안에서 한 번 더 함
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        Student s = new Student(
                UUID.randomUUID().toString(),
                loginId,
                PasswordHasher.hash(password),
                nickname,
                grade,
                DEFAULT_AVATAR,
                false,
                OffsetDateTime.now(SEOUL).truncatedTo(ChronoUnit.SECONDS));
        repository.insert(s);
        return s;
    }

    /** 로그인. 성공하면 학생, 아이디가 없거나 비밀번호가 틀리면 빈 Optional. */
    public Optional<Student> login(String loginId, String password) {
        if (loginId == null || password == null) return Optional.empty();
        Optional<Student> found = repository.findByLoginId(loginId.trim());
        if (found.isPresent() && PasswordHasher.verify(password, found.get().getPasswordHash())) {
            currentStudent = found.get();
            return found;
        }
        return Optional.empty();
    }

    public void logout() {
        currentStudent = null;
    }

    /** 현재 로그인한 학생 (없으면 null). 다른 화면에서 studentId가 필요할 때 사용. */
    public Student getCurrentStudent() {
        return currentStudent;
    }

    public boolean isLoginIdAvailable(String loginId) {
        return loginId != null && LOGIN_ID_RULE.matcher(loginId.trim()).matches()
                && !repository.existsByLoginId(loginId.trim());
    }

    /** 비밀번호 변경 (현재 비밀번호 확인 후). 틀리면 IllegalArgumentException. */
    public void changePassword(String loginId, String oldPassword, String newPassword) {
        if (loginId == null || oldPassword == null) {
            throw new IllegalArgumentException("현재 비밀번호가 맞지 않습니다.");
        }
        checkPasswordRule(newPassword);
        Student s = repository.findByLoginId(loginId.trim())
                .filter(st -> PasswordHasher.verify(oldPassword, st.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("현재 비밀번호가 맞지 않습니다."));
        s.setPasswordHash(PasswordHasher.hash(newPassword));
        s.setMustChangePassword(false);
        repository.update(s);
        // 로그인 중인 본인이면 메모리의 학생 정보도 새것으로 바꾼다
        if (currentStudent != null && currentStudent.getId().equals(s.getId())) {
            currentStudent = s;
        }
    }

    private static void checkPasswordRule(String password) {
        if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new IllegalArgumentException("비밀번호는 " + PASSWORD_MIN + "~" + PASSWORD_MAX + "자로 입력해 주세요.");
        }
    }
}
