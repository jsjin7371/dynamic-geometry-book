package geomystery.game;

import geomystery.data.Content;
import geomystery.data.Model.*;
import geomystery.data.SaveData;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** 게임 규칙: 해금, 경험치, 등불, 연속 학습, 의뢰, 리그, 훈장. */
public final class Game {
    public static final int XP_PER_LEVEL = 150;
    public static final int HINT_COST = 1;
    public static final int REFILL_COST = 3;

    public final Content content;
    public final SaveData save;

    public Game(Content content, SaveData save) {
        this.content = content;
        this.save = save;
        refreshLanterns();
        ensureQuestDay();
        settleLeague();
    }

    public LocalDate today() { return LocalDate.now(); }

    // ------------------------------------------------------------------ 진행 / 해금
    public boolean isDone(Lesson l) { return save.isDone(l.id); }

    /** 방금 처음 해결한 사건(지도에서 헤이즐이 다음 사건으로 걸어가는 연출용). 한 번만 꺼내진다. */
    private String justCleared;

    public Lesson takeJustCleared() {
        String id = justCleared;
        justCleared = null;
        return id == null ? null : content.lessonById.get(id);
    }

    public boolean allCleared() { return currentLesson() == null; }

    public boolean isUnlocked(Lesson l) {
        if (l.globalIndex == 0) return true;
        return isDone(content.lessons.get(l.globalIndex - 1));
    }

    /** 아직 해결하지 않은 첫 사건. 모두 해결했다면 null. */
    public Lesson currentLesson() {
        for (Lesson l : content.lessons) if (!isDone(l)) return l;
        return null;
    }

    public Chapter currentChapter() {
        Lesson l = currentLesson();
        if (l == null) return content.chapters.get(content.chapters.size() - 1);
        return content.chapterOf(l);
    }

    public boolean isChapterUnlocked(Chapter ch) {
        List<Lesson> ls = content.lessonsOf(ch);
        return !ls.isEmpty() && isUnlocked(ls.get(0));
    }

    public int doneCount(Chapter ch) {
        int n = 0;
        for (Lesson l : content.lessonsOf(ch)) if (isDone(l)) n++;
        return n;
    }

    public int lessonsDone() {
        int n = 0;
        for (Lesson l : content.lessons) if (isDone(l)) n++;
        return n;
    }

    public int chaptersDone() {
        int n = 0;
        for (Chapter ch : content.chapters) if (doneCount(ch) == content.lessonsOf(ch).size()) n++;
        return n;
    }

    public List<Theorem> cards() {
        List<Theorem> out = new ArrayList<>();
        for (Lesson l : content.lessons) {
            if (isDone(l) && content.theorems.containsKey(l.theoremId)) out.add(content.theorems.get(l.theoremId));
        }
        return out;
    }

    // ------------------------------------------------------------------ 호감도 / 후일담
    public static final int HEARTS = 5;

    /**
     * 인물의 호감도(하트 0~5). 그 인물이 나오는 장의 사건을 해결할수록, 최종 추리로 범인을 밝히면 찬다.
     * 장을 모두 해결하고 범인까지 밝혀야 5가 된다.
     */
    public int hearts(String npcId) {
        Chapter ch = content.chapterOfNpc(npcId);
        if (ch == null) return 0;
        List<Lesson> ls = content.lessonsOf(ch);
        int pts = 0, max = ls.size();
        for (Lesson l : ls) {
            if (isDone(l)) pts++;
            if (content.hasFinale(l)) {
                max++;
                if (save.solvedFinales.contains(l.id)) pts++;
            }
        }
        if (max == 0) return 0;
        if (pts >= max) return HEARTS;
        return Math.min(HEARTS - 1, (int) Math.floor(HEARTS * pts / (double) max));
    }

    public boolean epilogueOpen(String npcId) {
        return hearts(npcId) >= HEARTS && content.epilogues.containsKey(npcId);
    }

    /** 열렸지만 아직 보지 않은 후일담 수. */
    public int newEpilogues() {
        int n = 0;
        for (String id : content.epilogues.keySet()) if (epilogueOpen(id) && !save.epiloguesSeen.contains(id)) n++;
        return n;
    }

    public Lesson lessonForTheorem(String theoremId) { return content.lessonOfTheorem(theoremId); }

    // ------------------------------------------------------------------ 등불(하트)
    public int lanterns() {
        refreshLanterns();
        return save.lanterns;
    }

