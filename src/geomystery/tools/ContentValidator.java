package geomystery.tools;

import geomystery.data.Content;
import geomystery.data.Csv;
import geomystery.data.Model.*;
import geomystery.diagram.DiagramView;
import geomystery.game.Expr;
import geomystery.game.Game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * data/*.csv 콘텐츠 검사기. 사건·수수께끼를 추가한 뒤 실행해 깨진 참조나 잘못된 정답을 찾는다.
 * <pre>
 *   ./run.sh validate            (또는 java -cp out/app geomystery.tools.ContentValidator [프로젝트 폴더])
 * </pre>
 * 오류가 하나라도 있으면 종료 코드 1.
 */
public final class ContentValidator {
    public enum Level { ERROR, WARN }

    public static final class Issue {
        public final Level level;
        public final String file, where, message;

        Issue(Level level, String file, String where, String message) {
            this.level = level;
            this.file = file;
            this.where = where;
            this.message = message;
        }

        @Override public String toString() {
            return (level == Level.ERROR ? "[오류] " : "[주의] ") + file + (where.isEmpty() ? "" : " " + where) + " — " + message;
        }
    }

    private static final Set<String> QUEST_TYPES = new HashSet<>(Arrays.asList("solve", "nohint", "aux", "lesson", "xp", "review"));
    private static final Set<String> BADGE_STATS = new HashSet<>(Arrays.asList("aux_total", "nohint_best", "streak_best",
            "lessons_done", "perfect_lessons", "chapters_done", "cards", "solved_total", "finales", "league_tier"));
    private static final Set<String> EMOTIONS = new HashSet<>(Arrays.asList("happy", "surprised", "puzzled", "worried", "angry"));
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            Puzzle.NUMBER, Puzzle.CHOICE, Puzzle.ORDER, Puzzle.TF, Puzzle.EXPR));
    private static final Set<String> FINALE_KINDS = new HashSet<>(Arrays.asList(
            FinaleLine.INTRO, FinaleLine.CLAIM, FinaleLine.LIE, FinaleLine.REVEAL));

    private final List<Issue> issues = new ArrayList<>();
    private final Path home;

    private ContentValidator(Path home) { this.home = home; }

    private void err(String file, String where, String msg) { issues.add(new Issue(Level.ERROR, file, where, msg)); }
    private void warn(String file, String where, String msg) { issues.add(new Issue(Level.WARN, file, where, msg)); }

    /** 프로젝트 폴더(data/, assets/ 가 있는 곳)를 검사한다. */
    public static List<Issue> run(Path home) {
        ContentValidator v = new ContentValidator(home);
        v.check();
        return v.issues;
    }

    private void check() {
        Path data = home.resolve("data");
        Content c;
        try {
            c = Content.load(data);
        } catch (Exception e) {
            err("data/", "", "콘텐츠를 읽지 못했습니다: " + e);
            return;
        }
        duplicates(data);
        columnCounts(data);
        feedbackSyntax(data);

        // ---- 장 / 사건
        for (Npc n : c.npcs.values()) {
            if (!Files.exists(home.resolve("assets/img/" + n.image + "_face.png"))) {
                warn("npcs.csv", n.id, "얼굴 이미지가 없습니다: assets/img/" + n.image + "_face.png (AssetBaker로 굽기)");
            }
        }
        for (Chapter ch : c.chapters) {
            if (c.lessonsOf(ch).isEmpty()) warn("chapters.csv", ch.id, "사건이 하나도 없습니다");
            if (c.npc(ch.npc) == null) err("chapters.csv", ch.id, "없는 인물: " + ch.npc);
        }
        for (Lesson l : c.lessons) {
            if (!c.chapterById.containsKey(l.chapterId)) err("lessons.csv", l.id, "없는 장: " + l.chapterId);
            if (c.npc(l.npc) == null) err("lessons.csv", l.id, "없는 인물: " + l.npc);
            if (!c.theorems.containsKey(l.theoremId)) err("lessons.csv", l.id, "없는 정리: " + l.theoremId);
            if (!Files.exists(home.resolve("assets/img/bg_" + l.scene + ".png"))) warn("lessons.csv", l.id, "배경 이미지가 없습니다: bg_" + l.scene + ".png");
            if (c.puzzlesOf(l).isEmpty()) err("lessons.csv", l.id, "수수께끼가 하나도 없습니다");
            if (c.dialogueOf(l).isEmpty()) warn("dialogues.csv", l.id, "도입 대화가 없습니다(바로 풀이로 넘어갑니다)");
            if (l.boss && !c.hasFinale(l)) warn("finales.csv", l.id, "최종 사건인데 최종 추리 대본이 없습니다");
        }
        for (Map.Entry<String, List<Line>> e : c.dialogues.entrySet()) {
            if (!c.lessonById.containsKey(e.getKey())) err("dialogues.csv", e.getKey(), "없는 사건의 대사");
            for (Line ln : e.getValue()) {
                if (!"narration".equals(ln.speaker) && c.npc(ln.speaker) == null) err("dialogues.csv", ln.lessonId + "#" + ln.seq, "없는 인물: " + ln.speaker);
                if (!ln.emotion.isEmpty() && !EMOTIONS.contains(ln.emotion)) err("dialogues.csv", ln.lessonId + "#" + ln.seq, "모르는 표정: " + ln.emotion + " (happy|surprised|puzzled|worried|angry)");
                if (!ln.emotion.isEmpty() && "narration".equals(ln.speaker)) warn("dialogues.csv", ln.lessonId + "#" + ln.seq, "해설에는 표정이 보이지 않습니다");
            }
        }

        // ---- 수수께끼
        Set<String> usedDiagrams = new HashSet<>();
        for (Puzzle p : c.puzzleById.values()) checkPuzzle(c, p, usedDiagrams);

        // ---- 정리 탐구
        for (Explore x : c.explores.values()) {
            String w = x.theoremId;
            if (!c.theorems.containsKey(x.theoremId)) err("explore.csv", w, "없는 정리: " + x.theoremId);
            List<Cmd> sc = c.diagrams.get(x.diagram);
            if (sc == null) { err("explore.csv", w, "없는 도형: " + x.diagram); continue; }
            usedDiagrams.add(x.diagram);
            Map<String, java.awt.geom.Point2D.Double> pts = DiagramView.points(sc);
            if (x.drags.isEmpty()) err("explore.csv", w, "끌 수 있는 점(drag)이 없습니다");
            for (Drag d : x.drags) {
                if (!pts.containsKey(d.point)) err("explore.csv", w, "도형에 없는 점: " + d.point);
                if (!Arrays.asList("free", "hline", "vline", "circle", "arc").contains(d.kind)) err("explore.csv", w, "모르는 움직임: " + d.kind);
                if (("circle".equals(d.kind) || "arc".equals(d.kind)) && (d.center == null || !pts.containsKey(d.center))) err("explore.csv", w, d.point + "의 원 중심이 없습니다");
                if ("arc".equals(d.kind) && d.args.length != 2) err("explore.csv", w, d.point + ": arc 는 시작각과 끝각이 필요합니다");
            }
            geomystery.ui.ExploreBoard probe = new geomystery.ui.ExploreBoard(sc, x, null);
            String rel = probe.relationText();
            if (rel.contains("?")) err("explore.csv", w, "관계식을 계산하지 못한 부분이 있습니다: " + rel);
        }
        for (Theorem t : c.theorems.values()) if (!c.explores.containsKey(t.id)) warn("explore.csv", t.id, "정리 탐구 설정이 없습니다(도감에서 움직여 볼 수 없음)");

        // ---- 자백 만화
        Set<String> props = new HashSet<>(Arrays.asList("lamp", "gear", "rope", "lens"));
        for (Map.Entry<String, List<Confession>> e : c.confessions.entrySet()) {
            Lesson l = c.lessonById.get(e.getKey());
            if (l == null) { err("confessions.csv", e.getKey(), "없는 사건"); continue; }
            if (!c.hasFinale(l)) warn("confessions.csv", l.id, "최종 추리가 없는 사건이라 자백 만화가 나오지 않습니다");
            for (Confession cf : e.getValue()) {
                String w = l.id + "#" + cf.panel;
                if (!Arrays.asList(Confession.MOTIVE, Confession.METHOD, Confession.ARREST).contains(cf.kind)) err("confessions.csv", w, "모르는 칸 종류: " + cf.kind + " (motive|method|arrest)");
                if (Confession.METHOD.equals(cf.kind)) {
                    // 수법 칸: 범행 장면 이름(channel|gear|rope|disc) 또는 칠판에 그릴 도형 이름
                    if (!java.util.Arrays.asList("channel", "gear", "rope", "disc").contains(cf.prop)) {
                        if (!c.diagrams.containsKey(cf.prop)) err("confessions.csv", w, "수법 칸의 장면(channel|gear|rope|disc)이나 도형이 없습니다: " + cf.prop);
                        usedDiagrams.add(cf.prop);
                    }
                }
                if (Confession.MOTIVE.equals(cf.kind) && !props.contains(cf.prop)) warn("confessions.csv", w, "모르는 소품(lamp|gear|rope|lens): " + cf.prop);
            }
        }
        for (Lesson l : c.lessons) if (c.hasFinale(l) && !c.confessions.containsKey(l.id)) warn("confessions.csv", l.id, "자백 만화가 없습니다");

        // ---- 후일담
        for (Map.Entry<String, List<Line>> e : c.epilogues.entrySet()) {
            if (c.npc(e.getKey()) == null) err("epilogues.csv", e.getKey(), "없는 인물");
            else if (c.chapterOfNpc(e.getKey()) == null) warn("epilogues.csv", e.getKey(), "어느 장에도 나오지 않는 인물이라 호감도가 차지 않습니다");
            for (Line ln : e.getValue()) {
                if (!"narration".equals(ln.speaker) && c.npc(ln.speaker) == null) err("epilogues.csv", e.getKey() + "#" + ln.seq, "없는 인물: " + ln.speaker);
                if (!ln.emotion.isEmpty() && !EMOTIONS.contains(ln.emotion)) err("epilogues.csv", e.getKey() + "#" + ln.seq, "모르는 표정: " + ln.emotion);
            }
        }
        for (Npc n : c.npcs.values()) {
            if (!"hazel".equals(n.id) && c.chapterOfNpc(n.id) != null && !c.epilogues.containsKey(n.id)) warn("epilogues.csv", n.id, "후일담이 없습니다");
        }

        // ---- 도형
        for (Map.Entry<String, List<Cmd>> e : c.diagrams.entrySet()) {
            for (String problem : DiagramView.check(e.getValue())) err("diagrams.csv", e.getKey(), problem);
            if (!usedDiagrams.contains(e.getKey())) warn("diagrams.csv", e.getKey(), "어느 수수께끼도 쓰지 않는 도형");
        }

        // ---- 기타 표
        for (Quest q : c.quests) if (!QUEST_TYPES.contains(q.type)) err("quests.csv", q.id, "모르는 의뢰 종류: " + q.type);
        for (Badge b : c.badges) if (!BADGE_STATS.contains(b.stat)) err("badges.csv", b.id, "모르는 통계: " + b.stat);
        if (c.bots.size() < Game.PROMOTE_RANK + Game.DEMOTE_FROM_BOTTOM) warn("league.csv", "", "경쟁자가 너무 적어 승급·강등이 어색합니다");
        checkFinales(c);
    }

    private void duplicates(Path data) {
        String[][] tables = {{"chapters.csv", "id"}, {"lessons.csv", "id"}, {"puzzles.csv", "id"}, {"theorems.csv", "id"},
                {"npcs.csv", "id"}, {"quests.csv", "id"}, {"badges.csv", "id"}, {"misconceptions.csv", "id"}};
        for (String[] t : tables) {
            try {
                Set<String> seen = new HashSet<>();
                for (Map<String, String> r : Csv.readTable(data.resolve(t[0]))) {
                    String id = r.get(t[1]) == null ? "" : r.get(t[1]).trim();
                    if (id.isEmpty()) err(t[0], "", "id가 빈 줄이 있습니다");
                    else if (!seen.add(id)) err(t[0], id, "id가 중복됩니다");
                }
            } catch (Exception e) {
                err(t[0], "", "읽기 실패: " + e);
            }
        }
    }

    /**
     * 모든 표의 각 줄이 머리글과 같은 칸 수인지 본다.
     * 따옴표 없이 쓴 쉼표가 칸을 밀어 내면 뒤쪽 칸(예: puzzles.csv 의 feedback)이 조용히 사라지기 때문이다.
     */
    private void columnCounts(Path data) {
        try (java.util.stream.Stream<Path> files = Files.list(data)) {
            List<Path> csvs = new ArrayList<>();
            files.filter(x -> x.getFileName().toString().endsWith(".csv")).sorted().forEach(csvs::add);
            for (Path file : csvs) {
                String name = file.getFileName().toString();
                try {
                    List<List<String>> rows = Csv.parse(new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8));
                    if (rows.isEmpty()) continue;
                    int cols = rows.get(0).size();
                    for (int i = 1; i < rows.size(); i++) {
                        int n = rows.get(i).size();
                        if (n > cols) err(name, rows.get(i).get(0), "칸이 " + n + "개로 머리글(" + cols + "개)보다 많습니다. 칸 안에 쉼표(,)가 있으면 큰따옴표(\"...\")로 감싸세요");
                    }
                } catch (Exception e) {
                    err(name, "", "읽기 실패: " + e);
                }
            }
        } catch (Exception e) {
            err("data/", "", "표 목록을 읽지 못했습니다: " + e);
        }
    }

    /** puzzles.csv feedback 열의 각 조각이 '값=대사' 모양인지 본다('='가 없으면 읽을 때 조용히 버려진다). */
    private void feedbackSyntax(Path data) {
        try {
            for (Map<String, String> r : Csv.readTable(data.resolve("puzzles.csv"))) {
                String fb = r.get("feedback");
                if (fb == null || fb.trim().isEmpty()) continue;
                for (String part : fb.split("\\|")) {
                    if (part.trim().isEmpty()) continue;
                    if (part.indexOf('=') <= 0) err("puzzles.csv", r.get("id"), "feedback 조각에 '값=대사' 형식의 '='가 없어 무시됩니다: " + part.trim());
                }
            }
        } catch (Exception e) {
            err("puzzles.csv", "", "읽기 실패: " + e);
        }
    }

    private void checkPuzzle(Content c, Puzzle p, Set<String> usedDiagrams) {
        String f = "puzzles.csv", w = p.id;
        if (!c.lessonById.containsKey(p.lessonId)) err(f, w, "없는 사건: " + p.lessonId);
        if (p.diagram == null || p.diagram.trim().isEmpty()) warn(f, w, "도형이 없습니다(칠판이 비어 보입니다)");
        else if (!c.diagrams.containsKey(p.diagram.trim())) err(f, w, "없는 도형: " + p.diagram);
        else usedDiagrams.add(p.diagram.trim());
        if (!TYPES.contains(p.type)) { err(f, w, "모르는 문제 유형: " + p.type + " (number|choice|order|tf|expr)"); return; }
        if (p.hints.isEmpty()) warn(f, w, "힌트가 없습니다");
        for (String st : p.steps) {
            if (st.startsWith("@") && !st.startsWith("@aux ") && !st.startsWith("@ans ")) err(f, w, "풀이 단계 지시어는 '@aux ' 또는 '@ans '만 쓸 수 있습니다: " + st);
        }
        if (Game.solutionSteps(p, false).size() < 2) warn(f, w, "풀이 다시 보기 단계가 너무 짧습니다(해설을 늘리거나 steps 열을 쓰세요)");
        if (p.explanation == null || p.explanation.trim().isEmpty()) warn(f, w, "풀이 설명이 없습니다");
        switch (p.type) {
            case Puzzle.NUMBER:
                try {
                    double v = Double.parseDouble(p.answer);
                    if (v < 0 || v > 9999 || v != Math.floor(v)) err(f, w, "숫자 문제의 정답은 0~9999 정수여야 합니다(키패드 4자리): " + p.answer);
                } catch (NumberFormatException e) {
                    err(f, w, "정답이 숫자가 아닙니다: " + p.answer + " (식이면 type을 expr로)");
                }
                break;
            case Puzzle.CHOICE:
                if (p.choices.size() < 2) err(f, w, "보기가 2개 이상 필요합니다");
                if (p.choices.size() > 4) warn(f, w, "보기가 4개를 넘으면 화면에 다 보이지 않습니다");
                if (!p.choices.contains(p.answer)) err(f, w, "정답이 보기에 없습니다: " + p.answer);
                break;
            case Puzzle.ORDER:
                if (p.choices.size() < 2) err(f, w, "추리 단계가 2개 이상 필요합니다(choices 열에 올바른 순서대로)");
                if (p.choices.size() > 6) warn(f, w, "단계가 6개를 넘으면 화면에 다 보이지 않습니다");
                if (new HashSet<>(p.choices).size() != p.choices.size()) err(f, w, "같은 문구의 단계가 있습니다(순서를 구별할 수 없음)");
                for (String s : p.choices) if (s.length() > 34) warn(f, w, "단계 문구가 길어 잘릴 수 있습니다: " + s);
                break;
            case Puzzle.TF:
                if (!"참".equals(p.tfVerdict()) && !"거짓".equals(p.tfVerdict())) err(f, w, "answer는 '참|이유' 또는 '거짓|이유' 형식이어야 합니다");
                if (!p.choices.contains(p.tfReason())) err(f, w, "올바른 이유가 보기(choices)에 없습니다: " + p.tfReason());
                if (p.choices.size() > 4) warn(f, w, "이유 보기가 4개를 넘으면 화면에 다 보이지 않습니다");
                break;
            case Puzzle.EXPR:
                if (!Expr.isValid(p.answer)) err(f, w, "정답 식을 해석할 수 없습니다: " + p.answer);
                if (p.answer.length() > 18) warn(f, w, "정답 식이 입력 칸(18자)보다 깁니다");
                break;
            default: break;
        }
        // 숫자·식 문제는 도형에 '?' 이름표가 있어야 정답을 맞혔을 때 칠판에 답이 써진다
        if ((p.isNumber() || p.isExpr()) && p.diagram != null && c.diagrams.containsKey(p.diagram.trim())) {
            boolean hasSlot = DiagramView.hasQuestionLabel(c.diagrams.get(p.diagram.trim()));
            if (!hasSlot) warn(f, w, "도형 " + p.diagram + "에 '?' 이름표가 없어 정답이 칠판에 써지지 않습니다");
        }
        for (Feedback fb : p.feedback) {
            if (fb.tag != null && !fb.tag.isEmpty() && !c.misconceptions.containsKey(fb.tag)) warn(f, w, "모르는 실수 유형 #" + fb.tag);
            if (fb.text == null || fb.text.isEmpty()) warn(f, w, "피드백 대사가 비었습니다: " + fb.value);
            String probe = fb.value;
            if (p.isChoice() && !p.choices.contains(probe)) {
                // 값에 '='가 들어 있으면 첫 '='에서 잘려 값이 달라진다(예: "A = B=#태그 말" → 값 "A")
                err(f, w, "피드백 값이 보기에 없어 오답 피드백이 나오지 않습니다: '" + probe + "' (보기에 '='가 들어 있으면 피드백 값으로 쓸 수 없습니다)");
                continue;
            }
            if (p.isTf() && !probe.contains("|")) continue; // 판단만 적은 피드백
            if (p.isExpr() && !Expr.isValid(probe)) { err(f, w, "피드백의 식을 해석할 수 없습니다: " + probe); continue; }
            if (Game.checkAnswer(p, probe)) err(f, w, "피드백 값이 정답과 같습니다: " + probe);
        }
    }

    private void checkFinales(Content c) {
        String f = "finales.csv";
        for (Map.Entry<String, List<FinaleLine>> e : c.finales.entrySet()) {
            Lesson l = c.lessonById.get(e.getKey());
            if (l == null) { err(f, e.getKey(), "없는 사건"); continue; }
            if (!l.boss) warn(f, l.id, "최종 사건(boss=1)이 아니어서 최종 추리가 나오지 않습니다");
            int lies = 0, testimonies = 0;
            for (FinaleLine ln : e.getValue()) {
                String w = l.id + "#" + ln.seq;
                if (!FINALE_KINDS.contains(ln.kind)) err(f, w, "모르는 종류: " + ln.kind + " (intro|claim|lie|reveal)");
                if (ln.isTestimony()) testimonies++;
                if (FinaleLine.LIE.equals(ln.kind)) {
                    lies++;
                    Lesson src = c.lessonOfTheorem(ln.theoremId);
                    if (!c.theorems.containsKey(ln.theoremId)) err(f, w, "없는 정리: " + ln.theoremId);
                    else if (src == null || src.globalIndex > l.globalIndex) err(f, w, "이 시점에는 아직 얻을 수 없는 정리 카드입니다: " + ln.theoremId);
                }
                if (ln.isTestimony() && (ln.speaker == null || ln.speaker.isEmpty())) err(f, w, "증언에는 말하는 사람이 필요합니다");
                else if (!"narration".equals(ln.speaker) && c.npc(ln.speaker) == null) {
                    warn(f, w, "npcs.csv에 없는 인물이라 얼굴 대신 이름 글자로 표시됩니다: " + ln.speaker);
                }
            }
            if (lies == 0) err(f, l.id, "모순되는 증언(lie)이 없습니다");
            if (testimonies < 2) warn(f, l.id, "증언이 2개 이상이어야 고르는 재미가 있습니다");
        }
    }

    // ------------------------------------------------------------------
    public static void main(String[] args) {
        Path home = args.length > 0 ? Paths.get(args[0]) : findHome();
        List<Issue> list = run(home.toAbsolutePath());
        int errors = 0, warns = 0;
        for (Issue i : list) {
            System.out.println(i);
            if (i.level == Level.ERROR) errors++; else warns++;
        }
        System.out.println();
        System.out.println(errors == 0 ? "콘텐츠 검사 통과" : "콘텐츠 검사 실패");
        System.out.println("오류 " + errors + "개, 주의 " + warns + "개  (" + home.toAbsolutePath().resolve("data") + ")");
        System.exit(errors == 0 ? 0 : 1);
    }

    static Path findHome() {
        for (Path c = Paths.get("").toAbsolutePath(); c != null; c = c.getParent()) {
            if (Files.exists(c.resolve("data/chapters.csv"))) return c;
        }
        return Paths.get("").toAbsolutePath();
    }
}
