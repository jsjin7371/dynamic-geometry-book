package geometrybook.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 비밀번호 해시 (ERD: passwordHash에 salt·방식·작업량 포함).
 * 저장 형식: pbkdf2-sha256$반복횟수$salt(Base64)$hash(Base64)
 * 자바 기본 라이브러리만 사용한다 (추가 jar 필요 없음).
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int ITERATIONS = 120_000;
    // 파일 값이 조작돼도 로그인 한 번에 CPU를 너무 오래 쓰지 않도록 허용 범위를 둔다
    private static final int MIN_ITERATIONS = 10_000;
    private static final int MAX_ITERATIONS = 1_000_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(String password) {
        if (password == null) throw new IllegalArgumentException("비밀번호가 비어 있습니다.");
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    /** 저장된 값이 이 클래스가 만든 형식(방식·반복횟수 범위·salt/hash 길이)에 맞는지. */
    public static boolean isValidFormat(String stored) {
        return decode(stored) != null;
    }

    public static boolean verify(String password, String stored) {
        if (password == null) return false;
        Decoded d = decode(stored);
        if (d == null) return false;                       // 형식이 틀리거나 범위 밖이면 계산하지 않음
        byte[] actual = pbkdf2(password.toCharArray(), d.salt, d.iterations);
        return MessageDigest.isEqual(d.hash, actual);      // 시간 차이로 정보가 새지 않게 비교
    }

    /** 저장 문자열을 나눠서 검사한다. 틀리면 null. */
    private static Decoded decode(String stored) {
        if (stored == null) return null;
        String[] parts = stored.split("\\$", -1);
        if (parts.length != 4 || !parts[0].equals(PREFIX)) return null;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) return null;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] hash = Base64.getDecoder().decode(parts[3]);
            if (salt.length != SALT_BYTES || hash.length != HASH_BYTES) return null;
            return new Decoded(iterations, salt, hash);
        } catch (IllegalArgumentException e) {   // 숫자·Base64 형식 오류
            return null;
        }
    }

    private static final class Decoded {
        final int iterations;
        final byte[] salt;
        final byte[] hash;

        Decoded(int iterations, byte[] salt, byte[] hash) {
            this.iterations = iterations;
            this.salt = salt;
            this.hash = hash;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("비밀번호 해시 알고리즘을 사용할 수 없습니다.", e);
        } finally {
            spec.clearPassword();            // 계산이 끝나면 메모리에 남은 비밀번호를 지운다
            Arrays.fill(password, '\0');
        }
    }
}