    public void refreshLanterns() {
        long now = System.currentTimeMillis();
        if (save.lanterns >= SaveData.MAX_LANTERNS) { save.lanternStamp = now; return; }
        if (save.lanternStamp <= 0) save.lanternStamp = now;
        long elapsed = now - save.lanternStamp;
        int gained = (int) (elapsed / SaveData.REGEN_MS);
        if (gained > 0) {
            save.lanterns = Math.min(SaveData.MAX_LANTERNS, save.lanterns + gained);
            save.lanternStamp += gained * SaveData.REGEN_MS;
            if (save.lanterns >= SaveData.MAX_LANTERNS) save.lanternStamp = now;
            save.save();
        }
    }

    /** 다음 등불이 켜질 때까지 남은 밀리초(가득 차 있으면 0). */
    public long msToNextLantern() {
        refreshLanterns();
        if (save.lanterns >= SaveData.MAX_LANTERNS) return 0;
        return Math.max(0, save.lanternStamp + SaveData.REGEN_MS - System.currentTimeMillis());
    }

    public boolean refillLanterns() {
        if (save.tokens < REFILL_COST) return false;
        save.tokens -= REFILL_COST;
        save.lanterns = SaveData.MAX_LANTERNS;
        save.lanternStamp = System.currentTimeMillis();
        save.save();
        return true;
    }

    // ------------------------------------------------------------------ 연속 학습
    private static LocalDate parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    public int streakNow() {
        LocalDate last = parseDate(save.lastStudy);
        if (last == null) return 0;
        long gap = ChronoUnit.DAYS.between(last, today());
        return gap <= 1 ? save.streak : 0;
    }

    private void recordStudyDay() {
        LocalDate t = today();
        if (t.toString().equals(save.lastStudy)) return;
        LocalDate last = parseDate(save.lastStudy);
        if (last != null && last.plusDays(1).equals(t)) save.streak++;
        else save.streak = 1;
        save.lastStudy = t.toString();
        save.bestStreak = Math.max(save.bestStreak, save.streak);
    }

    private void addHistory(int xp, int puzzles) {
        int[] h = save.history.computeIfAbsent(today().toString(), k -> new int[2]);
        h[0] += xp;
        h[1] += puzzles;
    }

    // ------------------------------------------------------------------ 레벨
    public int level() { return 1 + save.xp / XP_PER_LEVEL; }

    public float levelProgress() { return (save.xp % XP_PER_LEVEL) / (float) XP_PER_LEVEL; }

    public static String titleFor(int level) {
        if (level >= 30) return "명탐정";
        if (level >= 20) return "수석 탐정";
        if (level >= 10) return "정식 탐정";
        return "견습 탐정";
    }

    public String title() { return titleFor(level()); }

    /** 레벨 lv 다음에 칭호가 바뀌는 레벨. */
    private static int nextTitleLevel(int lv) { return lv < 10 ? 10 : lv < 20 ? 20 : 30; }

    public String nextTitle() {
        int lv = level();
        return lv >= 30 ? null : titleFor(nextTitleLevel(lv));
    }

    public int xpToNextTitle() {
        return (nextTitleLevel(level()) - 1) * XP_PER_LEVEL - save.xp;
    }

    // ------------------------------------------------------------------ 오늘의 의뢰
    private void ensureQuestDay() {
        String t = today().toString();
        if (!t.equals(save.questDate)) {
            save.questDate = t;
            save.questProgress.clear();
            save.questDone.clear();
            for (Quest q : todayQuests()) save.questProgress.put(q.id, 0);
            save.save();
        }
    }

    public List<Quest> todayQuests() {
        List<Quest> all = new ArrayList<>(content.quests);
        Collections.shuffle(all, new Random(today().toEpochDay() * 31 + 7));
        return all.subList(0, Math.min(3, all.size()));
    }

    public int questProgress(Quest q) {
        ensureQuestDay();
        return Math.min(q.target, save.questProgress.getOrDefault(q.id, 0));
    }

    public boolean questDone(Quest q) { return save.questDone.contains(q.id); }

    /** 의뢰 진행도를 올리고 새로 완료된 의뢰의 안내 문구를 돌려준다. */
    public List<String> bump(String type, int amount) {
        ensureQuestDay();
        List<String> msgs = new ArrayList<>();
        for (Quest q : todayQuests()) {
            if (!q.type.equals(type) || save.questDone.contains(q.id)) continue;
            int v = save.questProgress.getOrDefault(q.id, 0) + amount;
            save.questProgress.put(q.id, v);
            if (v >= q.target) {
                save.questDone.add(q.id);
                save.tokens += q.reward;
                msgs.add("오늘의 의뢰 완료 · " + q.text + "  (단서 토큰 +" + q.reward + ")");
            }
        }
        return msgs;
    }

