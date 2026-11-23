package geomystery.game;

import geomystery.data.Model.Lesson;
import geomystery.data.Model.Puzzle;

import java.util.*;

/** 한 번의 수수께끼 풀이 세션(사건 하나 또는 연습). */
public final class Session {
    public enum Mode { LESSON, PRACTICE, REVIEW }

    public final Lesson lesson;           // 연습 모드면 null
    public final List<Puzzle> puzzles;
    public final Mode mode;
    public final long startMs = System.currentTimeMillis();

    public int index = 0;
    public int hintsUsed = 0;
    public final Set<String> wrongIds = new LinkedHashSet<>();
    public final Map<String, Integer> hintsOpened = new HashMap<>();
    public final Set<String> auxUsedOn = new HashSet<>();
    public boolean failed = false;
    /** 지금 문제를 화면에 띄운 시각(문제별 풀이 시간 측정). */
    public long shownMs = System.currentTimeMillis();
    /** 최종 추리: 성공 여부와 잘못 제시한 횟수. */
    public boolean finaleSolved = false;
    public int finaleMistakes = 0;

    public Session(Lesson lesson, List<Puzzle> puzzles, Mode mode) {
        this.lesson = lesson;
        this.puzzles = new ArrayList<>(puzzles);
        this.mode = mode;
    }

    public Puzzle current() { return puzzles.get(index); }

    public boolean isLast() { return index >= puzzles.size() - 1; }

    public int hintsOpened(Puzzle p) { return hintsOpened.getOrDefault(p.id, 0); }

    public int firstTryCount() {
        int n = 0;
        for (Puzzle p : puzzles) if (!wrongIds.contains(p.id)) n++;
        return n;
    }
}
