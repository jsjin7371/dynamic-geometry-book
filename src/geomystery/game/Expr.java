package geomystery.game;

/**
 * 식 입력 문제를 채점하는 작은 수식 계산기.
 * <p>
 * 지원: 정수·소수, 변수 x, π(키보드 입력용), + - × ÷ * /, ^ 와 ², √(바로 뒤 항에 적용), 괄호,
 * 곱셈 기호 생략(2x, 3(x+1), 2√3, (x+1)(x-1)).
 * 두 식은 x에 여러 값을 넣어 계산한 결과가 모두 같으면 같은 식으로 본다
 * (그래서 3x+30 과 30+3x, 5/2 와 2.5, 2√3 과 √12 는 같은 답으로 인정된다).
 */
public final class Expr {
    private static final double[] SAMPLES = {0.37, 1.3, 2.9, 7.25};

    private final String s;
    private int pos;
    private double x;

    private Expr(String s) { this.s = s; }

    /** 식 문자열을 정리한다(공백 제거, 기호 통일). */
    public static String normalize(String in) {
        if (in == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : in.toCharArray()) {
            if (Character.isWhitespace(c)) continue;
            switch (c) {
                case '×': case '·': b.append('*'); break;
                case '÷': b.append('/'); break;
                case '−': case '–': b.append('-'); break;
                case 'X': b.append('x'); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    /** 문법에 맞는 식이면 true. */
    public static boolean isValid(String in) {
        try {
            eval(in, 1.0);
            return !normalize(in).isEmpty();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** x = xv 일 때의 값. 문법이 틀리면 IllegalArgumentException. */
    public static double eval(String in, double xv) {
        Expr e = new Expr(normalize(in));
        e.x = xv;
        if (e.s.isEmpty()) throw new IllegalArgumentException("빈 식");
        double v = e.sum();
        if (e.pos != e.s.length()) throw new IllegalArgumentException("알 수 없는 기호: " + e.s.charAt(e.pos));
        return v;
    }

    /** 두 식이 같은 값을 갖는지. 어느 한쪽이라도 문법이 틀리면 false. */
    public static boolean equivalent(String a, String b) {
        try {
            for (double xv : SAMPLES) {
                double va = eval(a, xv), vb = eval(b, xv);
                if (Double.isNaN(va) || Double.isNaN(vb)) return false;
                if (Math.abs(va - vb) > 1e-9 * Math.max(1, Math.max(Math.abs(va), Math.abs(vb)))) return false;
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 식에 x가 들어 있는지. */
    public static boolean hasVariable(String in) { return normalize(in).indexOf('x') >= 0; }

    // ---------------------------------------------------------------- 재귀 하강 파서
    private boolean eat(char c) {
        if (pos < s.length() && s.charAt(pos) == c) { pos++; return true; }
        return false;
    }

    private double sum() {
        double v = product();
        while (true) {
            if (eat('+')) v += product();
            else if (eat('-')) v -= product();
            else return v;
        }
    }

    private double product() {
        double v = unary();
        while (true) {
            if (eat('*')) v *= unary();
            else if (eat('/')) {
                double d = unary();
                if (d == 0) throw new IllegalArgumentException("0으로 나눔");
                v /= d;
            } else if (startsAtom()) v *= power(); // 곱셈 기호 생략
            else return v;
        }
    }

    private double unary() {
        if (eat('-')) return -unary();
        if (eat('+')) return unary();
        return power();
    }

    private double power() {
        double v = atom();
        while (true) {
            if (eat('²')) v = v * v;
            else if (eat('³')) v = v * v * v;
            else if (eat('^')) v = Math.pow(v, unary());
            else return v;
        }
    }

    private boolean startsAtom() {
        if (pos >= s.length()) return false;
        char c = s.charAt(pos);
        return Character.isDigit(c) || c == '.' || c == 'x' || c == 'π' || c == '√' || c == '(';
    }

    private double atom() {
        if (pos >= s.length()) throw new IllegalArgumentException("식이 끝났습니다");
        char c = s.charAt(pos);
        if (c == '(') {
            pos++;
            double v = sum();
            if (!eat(')')) throw new IllegalArgumentException("닫는 괄호가 없습니다");
            return v;
        }
        if (c == '√') {
            pos++;
            double v = power();
            if (v < 0) throw new IllegalArgumentException("음수의 제곱근");
            return Math.sqrt(v);
        }
        if (c == 'x') { pos++; return x; }
        if (c == 'π') { pos++; return Math.PI; }
        if (Character.isDigit(c) || c == '.') {
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            try {
                return Double.parseDouble(s.substring(start, pos));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("숫자 형식: " + s.substring(start, pos));
            }
        }
        throw new IllegalArgumentException("알 수 없는 기호: " + c);
    }
}