    public long hoursToMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime mid = now.toLocalDate().plusDays(1).atStartOfDay();
        return Math.max(1, ChronoUnit.MINUTES.between(now, mid) / 60);
    }

    // ------------------------------------------------------------------ 리그
    public static final String[] TIERS = {"청동 돋보기", "은 돋보기", "금 돋보기", "수정 돋보기", "명탐정"};
    /** 단계마다 경쟁자들이 얼마나 더 열심히 하는지. */
    private static final float[] TIER_FACTOR = {1f, 1.35f, 1.75f, 2.2f, 2.8f};
    /** 이 순위 이내면 승급, 이 순위 이상이면 강등(전체 인원 기준). */
    public static final int PROMOTE_RANK = 5;
    public static final int DEMOTE_FROM_BOTTOM = 2;

    public static final class LeagueRow {
        public String name;
        public int xp, color;
        public boolean me;

        public int solvedCases;
        public int accuracy;
        public int bestStreak;
        public int studySeconds;
        public int auxTotal;   /* 가상 탐정 한 명마다 상세 프로필 통계값을 담을 공간 */
    
        public int cardsCount; /*정리 카드 수와 각 챕터의 완료 사건 수를 저장 290~292 */
        public List<Integer> chapterDone = new ArrayList<>();
        public List<Integer> chapterTotal = new ArrayList<>();
    }

    public int tier() { return Math.max(0, Math.min(TIERS.length - 1, save.leagueTier)); }

    public String tierName() { return TIERS[tier()] + " 리그"; }

    public String nextTierName() { return tier() < TIERS.length - 1 ? TIERS[tier() + 1] : null; }

    public int weekXp() { return weekXp(today().with(DayOfWeek.MONDAY), today()); }

    private int weekXp(LocalDate monday, LocalDate until) {
        int sum = 0;
        for (Map.Entry<String, int[]> e : save.history.entrySet()) {
            LocalDate d = parseDate(e.getKey());
            if (d != null && !d.isBefore(monday) && !d.isAfter(until) && !d.isAfter(monday.plusDays(6))) sum += e.getValue()[0];
        }
        return sum;
    }

    public List<LeagueRow> league() {
        LocalDate t = today();
        return league(t.with(DayOfWeek.MONDAY), t.getDayOfWeek().getValue() - 1, tier());
    }

    /** monday 주의 dayIndex(0=월 … 6=일)까지 순위표. */
    private List<LeagueRow> league(LocalDate monday, int day, int tier) {
        long week = monday.toEpochDay();
        float f = TIER_FACTOR[Math.max(0, Math.min(TIER_FACTOR.length - 1, tier))];
        List<LeagueRow> rows = new ArrayList<>();
        for (Bot b : content.bots) {
            LeagueRow r = new LeagueRow();
            r.name = b.name;
            r.color = b.color;
            long h = Math.abs((b.name.hashCode() * 31L + week * 17L) % 97);
            r.xp = Math.round((b.base + b.daily * day + h * (day + 1) / 3f) * f);
            int seed = Math.floorMod(b.name.hashCode(), 1000);
            
            r.solvedCases = 5 + seed % 80;    /*가상 탐정 데이터 추가 326~330 */
            r.accuracy = 55 + seed % 46;
            r.bestStreak = 1 + seed % 20;
            r.studySeconds = (10 + seed % 300) * 60;
            r.auxTotal = seed % 120;

            r.cardsCount = Math.min(r.solvedCases, content.theorems.size()); /*가상 조사관 데이터 추가 336~346 */
            
            for (int i = 0; i < content.chapters.size(); i++) {
                Chapter ch = content.chapters.get(i);

                int total = content.lessonsOf(ch).size();
                int done = total == 0 ? 0
                        : Math.floorMod(seed + i * 7, total + 1);
                
                r.chapterDone.add(done);
                r.chapterTotal.add(total);
            }

            rows.add(r);
        }
        LeagueRow me = new LeagueRow();
        me.name = "나 (" + save.name + ")";
        me.xp = weekXp(monday, monday.plusDays(day));
        me.me = true;
        me.color = 0xB8903A;

        me.solvedCases = lessonsDone();
        me.accuracy = overallAccuracy();
        me.bestStreak = Math.max(save.bestStreak, streakNow());
        me.auxTotal = save.auxTotal;

        me.cardsCount = cards().size(); /*내 프로필 데이터 추가  362~367*/
        
        for (Chapter ch : content.chapters) {
            me.chapterDone.add(doneCount(ch));
            me.chapterTotal.add(content.lessonsOf(ch).size());
        }
        
        for (SaveData.PuzzleStat st : save.stats.values()) {
            me.studySeconds += st.seconds;
        } /*내 프로필 통계 추가 340~347 */

        rows.add(me);
        rows.sort((a, b) -> b.xp != a.xp ? Integer.compare(b.xp, a.xp) : Boolean.compare(b.me, a.me));
        return rows;
    }

    public int myRank() { return rankOf(league()); }

    private static int rankOf(List<LeagueRow> rows) {
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).me) return i + 1;
        return rows.size();
    }

    /** 강등되는 첫 순위(이 순위부터 강등). */
    public int demoteRank() { return content.bots.size() + 1 - DEMOTE_FROM_BOTTOM + 1; }

    /**
     * 지난주 리그를 정산한다: 상위 PROMOTE_RANK명은 승급, 하위 DEMOTE_FROM_BOTTOM명은 강등.
     * 여러 주를 건너뛰었다면 마지막으로 참가한 주만 정산하고, 쉬는 동안의 주는 한 번 더 강등으로 친다.
     */
    public void settleLeague() {
        LocalDate monday = today().with(DayOfWeek.MONDAY);
        if (save.leagueWeek == null || save.leagueWeek.isEmpty()) {
            save.leagueWeek = monday.toString();
            save.save();
            return;
        }
        LocalDate last;
        try { last = LocalDate.parse(save.leagueWeek); } catch (Exception e) { last = monday; }
        if (!last.isBefore(monday)) return;
        int before = tier();
        List<LeagueRow> rows = league(last, 6, before);
        int rank = rankOf(rows);
        int t = before;
        int delta = 0; // +1 승급, -1 강등
        if (rank <= PROMOTE_RANK && t < TIERS.length - 1) { t++; delta = 1; }
        else if (rank >= demoteRank() && t > 0) { t--; delta = -1; }
        boolean skipped = ChronoUnit.WEEKS.between(last, monday) > 1;
        if (skipped && t > 0 && delta <= 0) { t--; delta = -1; }
        save.leagueTier = t;
        save.leagueWeek = monday.toString();
        String msg = "지난주 " + TIERS[before] + " 리그 " + rank + "위 — ";
        if (delta > 0) msg += TIERS[t] + " 리그로 승급했습니다!";
        else if (delta < 0) msg += TIERS[t] + " 리그로 내려갔습니다." + (skipped ? " (쉬는 동안 순위가 밀렸어요)" : "");
        else msg += TIERS[t] + " 리그에 머뭅니다.";
        save.leagueNotice = msg;
        if (delta > 0) save.tokens += 2;
        save.save();
    }

    /** 정산 결과를 한 번만 꺼내 준다(없으면 null). */
    public String takeLeagueNotice() {
        String n = save.leagueNotice;
        if (n == null || n.isEmpty()) return null;
        save.leagueNotice = "";
        save.save();
        return n;
    }

    public String leagueTimeLeft() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = today().with(DayOfWeek.SUNDAY).plusDays(1).atStartOfDay();
        long mins = ChronoUnit.MINUTES.between(now, end);
        long d = mins / (60 * 24), h = (mins / 60) % 24;
        return d > 0 ? d + "일 " + h + "시간" : h + "시간";
    }

    public long leagueDaysLeft() {
        return ChronoUnit.DAYS.between(today(), today().with(DayOfWeek.SUNDAY)) + 1;
    }

    // ------------------------------------------------------------------ 훈장 / 통계
    public int stat(String key) {
        switch (key) {
            case "aux_total": return save.auxTotal;
            case "nohint_best": return save.noHintBest;
            case "streak_best": return Math.max(save.bestStreak, streakNow());
            case "lessons_done": return lessonsDone();
            case "perfect_lessons": return save.perfectLessons;
            case "chapters_done": return chaptersDone();
            case "cards": return cards().size();
            case "solved_total": return save.solvedTotal;
            case "finales": return save.finalesSolved;
            case "league_tier": return tier();
            default: return 0;
        }
    }

    public boolean hasBadge(Badge b) { return stat(b.stat) >= b.target; }

    public int overallAccuracy() {
        if (save.solvedTotal == 0) return 0;
        return Math.round(save.firstTryTotal * 100f / save.solvedTotal);
    }

    public Quote quoteOfDay() {
        if (content.quotes.isEmpty()) return null;
        return content.quotes.get((int) (today().toEpochDay() % content.quotes.size()));
    }

    // ------------------------------------------------------------------ 오답 노트
    public List<Puzzle> mistakePuzzles() {
        List<Puzzle> out = new ArrayList<>();
        for (SaveData.Mistake m : save.mistakes.values()) {
            Puzzle p = content.puzzleById.get(m.puzzleId);
            if (p != null) out.add(p);
        }
        return out;
    }

    public List<Puzzle> practicePool() {
        List<Puzzle> out = new ArrayList<>();
        for (Lesson l : content.lessons) if (isDone(l)) out.addAll(content.puzzlesOf(l));
        return out;
    }

    // ------------------------------------------------------------------ 풀이 이벤트
    /**
     * 정답 확인. 입력 형식:
     * number/choice — 값 그대로, expr — 식, order — 단계들을 '|'로 이은 것, tf — "참|이유".
     */
    public static boolean checkAnswer(Puzzle p, String input) {
        if (input == null) return false;
        String a = input.trim();
        if (p.isChoice()) return a.equals(p.answer);
        if (p.isOrder()) return a.equals(String.join("|", p.choices));
        if (p.isTf()) {
            int i = a.indexOf('|');
            if (i < 0) return false;
            return a.substring(0, i).trim().equals(p.tfVerdict()) && a.substring(i + 1).trim().equals(p.tfReason());
        }
        if (p.isExpr()) return Expr.equivalent(a, p.answer);
        return sameNumber(a, p.answer);
    }

    private static boolean sameNumber(String a, String b) {
        try {
            return Double.parseDouble(a.trim()) == Double.parseDouble(b.trim());
        } catch (NumberFormatException e) {
            return a.trim().equals(b.trim());
        }
    }

    /** 오답 진단 결과: 헤이즐이 할 말과 실수 유형(없을 수 있음). */
    public static final class Diagnosis {
        public String text, tag;
        Diagnosis(String text, String tag) { this.text = text; this.tag = tag; }
    }

    /**
     * 오답을 살펴 맞춤 피드백을 찾는다.
     * 1) puzzles.csv 의 feedback 열 → 2) 문제 유형별 규칙 → 3) 각도 문제의 흔한 실수(보각·여각·절반) 순서.
     * 해당하는 것이 없으면 null.
     */
    public Diagnosis diagnose(Puzzle p, String input) {
        if (input == null) return null;
        String in = input.trim();
        for (Feedback f : p.feedback) {
            boolean hit;
            if (p.isExpr()) hit = Expr.equivalent(in, f.value);
            else if (p.isChoice() || p.isTf() || p.isOrder()) hit = in.equals(f.value) || in.startsWith(f.value + "|");
            else hit = sameNumber(in, f.value);
            if (hit) return new Diagnosis(f.text, f.tag);
        }
        if (p.isTf()) {
            int i = in.indexOf('|');
            String verdict = i < 0 ? in : in.substring(0, i).trim();
            if (verdict.equals(p.tfVerdict())) {
                return new Diagnosis("'" + verdict + "'이라는 판단은 맞았어! 그런데 그 이유로는 설명이 안 돼. 어떤 정리가 근거가 될지 다시 골라 봐.", "reason");
            }
            return new Diagnosis("앗, 판단부터 다시 해 보자. 문장이 정말 " + verdict + "일까? 정리 카드를 떠올려 봐.", null);
        }
        if (p.isOrder()) {
            String[] got = in.split("\\|");
            for (int k = 0; k < Math.min(got.length, p.choices.size()); k++) {
                if (!got[k].equals(p.choices.get(k))) {
                    return new Diagnosis((k + 1) + "번째 단계부터 순서가 어긋났어. '" + shorten(got[k]) + "'보다 먼저 밝혀야 할 사실이 있지 않을까?", "sequence");
                }
            }
            return null;
        }
        if (p.isExpr()) {
            if (!Expr.hasVariable(in) && Expr.hasVariable(p.answer)) {
                return new Diagnosis("답은 x가 들어간 식이어야 해. x의 값을 구하는 게 아니라 x로 나타내 보자.", null);
            }
            try {
                double a = Expr.eval(p.answer, 1.3), b = Expr.eval(in, 1.3);
                if (Math.abs(b * 2 - a) < 1e-9 || Math.abs(b - a * 2) < 1e-9) {
                    return new Diagnosis("딱 두 배 차이가 나! 반으로 나누거나 두 배 하는 걸 거꾸로 한 건 아닐까?", "half");
                }
            } catch (IllegalArgumentException ignored) { }
            return null;
        }
        if (p.isNumber()) {
            double ans, v;
            try { ans = Double.parseDouble(p.answer); v = Double.parseDouble(in); } catch (NumberFormatException e) { return null; }
            boolean deg = p.unit != null && p.unit.trim().equals("°");
            if (deg && ans != 90 && v == 180 - ans) return new Diagnosis("너의 답과 정답을 더하면 180°가 돼. 한 직선 위의 이웃한 각(보각)을 구한 건 아닐까?", "supp");
            if (deg && ans < 90 && v == 90 - ans) return new Diagnosis("너의 답과 정답을 더하면 90°야. 직각을 나눈 다른 쪽 각을 구한 것 같아.", "comp");
            if (deg && v == 360 - ans) return new Diagnosis("한 바퀴 360°에서 뺀 값이네. 작은 쪽 각을 다시 확인해 봐.", "full");
            if (ans != 0 && (v == ans * 2 || v * 2 == ans)) return new Diagnosis("정답과 딱 두 배 차이야. 절반으로 나누는 단계(혹은 두 배)를 다시 확인해 봐!", "half");
        }
        return null;
    }

    /**
     * 풀이 다시 보기 단계. 직접 쓴 단계가 없으면 해설을 문장·'~므로' 단위로 나눠 만든다.
     * 보조선이 있으면 맨 앞에 보조선 긋기, 숫자·식 문제는 맨 끝에 답 쓰기를 붙인다.
     */
    public static List<String> solutionSteps(Puzzle p, boolean hasAux) {
        List<String> out = new ArrayList<>();
        if (!p.steps.isEmpty()) {
            out.addAll(p.steps);
            return out;
        }
        if (hasAux) out.add("@aux 보조선을 그어 숨은 관계를 드러낸다.");
        if (p.isOrder()) {
            // 추리 순서 문제는 올바른 단계를 하나씩 보여 준다
            for (int i = 0; i < p.choices.size(); i++) out.add((i + 1) + "단계 — " + p.choices.get(i));
            return out;
        }
        if (p.isTf()) {
            out.add("판단: " + p.tfVerdict());
            out.add("근거: " + p.tfReason());
        }
        String ex = p.explanation == null ? "" : p.explanation.trim();
        for (String sentence : ex.split("(?<=[.!?])\\s+")) {
            String rest = sentence.trim();
            // '~이므로', '~하므로', '~에서' 뒤에서 한 번 끊어 근거와 결론을 나눈다
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.{6,}?(?:이므로|하므로|으므로|므로|에서))\\s+(.{4,})$").matcher(rest);
            if (m.matches()) {
                out.add(m.group(1).trim());
                out.add(m.group(2).trim());
            } else if (!rest.isEmpty()) {
                out.add(rest);
            }
        }
        if (p.isNumber() || p.isExpr()) out.add("@ans 답: " + p.answerText());
        else if (p.isChoice()) out.add("답: " + p.answerText());
        return out;
    }

    private static String shorten(String s) { return s.length() > 18 ? s.substring(0, 17) + "…" : s; }

    public boolean useHint(Session s, Puzzle p) {
        if (save.tokens < HINT_COST) return false;
        save.tokens -= HINT_COST;
        s.hintsUsed++;
        s.hintsOpened.merge(p.id, 1, Integer::sum);
        save.stat(p.id).hints++;
        save.save();
        return true;
    }

    public List<String> useAux(Session s, Puzzle p) {
        if (!s.auxUsedOn.add(p.id)) return Collections.emptyList();
        save.auxTotal++;
        List<String> msgs = bump("aux", 1);
        save.save();
        return msgs;
    }

    /** 등불 하나를 끈다(오답·잘못된 증거 제시). */
    private void loseLantern() {
        refreshLanterns();
        if (save.lanterns >= SaveData.MAX_LANTERNS) save.lanternStamp = System.currentTimeMillis();
        save.lanterns = Math.max(0, save.lanterns - 1);
    }

    /** 오답 처리. 맞춤 피드백이 있으면 돌려준다(없으면 null). */
    public Diagnosis onWrong(Session s, Puzzle p, String input) {
        loseLantern();
        s.wrongIds.add(p.id);
        save.noHintRun = 0;
        SaveData.Mistake m = save.mistakes.get(p.id);
        if (m == null) {
            m = new SaveData.Mistake();
            m.puzzleId = p.id;
            save.mistakes.put(p.id, m);
        }
        m.count++;
        m.last = today().toString();
        SaveData.PuzzleStat st = save.stat(p.id);
        st.attempts++;
        st.wrong++;
        Diagnosis d = diagnose(p, input);
        if (d != null && d.tag != null && !d.tag.isEmpty()) save.slips.merge(d.tag, 1, Integer::sum);
        save.save();
        return d;
    }

    public List<String> onCorrect(Session s, Puzzle p) {
        List<String> msgs = new ArrayList<>();
        boolean first = !s.wrongIds.contains(p.id);
        boolean noHint = s.hintsOpened(p) == 0;
        save.solvedTotal++;
        if (first) save.firstTryTotal++;
        SaveData.PuzzleStat st = save.stat(p.id);
        st.attempts++;
        st.solved++;
        if (first) st.firstTry++;
        st.seconds += (int) Math.min(3600, (System.currentTimeMillis() - s.shownMs) / 1000);
        if (first && noHint) {
            save.noHintRun++;
            save.noHintBest = Math.max(save.noHintBest, save.noHintRun);
            msgs.addAll(bump("nohint", 1));
        } else {
            save.noHintRun = 0;
        }
        msgs.addAll(bump("solve", 1));
        if (s.mode == Session.Mode.REVIEW && first) {
            if (save.mistakes.remove(p.id) != null) msgs.addAll(bump("review", 1));
        }
        save.save();
        return msgs;
    }

    // ------------------------------------------------------------------ 최종 추리
    /** 최종 추리에서 꺼낼 수 있는 정리 카드: 모은 카드 + 이번 사건의 카드. */
    public List<Theorem> finaleHand(Lesson l) {
        List<Theorem> out = new ArrayList<>(cards());
        Theorem own = content.theorems.get(l.theoremId);
        if (own != null && !out.contains(own)) out.add(own);
        return out;
    }

    /** 잘못된 증거 제시: 등불 하나를 끈다. */
    public void onFinaleWrong(Session s) {
        loseLantern();
        s.finaleMistakes++;
        save.save();
    }

    public static int finaleBonus(int mistakes) { return Math.max(20, 60 - mistakes * 15); }

    public int finaleBonusFor(Session s) {
        int b = finaleBonus(s.finaleMistakes);
        return s.lesson != null && save.solvedFinales.contains(s.lesson.id) ? b / 2 : b;
    }

    // ------------------------------------------------------------------ 약점 분석
    /** 정리(사건) 하나에 대한 누적 실력. */
    public static final class Skill {
        public Theorem theorem;
        public Lesson lesson;
        public int puzzles, seen, attempts, wrong, solved, firstTry, hints, seconds;

        /** 첫 시도 정답률(0~100). 푼 적이 없으면 -1. */
        public int accuracy() { return solved == 0 ? -1 : Math.round(firstTry * 100f / solved); }
        public int avgSeconds() { return solved == 0 ? 0 : Math.round(seconds / (float) solved); }
        public float hintsPerPuzzle() { return solved == 0 ? 0 : hints / (float) solved; }

        /** 낮을수록 약하다. 정답률에서 힌트 의존·느린 풀이를 깎는다. */
        public float score() {
            if (solved == 0) return 999;
            float sc = accuracy() - hintsPerPuzzle() * 12f;
            if (avgSeconds() > 90) sc -= Math.min(20, (avgSeconds() - 90) / 6f);
            return sc;
        }

        public String verdict() {
            float sc = score();
            if (sc >= 85) return "탄탄함";
            if (sc >= 60) return "보통";
            return "약점";
        }
    }

    /** 푼 기록이 있는 정리별 실력. 약한 순으로 정렬. */
    public List<Skill> skills() {
        List<Skill> out = new ArrayList<>();
        for (Lesson l : content.lessons) {
            Theorem t = content.theorems.get(l.theoremId);
            if (t == null) continue;
            Skill k = new Skill();
            k.theorem = t;
            k.lesson = l;
            for (Puzzle p : content.puzzlesOf(l)) {
                k.puzzles++;
                SaveData.PuzzleStat st = save.stats.get(p.id);
                if (st == null) continue;
                k.seen++;
                k.attempts += st.attempts; k.wrong += st.wrong; k.solved += st.solved;
                k.firstTry += st.firstTry; k.hints += st.hints; k.seconds += st.seconds;
            }
            if (k.solved > 0) out.add(k);
        }
        out.sort(Comparator.comparingDouble(Skill::score));
        return out;
    }

    /** 문제 유형별 {푼 수, 첫 시도 정답 수}. */
    public Map<String, int[]> typeStats() {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (String t : new String[]{Puzzle.NUMBER, Puzzle.CHOICE, Puzzle.EXPR, Puzzle.TF, Puzzle.ORDER}) out.put(t, new int[2]);
        for (SaveData.PuzzleStat st : save.stats.values()) {
            Puzzle p = content.puzzleById.get(st.puzzleId);
            if (p == null) continue;
            String key = p.isNumber() ? Puzzle.NUMBER : p.type;
            int[] v = out.computeIfAbsent(key, k -> new int[2]);
            v[0] += st.solved;
            v[1] += st.firstTry;
        }
        return out;
    }

    /** 자주 하는 실수(많은 순). */
    public List<Map.Entry<String, Integer>> topSlips() {
        List<Map.Entry<String, Integer>> out = new ArrayList<>(save.slips.entrySet());
        out.removeIf(e -> e.getValue() <= 0);
        out.sort((a, b) -> b.getValue() - a.getValue());
        return out;
    }

    /** 약점 집중 연습 문제: 그 정리의 문제 중 첫 시도 정답률이 낮은 순. */
    public List<Puzzle> drillFor(Skill k) {
        List<Puzzle> ps = new ArrayList<>(content.puzzlesOf(k.lesson));
        ps.sort(Comparator.comparingDouble(p -> {
            SaveData.PuzzleStat st = save.stats.get(p.id);
            return st == null || st.solved == 0 ? 0.5 : st.firstTry / (double) st.solved;
        }));
        return ps.subList(0, Math.min(5, ps.size()));
    }

    /** 세션 종료 처리: 경험치, 기록, 정리 카드, 토큰을 반영한다. */
    public Result finish(Session s) {
        Result r = new Result();
        r.session = s;
        r.total = s.puzzles.size();
        r.firstTry = s.firstTryCount();
        r.accuracy = r.total == 0 ? 100 : Math.round(r.firstTry * 100f / r.total);
        r.seconds = (int) ((System.currentTimeMillis() - s.startMs) / 1000);
        for (Puzzle p : s.puzzles) if (s.wrongIds.contains(p.id)) r.wrong.add(p);

        int xp;
        if (s.lesson != null) {
            r.firstClear = !isDone(s.lesson);
            xp = Math.round(s.lesson.xp * (0.5f + 0.5f * r.firstTry / Math.max(1, r.total))) - s.hintsUsed * 5;
            if (!r.firstClear) xp /= 2;
            xp = Math.max(10, xp);
        } else {
            xp = r.firstTry * 10 + (r.total - r.firstTry) * 5;
        }
        if (s.finaleSolved) {
            r.finaleXp = finaleBonusFor(s);
            xp += r.finaleXp;
        }
        r.xp = xp;

        r.levelBefore = level();
        int before = streakNow();
        recordStudyDay();
        r.streak = save.streak;
        r.streakGrew = save.streak > before;
        addHistory(xp, r.total);
        save.xp += xp;

        Map<String, Integer> heartsBefore = new HashMap<>();
        Chapter heartCh = s.lesson == null ? null : content.chapterOf(s.lesson);
        if (heartCh != null) for (Npc n : content.npcsOf(heartCh)) heartsBefore.put(n.id, hearts(n.id));
        if (s.lesson != null) {
            SaveData.LessonRecord rec = save.record(s.lesson.id);
            if (rec.done && rec.lastTime > 0) r.previousSeconds = rec.lastTime;
            rec.done = true;
            rec.plays++;
            rec.bestAcc = Math.max(rec.bestAcc, r.accuracy);
            rec.bestTime = rec.bestTime == 0 ? r.seconds : Math.min(rec.bestTime, r.seconds);
            rec.lastTime = r.seconds;
            if (rec.completedAt == null || rec.completedAt.isEmpty()) rec.completedAt = today().toString();

            if (r.firstClear) { save.tokens += 2; r.tokensGained += 2; }
            if (s.wrongIds.isEmpty()) {
                save.perfectLessons++;
                if (r.firstClear) {
                    save.tokens += 1;
                    r.tokensGained += 1;
                }
            }
            if (r.firstClear) {
                justCleared = s.lesson.id;
                r.gameCleared = s.lesson == content.lessons.get(content.lessons.size() - 1);
            }
            if (s.finaleSolved) {
                if (save.solvedFinales.add(s.lesson.id)) {
                    save.finalesSolved = Math.max(save.finalesSolved, save.solvedFinales.size());
                    save.tokens += 1;
                    r.tokensGained += 1;
                }
            }
            r.card = content.theorems.get(s.lesson.theoremId);
            r.cardIsNew = r.firstClear;
            r.messages.addAll(bump("lesson", 1));
            r.heartNpc = s.lesson.npc;
            r.heartsBefore = heartsBefore.getOrDefault(s.lesson.npc, 0);
            r.heartsAfter = hearts(s.lesson.npc);
            for (Map.Entry<String, Integer> e : heartsBefore.entrySet()) {
                if (e.getValue() < HEARTS && epilogueOpen(e.getKey())) r.epiloguesOpened.add(e.getKey());
            }
        }
        r.levelAfter = level();
        r.cardCount = cards().size();
        r.cardTotal = content.theorems.size();
        r.messages.addAll(bump("xp", xp));
        save.save();
        return r;
    }
}
