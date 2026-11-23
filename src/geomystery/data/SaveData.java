package geomystery.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 플레이어 진행 상황. data/save/ 아래 여러 CSV 파일로 저장된다.
 * <pre>
 *  profile.csv   key,value            — 이름, XP, 토큰, 등불, 연속 학습, 통계
 *  progress.csv  lesson,done,...      — 사건(레슨)별 기록
 *  history.csv   date,xp,puzzles      — 날짜별 학습량(촛불·주간 리그)
 *  mistakes.csv  puzzle,count,last    — 오답 노트
 *  quests.csv    date,quest,progress,done — 오늘의 의뢰 진행도
 *  stats.csv     puzzle,attempts,...  — 문제별 풀이 기록(약점 분석)
 *  slips.csv     tag,count,last       — 실수 유형별 횟수(맞춤 피드백)
 * </pre>
 * 설정(효과음·테마·글자 크기)도 profile.csv 에 함께 저장된다.
 */
public final class SaveData {
    public static final int MAX_LANTERNS = 5;
    public static final long REGEN_MS = 30L * 60 * 1000;

    private final Path dir;

    public String name = "견습 탐정";
    public int xp = 0, tokens = 5, lanterns = MAX_LANTERNS, streak = 0, bestStreak = 0;
    public String lastStudy = "";
    public long lanternStamp = 0;
    public int auxTotal, noHintRun, noHintBest, perfectLessons, solvedTotal, firstTryTotal;
    public int typingSpeed = 2;
    /** 효과음 음량 0(끔)~3. */
    public int sfxLevel = 2;
    /** 배경음악 음량 0(끔)~3. */
    public int musicLevel = 2;
    public boolean darkMode = false;
    /** 글자 크기 단계 0(작게) 1(보통) 2(크게). */
    public int fontStep = 1;
    /** 리그 단계(0 청동 ~ 4 명탐정)와 그 리그가 시작된 주의 월요일. */
    public int leagueTier = 0;
    public String leagueWeek = "";
    /** 아직 보여 주지 않은 리그 정산 결과. */
    public String leagueNotice = "";
    public int finalesSolved = 0;
    /** 최종 추리를 해낸 사건들(사건 보드). */
    public final Set<String> solvedFinales = new LinkedHashSet<>();
    /** 도감에서 이미 뒤집어 본 정리 카드(새 카드 뒤집기 연출용). */
    public final Set<String> seenCards = new LinkedHashSet<>();
    public boolean endingSeen = false;
    /** 헤이즐 꾸미기(모자, 코트, 나비넥타이 번호). */
    public String outfit = "0,0,0";
    /** 리그 화면에서 마지막으로 본 순위("주:순위"). 순위가 바뀌면 줄이 움직이는 연출에 쓴다. */
    public String leagueSeen = "";
    /** 사건 보드에 이미 꽂아 둔 증거 사진(새 사진이 핀에 꽂히는 연출용). */
    public final Set<String> photosPinned = new LinkedHashSet<>();
    /** 이미 본 후일담(인물 id). */
    public final Set<String> epiloguesSeen = new LinkedHashSet<>();

    public String questDate = "";
    public final Map<String, Integer> questProgress = new LinkedHashMap<>();
    public final Set<String> questDone = new LinkedHashSet<>();

    public final Map<String, LessonRecord> progress = new LinkedHashMap<>();
    public final TreeMap<String, int[]> history = new TreeMap<>();
    public final Map<String, Mistake> mistakes = new LinkedHashMap<>();
    public final Map<String, PuzzleStat> stats = new LinkedHashMap<>();
    public final Map<String, Integer> slips = new LinkedHashMap<>();

    public static class LessonRecord {
        public String id;
        public boolean done;
        public int bestAcc, bestTime, lastTime, plays;
        public String completedAt = "";
    }

    public static class Mistake {
        public String puzzleId;
        public int count;
        public String last = "";
    }

    /** 문제 하나의 누적 풀이 기록. */
    public static class PuzzleStat {
        public String puzzleId;
        /** 제출 횟수, 틀린 횟수, 푼 횟수, 첫 시도에 맞힌 횟수, 연 힌트 수, 푼 시간 합(초). */
        public int attempts, wrong, solved, firstTry, hints, seconds;
    }

    public PuzzleStat stat(String puzzleId) {
        return stats.computeIfAbsent(puzzleId, k -> {
            PuzzleStat st = new PuzzleStat();
            st.puzzleId = k;
            return st;
        });
    }

    private SaveData(Path dir) { this.dir = dir; }

    public Path dir() { return dir; }

    public static SaveData load(Path dir) throws IOException {
        SaveData s = new SaveData(dir);
        Files.createDirectories(dir);
        for (Map<String, String> r : Csv.readTable(dir.resolve("profile.csv"))) {
            s.applyProfile(r.get("key"), r.get("value"));
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("progress.csv"))) {
            LessonRecord rec = new LessonRecord();
            rec.id = r.get("lesson");
            rec.done = "1".equals(r.get("done"));
            rec.bestAcc = Csv.toInt(r.get("best_accuracy"), 0);
            rec.bestTime = Csv.toInt(r.get("best_time"), 0);
            rec.lastTime = Csv.toInt(r.get("last_time"), 0);
            rec.plays = Csv.toInt(r.get("plays"), 0);
            rec.completedAt = r.get("completed_at");
            s.progress.put(rec.id, rec);
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("history.csv"))) {
            s.history.put(r.get("date"), new int[]{Csv.toInt(r.get("xp"), 0), Csv.toInt(r.get("puzzles"), 0)});
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("mistakes.csv"))) {
            Mistake m = new Mistake();
            m.puzzleId = r.get("puzzle"); m.count = Csv.toInt(r.get("count"), 1); m.last = r.get("last");
            s.mistakes.put(m.puzzleId, m);
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("quests.csv"))) {
            s.questDate = r.get("date");
            s.questProgress.put(r.get("quest"), Csv.toInt(r.get("progress"), 0));
            if ("1".equals(r.get("done"))) s.questDone.add(r.get("quest"));
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("stats.csv"))) {
            PuzzleStat st = s.stat(r.get("puzzle"));
            st.attempts = Csv.toInt(r.get("attempts"), 0);
            st.wrong = Csv.toInt(r.get("wrong"), 0);
            st.solved = Csv.toInt(r.get("solved"), 0);
            st.firstTry = Csv.toInt(r.get("first_try"), 0);
            st.hints = Csv.toInt(r.get("hints"), 0);
            st.seconds = Csv.toInt(r.get("seconds"), 0);
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("slips.csv"))) {
            s.slips.put(r.get("tag"), Csv.toInt(r.get("count"), 0));
        }
        if (!Files.exists(dir.resolve("profile.csv"))) {
            s.lanternStamp = System.currentTimeMillis();
            s.save();
        }
        return s;
    }

    private void applyProfile(String k, String v) {
        if (k == null) return;
        switch (k) {
            case "name": name = v; break;
            case "xp": xp = Csv.toInt(v, 0); break;
            case "tokens": tokens = Csv.toInt(v, 5); break;
            case "lanterns": lanterns = Csv.toInt(v, MAX_LANTERNS); break;
            case "lantern_stamp": lanternStamp = Csv.toLong(v, 0); break;
            case "streak": streak = Csv.toInt(v, 0); break;
            case "best_streak": bestStreak = Csv.toInt(v, 0); break;
            case "last_study": lastStudy = v; break;
            case "aux_total": auxTotal = Csv.toInt(v, 0); break;
            case "nohint_run": noHintRun = Csv.toInt(v, 0); break;
            case "nohint_best": noHintBest = Csv.toInt(v, 0); break;
            case "perfect_lessons": perfectLessons = Csv.toInt(v, 0); break;
            case "solved_total": solvedTotal = Csv.toInt(v, 0); break;
            case "first_try_total": firstTryTotal = Csv.toInt(v, 0); break;
            case "typing_speed": typingSpeed = Csv.toInt(v, 2); break;
            case "sfx_level": sfxLevel = Csv.toInt(v, 2); break;
            case "music_level": musicLevel = Csv.toInt(v, 2); break;
            case "dark_mode": darkMode = "1".equals(v); break;
            case "font_step": fontStep = Csv.toInt(v, 1); break;
            case "league_tier": leagueTier = Csv.toInt(v, 0); break;
            case "league_week": leagueWeek = v; break;
            case "league_notice": leagueNotice = v; break;
            case "finales_solved": finalesSolved = Csv.toInt(v, 0); break;
            case "finales_list": splitInto(v, solvedFinales); break;
            case "seen_cards": splitInto(v, seenCards); break;
            case "ending_seen": endingSeen = "1".equals(v); break;
            case "outfit": outfit = v; break;
            case "league_seen": leagueSeen = v; break;
            case "photos_pinned": splitInto(v, photosPinned); break;
            case "epilogues_seen": splitInto(v, epiloguesSeen); break;
            default: break;
        }
    }

    private static void splitInto(String v, Set<String> out) {
        out.clear();
        if (v == null) return;
        for (String p : v.split("\\|")) if (!p.trim().isEmpty()) out.add(p.trim());
    }

    public LessonRecord record(String lessonId) {
        return progress.computeIfAbsent(lessonId, k -> {
            LessonRecord r = new LessonRecord();
            r.id = k;
            return r;
        });
    }

    public boolean isDone(String lessonId) {
        LessonRecord r = progress.get(lessonId);
        return r != null && r.done;
    }

    public void save() {
        try {
            Files.createDirectories(dir);
            List<List<String>> rows = new ArrayList<>();
            kv(rows, "name", name);
            kv(rows, "xp", xp);
            kv(rows, "tokens", tokens);
            kv(rows, "lanterns", lanterns);
            kv(rows, "lantern_stamp", lanternStamp);
            kv(rows, "streak", streak);
            kv(rows, "best_streak", bestStreak);
            kv(rows, "last_study", lastStudy);
            kv(rows, "aux_total", auxTotal);
            kv(rows, "nohint_run", noHintRun);
            kv(rows, "nohint_best", noHintBest);
            kv(rows, "perfect_lessons", perfectLessons);
            kv(rows, "solved_total", solvedTotal);
            kv(rows, "first_try_total", firstTryTotal);
            kv(rows, "typing_speed", typingSpeed);
            kv(rows, "sfx_level", sfxLevel);
            kv(rows, "music_level", musicLevel);
            kv(rows, "dark_mode", darkMode ? 1 : 0);
            kv(rows, "font_step", fontStep);
            kv(rows, "league_tier", leagueTier);
            kv(rows, "league_week", leagueWeek);
            kv(rows, "league_notice", leagueNotice);
            kv(rows, "finales_solved", finalesSolved);
            kv(rows, "finales_list", String.join("|", solvedFinales));
            kv(rows, "seen_cards", String.join("|", seenCards));
            kv(rows, "ending_seen", endingSeen ? 1 : 0);
            kv(rows, "outfit", outfit);
            kv(rows, "league_seen", leagueSeen);
            kv(rows, "photos_pinned", String.join("|", photosPinned));
            kv(rows, "epilogues_seen", String.join("|", epiloguesSeen));
            Csv.writeTable(dir.resolve("profile.csv"), Arrays.asList("key", "value"), rows);

            rows = new ArrayList<>();
            for (LessonRecord r : progress.values()) {
                rows.add(Arrays.asList(r.id, r.done ? "1" : "0", "" + r.bestAcc, "" + r.bestTime,
                        "" + r.lastTime, "" + r.plays, r.completedAt == null ? "" : r.completedAt));
            }
            Csv.writeTable(dir.resolve("progress.csv"),
                    Arrays.asList("lesson", "done", "best_accuracy", "best_time", "last_time", "plays", "completed_at"), rows);

            rows = new ArrayList<>();
            for (Map.Entry<String, int[]> e : history.entrySet()) {
                rows.add(Arrays.asList(e.getKey(), "" + e.getValue()[0], "" + e.getValue()[1]));
            }
            Csv.writeTable(dir.resolve("history.csv"), Arrays.asList("date", "xp", "puzzles"), rows);

            rows = new ArrayList<>();
            for (Mistake m : mistakes.values()) rows.add(Arrays.asList(m.puzzleId, "" + m.count, m.last));
            Csv.writeTable(dir.resolve("mistakes.csv"), Arrays.asList("puzzle", "count", "last"), rows);

            rows = new ArrayList<>();
            for (Map.Entry<String, Integer> e : questProgress.entrySet()) {
                rows.add(Arrays.asList(questDate, e.getKey(), "" + e.getValue(), questDone.contains(e.getKey()) ? "1" : "0"));
            }
            Csv.writeTable(dir.resolve("quests.csv"), Arrays.asList("date", "quest", "progress", "done"), rows);

            rows = new ArrayList<>();
            for (PuzzleStat st : stats.values()) {
                rows.add(Arrays.asList(st.puzzleId, "" + st.attempts, "" + st.wrong, "" + st.solved,
                        "" + st.firstTry, "" + st.hints, "" + st.seconds));
            }
            Csv.writeTable(dir.resolve("stats.csv"),
                    Arrays.asList("puzzle", "attempts", "wrong", "solved", "first_try", "hints", "seconds"), rows);

            rows = new ArrayList<>();
            for (Map.Entry<String, Integer> e : slips.entrySet()) rows.add(Arrays.asList(e.getKey(), "" + e.getValue()));
            Csv.writeTable(dir.resolve("slips.csv"), Arrays.asList("tag", "count"), rows);
        } catch (IOException e) {
            System.err.println("[geomystery] 저장 실패: " + e);
        }
    }

    private static void kv(List<List<String>> rows, String k, Object v) {
        rows.add(Arrays.asList(k, String.valueOf(v)));
    }

    /** 저장 파일 이름들(내보내기·불러오기에 사용). */
    public static final List<String> FILES = Collections.unmodifiableList(Arrays.asList(
            "profile.csv", "progress.csv", "history.csv", "mistakes.csv", "quests.csv", "stats.csv", "slips.csv"));

    /** 모든 진행 상황을 지우고 처음 상태로 되돌린다(화면 설정은 유지). */
    public void reset() {
        name = "견습 탐정";
        xp = 0; tokens = 5; lanterns = MAX_LANTERNS; streak = 0; bestStreak = 0; lastStudy = "";
        lanternStamp = System.currentTimeMillis();
        auxTotal = noHintRun = noHintBest = perfectLessons = solvedTotal = firstTryTotal = 0;
        questDate = ""; questProgress.clear(); questDone.clear();
        progress.clear(); history.clear(); mistakes.clear(); stats.clear(); slips.clear();
        leagueTier = 0; leagueWeek = ""; leagueNotice = ""; finalesSolved = 0;
        solvedFinales.clear(); seenCards.clear(); endingSeen = false;
        photosPinned.clear(); epiloguesSeen.clear();
        save();
    }
}
